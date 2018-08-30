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
 * Created: 5/16/14
 */
package com.comcast.hesperius.dataaccess.core.dao.query.impl;

import com.comcast.hesperius.dataaccess.core.dao.query.KeysQuery;
import com.comcast.hesperius.dataaccess.core.dao.query.Query;
import com.comcast.hesperius.dataaccess.core.dao.query.RowQuery;
import com.netflix.astyanax.query.AllRowsQuery;
import com.netflix.astyanax.query.ColumnFamilyQuery;

import java.util.Collection;

public class QueryImpl<K, N> implements Query<K, N> {

    private final ColumnFamilyQuery<K, N> columnFamilyQuery;

    public QueryImpl(ColumnFamilyQuery<K, N> columnFamilyQuery) {
        this.columnFamilyQuery = columnFamilyQuery;
    }

    @Override
    public RowQuery<N> getRow(K rowKey) {
        return new RowQueryImpl<N>(columnFamilyQuery.getRow(rowKey));
    }

    @Override
    public KeysQuery<K, N> getKeys() {
        AllRowsQuery<K, N> allRowsQuery = columnFamilyQuery.getAllRows().withColumnRange((N)null, null, false, 0);
        return new KeysQueryImpl<K, N>(allRowsQuery);
    }

    @Override
    public RowSliceQueryImpl<K, N> getRowSlice(K... keys) {
        return new RowSliceQueryImpl<K, N>(columnFamilyQuery.getRowSlice(keys));
    }

    @Override
    public RowSliceQueryImpl<K, N> getRowSlice(Collection<K> keys) {
        return new RowSliceQueryImpl<K, N>(columnFamilyQuery.getRowSlice(keys));
    }

    @Override
    public RowSliceQueryImpl<K, N> getRowSlice(Iterable<K> keys) {
        return new RowSliceQueryImpl<K, N>(columnFamilyQuery.getRowSlice(keys));
    }
}
