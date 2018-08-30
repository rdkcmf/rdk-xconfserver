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
package com.comcast.hesperius.dataaccess.core.dao.query;

import com.netflix.astyanax.Execution;
import com.netflix.astyanax.model.ColumnList;

public interface PageQuery<N> extends Execution<ColumnList<N>> {

    PageQuery<N> from(N columnName);

    PageQuery<N> to(N columnName);

    PageQuery<N> reversed();

    PageQuery<N> reversed(boolean reversed);

    /**
     * default 300
     */
    PageQuery<N> withPageSize(int pageSize);
}
