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

import com.comcast.hesperius.dataaccess.core.jmx.annotations.JMXExpose;

import java.lang.reflect.*;
import java.util.*;

/**
 * Invocation handler for MBean proxies.
 *
 */
public class MBeanInvocationHandler implements InvocationHandler {
    /**
     * Constructs a new MBean proxy invocation handler.
     * @param clazz the class
     */
    public MBeanInvocationHandler(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Object invoke(Object proxy, Method interfaceMethod, Object[] args) throws Throwable {
        Object object = null;

        Method classMethod = getClassMethod(interfaceMethod);
        if (classMethod != null) {
            object = classMethod.invoke(clazz, args);
            if (interfaceMethod.getName().startsWith("toString")) { return object; }

            if (object != null) {
                return expose(object, getJMXExposeAnnotation(interfaceMethod), classMethod.getReturnType());
            }

            return object;
        }

        Field classField = getClassField(interfaceMethod);
        if (classField != null) {
            if (interfaceMethod.getName().startsWith("get")) {
                object = classField.get(null);

                return expose(object, getJMXExposeAnnotation(interfaceMethod), classField.getType());
            } else if (interfaceMethod.getName().startsWith("set")) {
                if (JMXUtils.isPrimitive(classField.getType())) {
                    classField.set(null, args[0]);
                }

                return null;
            }
        }

        return object;
    }


    private Object expose(Object object, JMXExpose annotation, Class<?> returnType) throws Exception {
        if (JMXUtils.isPrimitive(returnType)) {
            return object;
        } else if (JMXUtils.isCollection(returnType)) {
            Collection<?> tempObject = (Collection<?>) object;
            List result = new ArrayList();

            ComplexObjectTransformer transformer = annotation.transform().newInstance();

            for (Iterator i = tempObject.iterator(); i.hasNext();) {
                Object o = i.next();
                result.add(transformer.transform(o));
            }

            return result;
        } else if (JMXUtils.isMap(returnType)) {
            Map<?, ?> tempObject = (Map<?, ?>) object;
            Map result = new HashMap();
            ComplexObjectTransformer transformerKeys = annotation.transformKeys().newInstance();
            ComplexObjectTransformer transformerValues = annotation.transformValues().newInstance();

            for(Map.Entry<?, ?> entry : tempObject.entrySet()) {
                result.put(transformerKeys.transform(entry.getKey()), transformerValues.transform(entry.getValue()));
            }

            return result;
        } else {
            ComplexObjectTransformer transformer = annotation.transform().newInstance();
            return transformer.transform(object);
        }
    }

    private Method getClassMethod(Method interfaceMethod)  {
        Method method = null;
        try {
            method = clazz.getDeclaredMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
            method.setAccessible(true);
        } catch (NoSuchMethodException e) {
            try {
                method = clazz.getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
            } catch (NoSuchMethodException e1) { return null; }
        }

        return method;
    }

    private Field getClassField(Method interfaceMethod) {
        Field field = null;
        try {
            field = clazz.getDeclaredField(getNameField(interfaceMethod.getName()));
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            try {
                field = clazz.getField(getNameField(interfaceMethod.getName()));
            } catch (NoSuchFieldException e1) { return null; }
        }

        return field;
    }

    private JMXExpose getJMXExposeAnnotation(Method interfaceMethod) {
        Method method = getClassMethod(interfaceMethod);
        if (method != null) {
            return method.getAnnotation(JMXExpose.class);
        }

        Field field = getClassField(interfaceMethod);
        if (field != null) {
            return field.getAnnotation(JMXExpose.class);
        }

        return null;
    }

    private static String getNameField(String str) {
        StringBuilder sb = new StringBuilder(str.substring(3, 4).toLowerCase()).append(str.substring(4, str.length()));
        return sb.toString();
    }

    private final Class<?> clazz;
}
