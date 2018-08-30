/*
 * If not stated otherwise in this file or this component's Licenses.txt file the
 * following copyright and licenses apply:
 *
 * Copyright 2018 RDK Management
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.comcast.hesperius.dataaccess.core.cache;

import com.comcast.hesperius.data.annotation.ListingCF;
import com.comcast.hesperius.data.annotation.SchemaProvider;
import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.cache.support.dao.ChangedKeysProcessingDAO;
import com.comcast.hesperius.dataaccess.core.cache.support.data.ChangedData;
import com.comcast.hesperius.dataaccess.core.config.ColumnFamilySchemaDefinition;
import com.comcast.hesperius.dataaccess.core.dao.DaoFactory;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.hesperius.dataaccess.core.util.CoreUtil;
import com.google.common.base.Optional;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.*;
import com.netflix.astyanax.serializers.ComparatorType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cache change observer tries to figure out what has changed in cassandra since last check
 * and then just refreshes cache
 */
@SchemaProvider(method = "getSchemas")
public final class CacheConsistencyProvider {
    private static final Logger log = LoggerFactory.getLogger(CacheConsistencyProvider.class);

    private final ExecutorService changeWriter = Executors.newFixedThreadPool(4);
    private long lastRefreshedTimestamp;
    private final ChangedKeysProcessingDAO changedKeysDAO;
    private final long changedKeysTimeWindowSize;
    private final int retryUntilFullReload;
    private int refreshAttemptsLeft;

    public static List<ColumnFamilySchemaDefinition> getSchemas() {
        final List<ColumnFamilySchemaDefinition> schemas = Lists.newArrayList();
        final ColumnFamilySchemaDefinition schemaDefinition = new ColumnFamilySchemaDefinition();
        String projectName = CoreUtil.dsconfig.getDomainClassesBasePackage();
        projectName = projectName.substring(projectName.lastIndexOf(".") + 1);
        schemaDefinition.setColumnFamilyName(
                StringUtils.capitalize(
                        projectName
                                + ChangedData.class.getAnnotation(ListingCF.class).cfName()));
        schemaDefinition.setKeyValidationClass("LongType");
        schemaDefinition.setComparatorType(ComparatorType.TIMEUUIDTYPE.getTypeName());
        schemas.add(schemaDefinition);
        return schemas;
    }

    public CacheConsistencyProvider(long changedKeysTimeWindowSize, int retryUntilFullReload, long delay, long refreshPeriod) {
        this.lastRefreshedTimestamp = DateTime.now(DateTimeZone.UTC).getMillis() + delay;
        this.changedKeysTimeWindowSize = changedKeysTimeWindowSize;
        this.retryUntilFullReload = retryUntilFullReload;
        refreshAttemptsLeft = retryUntilFullReload;
        String projectName = CoreUtil.dsconfig.getDomainClassesBasePackage();
        projectName = projectName.substring(projectName.lastIndexOf(".") + 1);
        changedKeysDAO = new ChangedKeysProcessingDAO(StringUtils.capitalize(projectName), changedKeysTimeWindowSize);
        new Timer().scheduleAtFixedRate(new ReadTask(), delay, refreshPeriod);
    }

    public void writeCacheLog(final String columnFamilyName, final Object key, final ChangedData.Operation operation, final int daoId, final int daoCacheSize) {
        changeWriter.execute(new WriteTask(columnFamilyName, key, operation, daoId, daoCacheSize));
    }

    private final class WriteTask implements Runnable {
        final String cfName;
        final Object key;
        final ChangedData.Operation operation;
        final int daoId;
        final int cacheSize;

        public WriteTask(final String columnFamilyName, final Object key, final ChangedData.Operation operation, final int daoId, final int cacheSize) {
            this.cfName = columnFamilyName;
            this.key = key;
            this.operation = operation;
            this.daoId = daoId;
            this.cacheSize = cacheSize;
        }

        @Override
        public void run() {
            final long now = DateTime.now(DateTimeZone.UTC).getMillis();
            final long rowKey = now - (now % changedKeysTimeWindowSize);
            final ChangedData data = new ChangedData();
            data.setColumnName(generateUuid());
            data.setCfName(cfName);
            data.setChangedKey(CoreUtil.toJSON(key));
            data.setOperation(operation);
            data.setDAOid(daoId);
            data.setValidCacheSize(cacheSize);
            log.debug("writing cache changeLog: " + data.getChangedKey() + " " + operation + " " + daoId);
            try {
                changedKeysDAO.setOne(rowKey, data);
            } catch (ValidationException ex) {
                log.debug("non valid changed data");
                log.info("RetryAttemptsLeft=" + refreshAttemptsLeft);
            }
        }

        private UUID generateUuid() {
            // Generate the new id
            return UUID.fromString(new com.eaio.uuid.UUID().toString());
        }
    }

    private final class ReadTask extends TimerTask {
        /**
         * Loads changed data. Or completely reloads cache if partial load failed retryUntilFullReload times in a row.
         */
        @Override
        public void run() {
            long now = DateTime.now(DateTimeZone.UTC).getMillis();
            log.info("starting cache update for[{} - {}], system time - {}", lastRefreshedTimestamp, now, System.currentTimeMillis());
            if (refreshAttemptsLeft == 0) {     // load all data
                log.info("Attempting full refresh");
                refreshAttemptsLeft++;
                lastRefreshedTimestamp = now;
                CacheManager.refreshAll();
            } else {    // load only changed data
                try {
                    loadChanges(lastRefreshedTimestamp, now);
                    lastRefreshedTimestamp = now;
                    refreshAttemptsLeft = retryUntilFullReload;
                } catch (Exception e) {
                    log.warn("Exception caught while trying to sync cache changes", e);
                    refreshAttemptsLeft--;
                }
            }
        }

        /**
         * Loads changes for given time-window defined by {@param #start} & {@param #end} parameters
         *
         * @param start lower time-window bound, inclusive
         * @param end   upper time-window bound, exclusive
         */
        private void loadChanges(long start, long end) throws Exception {
            final PeekingIterator<ChangedData> changedKeysIterator = Iterators.peekingIterator(
                    changedKeysDAO.getIteratedChangedKeysForTick(start, end));
            MutableInt maybeUpdated = new MutableInt(0);

            while (changedKeysIterator.hasNext()) {
                final ChangedData data = changedKeysIterator.next();
                final ChangedData nextData = changedKeysIterator.hasNext() ? changedKeysIterator.peek() : null;

                if(data.getChangedKey() == null || data.getDAOid() == null
                        || data.getOperation() == null || data.getValidCacheSize() == null
                        || (nextData != null && nextData.getOperation() == null)) {
                    log.warn("Unable to load changed data");
                    continue;
                }

                final boolean updateDetected = nextData != null ?
                        data.getChangedKey().equals(nextData.getChangedKey())
                                && data.getDAOid().equals(nextData.getDAOid())
                                && data.getOperation().equals(ChangedData.Operation.DELETE)
                                && nextData.getOperation().equals(ChangedData.Operation.CREATE)
                        : false;

                if (!CacheManager.hasCacheFor(data.getDAOid())) {
                    log.warn("Unable to locate cache for {} given {} as id (changed key is {})",
                            data.getCfName(), data.getDAOid(), data.getChangedKey());
                    continue;
                }

                List<Class<?>> typeParams;
                try {
                    typeParams = Lists.newArrayList(CacheManager.findTypeParamsByDAOId(data.getDAOid()));
                } catch (Exception e) {
                    log.warn("Unable to get typeParams by daoId: {}", data.getDAOid());
                    throw e;
                }

                LoadingCache cache;
                try {
                    cache = CacheManager.findCacheByDAOId(data.getDAOid());
                } catch(Exception e) {
                    log.warn("Unable to get cache by daoId: {}", data.getDAOid());
                    throw e;
                }

                try {
                    refreshCache(data, typeParams, cache, updateDetected, changedKeysIterator, maybeUpdated);
                } catch(Exception e) {
                    log.warn("Unable to refresh cache for {} CF", data.getCfName());
                    throw e;
                }
            }

            if (maybeUpdated.toInteger() > 0) {
                log.info("Expected to refresh {} entries", maybeUpdated);
            }
        }

        private void refreshCache(ChangedData data, List<Class<?>> typeParams, LoadingCache cache, boolean updateDetected, PeekingIterator<ChangedData> changedKeysIterator, MutableInt maybeUpdated) throws Exception {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Processing ").append(data.getOperation()).append(" for ").append(typeParams.get(0).getSimpleName());
            final String changedKeyString = data.getChangedKey();
            final Object changedKey = CoreUtil.fromJSON(typeParams.get(0), data.getChangedKey());
            stringBuilder.append(" type=").append(typeParams.get(1).getSimpleName()).append(" key=").append(changedKeyString);
            if(updateDetected) {
                log.info("detected UPDATE for "+changedKeyString);
                data.setOperation(ChangedData.Operation.UPDATE);
                changedKeysIterator.next();
            }
            switch (data.getOperation()) {
                case CREATE:
                    cache.refresh(changedKey);   //add to cache
                case UPDATE: {
                    maybeUpdated.increment();
                    cache.refresh(changedKey);  // to guarantee key is not stuck Optional::Absent
                    log.info(stringBuilder.toString());
                    break;
                }
                case DELETE: {
                    maybeUpdated.increment();
                    cache.invalidate(changedKey);    // evict key
                    log.info(stringBuilder.toString());
                    break;
                }
                case TRUNCATE_CF: {
                    cache.invalidateAll();
                    log.info(stringBuilder.toString());
                    break;
                }
            }

            final int cachesize = Iterables.size(Optional.presentInstances(cache.asMap().values()));
            if (cachesize < data.getValidCacheSize()) {
                final ISimpleCachedDAO dao = DaoFactory.Simple.findDaoInCachedDaoCacheById(data.getDAOid());
                if (dao != null) {
                    log.warn("sizes differ for caches, got {} instead of {}, scheduling full refresh for {}", new Object[]{cachesize, data.getValidCacheSize(), dao.getColumnFamilyName()});
                    dao.refreshAll();
                } else {
                    log.error("no dao found for id " + data.getDAOid());
                }
            }
        }
    }
}
