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
package com.comcast.hesperius.dataaccess.core.jmx;

import com.comcast.hesperius.dataaccess.core.jmx.annotations.JMX;
import com.comcast.hesperius.dataaccess.core.util.AnnotationScanner;
import com.comcast.hesperius.dataaccess.core.util.CoreUtil;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Helper class for use with JMX facility.
 *
 */
public abstract class JMXUtils {

    public static boolean isCollection(Class<?> clazz) {
        return Collection.class.isAssignableFrom(clazz);
    }

    public static boolean isMap(Class<?> clazz) {
        return Map.class.isAssignableFrom(clazz);
    }

    public static boolean isPrimitive(Class<?> clazz) {
        if (clazz.equals(Byte.TYPE) || clazz.equals(Byte.class) ||
                clazz.equals(Short.TYPE) || clazz.equals(Short.class) ||
                clazz.equals(Integer.TYPE) || clazz.equals(Integer.class) ||
                clazz.equals(Long.TYPE) || clazz.equals(Long.class) ||
                clazz.equals(Float.TYPE) || clazz.equals(Float.class) ||
                clazz.equals(Double.TYPE) || clazz.equals(Double.class) ||
                clazz.equals(Character.TYPE) || clazz.equals(Character.class) ||
                clazz.equals(Boolean.TYPE) || clazz.equals(Boolean.class) ||
                clazz.equals(String.class) || clazz.equals(Void.class) || clazz.equals(Void.TYPE)) {
            return true;
        }

        return false;
    }

    public static Set<Class<?>> getJMXClasses(String... packages) {
        return AnnotationScanner.getAnnotatedClasses(new Class[]{JMX.class}, packages);
    }

    public static class ObjectToStringTransformer implements ComplexObjectTransformer<Object, String> {
        @Override
        public String transform(Object input) {
            return input.toString();
        }
    }
}
