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
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class ConfigParser {
    private static Logger log = LoggerFactory.getLogger(ConfigParser.class);

    private static final String KEYSPACES_NODE_NAME = "keyspaces";
    private static final String COLUMN_FAMILIES_NODE_NAME = "columnFamilies";
    private static final String CONNECTION_POOLS_NODE_NAME = "connectionPools";
    private static final String ENDPOINTS_NODE_NAME = "endpoints";
    private static final String SPECIFIC_NODE_NAME = "specific";
    private static final String DEFAULTS_NODE_NAME = "defaults";
    private static final String NAME = "name";

    public static DataServiceConfiguration parse(URL configUrl, URL overrideConfigUrl, Class<?> specificConfigClass) {

        ObjectNode jsonTree = readAndProcessJsonTree(configUrl, overrideConfigUrl);
        DataServiceConfiguration dataServiceConfiguration = parseJsonTree(jsonTree, specificConfigClass);

        return dataServiceConfiguration;
    }

    private static ObjectNode readAndProcessJsonTree(URL configUrl, URL overrideConfigUrl) {

        JsonNode configJsonTree = readJsonTree(configUrl);

        if (overrideConfigUrl != null) {
            if (overrideConfigUrl.getFile().endsWith(".properties")) {
                readAndProcessPropertiesFile((ObjectNode) configJsonTree, overrideConfigUrl);
            } else {
                JsonNode overrideConfigJsonTree = readJsonTree(overrideConfigUrl);
                configJsonTree = merge(configJsonTree, overrideConfigJsonTree);
            }
        }

        applyDefaults(configJsonTree);
        addKeysAsNameValues(configJsonTree);
        return (ObjectNode) configJsonTree;
    }

    private static void readAndProcessPropertiesFile(ObjectNode jsonTree, URL overrideConfigUrl) {
        Properties properties = new Properties();
        try {
            InputStream input = overrideConfigUrl.openStream();
            properties.load(input);
        } catch (IOException e) {
            log.error("could not read {}", overrideConfigUrl, e);
        }

        overrideProperties(properties);

        for (String propertyName : properties.stringPropertyNames()) {
            String value = properties.get(propertyName).toString();
            String[] keys = propertyName.split("\\.");
            if (keys.length > 0 && jsonTree.has(keys[0])) {
                ObjectNode node = jsonTree;
                // traverse the json configuration from the root node to the leaf node
                int i = 0;
                while (i + 1 < keys.length) {
                    if (node.has(keys[i])) {
                        node = (ObjectNode) node.get(keys[i]);
                    } else {
                        node = node.putObject(keys[i]);
                    }
                    i++;
                }
                String propName = keys[i];
                if ("seeds".equals(propName)) {
                    // seeds nodes are arrays of Cassandra hosts/nodes
                    putArrayValueFromPropertiesFile(node, propName, value);
                } else {
                    updateNodeValueFromPropertiesFile(node,propName, value);
                }
            }
        }
    }

    private static void overrideProperties(Properties properties) {
        String overridePropertiesPrefix = System.getProperty("overridePropertiesPrefix");
        if (StringUtils.isNotEmpty(overridePropertiesPrefix)) {
            for (String propertyName : properties.stringPropertyNames()) {
                overrideProperty(properties, propertyName, overridePropertiesPrefix);
            }
        }
    }

    private static void overrideProperty(Properties properties, String propertyName, String overridePropertiesPrefix) {
        String overridePropertyName = getOverridePropertyName(propertyName, overridePropertiesPrefix);
        if (properties.containsKey(overridePropertyName)) {
            String overrideValue = properties.get(overridePropertyName).toString();
            properties.setProperty(propertyName, overrideValue);
            properties.remove(overridePropertyName);
        }
    }

    private static String getOverridePropertyName(String name, String prefix) {
        return prefix + "." + name;
    }

    private static void updateNodeValueFromPropertiesFile(ObjectNode node, String key, String value) {
        // check for a boolean value
        if ("false".equalsIgnoreCase(value)) {
            node.put(key, false);
            return;
        } else if ("true".equalsIgnoreCase(value)) {
            node.put(key, true);
            return;
        }

        // check for an integer value
        try {
            int intVal = Integer.parseInt(value);
            node.put(key, intVal);
            return;
        }
        catch (NumberFormatException ex) {
            // value is not an integer
        }

        // default case is a string value
        node.put(key, value);
    }

    private static void putArrayValueFromPropertiesFile(ObjectNode node, String key, String value) {
        String[] seeds = value.split(",\\s*");
        ArrayNode arrayNode = node.putArray(key);
        for (String host : seeds) {
            arrayNode.add(host);
        }
    }

    private static DataServiceConfiguration parseJsonTree(ObjectNode jsonTree, Class<?> specificConfigClass) {

        final JsonNode specificConfigNode = jsonTree.remove(SPECIFIC_NODE_NAME);
        final DataServiceConfiguration.ColumnFamilyConfiguration cfDefaults = readValue(
                ((ObjectNode) jsonTree.get(COLUMN_FAMILIES_NODE_NAME)).remove(DEFAULTS_NODE_NAME),
                DataServiceConfiguration.ColumnFamilyConfiguration.class);
        final DataServiceConfiguration.ConnectionPoolConfiguration connPoolDefaults = readValue(
                ((ObjectNode) jsonTree.get(CONNECTION_POOLS_NODE_NAME)).get(cfDefaults.getConnectionPoolName())
                , DataServiceConfiguration.ConnectionPoolConfiguration.class);

        final DataServiceConfiguration dataServiceConfiguration = readValue(jsonTree, DataServiceConfiguration.class);
        dataServiceConfiguration.setDefaultCFConfiguration(cfDefaults);
        dataServiceConfiguration.setDefaultConnectonPoolConfiguration(connPoolDefaults);

        if (specificConfigNode != null) {
            Object specificConfig = readValue(specificConfigNode, specificConfigClass);
            dataServiceConfiguration.setSpecificConfig(specificConfig);
        } else if (specificConfigClass.isAnnotationPresent(SpecificConfigBean.class)) {
            throw new RuntimeException("Deployment/JSON Configuration issue. Specific section is absent");
        }
        return dataServiceConfiguration;
    }

    /**
     * @throws NullPointerException if baseNode or overrideNode is null
     */
    private static JsonNode merge(JsonNode baseNode, JsonNode overrideNode) {

        if (!baseNode.isObject() || !overrideNode.isObject()) {
            return deepCopy(overrideNode);
        }
        ObjectNode result = (ObjectNode) deepCopy(baseNode);

        Iterator<Map.Entry<String, JsonNode>> overrideFieldsIterator = overrideNode.getFields();
        while (overrideFieldsIterator.hasNext()) {

            Map.Entry<String, JsonNode> overrideField = overrideFieldsIterator.next();
            String fieldName = overrideField.getKey();
            JsonNode overrideValue = overrideField.getValue();

            JsonNode baseValue = baseNode.get(fieldName);
            if (baseValue == null) {
                result.put(fieldName, overrideValue);
            } else {

                JsonNode mergedValue = merge(baseValue, overrideValue);
                result.put(fieldName, mergedValue);
            }
        }
        return result;
    }

    /**
     * @throws NullPointerException if sourceNode is null
     */
    private static JsonNode deepCopy(JsonNode sourceNode) {
        try {
            return new ObjectMapper().readTree(sourceNode.traverse());
        } catch (IOException ex) {
            throw new AssertionError(ex);
        }
    }

    private static void applyDefaults(JsonNode rootNode) {
        applyMapDefaults(rootNode.get(COLUMN_FAMILIES_NODE_NAME));
        applyMapDefaults(rootNode.get(CONNECTION_POOLS_NODE_NAME));
        applyMapDefaults(rootNode.get(ENDPOINTS_NODE_NAME));
    }

    private static void applyMapDefaults(JsonNode mapNode) {
        if (mapNode == null) {
            return;
        }
        ObjectNode mapObjectNode = (ObjectNode) mapNode;
        ObjectNode defaults = (ObjectNode) mapObjectNode.get(DEFAULTS_NODE_NAME);

        if (defaults == null) {
            return;
        }
        Iterator<String> keysIterator = mapNode.getFieldNames();
        while (keysIterator.hasNext()) {

            String key = keysIterator.next();
            JsonNode value = mapNode.get(key);

            JsonNode mergedValue = merge(defaults, value);
            mapObjectNode.put(key, mergedValue);
        }
    }

    private static void addKeysAsNameValues(JsonNode rootNode) {
        addMapKeyAsNameValue(rootNode.get(KEYSPACES_NODE_NAME));
        addMapKeyAsNameValue(rootNode.get(COLUMN_FAMILIES_NODE_NAME));
        addMapKeyAsNameValue(rootNode.get(CONNECTION_POOLS_NODE_NAME));
        addMapKeyAsNameValue(rootNode.get(ENDPOINTS_NODE_NAME));
    }

    private static void addMapKeyAsNameValue(JsonNode mapNode) {
        if (mapNode == null) {
            return;
        }
        Iterator<String> keysIterator = mapNode.getFieldNames();
        while (keysIterator.hasNext()) {
            String key = keysIterator.next();
            ObjectNode value = (ObjectNode) mapNode.get(key);
            value.put(NAME, key);
        }
    }

    /**
     * Workaround. Jackson (org.codehaus.jackson:jackson-mapper-asl:1.9.2) fails if url contains special characters and represents file.
     */
    private static JsonNode readJsonTree(URL source) {
        JsonNode result;
        try {
            InputStream inputStream = source.openStream();
            result = new ObjectMapper().readTree(inputStream);
            inputStream.close();
        } catch (IOException ex) {
            String message = ex.getClass().getSimpleName() + " has been thrown while parsing " + source;
            log.error(message, ex);
            throw new RuntimeException(message, ex);
        }
        return result;
    }

    private static <T> T readValue(JsonNode source, Class<T> clazz) {
        try {
            return new ObjectMapper().reader(clazz).readValue(source);
        } catch (IOException ex) {
            String message = ex.getClass().getSimpleName() + " has been thrown while parsing " + source;
            log.error(message, ex);
            throw new RuntimeException(message, ex);
        }
    }
}
