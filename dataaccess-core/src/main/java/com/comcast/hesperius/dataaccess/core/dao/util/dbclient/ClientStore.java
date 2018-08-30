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

import com.comcast.hesperius.dataaccess.core.config.DataServiceConfiguration.ConnectionPoolConfiguration;
import com.comcast.hesperius.dataaccess.core.util.store.BaseStore;
import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.connectionpool.Host;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.recipes.ConstantSupplier;
import com.netflix.astyanax.retry.BoundedExponentialBackoff;
import com.netflix.astyanax.retry.ConstantBackoff;
import com.netflix.astyanax.retry.ExponentialBackoff;
import com.netflix.astyanax.retry.IndefiniteRetry;
import com.netflix.astyanax.retry.RetryNTimes;
import com.netflix.astyanax.retry.RetryPolicy;
import com.netflix.astyanax.retry.RunOnce;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;

import java.util.ArrayList;
import java.util.List;

public class ClientStore<V> extends BaseStore<String, ClientConfiguration, V> {

    private static final int MAX_TO_INIT_CONNECTIONS_RATIO = 3; // value that is not configurable in hector

    @Override
    protected String keyFromArg(ClientConfiguration clientConfiguration) {
        return clientConfiguration.getClientName();
    }

    @Override
    protected V createValue(ClientConfiguration clientConfiguration) {
        ConnectionPoolConfiguration cpConfig = clientConfiguration.getConnectionPoolConfiguration();
        AstyanaxContext.Builder contextBuilder = new AstyanaxContext.Builder()
                .forCluster(cpConfig.getName())
                .forKeyspace(cpConfig.getKeyspaceName())
                .withAstyanaxConfiguration(new AstyanaxConfigurationImpl()
                        .setConnectionPoolType(cpConfig.getConnectionPoolType()) // loadBalancingPolicy in hector, was not configurable via service.properties
                        .setDiscoveryType(cpConfig.getNodeDiscoveryType())
                        .setDefaultReadConsistencyLevel(cpConfig.getReadConsistencyLevel())
                        .setDefaultWriteConsistencyLevel(cpConfig.getWriteConsistencyLevel())
                        .setDiscoveryDelayInSeconds(cpConfig.getDiscoveryDelayInSeconds())
                        .setRetryPolicy(createRetryPolicy(cpConfig))
                )
                .withHostSupplier(getHostsSupplierFromSeeds(cpConfig.getSeeds()))
                .withConnectionPoolConfiguration(new ConnectionPoolConfigurationImpl(cpConfig.getName())
                                .setSeeds(Joiner.on(",").skipNulls().join(cpConfig.getSeeds()))
                                .setAuthenticationCredentials(cpConfig.getAuthenticationCredentials())
                                .setMaxConnsPerHost(cpConfig.getMaxConnsPerHost())
                                .setMaxOperationsPerConnection(cpConfig.getMaxOperationsPerConnection()) // ConnectionPoolConfigurationImpl.DEFAULT_MAX_OPERATIONS_PER_CONNECTION
                                .setInitConnsPerHost(cpConfig.getMaxConnsPerHost() / MAX_TO_INIT_CONNECTIONS_RATIO) // compatible value, is not configurable in hector
                                .setSocketTimeout(cpConfig.getSocketTimeout())
                                .setMaxTimeoutWhenExhausted(cpConfig.getMaxTimeoutWhenExhausted())
                                .setConnectTimeout(cpConfig.getConnectTimeout())
                                .setTimeoutWindow(cpConfig.getTimeoutWindow())
                                .setMaxTimeoutCount(cpConfig.getMaxTimeoutCount())
                                .setMaxFailoverCount(cpConfig.getMaxFailoverCount())
                                .setLocalDatacenter(cpConfig.getLocalDatacenter())
                )
                .withConnectionPoolMonitor(clientConfiguration.getConnectionPoolMonitor());

        AstyanaxContext<?> context = (clientConfiguration.getClientType() == ClientConfiguration.ClientType.CLUSTER)
                ? contextBuilder.buildCluster(ThriftFamilyFactory.getInstance())
                : contextBuilder.buildKeyspace(ThriftFamilyFactory.getInstance());
        context.start();
        return (V)context.getClient();
    }

    private Supplier<List<Host>> getHostsSupplierFromSeeds(String[] seeds) {
        List<Host> hosts = new ArrayList<Host>();
        if (seeds != null) {
            for (String seed : seeds) {
                seed = seed.trim();
                if (seed.length() > 0) {
                    hosts.add(new Host(seed, ConnectionPoolConfigurationImpl.DEFAULT_PORT));
                }
            }
        }
        return new ConstantSupplier<List<Host>>(hosts);
    }

    // TODO: get rid of boilerplate validation code by using JSR-303
    private RetryPolicy createRetryPolicy(ConnectionPoolConfiguration config) {
        RetryPolicy retryPolicy = null;
        switch (config.getRetryPolicyType()) {
            case INDEFINITE:
                retryPolicy = new IndefiniteRetry();
                break;
            case RETRY_NTIMES:
                if (config.getMaxAttemptCount() == null || config.getMaxAttemptCount() <= 0) {
                    throw new IllegalArgumentException("maxAttemptCount is required for RETRY_NTIMES RetryPolicy");
                }
                retryPolicy = new RetryNTimes(config.getMaxAttemptCount());
                break;
            case RUN_ONCE:
                retryPolicy = new RunOnce();
                break;
            case CONSTANT_BACKOFF:
                if (config.getMaxAttemptCount() == null || config.getSleepTimeMs() == null) {
                    throw new IllegalArgumentException("maxAttemptCount and sleepTimeMs are required for EXPONENTIAL_BACKOFF RetryPolicy");
                }
                if (config.getMaxAttemptCount() <= 0 || config.getSleepTimeMs() <= 0) {
                    throw new IllegalArgumentException("maxAttemptCount and sleepTimeMs should be set to positive value for EXPONENTIAL_BACKOFF RetryPolicy");
                }
                retryPolicy = new ConstantBackoff(config.getSleepTimeMs(), config.getMaxAttemptCount());
                break;
            case EXPONENTIAL_BACKOFF:
                if (config.getMaxAttemptCount() == null || config.getSleepTimeMs() == null) {
                    throw new IllegalArgumentException("maxAttemptCount and sleepTimeMs are required for EXPONENTIAL_BACKOFF RetryPolicy");
                }
                if (config.getMaxAttemptCount() <= 0 || config.getSleepTimeMs() <= 0) {
                    throw new IllegalArgumentException("maxAttemptCount and sleepTimeMs should be set to positive value for EXPONENTIAL_BACKOFF RetryPolicy");
                }
                retryPolicy = new ExponentialBackoff(config.getSleepTimeMs(), config.getMaxAttemptCount());
                break;
            case BOUNDED_EXPONENTIAL_BACKOFF:
                if (config.getMaxAttemptCount() == null || config.getSleepTimeMs() == null || config.getMaxSleepTimeMs() == null) {
                    throw new IllegalArgumentException("maxAttemptCount, sleepTimeMs and maxSleepTimeMs are not set for EXPONENTIAL_BACKOFF RetryPolicy");
                }
                if (config.getMaxAttemptCount() <= 0 || config.getSleepTimeMs() <= 0 || config.getMaxSleepTimeMs() <= 0) {
                    throw new IllegalArgumentException("maxAttemptCount, sleepTimeMs and maxSleepTimeMs should be set to positive value for EXPONENTIAL_BACKOFF RetryPolicy");
                }
                retryPolicy = new BoundedExponentialBackoff(config.getSleepTimeMs(),
                        config.getMaxSleepTimeMs(), config.getMaxAttemptCount());
                break;
        }
        return retryPolicy;
    }
}
