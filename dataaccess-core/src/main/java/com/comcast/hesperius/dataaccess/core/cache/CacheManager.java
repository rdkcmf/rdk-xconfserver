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

import com.comcast.hesperius.data.annotation.CF;
import com.comcast.hesperius.data.annotation.NonCached;
import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.cache.mbean.CacheInfo;
import com.comcast.hesperius.dataaccess.core.cache.support.data.ChangedData;
import com.comcast.hesperius.dataaccess.core.config.DataServiceConfiguration;
import com.comcast.hesperius.dataaccess.core.dao.DaoFactory;
import com.comcast.hesperius.dataaccess.core.dao.ForwardingAdsSimpleDao;
import com.comcast.hesperius.dataaccess.core.dao.IADSSimpleDAO;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.hesperius.dataaccess.core.jmx.JMXAgent;
import com.comcast.hesperius.dataaccess.core.util.CoreUtil;
import com.comcast.hydra.astyanax.data.IPersistable;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.text.html.Option;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

/**
 * Performs routine cache-related tasks such as creating loaders and dao-augmentations
 * @author : pbura
 * Date: 14/10/2013
 * Time: 11:59
 */
public final class CacheManager {
    private static final Logger log = LoggerFactory.getLogger(CacheManager.class);

    private static final List<String> ignoreDuringRefresh = Arrays.asList("ApprovedChange", "PendingChange");

    private static ImmutableMap<Integer, LoadingCache> cachemap;
    private static ImmutableMultimap<Integer, Class<?>> daoTypeParams;
    private static Map<String, CacheInfo> cacheMBeans;

    private static final Map<Integer, LoadingCache> dynamicCaches = Maps.newConcurrentMap();
    private static final Map<Integer, List<Class<?>>> dynamicDaoTypeParams = Maps.newConcurrentMap();

    private static final DataServiceConfiguration.CacheConfiguration cacheConfig = CoreUtil.dsconfig.getCacheConfiguration();
    private static final int keySetChunkSize = cacheConfig.getKeysetChunkSizeForMassCacheLoad();


    public static final <K, T extends IPersistable> ISimpleCachedDAO<K, T> augmentWithCache(final IADSSimpleDAO<K, T> targetDAO) {
        Preconditions.checkState(cachemap != null && !cachemap.isEmpty(), "Cache manager not initialized properly, cacheMap not yet built");
        Preconditions.checkState(daoTypeParams != null && !daoTypeParams.isEmpty(), "Cache manager not initialized properly, daoTypeParams not yet built");

        final LoadingCache cache;
        if (hasCacheFor(targetDAO.id())) {
            cache = findCacheByDAOId(targetDAO.id());
        } else {
            cache = createDynamicCache(targetDAO);
        }
        return augmentWithCache(targetDAO, cache);

    }

    private static final <K, T extends IPersistable> ISimpleCachedDAO<K, T> augmentWithCache(final IADSSimpleDAO<K, T> targetDAO, final LoadingCache<K, Optional<T>> cache) {
        Preconditions.checkArgument(targetDAO != null, "Augmentable DAO cannot be null");
        Preconditions.checkArgument(cache != null, "Cannot augment non cached DAO");

        if (targetDAO instanceof ISimpleCachedDAO) {
            return (ISimpleCachedDAO) targetDAO;
        }

        return new CachedSimpleDAO<K, T>(targetDAO, cache);
    }

    private static final <K, T extends IPersistable> CacheLoader<K, Optional<T>> createSimpleCacheLoader(final IADSSimpleDAO<K, T> source, final Class<K> keytype, final Class<T> valueType) {
        return new CacheLoader<K, Optional<T>>() {
            @Override
            public Optional<T> load(K key) throws Exception {
                final T value = source.getOne(key);
                if (value == null) {
                    log.warn("loaded null for ".concat(valueType.getSimpleName()).concat(" ").concat(CoreUtil.toJSON(key)).concat(", rendering value Absent"));
                }
                return Optional.fromNullable(value);
            }

            @Override
            public Map<K, Optional<T>> loadAll(Iterable<? extends K> keys) throws Exception {
                final Map<K, Optional<T>> loaded = Maps.newHashMap();
                final Set<K> reqKeys = Sets.newHashSet(keys);
                final Map<K, Optional<T>> values = source.getAllAsMap(Sets.newHashSet(reqKeys));
                if (values.size() < reqKeys.size()) {
                    for (K key : keys) {
                        loaded.put(key, load(key));
                    }
                    return loaded;
                }
                return values;
            }
        };
    }

    public static synchronized void initCaches(final Iterable<Class<?>> cfDefs) {

        log.info("Initializing caches");
        final String domainName = CacheManager.class.getPackage().getName();
        final ImmutableMultimap.Builder<Integer, Class<?>> builder = new ImmutableMultimap.Builder<Integer, Class<?>>();
        final ImmutableMap.Builder<Integer, LoadingCache> cacheMapBuilder = new ImmutableMap.Builder<Integer, LoadingCache>();
        final ImmutableMap.Builder<String, CacheInfo> cacheMBeansBuilder = new ImmutableMap.Builder<String, CacheInfo>();

        for (Class<?> cfDef : cfDefs) {
            CF cfAnnotation = cfDef.getAnnotation(CF.class);
            final Class<?> keyClass = cfAnnotation.keyType();
            final Class<? extends IPersistable> valueClass = (Class<IPersistable>) cfDef;
            final IADSSimpleDAO targetDao = DaoFactory.Simple.createDAO(keyClass, valueClass);
            builder.putAll(targetDao.id(), keyClass, valueClass);

            if (!cfDef.isAnnotationPresent(NonCached.class)) {   // if cached try to load the cache
                final LoadingCache tCache = createCache(targetDao);
                cacheMapBuilder.put(targetDao.id(), tCache);

                String cfName = cfAnnotation.cfName();
                CacheInfo mbean = new CacheInfo(cfName, tCache);
                JMXAgent.INSTANCE.registerMbean(domainName, cfName, mbean);
                cacheMBeansBuilder.put(cfName, mbean);
            }
        }
        cachemap = cacheMapBuilder.build();
        daoTypeParams = builder.build();
        cacheMBeans = cacheMBeansBuilder.build();

        JMXAgent.INSTANCE.registerMbean(domainName, "AllColumnFamilies", new com.comcast.hesperius.dataaccess.core.cache.mbean.CacheLoader());
    }

    public static Map<String, CacheInfo> getCacheMBeans() {
        return cacheMBeans;
    }

    /**
     * Refresh all caches.
     * @return list with cfNames which were not refreshed
     */
    public static List<String> refreshAll() {
        List<String> list = new ArrayList<>();
        for (Class<?> clazz : CoreUtil.CF_DEFINITIONS) {
            final CF cfDef = clazz.getAnnotation(CF.class);
            if (!ignoreDuringRefresh.contains(cfDef.cfName())) {
                ISimpleCachedDAO dao = DaoFactory.Simple.createCachedDAO(cfDef.keyType(), (Class<IPersistable>) clazz);
                if (!refresh(cfDef.cfName(), dao)) {
                    list.add(cfDef.cfName());
                }
            }
        }
        return list;
    }

    /**
     * Calculate hash for all objects in cf
     * @return long - hash value
     */
    public static long calculateHash(String cfName) {
        if (ignoreDuringRefresh.contains(cfName)) {
            log.warn("Cache doesn't exist for CF: " + cfName);
            return 0;
        }
        ISimpleCachedDAO cachedDao = createCachedDao(cfName);
        CRC32 globalCrc = new CRC32();
        calculateCrcForCache(cachedDao.asLoadingCache(), globalCrc);
        return globalCrc.getValue();
    }

    /**
     * Calculate hash for a specific object in cf
     * @return long - hash value
     */
    public static long calculateHash(String cfName, String itemId) {
        if (ignoreDuringRefresh.contains(cfName)) {
            log.warn("Cache doesn't exist for CF: " + cfName);
            return 0;
        }
        ISimpleCachedDAO cachedDao = createCachedDao(cfName);
        return calculateCrcForItem(cachedDao.asLoadingCache(), itemId);
    }

    private static long calculateCrcForItem(LoadingCache cache, String itemId) {
        Set<Map.Entry> set = cache.asMap().entrySet();
        for (Map.Entry entry : set) {
            Object key = entry.getKey();
            Optional<IPersistable> value = (Optional<IPersistable>) entry.getValue();
            if (value.isPresent() && StringUtils.equals(itemId, key.toString())) {
                CRC32 localCrc = new CRC32();
                return countCrcForObject(value.get(), localCrc);
            }
        }
        return 0;
    }

    /**
     * Calculate hash of all objects in service
     * @return long - hash value
     */
    public static long calculateHash() {
        CRC32 globalCrc = new CRC32();

        //Sorting by class name since CoreUtil.CF_DEFINITIONS returns data in random order
        TreeMap<Integer, LoadingCache> treeMap = new TreeMap<>();
        treeMap.putAll(cachemap);
        treeMap.putAll(dynamicCaches);

        for (Map.Entry<Integer, LoadingCache> entry: treeMap.entrySet()) {
            LoadingCache value = entry.getValue();
            calculateCrcForCache(value, globalCrc);
        }
        return globalCrc.getValue();
    }

    private static void calculateCrcForCache(LoadingCache cache, CRC32 globalCrc) {
        Iterator<IPersistable> it = Optional.presentInstances(cache.asMap().values()).iterator();

        //store crc of each object to sorted map
        TreeMap<Long, Long> crcAndItsCount = new TreeMap<>();

        CRC32 localCrc = new CRC32();
        while(it.hasNext()) {
            IPersistable obj = it.next();
            if (obj != null) {
                Long crc = countCrcForObject(obj, localCrc);
                addObjectCrcToMap(crc, crcAndItsCount);
                localCrc.reset();
            }
        }

        for (Map.Entry<Long, Long> entryCrc : crcAndItsCount.entrySet()) {
            for (int i = 0; i < entryCrc.getValue(); i++) {
                globalCrc.update(entryCrc.getKey().byteValue());
            }
        }
    }

    private static Long countCrcForObject(IPersistable obj, CRC32 localCrc) {
        String json = CoreUtil.toJSON(obj);
        localCrc.update(json.getBytes());
        return localCrc.getValue();
    }

    private static void addObjectCrcToMap(Long crc, TreeMap<Long, Long> crcAndItsCount) {
        Long existingCount = crcAndItsCount.get(crc);
        if (existingCount == null) {
            existingCount = 1L;
        } else {
            existingCount++;
        }
        crcAndItsCount.put(crc, existingCount);
    }


    public static boolean refreshAll(String cfName) {
        return refresh(cfName, createCachedDao(cfName));
    }

    private static boolean refresh(String cfName, ISimpleCachedDAO dao) {
        if (dao != null) {
            long timestamp = System.currentTimeMillis();
            dao.refreshAll();
            long daoRefreshTime = System.currentTimeMillis();

            LoadingCache cache = dao.asLoadingCache();
            int effectiveSetSize = Iterables.size(Optional.presentInstances(cache.asMap().values()));
            log.info("Cache refreshed: {} precached {} entries in {}ms ( {} effective records, {} tombstones )",
                    cfName, cache.size(), daoRefreshTime - timestamp, effectiveSetSize, cache.size() - effectiveSetSize);
            return true;
        } else {
            log.warn("Couldn't refresh cache. Not found DAO for CF: " + cfName);
            return false;
        }
    }

    public static ISimpleCachedDAO createCachedDao(final String cfName) {
        for (Class<?> clazz : CoreUtil.CF_DEFINITIONS) {
            final CF cfDef = clazz.getAnnotation(CF.class);
            if (cfDef.cfName().equals(cfName)) {
                return DaoFactory.Simple.createCachedDAO(cfDef.keyType(), (Class<IPersistable>) clazz);
            }
        }
        return null;
    }

    private static void initiatePrecaching(final IADSSimpleDAO targetDao, final LoadingCache cache) {
        final Class<? extends IPersistable> valueClass = targetDao.getValueClass();
        log.info("Scheduling precaching for " + valueClass.getSimpleName());
        CoreUtil.doAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    final long timestamp = System.currentTimeMillis();
                    for (final Iterable<?> chunk : (Iterable<List>) Iterables.partition(targetDao.getKeys(), keySetChunkSize)) {
                        cache.getAll(chunk);
                    }
                    final int effectiveSetSize = Iterables.size(Optional.presentInstances(cache.asMap().values()));
                    log.info("{} precached {} entries in {}ms ( {} effective records, {} tombstones )",
                            new Object[]{
                                    valueClass.getSimpleName(),
                                    cache.size(),
                                    System.currentTimeMillis() - timestamp,
                                    effectiveSetSize,
                                    cache.size() - effectiveSetSize
                            }
                    );
                } catch (Exception e) {
                    log.info("Precaching failed ", e);
                }
            }
        });
    }

    private static LoadingCache createCache(final IADSSimpleDAO targetDao) {
        return createCache(targetDao, false);
    }

    private static LoadingCache createDynamicCache(final IADSSimpleDAO targetDao) {
        return createCache(targetDao, true);
    }

    private static LoadingCache createCache(final IADSSimpleDAO targetDao, boolean dynamic) {
        final Class<?> keyClass = targetDao.getKeyClass();
        final Class<? extends IPersistable> valueClass = targetDao.getValueClass();
        //  try looking in static caches first
        if (!dynamic && cachemap != null && cachemap.containsKey(targetDao.id())) return cachemap.get(targetDao.id());
            //  then in dynamic
        else if (dynamicCaches != null && dynamicCaches.containsKey(targetDao.id()))
            return dynamicCaches.get(targetDao.id());
        //  create new cache otherwise
        final CacheLoader loader = CacheLoader.asyncReloading(createSimpleCacheLoader(targetDao, keyClass, valueClass), CoreUtil.getAsyncTaskProcessor());
        CacheBuilder cBuilder = CacheBuilder.newBuilder().recordStats();
        if (Boolean.valueOf(cacheConfig.isReloadCacheEntries())) {
            final long refreshTimeout = cacheConfig.getReloadCacheEntriesTimeout();
            final TimeUnit refreshTimeUnit = cacheConfig.getReloadCacheEntriesTimeUnit();
            cBuilder = cBuilder.refreshAfterWrite(refreshTimeout, refreshTimeUnit);
        }


        final LoadingCache cache = cBuilder.build(loader);

        if (dynamic && !dynamicCaches.containsKey(targetDao.id())) {
            dynamicCaches.put(targetDao.id(), cache);
            dynamicDaoTypeParams.put(targetDao.id(), Arrays.asList(keyClass, valueClass));
        }

        initiatePrecaching(targetDao, cache);
        return cache;
    }

    public static boolean hasCacheFor(final int daoId) {
        return dynamicCaches.containsKey(daoId) || cachemap.containsKey(daoId);
    }

    public static boolean hasTypeParamsFor(final int daoId) {
        return dynamicDaoTypeParams.containsKey(daoId) || daoTypeParams.containsKey(daoId);
    }

    public static LoadingCache findCacheByDAOId(final int daoId) {
        return Objects.firstNonNull(cachemap.get(daoId), dynamicCaches.get(daoId));
    }

    public static Collection<Class<?>> findTypeParamsByDAOId(final int daoId) {
        return Objects.firstNonNull(dynamicDaoTypeParams.get(daoId), daoTypeParams.get(daoId).asList());
    }

    private static class CachedSimpleDAO<K, T extends IPersistable> extends ForwardingAdsSimpleDao<K, T> implements ISimpleCachedDAO<K, T> {

        final LoadingCache<K, Optional<T>> ownCache;

        CachedSimpleDAO(final IADSSimpleDAO<K, T> delegate, final LoadingCache<K, Optional<T>> cache) {
            super(delegate);
            this.ownCache = cache;
        }

        @Override
        public LoadingCache<K, Optional<T>> asLoadingCache() throws IllegalStateException {
            return ownCache;
        }

        @Override
        public void refresh(K key) {
            ownCache.refresh(key);
        }

        @Override
        public void refreshAll() {
            Set<K> allKeys = Sets.newHashSet(super.getKeys());
            allKeys.addAll(ownCache.asMap().keySet());
            for (final K key : allKeys) {
                ownCache.refresh(key);
            }
        }

        @Override
        public void invalidateOne(K rowKey) {
            ownCache.invalidate(rowKey);
        }

        @Override
        public void invalidateAll() {
            ownCache.invalidateAll();
        }

        @Override
        public Iterable<K> keys() {
            return ownCache.asMap().keySet();
        }

        @Override
        public void truncateColumnFamily() {
            try {
                super.truncateColumnFamily();
            } finally {
                CoreUtil.cacheConsistencyProvider.writeCacheLog(getColumnFamilyName(), null, ChangedData.Operation.TRUNCATE_CF, id(), 0);
            }
        }

        @Override
        public T getOne(K rowkey) {
            try {
                final T res = ownCache.get(rowkey).orNull();
                return res != null ? CoreUtil.clone(res) : null;
            } catch (ExecutionException e) {
                return super.getOne(rowkey);
            }
        }

        @Override
        public List<T> getAll(Set<K> keys) {
            try {
                final List<T> res = Lists.newArrayList(Optional.presentInstances(ownCache.getAll(keys).values()));
                return CoreUtil.clone(res);
            } catch (ExecutionException e) {
                log.debug("exception while trying to return cached falling back to noncached implementation");
                return super.getAll(keys);
            }
        }

        @Override
        public Iterator<T> getIteratedAll() {
            final Iterator<T> source = Optional.presentInstances(ownCache.asMap().values()).iterator();
            final Iterator<T> res = new Iterator<T>() {

                @Override
                public boolean hasNext() {
                    return source.hasNext();
                }

                @Override
                public T next() {
                    return CoreUtil.clone(source.next());
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("Can not remove via this iterator");
                }
            };
            return res;
        }

        @Override
        public List<T> getPage(K pageStart, Integer pageSize, boolean reversed) {
            return Lists.newArrayList(getRowsAsMap(pageStart, pageSize, reversed).values());
        }

        @Override
        public Map<K, T> getRowsAsMap(K from, int size, boolean reversed) {
            final Map<K, T> result = new LinkedHashMap<K, T>();
            final Map<K, Optional<T>> allEntities = asLoadingCache().asMap();
            Iterator<Map.Entry<K, Optional<T>>> iterator = allEntities.entrySet().iterator();
            boolean count = false;
            if (from == null && iterator.hasNext()) {
                count = true;
            }
            while (iterator.hasNext() && result.size() < size) {
                final Map.Entry<K, Optional<T>> entry = iterator.next();
                final K key = entry.getKey();
                if (!count)
                    if (!key.equals(from))
                        continue;
                    else
                        count = true;
                final Optional<T> value = entry.getValue();
                if (!value.isPresent()) {
                    continue;
                }
                result.put(CoreUtil.clone(key), CoreUtil.clone(value.get()));
            }
            return result;
        }

        @Override
        public List<T> getAll(int maxResults) {
            if (maxResults <= 0) {
                return Lists.newArrayList();
            }
            try {
                List<T> res = Lists.newArrayList(Optional.presentInstances(ownCache.asMap().values()));
                res = (res.size() <= maxResults) ? res : res.subList(0, maxResults);

                return CoreUtil.clone(res);
            } catch (Exception e) {
                log.debug("falling back to non-cached getAll()");
                return getPage(null, maxResults, false);
            }
        }

        @Override
        public T setOne(final K rowKey, final T obj) throws ValidationException {
            boolean successful = true;
            try {
                return delegate().setOne(rowKey, obj);
            } catch (Exception e) {
                successful = false;
                throw e;
            } finally {
                if (successful) {
                    ownCache.put(rowKey, Optional.fromNullable(obj));
                    CoreUtil.cacheConsistencyProvider.writeCacheLog(getColumnFamilyName(), rowKey, ChangedData.Operation.CREATE, super.id(), Iterables.size(Optional.presentInstances(ownCache.asMap().values())));
                }
            }
        }

        @Override
        public void setOneAsync(K rowKey, T obj) throws ValidationException {
            boolean successful = true;
            try {
                delegate().setOneAsync(rowKey, obj);
            } catch (Exception e) {
                successful = false;
                throw e;
            } finally {
                if (successful) {
                    ownCache.put(rowKey, Optional.fromNullable(obj));
                    CoreUtil.cacheConsistencyProvider.writeCacheLog(getColumnFamilyName(), rowKey, ChangedData.Operation.CREATE, super.id(), Iterables.size(Optional.presentInstances(ownCache.asMap().values())));
                }
            }
        }

        @Override
        public void setMultiple(Map<K, T> entities) throws ValidationException {
            boolean successful = true;
            try {
                super.setMultiple(entities);
            } catch (Exception e) {
                successful = false;
                throw e;
            } finally {
                if (successful) {
                    int cacheSize = Iterables.size(Optional.presentInstances(ownCache.asMap().values()));
                    for (final Map.Entry<K, T> entry : entities.entrySet()) {
                        ownCache.put(entry.getKey(), Optional.fromNullable(entry.getValue()));
                        CoreUtil.cacheConsistencyProvider.writeCacheLog(getColumnFamilyName(), entry.getKey(), ChangedData.Operation.CREATE, super.id(), cacheSize++);
                    }
                }
            }
        }

        @Override
        public void deleteOne(K rowKey) {
            boolean successful = true;
            try {
                super.deleteOne(rowKey);
            } catch (Exception e) {
                successful = false;
                throw new RuntimeException(e);
            } finally {
                if (successful) {
                    ownCache.invalidate(rowKey);
                    CoreUtil.cacheConsistencyProvider.writeCacheLog(getColumnFamilyName(), rowKey, ChangedData.Operation.DELETE, super.id(), Iterables.size(Optional.presentInstances(ownCache.asMap().values())));
                }
            }
        }

        @Override
        public List<T> getAll() {
            return CoreUtil.clone(Lists.newArrayList(Optional.presentInstances(ownCache.asMap().values())));
        }

        @Override
        public Map<K, Optional<T>> getAllAsMap(Set<K> keys) {
            try {
                final Map<K, Optional<T>> filtered = Maps.newHashMap(Maps.filterValues(ownCache.getAll(keys), new Predicate<Optional<T>>() {
                    @Override
                    public boolean apply(final Optional<T> input) {
                        return input.isPresent();
                    }
                }));
                return CoreUtil.clone(filtered);
            } catch (ExecutionException e) {
                log.error("could not get values as map", e);
                throw new RuntimeException(e);
            }
        }

        @Override
        public T getOne(K rowKey, boolean clone) {
            try {
                final T res = ownCache.get(rowKey).orNull();
                return res != null ? (clone ? CoreUtil.clone(res) : res) : null;
            } catch (ExecutionException e) {
                return super.getOne(rowKey);
            }
        }
    }
}
