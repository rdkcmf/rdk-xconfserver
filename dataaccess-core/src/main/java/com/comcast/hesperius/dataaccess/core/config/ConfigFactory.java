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
package com.comcast.hesperius.dataaccess.core.config;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Properties;

/**
 * Factory for (DS)config objects.
 */
public class ConfigFactory {

    private static final Logger log = LoggerFactory.getLogger(ConfigFactory.class);

    private static final String CONFIG_FILE_PARAM_NAME = "config";

    public static <T extends DSConfig> T create(final Class<T> configIface) {
        return (T) Proxy.newProxyInstance(configIface.getClassLoader(), new Class[]{configIface}, new InvocationHandler() {
            final Properties props = new Properties();

            /**
             * Here we try loading by propSRC but if no properties file found we do fallback to default value stated in annotation.
             * We check first external *-override.properties, then internal baked into jar *-override.properties, and at last baked *.properties
             * that is also expected to be baked in here. but if neither of them exists or is available we fallback to default values defined by annotation if any,
             * or return null otherwise.
             */
            {
                final Source propSrc = configIface.getAnnotation(Source.class);
                final String source = propSrc != null ? propSrc.value() : "/hydraJmxProperties.properties"; // Everything except for external config path is URL thus using '/' here
                final String overrideSrc = StringUtils.chomp(source, ".properties").concat("-override.properties");
                String externalOverrideSrc;

                try {

                    final ClassLoader cloader = configIface.getClassLoader();
                    final String configSysProp = System.getProperty(CONFIG_FILE_PARAM_NAME);
                    final URL externalPropertiesURL;
                    if (configSysProp != null) {
                        externalOverrideSrc = configSysProp.substring(0, configSysProp.lastIndexOf('/') + 1).concat(overrideSrc); // NOTE: "/" is effective for in-classpath URLS not File paths
                        externalPropertiesURL = cloader.getResource(externalOverrideSrc);
                    } else {
                        externalPropertiesURL = null;
                    }
                    final URL internalOverrideURL = cloader.getResource(overrideSrc);
                    final URL propertiesURL = cloader.getResource(source);
                    final URL effectiveURL = firstNonNull(externalPropertiesURL, internalOverrideURL, propertiesURL);
                    if (effectiveURL != null) {
                        props.load(effectiveURL.openStream());
                        log.info("loaded configuration from: {}", effectiveURL.toString());
                    } else {
                        log.info("failed to load configuration for {}, no resources available", configIface.getSimpleName());
                    }
                } catch (IOException e) {
                    log.error("Failed to load configuration from external resources", e);
                }
            }

            @Override
            public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                final Property boundProp = method.getAnnotation(Property.class);
                if (boundProp != null) {
                    final Object res = props.getProperty(boundProp.key());
                    return res != null ? res : boundProp.defaultValue();
                } else if (method.getName().startsWith("get")) {
                    return props.getProperty(method.getName().substring(3) /*get t position is 2 counting from 0*/);
                }
                return null;
            }

            public <T> T firstNonNull(T... values) {
                if (values != null) {
                    for (T val : values) {
                        if (val != null) {
                            return val;
                        }
                    }
                }
                return null;
            }
        });
    }
}

