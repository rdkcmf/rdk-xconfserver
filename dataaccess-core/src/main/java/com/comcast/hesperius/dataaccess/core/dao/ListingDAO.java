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
 *
 * Author: slavrenyuk
 * Created: 5/15/14
 */
package com.comcast.hesperius.dataaccess.core.dao;

import com.comcast.hesperius.dataaccess.core.AstyanaxException;
import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.dao.query.ColumnRange;
import com.comcast.hesperius.dataaccess.core.dao.query.KeysQuery;
import com.comcast.hesperius.dataaccess.core.dao.query.PageQuery;
import com.comcast.hesperius.dataaccess.core.dao.query.Query;
import com.comcast.hesperius.dataaccess.core.dao.query.RowQuery;
import com.comcast.hesperius.dataaccess.core.dao.query.RowSliceQuery;
import com.comcast.hesperius.dataaccess.core.dao.query.impl.ColumnRangeImpl;
import com.comcast.hesperius.dataaccess.core.dao.query.impl.QueryImpl;
import com.comcast.hesperius.dataaccess.core.dao.util.ExecuteWithUncheckedException;
import com.comcast.hesperius.dataaccess.core.util.EntityValidationUtils;
import com.comcast.hesperius.dataaccess.core.util.bean.BeanProperty;
import com.comcast.hydra.astyanax.data.IPersistable;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.NotFoundException;
import com.netflix.astyanax.model.*;
import com.netflix.astyanax.query.ColumnQuery;

import java.nio.ByteBuffer;
import java.util.*;

public class ListingDAO<K, N, T extends IPersistable> extends BaseDAO<K, N, T> implements IListingDAO<K, N, T> {

    protected final Serializer<T> valueSerializer;
    protected final BeanProperty<T, N> columnNameProperty;
    protected final int ttl;

    public ListingDAO(String columnFamilyName, Serializer<K> keySerializer, Serializer<T> valueSerializer,
                      BeanProperty<T, N> columnNameProperty, IPersistable.Factory<T> factory, int ttl) {
        super(columnFamilyName, keySerializer, columnNameProperty.getSerializer(), factory);
        this.columnNameProperty = columnNameProperty;
        this.valueSerializer = valueSerializer;
        this.ttl = ttl;
    }

    @Override
    public T setOne(K rowKey, T obj) throws ValidationException {
        MutationBatch mutationBatch = getKeyspace().prepareMutationBatch();
        fillColumnListMutation(mutationBatch.withRow(columnFamily, rowKey), obj);
        ExecuteWithUncheckedException.execute(mutationBatch);
        return obj;
    }

    @Override
    public List<T> setMultiple(K rowKey, List<T> list) throws ValidationException {
        MutationBatch mutationBatch = getKeyspace().prepareMutationBatch();
        ColumnListMutation<N> columnListMutation = mutationBatch.withRow(columnFamily, rowKey);
        for (T obj : list) {
            fillColumnListMutation(columnListMutation, obj);
        }
        ExecuteWithUncheckedException.execute(mutationBatch);
        return list;
    }

    @Override
    public void deleteOne(K rowKey, N name) {
        getKeyspace().prepareMutationBatch().withRow(columnFamily, rowKey).deleteColumn(name);
    }

    @Override
    public void deleteAll(K rowKey) {
        getKeyspace().prepareMutationBatch().withRow(columnFamily, rowKey).delete();
    }

    @Override
    public T getOne(K rowKey, N name) {
        return execute(query().getRow(rowKey).getColumn(name));
    }

    @Override
    public T getFirst(K rowKey) {
        List<T> list = execute(query().getRow(rowKey).withColumnRange(range().count(1)));
        if (!list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    @Override
    public T getLast(K rowKey) {
        List<T> list = execute(query().getRow(rowKey).withColumnRange(range().reversed().count(1)));
        if (!list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    @Override
    public List<T> getRange(K rowKey, N startName, N endName) {
        return execute(query().getRow(rowKey).withColumnRange(range().startColumn(startName).endColumn(endName)));
    }

    @Override
    public List<T> getRange(Map<K, ColumnRange<N>> ranges) {
        List<T> result = new ArrayList<>();
        for (Map.Entry<K, ColumnRange<N>> entry : ranges.entrySet()) {
            result.addAll(execute(query().getRow(entry.getKey()).withColumnRange(entry.getValue())));
        }
        return result;
    }

    @Override
    public List<T> getAll(K rowKey) {
        return execute(query().getRow(rowKey));
    }

    @Override
    public List<T> getAll(K rowKey, Predicate<T> filter) {
        return Lists.newArrayList(Iterables.filter(getAll(rowKey), filter));
    }

    public Query<K, N> query() {
        return new QueryImpl<K, N>(getKeyspace().prepareQuery(columnFamily));
    }


    public ColumnRange<N> range() {
        return new ColumnRangeImpl<N>();
    }

    public T execute(ColumnQuery<N> query) {
        try {
            return query.execute().getResult().getValue(valueSerializer);
        } catch (NotFoundException e) {
            return null;
        }  catch (ConnectionException e) {
            throw new AstyanaxException(e);
        }
    }

    public List<T> execute(RowQuery<N> query) {
        ColumnList<N> columnList = ExecuteWithUncheckedException.execute(query);
        return fromColumnList(columnList);
    }

    public Map<K, List<T>> execute(RowSliceQuery<K, N> query) {
        Rows<K, N> rows = ExecuteWithUncheckedException.execute(query);
        Map<K, List<T>> result = new LinkedHashMap<K, List<T>>();
        for (Row<K, N> row : rows) {
            List<T> list = fromColumnList(row.getColumns());
            result.put(row.getKey(), list);
        }
        return result;
    }

    @Override
    public Iterator<List<T>> execute(PageQuery<N> query) {
        return Iterators.transform(new PageIterator<N>(query), new Function<ColumnList<N>, List<T>>() {
            @Override
            public List<T> apply(ColumnList<N> columnList) {
                return fromColumnList(columnList);
            }
        });
    }

    @Override
    public Iterator<K> execute(KeysQuery<K, N> query) {
        Iterator<Row<K, N>> rowIterator = ExecuteWithUncheckedException.execute(query).iterator();
        return Iterators.transform(rowIterator, new Function<Row<K, N>, K>() {
            @Override
            public K apply(Row<K, N> row) {
                return row.getKey();
            }
        });
    }

    protected void fillColumnListMutation(ColumnListMutation<N> columnListMutation, T obj) throws ValidationException {
        EntityValidationUtils.validateForSave(obj);
        N columnName = columnNameProperty.invokeGet(obj);
        ByteBuffer columnValue = valueSerializer.toByteBuffer(obj);
        columnListMutation.putColumn(columnName, columnValue, ttl);
    }

    protected List<T> fromColumnList(ColumnList<N> columnList) {
        List<T> result = new ArrayList<T>();
        for (Column<N> column : columnList) {
            T obj = valueSerializer.fromByteBuffer(column.getByteBufferValue());
            result.add(obj);
        }
        return result;
    }

    private static class PageIterator<N> implements Iterator<ColumnList<N>> {
        private final PageQuery<N> pageQuery;
        private ColumnList<N> nextValue;

        public PageIterator(PageQuery<N> pageQuery) {
            this.pageQuery = pageQuery;
            this.nextValue = ExecuteWithUncheckedException.execute(pageQuery);
        }

        @Override
        public boolean hasNext() {
            return !nextValue.isEmpty();
        }

        @Override
        public ColumnList<N> next() {
            if(!hasNext()) {
                throw new NoSuchElementException();
            }
            ColumnList<N> result = nextValue;
            nextValue = ExecuteWithUncheckedException.execute(pageQuery);
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
