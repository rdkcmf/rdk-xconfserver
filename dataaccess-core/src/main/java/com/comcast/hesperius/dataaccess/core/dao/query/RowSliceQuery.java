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
package com.comcast.hesperius.dataaccess.core.dao.query;

import com.netflix.astyanax.Execution;
import com.netflix.astyanax.model.Rows;

import java.util.Collection;

public interface RowSliceQuery<K, N> extends Execution<Rows<K, N>> {
    /**
     * Specify a non-contiguous set of columns to retrieve.
     *
     * @param columns
     * @return
     */
    RowSliceQuery<K, N> withColumnSlice(N... columns);

    /**
     * Specify a non-contiguous set of columns to retrieve.
     *
     * @param columns
     * @return
     */
    RowSliceQuery<K, N> withColumnSlice(Collection<N> columns);

    /**
     * Specify a range of columns to return.
     *
     * @param startColumn
     *            First column in the range
     * @param endColumn
     *            Last column in the range
     * @param reversed
     *            True if the order should be reversed. Note that for reversed,
     *            startColumn should be greater than endColumn.
     * @param count
     *            Maximum number of columns to return (similar to SQL LIMIT)
     * @return
     */
    RowSliceQuery<K, N> withColumnRange(N startColumn, N endColumn, boolean reversed, int count);

    RowSliceQuery<K, N> withColumnRange(ColumnRange<N> columnRange);
}
