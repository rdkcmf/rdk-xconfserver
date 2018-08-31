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

import com.comcast.hesperius.dataaccess.core.util.bean.BeanProperty;
import com.comcast.hesperius.dataaccess.core.util.bean.BeanUtils;
import com.comcast.hydra.astyanax.data.IPersistable;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnList;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Simple mapper save obj with ttl
 * @param <T> extends IPersistable persistable object type
 */
public class ReflectionSimpleMapper<T extends IPersistable> implements ISimpleMapper<String, T> {
    protected Map<String, BeanProperty> beanPropertyMap;

    public ReflectionSimpleMapper(Class<T> clazz) {
        beanPropertyMap = BeanUtils.getOrCreateBeanPropertyMap(clazz);
    }

    @Override
    public void mapToMutation(T obj, ColumnListMutation<String> mutation) {
        for (BeanProperty beanProperty : beanPropertyMap.values()) {
            Object propertyValue = beanProperty.invokeGet(obj);
            if (propertyValue == null) {
                continue;
            }
            ByteBuffer serializedValue = beanProperty.getSerializer().toByteBuffer(propertyValue);

            String columnName = beanProperty.getName();
            mutation.putColumn(columnName, serializedValue, obj.getTTL(columnName));
        }
    }

    @Override
    public T mapFromColumnList(ColumnList<String> columnList, T obj) {
        for (Column<String> column : columnList) {
            BeanProperty beanProperty = beanPropertyMap.get(column.getName());
            if (beanProperty == null) {
                continue;
            }
            Object value = columnList.getValue(column.getName(), beanProperty.getSerializer(), null);
            beanProperty.invokeSet(obj, value);
            if (column.getTtl() != 0) {
                obj.setTTL(column.getName(), column.getTtl());
            }
        }
        return obj;
    }
}