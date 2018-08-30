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

import com.comcast.hesperius.dataaccess.core.dao.query.ColumnRange;
import com.comcast.hesperius.dataaccess.core.dao.query.PageQuery;
import com.comcast.hesperius.dataaccess.core.dao.query.RowQuery;
import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.query.ColumnQuery;

import java.util.Collection;

public class RowQueryImpl<N> implements RowQuery<N> {

    private final com.netflix.astyanax.query.RowQuery<?, N> rowQuery;

    public RowQueryImpl(com.netflix.astyanax.query.RowQuery<?, N> rowQuery) {
        this.rowQuery = rowQuery;
    }

    @Override
    public ColumnQuery<N> getColumn(N column) {
        return rowQuery.getColumn(column);
    }

    @Override
    public PageQuery<N> paginated() {
        return new PageQueryImpl<N>(rowQuery);
    }

    @Override
    public RowQuery<N> withColumnSlice(Collection<N> columns) {
        rowQuery.withColumnSlice(columns);
        return this;
    }

    @Override
    public RowQuery<N> withColumnSlice(N... columns) {
        rowQuery.withColumnSlice(columns);
        return this;
    }

    @Override
    public RowQuery<N> withColumnRange(ColumnRange<N> columnRange) {
        return withColumnRange(columnRange.getStartColumnName(), columnRange.getEndColumnName(),
                columnRange.isReversed(), columnRange.getCount());
    }

    @Override
    public RowQuery<N> withColumnRange(N startColumn, N endColumn, boolean reversed, int count) {
        rowQuery.withColumnRange(startColumn, endColumn, reversed, count);
        return this;
    }

    @Override
    public OperationResult<ColumnList<N>> execute() throws ConnectionException {
        return rowQuery.execute();
    }

    @Override
    public ListenableFuture<OperationResult<ColumnList<N>>> executeAsync() throws ConnectionException {
        return rowQuery.executeAsync();
    }
}
