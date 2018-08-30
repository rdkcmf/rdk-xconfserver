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

import com.comcast.hesperius.dataaccess.core.dao.provider.ICompositePropertyProvider;
import com.comcast.hesperius.dataaccess.core.dao.util.DataUtils;
import com.comcast.hydra.astyanax.data.IPersistable;
import com.google.common.collect.PeekingIterator;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.Composite;
import com.netflix.astyanax.serializers.SerializerTypeInferer;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ReflectionCompositeMapper<K, T extends IPersistable> implements ICompositeMapper<K, T> {
    private int propertyValueByteLimit = 0; // 0 = no limit
    protected ICompositePropertyProvider<T> provider;

    public ReflectionCompositeMapper(ICompositePropertyProvider<T> propertyProvider) {
        this.provider = propertyProvider;
    }

    @Override
    public ICompositePropertyProvider<T> getProvider() {
        return provider;
    }

    @Override
    public void mapToMutation(T obj, ColumnListMutation<Composite> mutation) {
        int colCount = 0;
        for(String propName: provider.getColumnNames()) {
            Object value = DataUtils.invokeGetter(obj, propName);
            if (value == null) {
                continue;
            }

            Composite columnName = provider.createCompositeForObject(obj, propName);
            if(columnName != null) {
                ByteBuffer serializedValue = SerializerTypeInferer.getSerializer(value).toByteBuffer(value);
                mutation.putColumn(columnName, serializedValue, obj.getTTL(propName));
                colCount++;
            }
        }

        obj.clearTTL();

        if (colCount < provider.getColumnNumberTobeSaved()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                String errorMessage = mapper.writeValueAsString(obj);
                throw new IllegalArgumentException(String.format("Unable to save object due to some required columns were not persisted, object: %s", errorMessage));
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to serialize object: " + obj);
            }
        }

    }

    @Override
    public void mapFromColumnIterator(K rowKey, PeekingIterator<Column<Composite>> columnIterator, T obj) {
        final Object id = provider.getIdComponent(columnIterator.peek().getName());

        while (columnIterator.hasNext()) {
            Column<Composite> currColumn = columnIterator.peek();
            if (!id.equals(provider.getIdComponent(currColumn.getName()))) {
                break; // we reach next object`s column
            }
            initObjectProperty(obj, currColumn);
            columnIterator.next();
        }

        finishObjectInit(obj, rowKey, id);
    }

    public void initObjectProperty(T obj, Column<Composite> column) {
        String fieldName = getProvider().getPropertyComponent(column.getName());
        if (fieldName != null) {
            DataUtils.invokeSetter(obj, fieldName,
                    DataUtils.truncate(column.getByteBufferValue(), propertyValueByteLimit));
            if (column.getTtl() != 0) {
                obj.setTTL(fieldName, column.getTtl());
            }
        }
    }

    @Override
    public void finishObjectInit(T obj, K rowKey, Object id) {
        if (obj.getId() == null) {
            obj.setId(id.toString());
        }
    }

    @Override
    public int getPropertyValueByteLimit() {
        return propertyValueByteLimit;
    }

    @Override
    public void setPropertyValueByteLimit(int propertyValueByteLimit) {
        this.propertyValueByteLimit = propertyValueByteLimit;
    }
}
