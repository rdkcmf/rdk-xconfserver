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
package com.comcast.hesperius.dataaccess.core.dao.util;

import com.netflix.astyanax.ddl.KeyspaceDefinition;

/**
 * Convenient class for building {@link com.netflix.astyanax.ddl.KeyspaceDefinition}.
 * See http://www.datastax.com/docs/1.1/configuration/storage_configuration for details.
 */
public class KeyspaceDefinitionBuilder {
    public static final String SIMPLE_STRATEGY = "org.apache.cassandra.locator.SimpleStrategy";
    public static final String NETWORK_TOPOLOGY_STRATEGY = "org.apache.cassandra.locator.NetworkTopologyStrategy";

    private static final String REPLICATION_FACTOR_KEY = "replication_factor";

    private KeyspaceDefinition keyspaceDefinition;

    public KeyspaceDefinitionBuilder(KeyspaceDefinition keyspaceDefinition) {
        this.keyspaceDefinition = keyspaceDefinition;
    }

    public KeyspaceDefinitionBuilder forKeyspace(String keyspaceName) {
        keyspaceDefinition.setName(keyspaceName);
        return this;
    }

    /**
     * There are two available strategies - SimpleStrategy and NetworkTopologyStrategy.
     *
     * @param strategyClass Cassandra's keyspace placement strategy. Allowed values:
     *        "org.apache.cassandra.locator.SimpleStrategy" or "SimpleStrategy" for SimpleStrategy,
     *        "org.apache.cassandra.locator.NetworkTopologyStrategy" or "NetworkTopologyStrategy" for NetworkTopologyStrategy.
     * @return this builder
     */
    public KeyspaceDefinitionBuilder withStrategyClass(String strategyClass) {
        keyspaceDefinition.setStrategyClass(strategyClass);
        return this;
    }

    public KeyspaceDefinitionBuilder withSimpleStrategy() {
        return withStrategyClass(SIMPLE_STRATEGY);
    }

    public KeyspaceDefinitionBuilder withNetworkTopologyStrategy() {
        return withStrategyClass(NETWORK_TOPOLOGY_STRATEGY);
    }

    public KeyspaceDefinitionBuilder addStrategyOption(String key, String value) {
        keyspaceDefinition.addStrategyOption(key, value);
        return this;
    }

    /**
     * Convenient method for setting replication factor, the only strategy option for SimpleStrategy.
     *
     * @param replicationFactor should be positive integer
     * @return this builder
     */
    public KeyspaceDefinitionBuilder withReplicationFactor(int replicationFactor) {
        return addStrategyOption(REPLICATION_FACTOR_KEY, String.valueOf(replicationFactor));
    }

    /**
     * @return built KeyspaceDefinition
     */
    public KeyspaceDefinition build() {
        return keyspaceDefinition;
    }
}
