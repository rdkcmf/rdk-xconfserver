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


import com.comcast.hesperius.dataaccess.core.dao.mapper.ISimpleMapper;
import com.comcast.hesperius.dataaccess.core.dao.util.CFPersistenceDefinition;
import com.comcast.hydra.astyanax.data.IPersistable;
import com.google.common.base.Optional;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ADS specific SimpleDAO interface
 */
public interface IADSSimpleDAO<K, T extends IPersistable> extends ISimpleDAO<K, T> {

    int id();

    Class<K> getKeyClass();

    Class<T> getValueClass();

    Map<K, Optional<T>> getAllAsMap(Set<K> keys);

    Iterator<T> getIteratedAll();

    List<T> getPage(K pageStart, Integer pageSize, boolean reversed);

    Map<K, T> getRowsAsMap(K from, int size, boolean reversed);

    Iterable<K> getKeys();

    public interface Builder<K, T extends IPersistable> {
        Builder setCfDef(CFPersistenceDefinition cfDef);

        Builder<K, T> setKeyType(Class<K> keyType);

        Builder<K, T> setEntityType(Class<T> entityType);

        Builder<K, T> setMapper(ISimpleMapper<String, T> mapper);

        Builder setAvoidSelfBinding(boolean avoidSelfBinding);

        public <K, T extends IPersistable> IADSSimpleDAO<K, T> build();
    }
}
