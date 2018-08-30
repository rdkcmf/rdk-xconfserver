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
package com.comcast.hesperius.dataaccess.core.dao;

import com.comcast.hesperius.data.annotation.CF;
import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.bindery.BindingFacility;
import com.comcast.hesperius.dataaccess.core.dao.mapper.CommonRowMapper;
import com.comcast.hesperius.dataaccess.core.dao.mapper.ISimpleMapper;
import com.comcast.hesperius.dataaccess.core.dao.util.CFPersistenceDefinition;
import com.comcast.hesperius.dataaccess.core.dao.util.ExecuteWithUncheckedException;
import com.comcast.hesperius.dataaccess.core.util.CoreUtil;
import com.comcast.hesperius.dataaccess.core.util.EntityValidationUtils;
import com.comcast.hesperius.dataaccess.core.util.bean.BeanUtils;
import com.comcast.hydra.astyanax.data.IPersistable;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netflix.astyanax.Execution;
import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.recipes.reader.AllRowsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * base ADS dao
 *
 * @author PBura
 */
public class RowMappingPartialDAO<K, T extends IPersistable> extends SimpleDAO<K, T> implements IADSSimpleDAO<K, T> {

    protected static final Logger log = LoggerFactory.getLogger(RowMappingPartialDAO.class);

    public static final <K, T extends IPersistable> IADSSimpleDAO<K, T> createImpl(Class<T> valueType) {
        return createImpl(CFPersistenceDefinition.fromAnnotation(valueType.getAnnotation(CF.class)), valueType);
    }

    public static final <K, T extends IPersistable> IADSSimpleDAO<K, T> createImpl(final CFPersistenceDefinition cfDef, Class<T> valueType) {
        return new RowMappingPartialDAO(cfDef, cfDef.keyType, BeanUtils.getSerializer(cfDef.keyType), valueType, null, false);
    }

    public static class Builder<K, T extends IPersistable>  implements IADSSimpleDAO.Builder<K, T>{
        private CFPersistenceDefinition cfDef;
        private Class<K> keyType;
        private Class<T> entityType;
        private ISimpleMapper<String, T> mapper;
        private boolean avoidSelfBinding = false;

        public Builder setCfDef(CFPersistenceDefinition cfDef) {
            this.cfDef = cfDef;
            return this;
        }

        public Builder setKeyType(Class<K> keyType) {
            this.keyType = keyType;
            return this;
        }

        public Builder setEntityType(Class<T> entityType) {
            this.entityType = entityType;
            return this;
        }

        public Builder setMapper(ISimpleMapper<String, T> mapper) {
            this.mapper = mapper;
            return this;
        }

        public Builder setAvoidSelfBinding(boolean avoidSelfBinding) {
            this.avoidSelfBinding = avoidSelfBinding;
            return this;
        }

        public RowMappingPartialDAO<K, T> build() {
            Preconditions.checkArgument(entityType != null, "null inapplicable as entityType");
            Preconditions.checkState(cfDef != null || keyType != null, "either cfDef or keyClass must be declared not null");

            return new RowMappingPartialDAO(
                    Optional.fromNullable(cfDef).or(CFPersistenceDefinition.fromAnnotation(entityType.getAnnotation(CF.class))),
                    Optional.fromNullable(keyType).or((Class<K>) (cfDef!=null?cfDef.keyType:keyType)),
                    BeanUtils.getSerializer(Optional.fromNullable(keyType).or((Class<K>) (cfDef!=null?cfDef.keyType:keyType))),
                    entityType,
                    mapper, avoidSelfBinding);
        }
    }


    private RowMappingPartialDAO(final CFPersistenceDefinition cfDef, final Class<K> keyType, final Serializer<K> keySerializer, final Class<T> entityType, final ISimpleMapper<String, T> mapper, final boolean avoidSelfBinding) {
        super(cfDef.cfName, keySerializer, entityType, Optional.fromNullable(mapper).or(new CommonRowMapper(entityType, cfDef)));
        this.valueClass = entityType;
        this.keyClass = keyType;
        this.cfdef = cfDef;
        this.avoidSelfBinding = avoidSelfBinding;
    }

    private final Class<T> valueClass;
    private final Class<K> keyClass;
    private boolean avoidSelfBinding;
    private final CFPersistenceDefinition cfdef;

    protected final CFPersistenceDefinition getCfdef() {
        return cfdef;
    }

    @Override
    public final int id() {
        return getColumnFamilyName().concat(keyClass.getCanonicalName()).concat(valueClass.getCanonicalName()).hashCode();
    }

    @Override
    public final Class<K> getKeyClass() {
        return keyClass;
    }

    @Override
    public final Class<T> getValueClass() {
        return valueClass;
    }

    @Override
    public void truncateColumnFamily() {
        super.truncateColumnFamily();
        //safe since we don't care
    }

    @Override
    public List<T> getAll(Set<K> keys) {
        if (keys == null || keys.isEmpty()) {
            return new ArrayList<T>();
        }
        return super.getAll(keys);
    }

    @Override
    public Map<K, Optional<T>> getAllAsMap(final Set<K> keys) {
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("Invalid set of keys");
        }

        final Map<K, Optional<T>> results = Maps.newHashMap();
        final Rows<K, String> queryResult = ExecuteWithUncheckedException.execute(getKeyspace().prepareQuery(columnFamily).getRowSlice((Iterable<K>) keys));
        for (final Row<K, String> row : queryResult) {
            if (row.getColumns().isEmpty()) {
                results.put(row.getKey(), Optional.<T>absent());
                continue;
            }
            results.put(row.getKey(), Optional.of(mapper.mapFromColumnList(row.getColumns(), factory.newObject())));
        }

        return results;
    }

    /**
     * Please note: this method may return null despite it seems not to.
     * It is due to cassandra tends returning dead(deleted) data for
     * queries so that we ought to deal with it. Iterator will iterate over
     * list of non-dead records and may return null if and only if
     * queryResult contains ghost as the very last item.
     *
     * @return iterator&lt;T&gt; interface to the whole columnFamily managed by
     * this DAO
     */
    @Override
    public Iterator<T> getIteratedAll() {
        try {
            return new Iterator<T>() {
                final Iterator<Row<K, String>> rowiterator = getKeyspace().prepareQuery(columnFamily)
                        .getAllRows()
                        .setIncludeEmptyRows(Boolean.FALSE)
                        .execute()
                        .getResult()
                        .iterator();

                @Override
                public boolean hasNext() {
                    return rowiterator.hasNext();
                }

                @Override
                public T next() {
                    T res = factory.newObject();
                    mapper.mapFromColumnList(rowiterator.next().getColumns(), res);
                    return res;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("remove not supported here");
                }
            };

        } catch (ConnectionException ce) {
            log.error("Exception caught while attempting to scan entire CF for {}", getColumnFamilyName());
            throw new RuntimeException(ce);
        }
    }

    /**
     * Single noncached method for DAO since records order is different for cassandra and cache
     *
     * @param pageStart page starting key
     * @param pageSize  size of page in records
     * @param reversed  ik query should be reversed
     * @return
     */
    @Override
    public List<T> getPage(K pageStart, final Integer pageSize, boolean reversed) {
        return Lists.newArrayList(getRowsAsMap(pageStart, pageSize, reversed).values());
    }

    @Override
    public Map<K, T> getRowsAsMap(final K from, final int size, final boolean reversed) {
        final Map<K, T> result = new LinkedHashMap<K, T>();
        final Execution<Rows<K, String>> query = (from == null) ?
                getKeyspace().prepareQuery(columnFamily).getAllRows().setIncludeEmptyRows(false).setRowLimit(size) :
                getKeyspace().prepareQuery(columnFamily).getKeyRange(from, null, null, null, size);
        final Rows<K, String> rows = ExecuteWithUncheckedException.execute(query);
        final Iterator<Row<K, String>> rowIterator = rows.iterator();
        while (rowIterator.hasNext() && result.size() < size) {
            final Row<K, String> row = rowIterator.next();
            result.put(row.getKey(), mapper.mapFromColumnList(row.getColumns(), factory.newObject()));
        }
        return result;
    }

    @Override
    public List<T> getAll(int maxResults) {
        return getPage(null, maxResults, false);
    }

    @Override
    public T setOne(final K rowKey, final T obj) throws ValidationException {
        EntityValidationUtils.validateForSave(obj);
        if (cfdef.marshalingPolicy == CF.MarshalingPolicy.PER_FIELD) {
            super.deleteOne(rowKey);
        }

        return internalSetOne(rowKey, obj);
    }

    protected final T internalSetOne(final K rowKey, final T obj) throws ValidationException {
        T result = super.setOne(rowKey, obj);
        BindingFacility.entityCreated(rowKey, obj, avoidSelfBinding);
        return result;
    }

    @Override
    public void deleteOne(final K rowKey) {
        final T boundObject = getOne(rowKey);
        if (boundObject == null) {
            log.debug("can not process boundEntityDeleted binding for key:".concat(rowKey.toString()));
            return;
        }
        BindingFacility.entityDeleted(rowKey, boundObject, avoidSelfBinding);
        super.deleteOne(rowKey);
    }

    private static final ExecutorService keyfetcher = Executors.newFixedThreadPool(CoreUtil.getThreadsAvailable(),
            new ThreadFactoryBuilder().setDaemon(true)
                    .setNameFormat("RMPD-keyfetcher-%d")
                    .build());

    @Override
    public Iterable<K> getKeys() {
        try {
            final List<K> res = Lists.newLinkedList();
            final List<K> syncList = Collections.synchronizedList(res);
            new AllRowsReader.Builder<K, String>(getKeyspace(), columnFamily)
                    .withColumnRange(null, null, false, 0)
                    .withExecutor(keyfetcher)
                    .forEachRow(new Function<Row<K, String>, Boolean>() {
                        @Override
                        public Boolean apply(Row<K, String> input) {
                            syncList.add(input.getKey());
                            return true;
                        }
                    })
                    .build()
                    .call();
            return res;
        } catch (Exception e) {
            log.error("{} exception thrown while trying to iterate over keys",getColumnFamilyName(),e);
        }
        return Collections.EMPTY_LIST;
    }
}
