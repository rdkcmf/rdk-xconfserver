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

/**
 * Copyright 2011 Netflix
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 /*
 * Author: slavrenyuk
 * Created: 5/16/14
 */
package com.comcast.hesperius.dataaccess.core.dao.query.impl;

import com.comcast.hesperius.dataaccess.core.dao.query.ColumnRange;
import com.comcast.hesperius.dataaccess.core.dao.query.RowSliceQuery;
import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Rows;

import java.util.Collection;

public class RowSliceQueryImpl<K, N> implements RowSliceQuery<K, N> {

    private final com.netflix.astyanax.query.RowSliceQuery<K, N> rowSliceQuery;

    public RowSliceQueryImpl(com.netflix.astyanax.query.RowSliceQuery<K, N> rowSliceQuery) {
        this.rowSliceQuery = rowSliceQuery;
    }

    @Override
    public RowSliceQuery<K, N> withColumnSlice(N... columns) {
        rowSliceQuery.withColumnSlice(columns);
        return this;
    }

    @Override
    public RowSliceQuery<K, N> withColumnSlice(Collection<N> columns) {
        rowSliceQuery.withColumnSlice(columns);
        return this;
    }

    @Override
    public RowSliceQuery<K, N> withColumnRange(ColumnRange<N> columnRange) {
        return withColumnRange(columnRange.getStartColumnName(), columnRange.getEndColumnName(),
                columnRange.isReversed(), columnRange.getCount());
    }

    @Override
    public RowSliceQuery<K, N> withColumnRange(N startColumn, N endColumn, boolean reversed, int count) {
        rowSliceQuery.withColumnRange(startColumn, endColumn, reversed, count);
        return this;
    }

    @Override
    public OperationResult<Rows<K, N>> execute() throws ConnectionException {
        return rowSliceQuery.execute();
    }

    @Override
    public ListenableFuture<OperationResult<Rows<K, N>>> executeAsync() throws ConnectionException {
        return rowSliceQuery.executeAsync();
    }
}
