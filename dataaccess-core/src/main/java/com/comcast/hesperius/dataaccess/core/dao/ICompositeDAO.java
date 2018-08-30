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

import com.comcast.hesperius.dataaccess.core.cache.support.data.ChangedData;
import com.comcast.hesperius.dataaccess.core.rest.query.Filter;
import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hydra.astyanax.data.IPersistable;
import com.netflix.astyanax.model.Composite;

import java.util.List;

/**
 * Defines base interface of the composite data access object
 */
public interface ICompositeDAO<K, T extends IPersistable> extends IBaseDAO<T> {

    T getOne(K rowKey, String id);

    T setOne(K rowKey, T obj) throws ValidationException;

    List<T> setMultiple(K rowKey, List<T> list) throws ValidationException;

    void deleteOne(K rowKey, String id);

    // "throws ValidationException" is used only in TransactionalCompositeDAOProxy, probably will be removed. see APPDS-173
    void deleteAll(K rowKey, List<T> objects) throws ValidationException;

    void deleteAll(K rowKey);

    List<T> getPage(K rowKey, Composite start, int maxResults);

    List<T> getRange(K rowKey, Composite start, Composite end, int maxResults);

    List<T> getAll(K rowKey, List<Filter> filters, int maxResults, String deduplicateField);

    List<T> getAll(K rowKey, List<Filter> filters, int maxResults);

    List<T> getAll(K rowKey, int maxResults);

    List<T> getAll(K rowKey);

    List<T> getAll(K rowKey, List<String> ids);

    List<T> getAll(List<K> rowKeys);

    List<T> getRange(List<K> rowKeys, Composite start, Composite end, int maxResults);

    List<T> getAll(List<K> rowKeys, List<String> ids, List<Filter> filters);

    List<T> getAll(List<K> rowKeys, List<Filter> filters, int maxResults, String deduplicateField);

    /**
     * TODO: testing is required
     */
    List<T> getAll(int maxResults);

    /**
     * TODO: is not supported
     */
    List<T> getAll(List<Filter> filters, int maxResults, String deduplicateField);

    void setFastSliceMode(boolean fastSliceMode);

    boolean isFastSliceMode();

    String generateID(T obj);
}
