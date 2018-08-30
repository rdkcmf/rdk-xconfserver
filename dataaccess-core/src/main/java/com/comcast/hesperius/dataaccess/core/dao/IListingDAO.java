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
 * Created: 5/15/14
 */
package com.comcast.hesperius.dataaccess.core.dao;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.dao.query.ColumnRange;
import com.comcast.hesperius.dataaccess.core.dao.query.KeysQuery;
import com.comcast.hesperius.dataaccess.core.dao.query.PageQuery;
import com.comcast.hesperius.dataaccess.core.dao.query.Query;
import com.comcast.hesperius.dataaccess.core.dao.query.RowQuery;
import com.comcast.hesperius.dataaccess.core.dao.query.RowSliceQuery;
import com.comcast.hydra.astyanax.data.IPersistable;
import com.google.common.base.Predicate;
import com.netflix.astyanax.query.ColumnQuery;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public interface IListingDAO<K, N, T extends IPersistable> extends IBaseDAO<T> {

    T setOne(K rowKey, T obj) throws ValidationException;

    List<T> setMultiple(K rowKey, List<T> list) throws ValidationException;

    void deleteOne(K rowKey, N name);

    void deleteAll(K rowKey);

    T getOne(K rowKey, N name);

    T getFirst(K rowKey);

    T getLast(K rowKey);

    List<T> getRange(K rowKey, N startName, N endName);

    List<T> getRange(Map<K, ColumnRange<N>> ranges);

    List<T> getAll(K rowKey);

    List<T> getAll(K rowKey, Predicate<T> filter);

    /**
     * Examples:
     * T getOne(K rowKey, N columnName) -&gt; execute(query().getRow(rowKey).getColumn(columnName))
     *
     * T getLast(K rowKey) -&gt; execute(query().getRow(rowKey).withColumnRange(range().limit(1).reversed()))
     *
     * List&lt;T&gt; getAll(K rowKey) -&gt; execute(query().getRow(rowKey))
     *
     * List&lt;T&gt; getRange(K rowKey, N start, N end, int maxColumns) -&gt;
     *     execute(query().getRow(rowKey).withColumnRange(range().startColumn(start).endColumn(end).limit(maxColumns))
     *
     * Iterator&lt;List&lt;T&gt;&gt; getPaginated(K rowKey) -&gt; execute(query().getRow(rowKey).paginated())
     *
     * Iterator&lt;List&lt;T&gt;&gt; getPaginated(K rowKey, N startColumn, N endColumn, int pageSize) -&gt;
     *     execute(query().getRow(rowKey).paginated().from(startColumn).to(endColumn).withPageSize(pageSize))
     *
     * Iterator&lt;K&gt; getKeys() -&gt; execute(query().getKeys())
     */
    Query<K, N> query();

    ColumnRange<N> range();

    T execute(ColumnQuery<N> query);

    List<T> execute(RowQuery<N> query);

    Map<K, List<T>> execute(RowSliceQuery<K, N> query);

    Iterator<List<T>> execute(PageQuery<N> query);

    Iterator<K> execute(KeysQuery<K, N> query);
}
