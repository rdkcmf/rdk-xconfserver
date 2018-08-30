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

import com.comcast.hesperius.dataaccess.core.acl.AccessControlManager;
import com.comcast.hesperius.dataaccess.core.dao.util.ClientProvider;
import com.comcast.hesperius.dataaccess.core.dao.util.dbclient.ClientStore;
import com.comcast.hesperius.dataaccess.core.dao.util.dbclient.RetryPolicyType;
import com.netflix.astyanax.AuthenticationCredentials;
import com.netflix.astyanax.connectionpool.NodeDiscoveryType;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolType;
import com.netflix.astyanax.connectionpool.impl.SimpleAuthenticationCredentials;
import com.netflix.astyanax.model.ConsistencyLevel;
import org.apache.cassandra.config.CFMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@XmlRootElement
public class DataServiceConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DataServiceConfiguration.class);

    /**
     * Defines core packages to be included into classPath scan for annotated persistable objects
     * needed since core on its own has serializable beans {@see ChangedData}
     * for example, that is used to track changes and cache updating.
     */
    private static final String[] corePackages = new String[]{"com.comcast.hesperius.dataaccess.core"};

    private String domainClassesBasePackage;
    private String validatorsBasePackage;
    private String bindersBasePackage;

    private boolean autoGenerateSchema = true;
    private Integer maxDataSizeForPut;
    private Map<String, KeyspaceSchemaDefinition> keyspaces = new HashMap<String, KeyspaceSchemaDefinition>();
    private Map<String, ColumnFamilySchemaDefinition> columnFamiliesSchema = new HashMap<String, ColumnFamilySchemaDefinition>();
    private Map<String, ColumnFamilyConfiguration> columnFamilies = new HashMap<String, ColumnFamilyConfiguration>();
    private Map<String, ConnectionPoolConfiguration> connectionPools = new HashMap<String, ConnectionPoolConfiguration>();
    private Map<String, EndpointConfiguration> endpoints = new HashMap<String, EndpointConfiguration>();
    private ColumnFamilyConfiguration defaultCFConfiguration;
    private ConnectionPoolConfiguration defaultConnectonPoolConfiguration = new ConnectionPoolConfiguration();

    private CacheConfiguration cacheConfiguration;

    private Object specificConfig;



    private AccessControlManager.AccessControlPolicy defaultAccessControlPolicy = AccessControlManager.AccessControlPolicy.RESTRICTIVE;

    public String getDomainClassesBasePackage() {
        return domainClassesBasePackage;
    }

    public void setDomainClassesBasePackage(String domainClassesBasePackage) {
        this.domainClassesBasePackage = domainClassesBasePackage;
    }

    public String getBindersBasePackage() {
        return bindersBasePackage;
    }

    public void setBindersBasePackage(String bindersBasePackage) {
        this.bindersBasePackage = bindersBasePackage;
    }

    public String getValidatorsBasePackage() {
        return validatorsBasePackage;
    }

    public void setValidatorsBasePackage(String validatorsBasePackage) {
        this.validatorsBasePackage = validatorsBasePackage;
    }

    public boolean isAutoGenerateSchema() {
        return autoGenerateSchema;
    }

    public void setAutoGenerateSchema(boolean autoGenerateSchema) {
        this.autoGenerateSchema = autoGenerateSchema;
    }

    public Map<String, ColumnFamilySchemaDefinition> getColumnFamiliesSchema() {
        return columnFamiliesSchema;
    }

    public void setColumnFamiliesSchema(Map<String, ColumnFamilySchemaDefinition> columnFamiliesSchema) {
        this.columnFamiliesSchema = columnFamiliesSchema;
    }

    public Map<String, KeyspaceSchemaDefinition> getKeyspaces() {
        return keyspaces;
    }

    public void setKeyspaces(Map<String, KeyspaceSchemaDefinition> keyspaces) {
        this.keyspaces = keyspaces;
    }

    public Map<String, ColumnFamilyConfiguration> getColumnFamilies() {
        return columnFamilies;
    }

    public void setColumnFamilies(Map<String, ColumnFamilyConfiguration> columnFamilies) {
        this.columnFamilies = columnFamilies;
    }

    public Map<String, ConnectionPoolConfiguration> getConnectionPools() {
        return connectionPools;
    }

    public void setConnectionPools(Map<String, ConnectionPoolConfiguration> connectionPools) {
        this.connectionPools = connectionPools;
    }

    public Map<String, EndpointConfiguration> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Map<String, EndpointConfiguration> endpoints) {
        this.endpoints = endpoints;
    }

    @SuppressWarnings("unchecked")
    public <T> T getSpecificConfig() {
        return (T)specificConfig;
    }

    public void setSpecificConfig(Object specificConfig) {
        this.specificConfig = specificConfig;
    }


    public ColumnFamilyConfiguration getDefaultCFConfiguration() {
        return defaultCFConfiguration;
    }

    public void setDefaultCFConfiguration(ColumnFamilyConfiguration defaultCFConfiguration) {
        this.defaultCFConfiguration = defaultCFConfiguration;
    }

    public ConnectionPoolConfiguration getDefaultConnectonPoolConfiguration() {
        return defaultConnectonPoolConfiguration;
    }

    public void setDefaultConnectonPoolConfiguration(ConnectionPoolConfiguration defaultConnectonPoolConfiguration) {
        this.defaultConnectonPoolConfiguration = defaultConnectonPoolConfiguration;
    }

    public CacheConfiguration getCacheConfiguration() {
        return cacheConfiguration;
    }

    public void setCacheConfiguration(CacheConfiguration cacheConfiguration) {
        this.cacheConfiguration = cacheConfiguration;
    }

    public AccessControlManager.AccessControlPolicy getDefaultAccessControlPolicy() {
        return defaultAccessControlPolicy;
    }

    public void setDefaultAccessControlPolicy(AccessControlManager.AccessControlPolicy defaultAccessControlPolicy) {
        this.defaultAccessControlPolicy = defaultAccessControlPolicy;
    }

    /**
     * Maximum allowed size of data for PUT request in bytes.
     * If config lacks of such field, getter will return null. So client of this method can check on null to indicate
     * that some default value should be specified.
     *
     * @return maximum allowed size of data for PUT request in bytes
     */
    public Integer getMaxDataSizeForPut() {
        return maxDataSizeForPut;
    }

    public void setMaxDataSizeForPut(Integer maxDataSizeForPut) {
        this.maxDataSizeForPut = maxDataSizeForPut;
    }

    public static String[] getCorePackages() {
        return Arrays.copyOf(corePackages, corePackages.length);
    }

    public static class KeyspaceSchemaDefinition {
        private String name;
        private int replicationFactor = 1;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getReplicationFactor() {
            return replicationFactor;
        }

        public void setReplicationFactor(int replicationFactor) {
            this.replicationFactor = replicationFactor;
        }
    }

    /**
     * Note the presence of {@link #setDefaultConsistencyLevel} method, but the absence of defaultConsistencyLevel field.
     */
    public static class ConnectionPoolConfiguration {
        private String name;
        private String keyspaceName;
        private String[] seeds = new String[]{ "localhost:9160" }; // hosts
        private int maxConnsPerHost = 20; // maxActive
        private int maxTimeoutWhenExhausted = 4000; // maxWaitTimeWhenExhausted
        private int socketTimeout = 15000; // socketTimeout
        private int connectTimeout = 2000; // maxConnectTimeMillis = -1 in hector, -1 is not working for core, using core default
        private int timeoutWindow = 500; // hostTimeoutWindow
        private int maxTimeoutCount = 10; // hostTimeoutCounter
        private int maxFailoverCount = 2; // if first attempt is failed, one more will be performed; in hector enum FailoverPolicy (default ON_FAIL_TRY_ONE_NEXT_AVAILABLE) with int field numRetries
        private boolean useConnectionPoolMonitor = false;

        private ConsistencyLevel readConsistencyLevel = ConsistencyLevel.CL_ONE; // in hector defaultConsistencyLevel for both read / write
        private ConsistencyLevel writeConsistencyLevel = ConsistencyLevel.CL_ONE; // compatible, but bad value, CL_QUORUM is recommended
        private int discoveryDelayInSeconds = 60; // autoDiscoveryDelayInSeconds, if NodeDiscoveryType.NONE it's ignored
        private boolean autoDiscoverHosts = false; // is converted to NodeDiscoveryType in ClientProvider
        private String localDatacenter = null; // autoDiscoveryDataCenter
        private NodeDiscoveryType nodeDiscoveryType = NodeDiscoveryType.NONE;
        private ConnectionPoolType connectionPoolType = ConnectionPoolType.ROUND_ROBIN;
        private int maxOperationsPerConnection = ConnectionPoolConfigurationImpl.DEFAULT_MAX_OPERATIONS_PER_CONNECTION;

        // RetryPolicy configuration
        private RetryPolicyType retryPolicyType = RetryPolicyType.RUN_ONCE;
        private Integer maxAttemptCount;
        private Integer sleepTimeMs;
        private Integer maxSleepTimeMs;

        // Cassandra Authentication
        private String authKey = null;
        private String userId = null;
        private String password = null;

        public String getAuthKey() {
            return authKey;
        }

        public void setAuthKey(String key) {
            this.authKey = key;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public AuthenticationCredentials getAuthenticationCredentials() {
            if (userId == null) {
                return null;
            }

            String decryptedUser = userId;
            String decryptedPassword = password;

            return new SimpleAuthenticationCredentials(decryptedUser, decryptedPassword);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getKeyspaceName() {
            return keyspaceName;
        }

        public void setKeyspaceName(String keyspaceName) {
            this.keyspaceName = keyspaceName;
        }

        public String[] getSeeds() {
            return Arrays.copyOf(seeds, seeds.length);
        }

        public void setSeeds(String[] seeds) {
            this.seeds = Arrays.copyOf(seeds, seeds.length);
        }

        public int getMaxConnsPerHost() {
            return maxConnsPerHost;
        }

        public void setMaxConnsPerHost(int maxConnsPerHost) {
            this.maxConnsPerHost = maxConnsPerHost;
        }

        public int getMaxTimeoutWhenExhausted() {
            return maxTimeoutWhenExhausted;
        }

        public void setMaxTimeoutWhenExhausted(int maxTimeoutWhenExhausted) {
            this.maxTimeoutWhenExhausted = maxTimeoutWhenExhausted;
        }

        public int getSocketTimeout() {
            return socketTimeout;
        }

        public void setSocketTimeout(int socketTimeout) {
            this.socketTimeout = socketTimeout;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getTimeoutWindow() {
            return timeoutWindow;
        }

        public void setTimeoutWindow(int timeoutWindow) {
            this.timeoutWindow = timeoutWindow;
        }

        public int getMaxTimeoutCount() {
            return maxTimeoutCount;
        }

        public void setMaxTimeoutCount(int maxTimeoutCount) {
            this.maxTimeoutCount = maxTimeoutCount;
        }


        public boolean isUseConnectionPoolMonitor() {
            return useConnectionPoolMonitor;
        }

        public void setUseConnectionPoolMonitor(boolean useConnectionPoolMonitor) {
            this.useConnectionPoolMonitor = useConnectionPoolMonitor;
        }

        public int getMaxFailoverCount() {
            return maxFailoverCount;
        }

        public void setMaxFailoverCount(int maxFailoverCount) {
            this.maxFailoverCount = maxFailoverCount;
        }

        public ConsistencyLevel getReadConsistencyLevel() {
            return readConsistencyLevel;
        }

        public void setReadConsistencyLevel(ConsistencyLevel readConsistencyLevel) {
            this.readConsistencyLevel = readConsistencyLevel;
        }

        public ConsistencyLevel getWriteConsistencyLevel() {
            return writeConsistencyLevel;
        }

        public void setWriteConsistencyLevel(ConsistencyLevel writeConsistencyLevel) {
            this.writeConsistencyLevel = writeConsistencyLevel;
        }

        private void setDefaultConsistencyLevel(ConsistencyLevel defaultConsistencyLevel) {
            setWriteConsistencyLevel(defaultConsistencyLevel);
            setReadConsistencyLevel(defaultConsistencyLevel);
        }

        public int getDiscoveryDelayInSeconds() {
            return discoveryDelayInSeconds;
        }

        public void setDiscoveryDelayInSeconds(int discoveryDelayInSeconds) {
            this.discoveryDelayInSeconds = discoveryDelayInSeconds;
        }

        public boolean isAutoDiscoverHosts() {
            return autoDiscoverHosts;
        }

        public void setAutoDiscoverHosts(boolean autoDiscoverHosts) {
            this.autoDiscoverHosts = autoDiscoverHosts;
        }

        public String getLocalDatacenter() {
            return localDatacenter;
        }

        public void setLocalDatacenter(String localDatacenter) {
            this.localDatacenter = localDatacenter;
        }

        /**
         * Following retry policies are supported:
         * INDEFINITE - corresponds to {@link com.netflix.astyanax.retry.IndefiniteRetry} class,
         * RETRY_NTIMES - corresponds to {@link com.netflix.astyanax.retry.RetryNTimes} class,
         * RUN_ONCE - corresponds to {@link com.netflix.astyanax.retry.RunOnce} class,
         * CONSTANT_BACKOFF - corresponds to {@link com.netflix.astyanax.retry.ConstantBackoff} class,
         * EXPONENTIAL_BACKOFF - corresponds to {@link com.netflix.astyanax.retry.ExponentialBackoff} class,
         * BOUNDED_EXPONENTIAL_BACKOFF - corresponds to {@link com.netflix.astyanax.retry.BoundedExponentialBackoff} class
         *
         * @return RetryPolicyType type
         */
        public RetryPolicyType getRetryPolicyType() {
            return retryPolicyType;
        }

        public void setRetryPolicyType(RetryPolicyType retryPolicyType) {
            this.retryPolicyType = retryPolicyType;
        }

        /**
         * This getter makes sense for CONSTANT_BACKOFF, EXPONENTIAL_BACKOFF, BOUNDED_EXPONENTIAL_BACKOFF
         * and RETRY_NTIMES retry policy type
         * @return max retry attempts count
         */
        public Integer getMaxAttemptCount() {
            return maxAttemptCount;
        }

        public void setMaxAttemptCount(Integer maxAttemptCount) {
            this.maxAttemptCount = maxAttemptCount;
        }

        /**
         * This getter makes sense for CONSTANT_BACKOFF, EXPONENTIAL_BACKOFF, BOUNDED_EXPONENTIAL_BACKOFF
         * retry policy type
         * @return sleep time between retries
         */
        public Integer getSleepTimeMs() {
            return sleepTimeMs;
        }

        public void setSleepTimeMs(Integer sleepTimeMs) {
            this.sleepTimeMs = sleepTimeMs;
        }

        /**
         * This getter makes sense only for BOUNDED_EXPONENTIAL_BACKOFF retry policy type
         * @return max amount of sleep time between retries
         */
        public Integer getMaxSleepTimeMs() {
            return maxSleepTimeMs;
        }

        public void setMaxSleepTimeMs(Integer maxSleepTimeMs) {
            this.maxSleepTimeMs = maxSleepTimeMs;
        }

        public NodeDiscoveryType getNodeDiscoveryType() {
            return nodeDiscoveryType;
        }

        public void setNodeDiscoveryType(NodeDiscoveryType nodeDiscoveryType) {
            this.nodeDiscoveryType = nodeDiscoveryType;
        }

        public ConnectionPoolType getConnectionPoolType() {
            return connectionPoolType;
        }

        public void setConnectionPoolType(ConnectionPoolType connectionPoolType) {
            this.connectionPoolType = connectionPoolType;
        }

        public int getMaxOperationsPerConnection() {
            return maxOperationsPerConnection;
        }

        public void setMaxOperationsPerConnection(int maxOperationsPerConnection) {
            this.maxOperationsPerConnection = maxOperationsPerConnection;
        }
    }

    public static class ColumnFamilyConfiguration {
        private String name;
        private String keyspaceName;
        private String connectionPoolName;
        private int pageSize = 256;

        // APPDS-356    compactionStrategy, caching, readRepairChance, minCompactionThreshold, maxCompactionThreshold,
        // replicateOnWrite are moved from ColumnFamilySchemaDefinition for using in service-config.json.
        // For Cassandra version 1.1 and upper - parameters are marked as deprecated - see at http://www.datastax.com/docs/1.1/operations/tuning
        // Deprecated parameters are: "rowCacheSize", "keyCacheSize", "rowCacheSavePeriodInSeconds" and "keyCacheSavePeriodInSeconds". "caching" parameter should be used instead.
        private String compactionStrategy = ColumnFamilyDefinitionDefaults.DEFAULT_COMPACTION_STRATEGY;
        private double readRepairChance = ColumnFamilyDefinitionDefaults.DEFAULT_READ_REPAIR_CHANCE;
        private int minCompactionThreshold = ColumnFamilyDefinitionDefaults.DEFAULT_MIN_COMPACTION_THRESHOLD;
        private int maxCompactionThreshold = ColumnFamilyDefinitionDefaults.DEFAULT_MAX_COMPACTION_THRESHOLD;
        private boolean replicateOnWrite = ColumnFamilyDefinitionDefaults.DEFAULT_REPLICATE_ON_WRITE;
        private CFMetaData.Caching caching = ColumnFamilyDefinitionDefaults.DEFAULT_CACHING;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getConnectionPoolName() {
            return connectionPoolName;
        }

        public void setConnectionPoolName(String connectionPoolName) {
            this.connectionPoolName = connectionPoolName;
        }

        public String getKeyspaceName() {
            return keyspaceName;
        }

        public void setKeyspaceName(String keyspaceName) {
            this.keyspaceName = keyspaceName;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public String getCompactionStrategy() {
            return compactionStrategy;
        }

        public void setCompactionStrategy(String compactionStrategy) {
            this.compactionStrategy = compactionStrategy;
        }

        public double getReadRepairChance() {
            return readRepairChance;
        }

        public void setReadRepairChance(double readRepairChance) {
            this.readRepairChance = readRepairChance;
        }

        public int getMinCompactionThreshold() {
            return minCompactionThreshold;
        }

        public void setMinCompactionThreshold(int minCompactionThreshold) {
            this.minCompactionThreshold = minCompactionThreshold;
        }

        public int getMaxCompactionThreshold() {
            return maxCompactionThreshold;
        }

        public void setMaxCompactionThreshold(int maxCompactionThreshold) {
            this.maxCompactionThreshold = maxCompactionThreshold;
        }

        public boolean isReplicateOnWrite() {
            return replicateOnWrite;
        }

        public void setReplicateOnWrite(boolean replicateOnWrite) {
            this.replicateOnWrite = replicateOnWrite;
        }

        public CFMetaData.Caching getCaching() {
            return caching;
        }

        public void setCaching(CFMetaData.Caching caching) {
            this.caching = caching;
        }
    }

    public static class EndpointConfiguration {
        private String name;
        private int maxResults = 300;
        private int maxObjectFieldSizeForGet = 0;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getMaxResults() {
            return maxResults;
        }

        public void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
        }

        public int getMaxObjectFieldSizeForGet() {
            return maxObjectFieldSizeForGet;
        }

        public void setMaxObjectFieldSizeForGet(int maxObjectFieldSizeForGet) {
            this.maxObjectFieldSizeForGet = maxObjectFieldSizeForGet;
        }
    }

    public static class CacheConfiguration {
        private int retryCountUntilFullRefresh = 10;
        private int changedKeysTimeWindowSize = 900000;
        private boolean reloadCacheEntries = false;
        private int reloadCacheEntriesTimeout = 1;
        private TimeUnit reloadCacheEntriesTimeUnit = TimeUnit.DAYS;
        private int numberOfEntriesToProcessSequentially = 10000;
        private int keysetChunkSizeForMassCacheLoad = 500;
        private int tickDuration = 60000;

        public int getRetryCountUntilFullRefresh() {
            return retryCountUntilFullRefresh;
        }

        public void setRetryCountUntilFullRefresh(int retryCountUntilFullRefresh) {
            this.retryCountUntilFullRefresh = retryCountUntilFullRefresh;
        }

        public int getChangedKeysTimeWindowSize() {
            return changedKeysTimeWindowSize;
        }

        public void setChangedKeysTimeWindowSize(int changedKeysTimeWindowSize) {
            this.changedKeysTimeWindowSize = changedKeysTimeWindowSize;
        }

        public boolean isReloadCacheEntries() {
            return reloadCacheEntries;
        }

        public void setReloadCacheEntries(boolean reloadCacheEntries) {
            this.reloadCacheEntries = reloadCacheEntries;
        }

        public int getReloadCacheEntriesTimeout() {
            return reloadCacheEntriesTimeout;
        }

        public void setReloadCacheEntriesTimeout(int reloadCacheEntriesTimeout) {
            this.reloadCacheEntriesTimeout = reloadCacheEntriesTimeout;
        }

        public TimeUnit getReloadCacheEntriesTimeUnit() {
            return reloadCacheEntriesTimeUnit;
        }

        public void setReloadCacheEntriesTimeUnit(TimeUnit reloadCacheEntriesTimeUnit) {
            this.reloadCacheEntriesTimeUnit = reloadCacheEntriesTimeUnit;
        }

        public int getNumberOfEntriesToProcessSequentially() {
            return numberOfEntriesToProcessSequentially;
        }

        public void setNumberOfEntriesToProcessSequentially(int numberOfEntriesToProcessSequentially) {
            this.numberOfEntriesToProcessSequentially = numberOfEntriesToProcessSequentially;
        }

        public int getKeysetChunkSizeForMassCacheLoad() {
            return keysetChunkSizeForMassCacheLoad;
        }

        public void setKeysetChunkSizeForMassCacheLoad(int keysetChunkSizeForMassCacheLoad) {
            this.keysetChunkSizeForMassCacheLoad = keysetChunkSizeForMassCacheLoad;
        }

        public int getTickDuration() {
            return tickDuration;
        }

        public void setTickDuration(int tickDuration) {
            this.tickDuration = tickDuration;
        }
    }
}
