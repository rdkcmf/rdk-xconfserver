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
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.model.ColumnList;

/**
 * Mapper interface for SimpleDAO.
 *
 * @param <N> column name type
 * @param <T> persistable object type
 */
public interface ISimpleMapper<N, T extends IPersistable> {
    /**
     * Used before writing data to Cassandra. Maps an object to a {@link com.netflix.astyanax.ColumnListMutation}.
     */
    void mapToMutation(T obj, ColumnListMutation<N> mutation);

    /**
     * NOTE: Use case is to pass newly created T obj and it's properties will be initialized during mapping.
     * Check if columnList is empty before mapping to avoid unnecessary work.
     *
     * Method that is used after reading data from Cassandra, the inverse of mapToMutation(T, ColumnListMutation&lt;N&gt;).
     * Converts {@link com.netflix.astyanax.model.ColumnList} into an object.
     */
    T mapFromColumnList(ColumnList<N> columnList, T obj);
}
