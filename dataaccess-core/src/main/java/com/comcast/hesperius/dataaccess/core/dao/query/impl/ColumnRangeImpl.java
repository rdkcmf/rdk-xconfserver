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

public class ColumnRangeImpl<N> implements ColumnRange<N> {

    private N startColumn = null;
    private N endColumn = null;
    private boolean reversed = false;
    private int count = Integer.MAX_VALUE;

    @Override
    public ColumnRange<N> startColumn(N columnName) {
        this.startColumn = columnName;
        return this;
    }

    @Override
    public ColumnRange<N> endColumn(N columnName) {
        this.endColumn = columnName;
        return this;
    }

    @Override
    public ColumnRange<N> reversed() {
        return reversed(true);
    }

    @Override
    public ColumnRange<N> reversed(boolean reversed) {
        this.reversed = reversed;
        return this;
    }

    @Override
    public ColumnRange<N> count(int count) {
        this.count = count;
        return this;
    }

    @Override
    public N getStartColumnName() {
        return startColumn;
    }

    @Override
    public N getEndColumnName() {
        return endColumn;
    }

    @Override
    public boolean isReversed() {
        return reversed;
    }

    @Override
    public int getCount() {
        return count;
    }
}
