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

import com.comcast.hesperius.dataaccess.core.dao.util.ExecuteWithUncheckedException;
import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.query.ColumnFamilyQuery;

import java.util.Iterator;

/**
 * This class iterates over multiple ranges of columns {@link IColumnRange}.
 * {@link #hasNext()} method performs transition to next column range, so it is required to call {@link #hasNext()} before
 * each invocation of {@link #next()}. {@link #remove()} is not supported.
 *
 * @param <K> row key type
 * @param <N> column name type
 */
public class MultiColumnSliceIterator<K, N> implements Iterator<Column> {
    private final ColumnFamilyQuery<K, N> cfQuery;
    private final int bufferSize;
    private final Iterator<IColumnRange<K, N>> rangeIterator;
    private Iterator<Column<N>> columnIterator;

    /**
     * @param cfQuery      base SliceQuery to execute. rowKey of cfQuery is ignored and columnRanges are used instead
     * @param columnRanges tuples {rowKey, startColumnName, endColumnName}. if startColumnName is null, will
     *                     read from first column of row. if endColumnName is null, will read till last column of row
     * @param bufferSize   number of columns to read from Cassandra per query
     * @throws IllegalArgumentException if columnRanges parameter has no elements
     */
    public MultiColumnSliceIterator(final ColumnFamilyQuery<K, N> cfQuery, final Iterable<IColumnRange<K, N>> columnRanges, final int bufferSize) {
        this.cfQuery = cfQuery;
        this.bufferSize = bufferSize;
        rangeIterator = columnRanges.iterator();
        if (!rangeIterator.hasNext()) {
            throw new IllegalArgumentException("columnRanges should not be empty");
        }
        stepNext();
    }

    @Override
    public boolean hasNext() {
        if (!columnIterator.hasNext() && rangeIterator.hasNext()) {
            stepNext();
        }
        return columnIterator.hasNext();
    }

    @Override
    public Column<N> next() {
        return columnIterator.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    private void stepNext() {
        IColumnRange<K, N> columnRange = rangeIterator.next();
        columnIterator = ExecuteWithUncheckedException.execute(
                cfQuery.getKey(columnRange.getRowKey())
                        .withColumnRange(
                                columnRange.getStartColumnName(),
                                columnRange.getEndColumnName(), false, bufferSize)
                        .autoPaginate(true)
        ).iterator();

    }
}
