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

import com.comcast.hesperius.dataaccess.core.dao.query.PageQuery;
import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.query.RowQuery;

public class PageQueryImpl<N> implements PageQuery<N> {

    private final com.netflix.astyanax.query.RowQuery<?, N> rowQuery;
    private N startColumn = null;
    private N endColumn = null;
    private boolean reversed = false;
    private int pageSize = 300;

    public PageQueryImpl(RowQuery<?, N> rowQuery) {
        this.rowQuery = rowQuery;
    }

    @Override
    public PageQuery<N> from(N columnName) {
        this.startColumn = columnName;
        return this;
    }

    @Override
    public PageQuery<N> to(N columnName) {
        this.endColumn = columnName;
        return this;
    }

    @Override
    public PageQuery<N> reversed() {
        return reversed(true);
    }

    @Override
    public PageQuery<N> reversed(boolean reversed) {
        this.reversed = reversed;
        return this;
    }

    @Override
    public PageQuery<N> withPageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    @Override
    public OperationResult<ColumnList<N>> execute() throws ConnectionException {
        return prepareQuery().execute();
    }

    @Override
    public ListenableFuture<OperationResult<ColumnList<N>>> executeAsync() throws ConnectionException {
        return prepareQuery().executeAsync();
    }

    private RowQuery<?, N> prepareQuery() {
        return rowQuery.withColumnRange(startColumn, endColumn, reversed, pageSize).autoPaginate(true);
    }
}
