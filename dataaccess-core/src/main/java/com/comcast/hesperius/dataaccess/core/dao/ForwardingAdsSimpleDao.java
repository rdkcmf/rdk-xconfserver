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
 * Author: pbura
 * Created: 30/01/2014  15:41
 */
package com.comcast.hesperius.dataaccess.core.dao;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hydra.astyanax.data.IPersistable;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ForwardingObject;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Forwarding implementation for {@link IADSSimpleDAO} to simplify decorators creation
 *
 * @param <K>
 * @param <T>
 */
public class ForwardingAdsSimpleDao<K, T extends IPersistable> extends ForwardingObject implements IADSSimpleDAO<K, T> {

    private final IADSSimpleDAO<K, T> delegate;

    public ForwardingAdsSimpleDao(IADSSimpleDAO<K, T> delegate) {
        this.delegate = delegate;
    }

    @Override
    protected IADSSimpleDAO<K, T> delegate() {
        return this.delegate;
    }

    @Override
    public int id() {
        return delegate.id();
    }

    @Override
    public Class getKeyClass() {
        return delegate.getKeyClass();
    }

    @Override
    public Class getValueClass() {
        return delegate.getValueClass();
    }

    @Override
    public Map<K, Optional<T>> getAllAsMap(Set<K> keys) {
        return delegate.getAllAsMap(keys);
    }

    @Override
    public Iterator<T> getIteratedAll() {
        return delegate.getIteratedAll();
    }

    @Override
    public List<T> getPage(K pageStart, Integer pageSize, boolean reversed) {
        return delegate.getPage(pageStart, pageSize, reversed);
    }

    @Override
    public Map<K, T> getRowsAsMap(K from, int size, boolean reversed) {
        return delegate.getRowsAsMap(from, size, reversed);
    }

    @Override
    public Iterable<K> getKeys() {
        return delegate.getKeys();
    }

    @Override
    public T getOne(K rowKey) {
        return delegate.getOne(rowKey);
    }

    @Override
    public T setOne(K rowKey, T entity) throws ValidationException {
        return delegate.setOne(rowKey, entity);
    }

    @Override
    public void setOneAsync(K rowKey, T entity) throws ValidationException {
        delegate.setOneAsync(rowKey, entity);
    }

    @Override
    public void setMultiple(Map<K, T> entities) throws ValidationException {
        delegate.setMultiple(entities);
    }

    @Override
    public void deleteOne(K rowKey) {
        delegate.deleteOne(rowKey);
    }

    @Override
    public List<T> getAll() {
        return delegate.getAll();
    }

    @Override
    public List<T> getAll(Predicate<T> filter) {
        return Lists.newArrayList(Iterables.filter(getAll(), filter));
    }

    @Override
    public List<T> getAllMultiThreaded(int maxResults) {
        return delegate.getAllMultiThreaded(maxResults);
    }

    @Override
    public List<T> getAll(int maxResults) {
        return delegate.getAll(maxResults);
    }

    @Override
    public List<T> getAll(Set<K> keys) {
        return delegate.getAll(keys);
    }

    @Override
    public String getColumnFamilyName() {
        return delegate.getColumnFamilyName();
    }

    @Override
    public void truncateColumnFamily() {
        delegate.truncateColumnFamily();
    }

    @Override
    public IPersistable.Factory<T> getObjectFactory() {
        return delegate.getObjectFactory();
    }
}
