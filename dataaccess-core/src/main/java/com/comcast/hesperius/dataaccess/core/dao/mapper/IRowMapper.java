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
import com.google.common.collect.PeekingIterator;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.model.Column;

/**
 * User: lhall01c
 * Date: 11/21/13
 */
public interface IRowMapper<K, N, T extends IPersistable> {
    /**
     * Used before writing data to Cassandra. Maps an object to a {@link com.netflix.astyanax.ColumnListMutation}.
     */
    void mapToMutation(T obj, ColumnListMutation<N> mutation);

    /**
     * Used after reading data from Cassandra. Iterates over columns related to a single object and maps
     * them to properties in <i>obj</i>. The state of columnIterator is changed during mapping, and after
     * invocation it will point to the element previous to the first column of the next object. Implementations
     * can assume that columns related to a single object are adjacent to each other in the iterator.
     */
    void mapFromColumnIterator(K rowKey, PeekingIterator<Column<N>> columnIterator, T obj);
}
