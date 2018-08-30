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
import net.sf.cglib.core.ReflectUtils;
import net.sf.cglib.core.Signature;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

/**
 * MBean interface generation.
 *
 */
public class MBeanInterfaceBuilder {
    private final ClassWriter mbeanInterface;

    private MBeanInterfaceBuilder() {
        mbeanInterface = new ClassWriter(0);
    }


    /**
     * Returns the MBean interface byte code for given class.
     *
     * @param clazz
     * @return the byte code
     */
    public static byte[] buildInterface(Class clazz) {
        return new MBeanInterfaceBuilder().build(clazz);
    }

    private byte[] build(Class<?> clazz) {
        mbeanInterface.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT + Opcodes.ACC_INTERFACE,
                clazz.getName().replace(".", "/") + "MBean", null, "java/lang/Object", null);

        for(Field field : clazz.getDeclaredFields()) {
            Annotation annotation = jmxAnnotation(field);
            if (annotation != null) {
                String nameField = field.getName();
                String desc = Type.getType(field.getType()).toString();
                String tempNameField = nameField.substring(0, 1).toUpperCase() +
                        nameField.substring(1, nameField.length());

                JMXExpose anno = (JMXExpose) annotation;
                if (JMXUtils.isPrimitive(field.getType())) {
                    Signature signatureForGetMethod = new Signature(nameField, Type.getReturnType(desc),
                            new Type[] { });
                    addMethod("get" + tempNameField, signatureForGetMethod.getDescriptor());
                    if (!Modifier.isFinal(field.getModifiers())) {
                        Signature signatureForSetMethod = new Signature(nameField, Type.getReturnType("V"),
                                new Type[] { Type.getReturnType(desc) });
                        addMethod("set" + tempNameField, signatureForSetMethod.getDescriptor());
                    }
                } else if (JMXUtils.isCollection(field.getType())) {
                    Signature signature = new Signature(nameField, Type.getType(List.class), new Type[] { });
                    addMethod("get" + tempNameField, signature.getDescriptor());
                } else if (JMXUtils.isMap(field.getType())) {
                    Signature signature = new Signature(nameField, Type.getType(Map.class), new Type[] { });
                    addMethod("get" + tempNameField, signature.getDescriptor());
                } else {
                    Signature signature = new Signature(nameField, Type.getType(Object.class), new Type[] { });

                    addMethod("get" + tempNameField, signature.getDescriptor());
                }
            }
        }

        for (Method method : clazz.getDeclaredMethods()) {
            Annotation annotation = jmxAnnotation(method);
            if (annotation != null) {
                String name = method.getName();
                String desc = ReflectUtils.getSignature(method).getDescriptor();

                JMXExpose anno = (JMXExpose) annotation;
                if (JMXUtils.isPrimitive(method.getReturnType())) {
                    addMethod(name, desc);
                } else if (JMXUtils.isCollection(method.getReturnType())) {
                    Signature signature = new Signature(name, Type.getType(List.class),
                            Type.getArgumentTypes(method));
                    addMethod(name, signature.getDescriptor());
                } else if (JMXUtils.isMap(method.getReturnType())) {
                    Signature signature = new Signature(name, Type.getType(Map.class),
                            Type.getArgumentTypes(method));
                    addMethod(name, signature.getDescriptor());
                } else {
                    Signature signature = new Signature(name, Type.getType(Object.class),
                            Type.getArgumentTypes(method));

                    addMethod(name, signature.getDescriptor());
                }
            }
        }

        return mbeanInterface.toByteArray();
    }

    private Annotation jmxAnnotation(AnnotatedElement ae) {
        if (ae.isAnnotationPresent(JMXExpose.class)) {
            return ae.getAnnotation(JMXExpose.class);
        }

        return null;
    }

    private void addMethod(String name, String desc) {
        mbeanInterface.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT, name, desc, null, null);
    }
}
