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
package com.comcast.hesperius.dataaccess.core.rest.util;

import com.comcast.hydra.astyanax.data.IPersistable;

import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.serializers.BooleanSerializer;
import com.netflix.astyanax.serializers.DateSerializer;
import com.netflix.astyanax.serializers.DoubleSerializer;
import com.netflix.astyanax.serializers.FloatSerializer;
import com.netflix.astyanax.serializers.IntegerSerializer;
import com.netflix.astyanax.serializers.LongSerializer;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.serializers.UUIDSerializer;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SerializationUtils {
    private static final Map<Class, Serializer> serializerMap;
    private static final String GETTER_PREFIX = "get";
    private static final String SETTER_PREFIX = "set";

    static {
        // new serializers should be added carefully
        // we can't use generics here, thus no compile-time type-checks are performed
        serializerMap = new HashMap<Class, Serializer>();
        serializerMap.put(Boolean.class, BooleanSerializer.get());
        serializerMap.put(String.class, StringSerializer.get());
        serializerMap.put(Float.class, FloatSerializer.get());
        serializerMap.put(Double.class, DoubleSerializer.get());
        serializerMap.put(Integer.class, IntegerSerializer.get());
        serializerMap.put(Long.class, LongSerializer.get());
        serializerMap.put(Date.class, DateSerializer.get());
        serializerMap.put(UUID.class, UUIDSerializer.get());
    }

    public static Method getGetter(Object obj, String field) throws NoSuchMethodException {
        return getMethod(obj, GETTER_PREFIX + StringUtils.capitalize(field));
    }

    public static Object invokeGetter(Object obj, String field) {
        try {
            Method m = getGetter(obj, field);
            m.setAccessible(true);
            return m.invoke(obj);
        }
        catch(NoSuchMethodException nme) { }
        catch(IllegalAccessException iae) { }
        catch(InvocationTargetException ite) { }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static ByteBuffer toByteBuffer(Object value) {
        Serializer serializer = getSerializerForType(value.getClass());
        return (serializer != null) ? serializer.toByteBuffer(value) : null;
    }

    public static Serializer getSerializerForType(Class clazz) {
        return serializerMap.get(clazz);
    }

    public static Method getSetter(Object obj, String field) throws NoSuchMethodException {
        for(Method method : obj.getClass().getDeclaredMethods()) {
            if(getMethodSimpleName(method).equals(SETTER_PREFIX + StringUtils.capitalize(field)))
                return method;
        }
        throw new NoSuchMethodException();
    }

    public static boolean invokeSetter(Object obj, String field, ByteBuffer value) {
        try {
            Method m = getSetter(obj, field);
            m.setAccessible(true);
            Object deserializedValue = getSerializerForType(m.getParameterTypes()[0]).fromByteBuffer(value);
            m.invoke(obj, deserializedValue);
            return true;
        }
        catch(NoSuchMethodException nme) { }
        catch(IllegalAccessException iae) { }
        catch(InvocationTargetException ite) { }

        return false;
    }

    public static String getMethodSimpleName(Method method) {
        return method.getName().substring(method.getName().lastIndexOf(".") + 1);
    }

    public static Method getMethod(Object obj, String name, Class... parameterTypes) throws NoSuchMethodException {
        return obj.getClass().getMethod(name, parameterTypes);
    }


    public static String getterToFieldName(String getterName) {
        return StringUtils.uncapitalize(getterName.substring(GETTER_PREFIX.length()));
    }

    public static List<String> getColumnNames(Object obj) {
        List<String> columnNames = new ArrayList<String>();
        for(Method method : obj.getClass().getDeclaredMethods()) {
            String name = getMethodSimpleName(method);
            if(name.startsWith(GETTER_PREFIX)) {
                columnNames.add(getterToFieldName(name));
            }
        }
        return columnNames;
    }

    public static Class getFieldType(Class clazz, String field) {
        try {
            return clazz.getMethod(GETTER_PREFIX + StringUtils.capitalize(field)).getReturnType();
        } catch(NoSuchMethodException nsme) {
            // caller will handle this because null is returned
        }
        return null;
    }

    /**
     * Sets 'updated' field of the entry uses column's timestamp
     * @param obj database entry
     * @param column owner column
     * @param <T>
     */
    public static <T extends IPersistable> void setUpdatedTime(T obj, Column column) {
        if (column != null) {
            obj.setUpdated(new Date(column.getTimestamp()));
        }
    }
}

