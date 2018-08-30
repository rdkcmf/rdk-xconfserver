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
import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.AllRowsQuery;

public class KeysQueryImpl<K, N> implements KeysQuery<K, N> {

    private final AllRowsQuery<K, N> allRowsQuery;

    public KeysQueryImpl(AllRowsQuery<K, N> allRowsQuery) {
        this.allRowsQuery = allRowsQuery;
    }

    @Override
    public KeysQuery<K, N> withPageSize(int pageSize) {
        allRowsQuery.setRowLimit(pageSize);
        return this;
    }

    @Override
    public OperationResult<Rows<K, N>> execute() throws ConnectionException {
        return allRowsQuery.execute();
    }

    @Override
    public ListenableFuture<OperationResult<Rows<K, N>>> executeAsync() throws ConnectionException {
        return allRowsQuery.executeAsync();
    }
}
