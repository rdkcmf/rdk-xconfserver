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

import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.serializers.CompositeSerializer;
import com.netflix.astyanax.serializers.ObjectSerializer;
import com.netflix.astyanax.serializers.SerializerTypeInferer;

/**
 * Class represents column range from single row.
 *
 * @param <K> row key type
 * @param <N> column name type
 */
public class ColumnRange<K, N> implements IColumnRange<K, N> {
    private final K rowKey;
    private final Serializer<K> keySerializer;
    private final N startColumnName;
    private final N endColumnName;
    private final Serializer<N> columnnameSerializer;

    public ColumnRange(final K rowKey, final N startColumnName, final N endColumnName) {
        this(rowKey, (Serializer<K>) SerializerTypeInferer.getSerializer(rowKey), startColumnName, endColumnName, (Serializer<N>) ((SerializerTypeInferer.getSerializer(startColumnName) instanceof ObjectSerializer)? CompositeSerializer.get():SerializerTypeInferer.getSerializer(startColumnName)));
    }

    public ColumnRange(final K rowKey, final Serializer<K> keySerializer, final N startColumnName, final N endColumnName, final Serializer<N> columnNameSerializer) {
        this.rowKey = rowKey;
        this.keySerializer = keySerializer;
        this.startColumnName = startColumnName;
        this.endColumnName = endColumnName;
        this.columnnameSerializer = columnNameSerializer;
    }

    public K getRowKey() {
        return rowKey;
    }

    public N getStartColumnName() {
        return startColumnName;
    }

    public N getEndColumnName() {
        return endColumnName;
    }

    @Override
    public Serializer<K> getKeySerializer() {
        return keySerializer;
    }

    @Override
    public Serializer<N> getColumnNameSerializer() {
        return columnnameSerializer;
    }
}
