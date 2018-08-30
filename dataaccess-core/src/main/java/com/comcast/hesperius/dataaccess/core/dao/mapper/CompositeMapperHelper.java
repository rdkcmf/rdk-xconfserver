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
package com.comcast.hesperius.dataaccess.core.dao.mapper;

import com.comcast.hydra.astyanax.data.IPersistable;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Composite;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class - helper which requires ICompositeMapper and IPersistable.Factory; provides more convenient methods for mapping
 * rows to persistable objects.
 *
 * @param <K> row key type
 * @param <T> persistable object type
 */
public class CompositeMapperHelper<K, T extends IPersistable> {
    private ICompositeMapper<K, T> mapper;
    private IPersistable.Factory<T> factory;

    public CompositeMapperHelper(ICompositeMapper<K, T> mapper, IPersistable.Factory<T> factory) {
        this.mapper = mapper;
        this.factory = factory;
    }

    /**
     * Method that is used after reading from Cassandra. Maps first object found in the columnList param.
     * Use this method if columnList contains only one object.
     *
     * @return mapped object if columnList is not empty, null otherwise
     */
    public T mapToObject(K rowKey, ColumnList<Composite> columnList) {
        if (columnList.isEmpty()) {
            return null;
        }

        T obj = factory.newObject();
        mapper.mapFromColumnIterator(rowKey, peekingIterator(columnList), obj);
        return obj;
    }

    /**
     * Method that is used after reading from Cassandra. Maps all object found in the columnList param.
     *
     * @return mapped objects if columnList is not empty, empty list otherwise
     */
    public List<T> mapToObjectList(K rowKey, ColumnList<Composite> columnList) {
        List<T> result = new ArrayList<T>();
        PeekingIterator<Column<Composite>> columnIterator = peekingIterator(columnList);
        while (columnIterator.hasNext()) {
            T obj = factory.newObject();
            mapper.mapFromColumnIterator(rowKey, columnIterator, obj);
            result.add(obj);
        }
        return result;
    }

    /**
     * Method that is used after reading from Cassandra. Maps all object found in the columnList param.
     * Lazily maps objects during iteration.
     *
     * @return iterator over mapped objects; if columnList is empty, iterator won't have elements
     */
    public Iterator<T> mapToObjectsIterator(K rowKey, ColumnList<Composite> columnList) {
        return new IteratorImpl(rowKey, columnList);
    }

    protected PeekingIterator<Column<Composite>> peekingIterator(ColumnList<Composite> columnList) {
        Iterator<Column<Composite>> baseIterator = columnList.iterator();
        return Iterators.peekingIterator(baseIterator);
    }

    private class IteratorImpl implements Iterator<T> {
        private K rowKey;
        private PeekingIterator<Column<Composite>> columnIterator;

        private IteratorImpl(K rowKey, ColumnList<Composite> columnList) {
            this.rowKey = rowKey;
            this.columnIterator = peekingIterator(columnList);
        }

        @Override
        public boolean hasNext() {
            return columnIterator.hasNext();
        }

        @Override
        public T next() {
            T obj = factory.newObject();
            mapper.mapFromColumnIterator(rowKey, columnIterator, obj);
            return obj;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
