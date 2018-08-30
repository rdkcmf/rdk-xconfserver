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
package com.comcast.hesperius.dataaccess.core.util.bean;

import com.netflix.astyanax.Serializer;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

public class BeanPropertyImpl implements BeanProperty {
    private PropertyDescriptor propertyDescriptor;
    private Serializer serializer;

    public BeanPropertyImpl(PropertyDescriptor propertyDescriptor) {
        propertyDescriptor.getReadMethod().setAccessible(true);
        propertyDescriptor.getWriteMethod().setAccessible(true);
        this.propertyDescriptor = propertyDescriptor;
        this.serializer = BeanUtils.getSerializer(propertyDescriptor.getPropertyType());
    }

    @Override
    public String getName() {
        return propertyDescriptor.getName();
    }

    @Override
    public Class<?> getType() {
        return propertyDescriptor.getPropertyType();
    }

    @Override
    public Object invokeGet(Object obj) {
        try {
            return propertyDescriptor.getReadMethod().invoke(obj);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void invokeSet(Object obj, Object value) {
        try {
            propertyDescriptor.getWriteMethod().invoke(obj, value);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Serializer getSerializer() {
        return serializer;
    }
}
