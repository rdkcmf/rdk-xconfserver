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

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.dao.mapper.ISimpleMapper;
import com.comcast.hesperius.dataaccess.core.dao.mapper.ReflectionSimpleMapper;
import com.comcast.hesperius.dataaccess.core.dao.util.ExecuteWithUncheckedException;
import com.comcast.hesperius.dataaccess.core.util.EntityValidationUtils;
import com.comcast.hydra.astyanax.DataServiceConstants;
import com.comcast.hydra.astyanax.data.IPersistable;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.AllRowsQuery;
import com.netflix.astyanax.query.ColumnFamilyQuery;
import com.netflix.astyanax.query.RowQuery;
import com.netflix.astyanax.query.RowSliceQuery;
import com.netflix.astyanax.recipes.reader.AllRowsReader;
import com.netflix.astyanax.serializers.StringSerializer;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A data access object for storing data using an ID from the object as the row key, and the field names of the
 * object as each column name.  This is the simplest conceivable data model.  Example:
 * <p>
 * {
 * "ID_1": [
 * "field_1": "value 1",
 * "field_2": "value 2"
 * ],
 * "ID_2": [
 * "field_1": "value 1",
 * "field_2": "value 2"
 * ],
 * ...
 * }
 * </p>
 */
public class SimpleDAO<K, T extends IPersistable> extends BaseDAO<K, String, T> implements ISimpleDAO<K, T> {

    protected ISimpleMapper<String, T> mapper;

    public SimpleDAO(final String columnFamilyName, Serializer<K> keySerializer, Class<T> entityClass) {
        this(columnFamilyName, keySerializer, entityClass, new ReflectionSimpleMapper<T>(entityClass));
    }

    public SimpleDAO(final String columnFamilyName, Serializer<K> keySerializer, IPersistable.Factory<T> factory) {
        this(columnFamilyName, keySerializer, factory, new ReflectionSimpleMapper<T>(factory.getClassObject()));
    }

    public SimpleDAO(final String columnFamilyName, Serializer<K> keySerializer, Class<T> entityClass, ISimpleMapper<String, T> mapper) {
        super(columnFamilyName, keySerializer, StringSerializer.get(), entityClass);
        this.mapper = mapper;
    }

    public SimpleDAO(final String columnFamilyName, Serializer<K> keySerializer, IPersistable.Factory<T> factory, ISimpleMapper<String, T> mapper) {
        super(columnFamilyName, keySerializer, StringSerializer.get(), factory);
        this.mapper = mapper;
    }

    @Override
    public List<T> getAll() {
        return getAll(DataServiceConstants.DEFAULT_MAX_RESULTS);
    }

    @Override
    public List<T> getAll(Predicate<T> filter) {
        return Lists.newArrayList(Iterables.filter(getAll(), filter));
    }

    @Override
    public List<T> getAllMultiThreaded(final int maxResults) {
        final List<T> result = new ArrayList<T>();

        AllRowsReader<K, String> allRowsReader = new AllRowsReader.Builder<K, String>(getKeyspace(), columnFamily)
                .withPartitioner(ExecuteWithUncheckedException.getPartitioner(getKeyspace()))
                .withPageSize(getPageSize())
                .forEachPage(new Function<Rows<K, String>, Boolean>() { // Function.apply() should be thread safe
                    private ReentrantLock lock = new ReentrantLock();

                    @Override
                    public Boolean apply(Rows<K, String> rows) {
                        try {
                            lock.lock();
                            if (result.size() >= maxResults) {
                                return false;
                            }
                        } finally {
                            lock.unlock();
                        }

                        List<T> partialResult = new ArrayList<T>();
                        for (Row<K, String> currRow : rows) {
                            partialResult.add(mapper.mapFromColumnList(currRow.getColumns(), factory.newObject()));
                        }

                        try {
                            lock.lock();
                            result.addAll(partialResult);
                            return (result.size() < maxResults);
                        } finally {
                            lock.unlock();
                        }
                    }
                })
                .build();

        ExecuteWithUncheckedException.execute(allRowsReader);

        return(result.size() <= maxResults) ? result : result.subList(0,maxResults);
    }

    @Override
    public List<T> getAll(final int maxResults) {
        final List<T> entities = new ArrayList<T>();
        AllRowsQuery<K, String> allRowsQuery = getKeyspace().prepareQuery(columnFamily).getAllRows().setRowLimit(getPageSize());
        Rows<K, String> rows = ExecuteWithUncheckedException.execute(allRowsQuery);
        Iterator<Row<K, String>> rowsIterator = rows.iterator();
        while (rowsIterator.hasNext() && entities.size() < maxResults) {
            entities.add(mapper.mapFromColumnList(rowsIterator.next().getColumns(), factory.newObject()));
        }
        return entities;
    }

    /**
     * Performs multi_get_query to Cassandra data store using provided set of row keys
     *
     * @param keys set of row keys
     * @return
     */
    @Override
    public List<T> getAll(Set<K> keys) {
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("Invalid set of keys");
        }

        ColumnFamilyQuery<K, String> columnFamilyQuery = getKeyspace().prepareQuery(columnFamily);
        RowSliceQuery<K, String> query = columnFamilyQuery.getRowSlice((Iterable<K>)keys);
        Rows<K, String> rows = ExecuteWithUncheckedException.execute(query);

        List<T> entities = Lists.newArrayListWithExpectedSize(rows.size());
        for (Row<K, String> row : rows) {
            if (!row.getColumns().isEmpty()) {
                entities.add(mapper.mapFromColumnList(row.getColumns(), factory.newObject()));
            }
        }
        return entities;
    }

    @Override
    public T getOne(K rowKey) {
        RowQuery<K, String> query = getKeyspace().prepareQuery(columnFamily).getKey(rowKey);
        ColumnList<String> columns = ExecuteWithUncheckedException.execute(query);
        if (columns.isEmpty()) {
            return null;
        }
        return mapper.mapFromColumnList(columns, factory.newObject());
    }

    @Override
    public T setOne(K rowKey, T entity) throws ValidationException {
        EntityValidationUtils.validateForSave(entity);

        MutationBatch mutationBatch = getKeyspace().prepareMutationBatch();
        fillMutationBatch(mutationBatch, rowKey, entity);
        ExecuteWithUncheckedException.execute(mutationBatch);
        return entity;
    }

    @Override
    public void setOneAsync(final K rowKey, final T entity) throws ValidationException {
        EntityValidationUtils.validateForSave(entity);

        final MutationBatch mutationBatch = getKeyspace().prepareMutationBatch();
        fillMutationBatch(mutationBatch, rowKey, entity);
        ExecuteWithUncheckedException.executeAsync(mutationBatch);
    }

    @Override
    public void setMultiple(Map<K, T> entities) throws ValidationException {
        MutationBatch mutationBatch = getKeyspace().prepareMutationBatch();
        for (Map.Entry<K, T> entry : entities.entrySet()) {
            EntityValidationUtils.validateForSave(entry.getValue());
            fillMutationBatch(mutationBatch, entry.getKey(), entry.getValue());
        }
        ExecuteWithUncheckedException.execute(mutationBatch);
    }

    @Override
    public void deleteOne(K rowKey) {
        MutationBatch mutationBatch = getKeyspace().prepareMutationBatch();
        mutationBatch.withRow(columnFamily, rowKey).delete();
        ExecuteWithUncheckedException.execute(mutationBatch);
    }

    protected void fillMutationBatch(MutationBatch mutationBatch, K key, T entity) {
        ColumnListMutation<String> columnListMutation = mutationBatch.withRow(columnFamily, key);
        entity.setUpdated(new Date());
        mapper.mapToMutation(entity, columnListMutation);
    }
}
