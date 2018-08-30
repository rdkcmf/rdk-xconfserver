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
package com.comcast.hesperius.dataaccess.core.util.bean;

import com.comcast.hesperius.dataaccess.core.dao.util.JsonSerializer;
import com.comcast.hesperius.dataaccess.core.util.store.SimpleStore;
import com.comcast.hydra.astyanax.data.HColumn;
import com.google.common.base.Preconditions;
import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.serializers.ObjectSerializer;
import com.netflix.astyanax.serializers.SerializerTypeInferer;
import org.apache.commons.beanutils.PropertyUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class BeanUtils {
    private static BeanPropertyMapStore beanPropertyMapStore = new BeanPropertyMapStore();
    private static BeanPropertyStore beanPropertyStore = new BeanPropertyStore();

    /**
     * Infers serialized from Type if primitive or wrapper, or one of: Date, UUID, ByteArray
     * if could not infer returns JsonSerializer on subject class given
     *
     * @param clazz type to infer serializer from
     * @param <T>   GenericType for Serializer produced
     * @return
     */
    public static <T> Serializer<T> getSerializer(Class<T> clazz) {
        final Serializer<T> serializer = SerializerTypeInferer.getSerializer(clazz);
        return !(serializer instanceof ObjectSerializer) ? serializer : new JsonSerializer<T>(clazz);
    }

    public static Map<String, BeanProperty> getOrCreateBeanPropertyMap(Class<?> clazz) {
        return beanPropertyMapStore.getOrCreate(clazz);
    }

    @SuppressWarnings("unchecked")
    public static <T, V> BeanProperty<T, V> getOrCreateBeanProperty(Class<?> clazz, String propertyName) {
        return beanPropertyStore.getOrCreate(new BeanPropertyId(clazz, propertyName));
    }

    private static class BeanPropertyMapStore extends SimpleStore<Class<?>, Map<String, BeanProperty>> {
        @Override
        protected Map<String, BeanProperty> createValue(Class<?> arg) {
            Map<String, BeanProperty> result = new HashMap<String, BeanProperty>();
            PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors(arg);
            for (PropertyDescriptor propertyDescriptor : descriptors) {
                if (shouldBeMapped(arg, propertyDescriptor)) {
                    result.put(propertyDescriptor.getName(), new BeanPropertyImpl(propertyDescriptor));
                }
            }
            return result;
        }

        /**
         * @param propertyDescriptor object property representation
         * @return true if object property should be mapped to cassandra and vice versa, false otherwise
         */
        private boolean shouldBeMapped(Class<?> clazz, PropertyDescriptor propertyDescriptor) {
            Field field = getField(clazz, propertyDescriptor.getName());
            Method getter = propertyDescriptor.getReadMethod();
            Method setter = propertyDescriptor.getWriteMethod();
            return (field != null && !isExcluded(field) && getter != null && !isExcluded(getter) && setter != null);
        }

        /**
         * @param accessibleObject field or getter
         * @return true if accessibleObject contains @HColumn(excluded = true)
         */
        private boolean isExcluded(AccessibleObject accessibleObject) {
            HColumn hColumn = accessibleObject.getAnnotation(HColumn.class);
            return (hColumn != null && hColumn.excluded());
        }

        /**
         * @return field fieldName declared in clazz or it's superclass if such field is present, null otherwise
         */
        private Field getField(Class<?> clazz, String fieldName) {
            while (clazz != null) {
                Field[] declaredFields = clazz.getDeclaredFields();
                for (Field field : declaredFields) {
                    if (field.getName().equals(fieldName)) {
                        return field;
                    }
                }
                clazz = clazz.getSuperclass();
            }
            return null;
        }
    }

    private static class BeanPropertyStore extends SimpleStore<BeanPropertyId, BeanProperty> {
        @Override
        protected BeanProperty createValue(BeanPropertyId arg) {
            PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(arg.getBeanClass());
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                if (arg.getPropertyName().equals(propertyDescriptor.getName())) {
                    return new BeanPropertyImpl(propertyDescriptor);
                }
            }
            throw new RuntimeException("Property not found. Class=" + arg.getBeanClass() + " property=" + arg.getPropertyName());
        }
    }

    private static final class BeanPropertyId {
        private final Class<?> beanClass;
        private final String propertyName;

        public BeanPropertyId(Class<?> beanClass, String propertyName) {
            this.beanClass = Preconditions.checkNotNull(beanClass);
            this.propertyName = Preconditions.checkNotNull(propertyName);
        }

        public Class<?> getBeanClass() {
            return beanClass;
        }

        public String getPropertyName() {
            return propertyName;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BeanPropertyId)) {
                return false;
            }
            BeanPropertyId that = (BeanPropertyId)obj;
            return beanClass.equals(that.beanClass) && propertyName.equals(that.propertyName);
        }

        @Override
        public int hashCode() {
            return beanClass.hashCode() * 31 + propertyName.hashCode();
        }
    }
}
