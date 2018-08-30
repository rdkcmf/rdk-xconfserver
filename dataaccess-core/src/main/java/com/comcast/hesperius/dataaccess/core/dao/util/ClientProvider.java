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
 *
 * Author: slavrenyuk
 * Created: 5/15/14
 */
package com.comcast.hesperius.dataaccess.core.dao.util;

import com.comcast.hesperius.dataaccess.core.config.DataServiceConfiguration.ConnectionPoolConfiguration;
import com.comcast.hesperius.dataaccess.core.dao.util.dbclient.ClientConfiguration;
import com.comcast.hesperius.dataaccess.core.dao.util.dbclient.ClientStore;
import com.netflix.astyanax.Cluster;
import com.netflix.astyanax.Keyspace;

/**
 * Goal: get hector compatible behaviour.
 *
 * Hector creates connection pool for Cluster instance and caches it.
 * Hector's Keyspace objects share connection pool of corresponding Cluster instance.
 *
 * In core each Cluster / Keyspace instance has it's own connection pool and is not cached.
 *
 * TODO: we should remove Cluster clients in future and use only Keyspace clients. see https://www.teamccp.com/jira/browse/APPDS-335
 */
public class ClientProvider {
    private static ClientStore<Cluster> clusterStore = new ClientStore<Cluster>();
    private static ClientStore<Keyspace> keyspaceStore = new ClientStore<Keyspace>();

    public static Cluster getOrCreateClusterClient(String clusterName, ConnectionPoolConfiguration connectionPoolConfiguration) {
        ClientConfiguration clientConfiguration = ClientConfiguration
                .forCluster()
                .withName(clusterName)
                .withConnectionPoolConfiguration(connectionPoolConfiguration)
                .build();
        return clusterStore.getOrCreate(clientConfiguration);
    }

    public static Keyspace getOrCreateKeyspaceClient(String keyspaceName, ConnectionPoolConfiguration connectionPoolConfiguration) {
        ClientConfiguration clientConfiguration = ClientConfiguration
                .forKeyspace()
                .withName(keyspaceName)
                .withConnectionPoolConfiguration(connectionPoolConfiguration)
                .build();
        return keyspaceStore.getOrCreate(clientConfiguration);
    }
}


