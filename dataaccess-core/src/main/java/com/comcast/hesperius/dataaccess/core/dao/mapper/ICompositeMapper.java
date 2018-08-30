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

import com.comcast.hesperius.dataaccess.core.dao.provider.ICompositePropertyProvider;
import com.comcast.hydra.astyanax.data.IPersistable;
import com.google.common.collect.PeekingIterator;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.Composite;

/**
 * Mapper interface for CompositeDAO.
 *
 * @see ReflectionCompositeMapper
 *
 * @param <K> row key type
 * @param <T> persistable object type
 */
public interface ICompositeMapper<K, T extends IPersistable> extends IRowMapper<K, Composite, T> {

    ICompositePropertyProvider<T> getProvider();

    /**
     * @see CompositeMapperHelper
     *
     * This method is not very convenience, so you may want to use {@link CompositeMapperHelper}
     */
    void mapFromColumnIterator(K rowKey, PeekingIterator<Column<Composite>> columnIterator, T obj);

    /**
     * IMPORTANT: type of id is the type it's stored in Cassandra as composite component.
     * May be confusing, because it's saved as String in {@link com.comcast.hydra.astyanax.data.Persistable} bean.
     *
     * Method which is called at the end of mapFromColumnIterator
     */
    void finishObjectInit(T obj, K rowKey, Object id);

    /**
     * Limits the size of object property values set by mapFromColumnIterator(). If 0, there is no limit. Otherwise,
     * values read from Cassandra are to be truncated to the specified limit.
     * This can be used to prevent OutOfMemoryErrors when extremely large data values have gotten into Cassandra.
     */
    public void setPropertyValueByteLimit(int limit);
    public int getPropertyValueByteLimit();
}
