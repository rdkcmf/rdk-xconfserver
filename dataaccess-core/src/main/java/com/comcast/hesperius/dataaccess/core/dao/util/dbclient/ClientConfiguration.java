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
package com.comcast.hesperius.dataaccess.core.dao.util.dbclient;

import com.comcast.hesperius.dataaccess.core.CustomConnectionPoolMonitor;
import com.comcast.hesperius.dataaccess.core.config.DataServiceConfiguration.ConnectionPoolConfiguration;
import com.netflix.astyanax.connectionpool.ConnectionPoolMonitor;
import com.netflix.astyanax.connectionpool.impl.CountingConnectionPoolMonitor;

public class ClientConfiguration {
    private String clientName;
    private ClientType clientType;
    private ConnectionPoolConfiguration connectionPoolConfiguration;
    private ConnectionPoolMonitor connectionPoolMonitor;

    public ClientConfiguration(String clientName, ClientType clientType, ConnectionPoolConfiguration connectionPoolConfiguration,
                               ConnectionPoolMonitor connectionPoolMonitor) {
        this.clientName = clientName;
        this.clientType = clientType;
        this.connectionPoolConfiguration = connectionPoolConfiguration;
        this.connectionPoolMonitor = connectionPoolMonitor;
    }

    public String getClientName() {
        return clientName;
    }

    public ClientType getClientType() {
        return clientType;
    }

    public ConnectionPoolConfiguration getConnectionPoolConfiguration() {
        return connectionPoolConfiguration;
    }

    public ConnectionPoolMonitor getConnectionPoolMonitor() {
        return connectionPoolMonitor;
    }

    public static Builder forCluster() {
        return new Builder(ClientType.CLUSTER, new CountingConnectionPoolMonitor());
    }

    public static Builder forKeyspace() {
        return new Builder(ClientType.KEYSPACE, CustomConnectionPoolMonitor.getInstance());
    }

    public static class Builder {
        private final ClientType clientType;
        private final ConnectionPoolMonitor connectionPoolMonitor;
        private String clientName;
        private ConnectionPoolConfiguration connectionPoolConfiguration;

        public Builder(ClientType clientType, ConnectionPoolMonitor connectionPoolMonitor) {
            this.clientType = clientType;
            this.connectionPoolMonitor = connectionPoolMonitor;
        }

        public Builder withName(String clientName) {
            this.clientName = clientName;
            return this;
        }

        public Builder withConnectionPoolConfiguration(ConnectionPoolConfiguration connectionPoolConfiguration) {
            this.connectionPoolConfiguration = connectionPoolConfiguration;
            return this;
        }

        public ClientConfiguration build() {
            return new ClientConfiguration(clientName, clientType, connectionPoolConfiguration, connectionPoolMonitor);
        }
    }

    public enum ClientType {
        CLUSTER,
        KEYSPACE
    }
}
