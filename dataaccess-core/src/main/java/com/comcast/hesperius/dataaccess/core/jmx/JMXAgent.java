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

import com.comcast.hesperius.dataaccess.core.config.ConfigFactory;
import com.comcast.hesperius.dataaccess.core.jmx.config.JMXConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

/**
 * .
 */
public enum JMXAgent {
    /**
     * Returns an agent singleton.
     */
    INSTANCE;

    private final JMXConfig config = ConfigFactory.create(JMXConfig.class);
    private final Set<Class<?>> classes = new HashSet<Class<?>>();
    private final JMXClassLoader classLoader = new JMXClassLoader();
    private final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    private final Logger log = LoggerFactory.getLogger(JMXAgent.class);

    /**
     * Instantiate and register your MBeans.
     */
    {
        if (Boolean.parseBoolean(config.isJmxEnabled())) {
            classes.addAll(JMXUtils.getJMXClasses(config.getAppJMXPackage()));

            for(Class<?> clazz : classes) {
                try {
                    StandardMBean smbean = buildMBean(clazz);
                    registerMBean(smbean, clazz);
                } catch (Exception e) {
                    log.error("", e);
                }
            }
        }
    }

    /**
     * Register your MBeans.
     * @param smbean
     * @param clazz the class
     * @throws Exception
     */
    public void registerMBean(StandardMBean smbean, Class<?> clazz) throws Exception {
        ObjectName objectSmb = new ObjectName(config.getAppName() + ":type=" + clazz.getSimpleName());
        mbs.registerMBean(smbean, objectSmb);
    }

    public void registerMbean(String domainName, String name, Object mbean){
        String objName = domainName + ":name=" + name;
        try {
            ObjectName on = new ObjectName(objName);
            Set<ObjectName> queryResult = mbs.queryNames(on, null);
            if (!queryResult.isEmpty()) {
                mbs.unregisterMBean(on);
            }
            mbs.registerMBean(mbean, on);
            log.info("Mbean {} is registered", objName);
        } catch (Exception ex) {
            log.error("Not able to register MBean: " + objName, ex);
        }
    }

    public StandardMBean buildMBean(Class<?> clazz) {
        byte[] bytes = MBeanInterfaceBuilder.buildInterface(clazz);
        Class<?> clazzMBean = classLoader.defineClass(clazz.getName() + "MBean", bytes);
        InvocationHandler mbeanInvocationHandler = new MBeanInvocationHandler(clazz);

        return makeStandardMBean(clazzMBean, mbeanInvocationHandler);
    }

    private <T> StandardMBean makeStandardMBean(Class<T> intf, InvocationHandler handler) {
        Object proxy = Proxy.newProxyInstance(intf.getClassLoader(), new Class<?>[]{intf}, handler);
        T impl = intf.cast(proxy);

        try {
            return new StandardMBean(impl, intf);
        } catch (NotCompliantMBeanException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static class JMXClassLoader extends ClassLoader {
        public Class<?> findClass(String name) {
            return findLoadedClass(name);
        }

        public Class<?> defineClass(String className, byte[] classBytes) {
            Class<?> clazz = findClass(className);
            if (clazz != null) {
                return clazz;
            }

            return defineClass(className, classBytes, 0, classBytes.length);
        }
    }
}

