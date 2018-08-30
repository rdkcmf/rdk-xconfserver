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
import com.comcast.hydra.astyanax.data.IPersistable;
import com.google.common.base.Predicate;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ISimpleDAO<K, T extends IPersistable> extends IBaseDAO<T> {

    T getOne(K rowKey);

    T setOne(K rowKey, T entity) throws ValidationException;

    void setOneAsync(K rowKey, T entity) throws ValidationException;

    void setMultiple(Map<K, T> entities) throws ValidationException;

    void deleteOne(K rowKey);

    List<T> getAll();

    List<T> getAll(Predicate<T> filter);

    List<T> getAllMultiThreaded(int maxResults);

    List<T> getAll(int maxResults);

    List<T> getAll(Set<K> keys);
}
