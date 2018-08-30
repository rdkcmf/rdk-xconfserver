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

import com.comcast.hesperius.dataaccess.core.util.AnnotationScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

public class ConfigurationProvider {
    private static Logger log = LoggerFactory.getLogger(ConfigurationProvider.class);

    private static final String CONFIG_FILE_NAME = "service-config.json";
    private static final String JSON_OVERRIDE_FILE_NAME = "service-override-config.json";
    private static final String PLAIN_PROPERTIES_OVERRIDE_FILE_NAME = "service.properties";
    private static final String JSON_OVERRIDE_SYSTEM_PROPERTY = "config";
    private static final String PLAIN_PROPERTIES_OVERRIDE_SYSTEM_PROPERTY = "appConfig";

    private static final String SPECIFIC_CONFIG_BEAN_PACKAGE = "com.comcast.hydra.astyanax.config";
    private static final Class<SpecificConfigBean> SPECIFIC_CONFIG_BEAN_ANNOTATION = SpecificConfigBean.class;

    private static final DataServiceConfiguration DATA_SERVICE_CONFIGURATION;

    static {
        URL configUrl = urlFromFileName(CONFIG_FILE_NAME, true);
        URL overrideConfigUrl = getOverrideConfigUrl();
        Class<?> specificConfigClass = getSpecificConfigClass();

        DATA_SERVICE_CONFIGURATION = ConfigParser.parse(configUrl, overrideConfigUrl, specificConfigClass);
    }

    public static DataServiceConfiguration getConfiguration() {
        return DATA_SERVICE_CONFIGURATION;
    }

    private static URL urlFromFileName(String fileName, boolean failIfNotFound) {
        URL result = null;

        // if running in Servlet container
        if (ConfigurationProvider.class.getClassLoader().getParent() != null) {
            result = ConfigurationProvider.class.getClassLoader().getParent().getResource(fileName);
        }

        // if running in maven-jetty-plugin
        if (result == null) {
            result = ConfigurationProvider.class.getClassLoader().getResource(fileName);
        }

        if ((result== null) && failIfNotFound) {
            throw new RuntimeException("File \"" + fileName + "\" not found.");
        }
        return result;
    }

    private static URL getOverrideConfigUrl() {
        File baseDirectory = getBaseDirectory();
        URL result;
        result = getJsonOverrideConfigUrl(baseDirectory);
        if (result == null) {
            result = getPlainPropertiesOverrideConfigUrl(baseDirectory);
        }

        return result;
    }

    private static URL getPlainPropertiesOverrideConfigUrl(File baseDirectory) {
        URL result = null;

        String appConfig = System.getProperty(PLAIN_PROPERTIES_OVERRIDE_SYSTEM_PROPERTY);
        try {
            result = getFileAsURL(baseDirectory, appConfig);
        } catch (MalformedURLException ex) {
            log.error("invalid URL for appConfig file {}", appConfig, ex);
        }

        if (result == null) {
            try {
                result = getFileAsURL(baseDirectory, PLAIN_PROPERTIES_OVERRIDE_FILE_NAME);
            }
            catch (MalformedURLException ex) {
                log.error("Failed to load configuration file service.properties. ", ex);
            }
        }

        return result;
    }

    private static URL getJsonOverrideConfigUrl(File baseDirectory) {
        String overridePath = System.getProperty(JSON_OVERRIDE_SYSTEM_PROPERTY);
        URL result = null;

        try {
            result = getFileAsURL(baseDirectory, overridePath);
        } catch (MalformedURLException ex) {
            log.error("invalid URL for {} file {}", JSON_OVERRIDE_SYSTEM_PROPERTY, overridePath, ex);
        }

        if (result == null) {
            result = urlFromFileName(JSON_OVERRIDE_FILE_NAME, false);
        }

        return result;
    }

    public static URL getFileAsURL(File baseDirectory, String path) throws MalformedURLException {
        final String fileProtocol = "file:";
        if (path != null) {
            if (path.startsWith(fileProtocol)) {
                return new URL(path);
            }

            File file = new File(path);
            if (file.canRead()) {
                return file.toURI().toURL();
            }
            if (baseDirectory != null) {
                file = new File(baseDirectory, path);
            }
            if (file.canRead()) {
                return file.toURI().toURL();
            }
        }
        return null;
    }

    public static File getBaseDirectory() {
        File baseDirectory = getDirectoryFromSystemProperties("jetty.base");
        if (baseDirectory == null) {
            baseDirectory = getDirectoryFromSystemProperties("jetty.home");
        }
        return baseDirectory;
    }

    public static File getDirectoryFromSystemProperties(String key) {
        String dirName = System.getProperty(key);
        if (dirName != null) {
            File directory = new File(dirName);
            if (directory.isDirectory() && directory.canRead()) {
                return directory;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Class<?> getSpecificConfigClass() {
        Set<Class<?>> foundClasses = AnnotationScanner.getAnnotatedClasses(new Class[]{SPECIFIC_CONFIG_BEAN_ANNOTATION}, SPECIFIC_CONFIG_BEAN_PACKAGE);
        if (foundClasses.size() > 1) {
            throw new RuntimeException("Found multiple specific config beans. In package " + SPECIFIC_CONFIG_BEAN_PACKAGE + " may be placed one class annotated with @" + SPECIFIC_CONFIG_BEAN_ANNOTATION.getSimpleName() + ". This class will be used as specific config bean.");
        }
        return foundClasses.isEmpty() ? Map.class : foundClasses.iterator().next();
    }
}
