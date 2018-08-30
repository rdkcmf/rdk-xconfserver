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
package com.comcast.hesperius.dataaccess.core.dao;

import com.comcast.hesperius.data.annotation.CF;
import com.comcast.hesperius.data.annotation.CompositeCF;
import com.comcast.hesperius.data.annotation.ListingCF;
import com.comcast.hesperius.data.annotation.SchemaProvider;
import com.comcast.hesperius.dataaccess.core.config.ColumnFamilySchemaDefinition;
import com.comcast.hesperius.dataaccess.core.config.ConfigurationProvider;
import com.comcast.hesperius.dataaccess.core.config.DataServiceConfiguration;
import com.comcast.hesperius.dataaccess.core.config.DataServiceConfiguration.ColumnFamilyConfiguration;
import com.comcast.hesperius.dataaccess.core.config.DataServiceConfiguration.ConnectionPoolConfiguration;
import com.comcast.hesperius.dataaccess.core.config.DataServiceConfiguration.KeyspaceSchemaDefinition;
import com.comcast.hesperius.dataaccess.core.dao.util.ClientProvider;
import com.comcast.hesperius.dataaccess.core.dao.util.ExecuteWithUncheckedException;
import com.comcast.hesperius.dataaccess.core.dao.util.KeyspaceDefinitionBuilder;
import com.comcast.hesperius.dataaccess.core.util.AnnotationScanner;
import com.comcast.hesperius.dataaccess.core.util.CoreUtil;
import com.comcast.hesperius.dataaccess.core.util.bean.BeanUtils;
import com.comcast.hydra.astyanax.data.IPersistable;
import com.comcast.hydra.astyanax.util.PersistableFactory;
import com.comcast.hydra.astyanax.util.ReflectionUtils;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.netflix.astyanax.Cluster;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.ddl.ColumnFamilyDefinition;
import com.netflix.astyanax.ddl.KeyspaceDefinition;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.serializers.ComparatorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * This class represents the base class for all data access objects.  It contains the code to:
 * - Manage a cluster for each of the endpoints in the data service.  This class makes no
 * assumptions about the data model.
 * - Manage a Cassandra keyspace for each endpoint in the data service.
 * - Create the keyspace the first time it is used.
 * - Perform common operations needed by subclasses.
 */
public abstract class BaseDAO<K, N, T extends IPersistable> implements IBaseDAO<T> {

    private static Logger log = LoggerFactory.getLogger(BaseDAO.class);

    static Map<String, Keyspace> keyspaces = new HashMap<String, Keyspace>();

    protected final ColumnFamily<K, N> columnFamily;

    protected final Serializer<K> keySerializer;

    protected final Serializer<N> nameSerializer;

    protected final IPersistable.Factory<T> factory;

    static final Class[] SUPPORTED_DOMAIN_ANNOTATIONS = new Class[]{CF.class, CompositeCF.class, ListingCF.class};

    static {
        final DataServiceConfiguration dsConfig = ConfigurationProvider.getConfiguration();
        final Set<ColumnFamilySchemaDefinition> knownSchemas = getDefinedSchemas(dsConfig);
        if (dsConfig.isAutoGenerateSchema()) {
            createSchemaData(knownSchemas, dsConfig);
        } else {
            initSchemaData(knownSchemas, dsConfig);
        }
    }

    public BaseDAO(final String columnFamilyName, final Serializer<K> keySerializer, final Serializer<N> nameSerializer, Class<T> entityClass) {
        this(columnFamilyName, keySerializer, nameSerializer, new PersistableFactory<T>(entityClass));
    }

    public BaseDAO(final String columnFamilyName, final Serializer<K> keySerializer, final Serializer<N> nameSerializer, IPersistable.Factory<T> factory) {
        this.keySerializer = Preconditions.checkNotNull(keySerializer);
        this.nameSerializer = Preconditions.checkNotNull(nameSerializer);
        this.factory = Preconditions.checkNotNull(factory);
        this.columnFamily = new ColumnFamily<K, N>(columnFamilyName, keySerializer, nameSerializer);
    }

    private static void createSchemaData(final Iterable<ColumnFamilySchemaDefinition> schemas, final DataServiceConfiguration dsConfig) {
        /**
         * no locks here
         */
        log.info("Attempting to create/use schema");
        for (final ColumnFamilySchemaDefinition schemaDefinition : schemas) {

            final String cfname = schemaDefinition.getColumnFamilyName();
            final ColumnFamilyConfiguration cfConfig = findColumnFamilyConfigurationByName(dsConfig, cfname);
            final String keySpaceName = cfConfig.getKeyspaceName();
            final ConnectionPoolConfiguration connPoolConfig = findConnectionConfigurationForColumnFamily(dsConfig, cfname);


            log.info("Initializing {}::{}",keySpaceName, cfname);
            String connPoolName = cfConfig.getConnectionPoolName();
            Cluster cluster = ClientProvider.getOrCreateClusterClient(connPoolName, connPoolConfig);

            Keyspace keyspace = ClientProvider.getOrCreateKeyspaceClient(keySpaceName, connPoolConfig);
            keyspaces.put(cfname, keyspace);

            final KeyspaceDefinition keyspaceDef = ExecuteWithUncheckedException.describeKeyspace(keyspace);

            if (keyspaceDef == null) {
                ColumnFamilyDefinition cfDef = createColumnFamilyDefinition(dsConfig, schemaDefinition);

                log.info("No keyspace definition found. Creating one for keyspace name {}", keySpaceName);
                KeyspaceDefinition newKeyspace = createKeyspaceDefinition(cluster.makeKeyspaceDefinition(), dsConfig.getKeyspaces().get(keySpaceName));
                newKeyspace.addColumnFamily(cfDef);
                ExecuteWithUncheckedException.addKeyspace(cluster, newKeyspace);

            } else {
                ColumnFamilyDefinition cfDef = keyspaceDef.getColumnFamily(cfname);
                if (cfDef == null) {
                    log.info("Creating {}::{}", keySpaceName, cfname);
                    cfDef = createColumnFamilyDefinition(dsConfig, schemaDefinition);
                    ExecuteWithUncheckedException.addColumnFamily(cluster, cfDef);
                } else {
                    cfDef.setCompactionStrategy(cfConfig.getCompactionStrategy());
                    cfDef.setReadRepairChance(cfConfig.getReadRepairChance());
                    cfDef.setMinCompactionThreshold(cfConfig.getMinCompactionThreshold());
                    cfDef.setMaxCompactionThreshold(cfConfig.getMaxCompactionThreshold());
                    cfDef.setReplicateOnWrite(cfConfig.isReplicateOnWrite());
                    cfDef.setCaching(cfConfig.getCaching().name());
                    ExecuteWithUncheckedException.updateColumnFamily(cluster, cfDef);
                }
            }
        }
    }

    private static final ConnectionPoolConfiguration findConnectionConfigurationForColumnFamily(final DataServiceConfiguration dsConfig, final String cfName) {
        ColumnFamilyConfiguration cfConfig = dsConfig.getColumnFamilies().get(cfName);
        if (cfConfig == null) return dsConfig.getDefaultConnectonPoolConfiguration();
        return Optional.fromNullable(dsConfig.getConnectionPools().get(cfConfig.getConnectionPoolName())).or(dsConfig.getDefaultConnectonPoolConfiguration());
    }

    private static final ColumnFamilyConfiguration findColumnFamilyConfigurationByName(final DataServiceConfiguration dsConfig, final String cfName) {
        return Optional.fromNullable(dsConfig.getColumnFamilies().get(cfName)).or(dsConfig.getDefaultCFConfiguration());
    }

    private static final void initSchemaData(final Iterable<ColumnFamilySchemaDefinition> schemas, final DataServiceConfiguration dsConfig) {

        /**
         * we should init configs, clusters, keyspaces and columnFamilyDefinitions maps
         * to properly initialize DAOs. For example creation of columnFamily member requires
         * keyspace initialization
         */
        log.info("Initializing schema");
        for (final ColumnFamilySchemaDefinition schema : schemas) {
            final String cfname = schema.getColumnFamilyName();
            final ColumnFamilyConfiguration cfConfig = findColumnFamilyConfigurationByName(dsConfig, cfname);
            final String keySpaceName = cfConfig.getKeyspaceName();
            final ConnectionPoolConfiguration connPoolConfig = findConnectionConfigurationForColumnFamily(dsConfig, cfname);

            log.info("Initializing {}::{}", keySpaceName, cfname);
            Keyspace keyspace = ClientProvider.getOrCreateKeyspaceClient(keySpaceName, connPoolConfig);
            keyspaces.put(cfname, keyspace);
        }
    }

    private static final KeyspaceDefinition createKeyspaceDefinition(final KeyspaceDefinition keyspaceDefinition, final KeyspaceSchemaDefinition keySpaceSchemaDef) {
        return new KeyspaceDefinitionBuilder(keyspaceDefinition)
                .forKeyspace(keySpaceSchemaDef.getName())
                .withSimpleStrategy()  //TODO implement strategy support
                .withReplicationFactor(keySpaceSchemaDef.getReplicationFactor())
                .build();
    }

    protected static final ColumnFamilyDefinition createColumnFamilyDefinition(final DataServiceConfiguration dsConfig, final ColumnFamilySchemaDefinition cfSchemaDef) {

        final ColumnFamilyConfiguration cfConfig = Optional.fromNullable(dsConfig.getColumnFamilies().get(cfSchemaDef.getColumnFamilyName()))
                .or(dsConfig.getDefaultCFConfiguration());

        final ConnectionPoolConfiguration connPoolConfig = findConnectionConfigurationForColumnFamily(dsConfig, cfSchemaDef.getColumnFamilyName());

        String connPoolName = cfConfig.getConnectionPoolName();
        Cluster cluster = ClientProvider.getOrCreateClusterClient(connPoolName, connPoolConfig);
        ColumnFamilyDefinition cfDef = cluster.makeColumnFamilyDefinition();

        if (cfConfig.getCompactionStrategy() != null) {
            cfDef.setCompactionStrategy(cfConfig.getCompactionStrategy());
        }

        String rowKeyType = cfSchemaDef.getKeyValidationClass();
        String columnNameType = cfSchemaDef.getComparatorType();

        return cfDef.setName(cfSchemaDef.getColumnFamilyName())
                .setKeyspace(cfConfig.getKeyspaceName())
                .setKeyValidationClass(rowKeyType)
                .setComparatorType(columnNameType)
                .setReadRepairChance(cfConfig.getReadRepairChance())
                .setGcGraceSeconds(cfSchemaDef.getGcGraceSeconds())
                .setMaxCompactionThreshold(cfConfig.getMaxCompactionThreshold())
                .setMinCompactionThreshold(cfConfig.getMinCompactionThreshold())
                .setReplicateOnWrite(cfConfig.isReplicateOnWrite())
                .setCaching(cfConfig.getCaching().name());
    }

    public Keyspace getKeyspace() {
        return keyspaces.get(getColumnFamilyName());
    }

    @Override
    public String getColumnFamilyName() {
        return this.columnFamily.getName();
    }

    @Override
    public IPersistable.Factory<T> getObjectFactory() {
        return factory;
    }

    // CAREFUL!
    @Override
    public void truncateColumnFamily() {
        log.warn("truncating column family {}", getColumnFamilyName());
        ExecuteWithUncheckedException.truncateColumnFamily(getKeyspace(), getColumnFamilyName());
    }

    protected int getFieldCount() {
        return ReflectionUtils.getFieldCount(factory.getClassObject());
    }

    protected int getPageSize() {
        return findColumnFamilyConfigurationByName(ConfigurationProvider.getConfiguration(), getColumnFamilyName()).getPageSize();
    }

    private static Set<ColumnFamilySchemaDefinition> getDefinedSchemas(final DataServiceConfiguration dsConfig) {
        final Set<ColumnFamilySchemaDefinition> result = new HashSet<ColumnFamilySchemaDefinition>();
        final List<String> packages = Lists.newArrayList(DataServiceConfiguration.getCorePackages());
        packages.add(dsConfig.getDomainClassesBasePackage());
        for (Class<?> annotatedClass : AnnotationScanner.getAnnotatedClasses(SUPPORTED_DOMAIN_ANNOTATIONS, packages.toArray(new String[]{}))) {
            if (annotatedClass.isAnnotationPresent(CF.class)) {
                final ColumnFamilySchemaDefinition schema = new ColumnFamilySchemaDefinition();
                final CF domainAnnotation = annotatedClass.getAnnotation(CF.class);

                schema.setColumnFamilyName(domainAnnotation.cfName());
                schema.setKeyValidationClass(BeanUtils.getSerializer(domainAnnotation.keyType()).getComparatorType().getTypeName());
                schema.setComparatorType("".equals(domainAnnotation.comparatorTypeAlias()) ? ComparatorType.UTF8TYPE.getTypeName() : domainAnnotation.comparatorTypeAlias());

                result.add(schema);
            }
            if (annotatedClass.isAnnotationPresent(CompositeCF.class)) {
                final ColumnFamilySchemaDefinition schema = new ColumnFamilySchemaDefinition();
                final CompositeCF domainAnnotation = annotatedClass.getAnnotation(CompositeCF.class);

                schema.setColumnFamilyName(domainAnnotation.cfName());
                schema.setKeyValidationClass(BeanUtils.getSerializer(domainAnnotation.keyType()).getComparatorType().getTypeName());
                schema.setComparatorType(domainAnnotation.comparatorTypeAlias());

                result.add(schema);
            }
            if (annotatedClass.isAnnotationPresent(ListingCF.class)) {
                final ColumnFamilySchemaDefinition schema = new ColumnFamilySchemaDefinition();
                final ListingCF domainAnnotation = annotatedClass.getAnnotation(ListingCF.class);

                schema.setColumnFamilyName(domainAnnotation.cfName());
                schema.setKeyValidationClass(BeanUtils.getSerializer(domainAnnotation.keyType()).getComparatorType().getTypeName());
                schema.setComparatorType("".equals(domainAnnotation.comparatorTypeAlias()) ? ComparatorType.BYTESTYPE.getTypeName() : domainAnnotation.comparatorTypeAlias());

                result.add(schema);
            }
        }

        for (final Class<?> schemaProvider : AnnotationScanner.getAnnotatedClasses(new Class[]{SchemaProvider.class}, DataServiceConfiguration.getCorePackages())) {
            final SchemaProvider providerDef = schemaProvider.getAnnotation(SchemaProvider.class);
            try {
                final Method providerMethod = schemaProvider.getMethod(providerDef.method());
                if (providerMethod != null && Collection.class.isAssignableFrom(providerMethod.getReturnType())) {
                    providerMethod.setAccessible(true);

                    log.info("trying to get schema from {}", schemaProvider.getSimpleName());
                    result.addAll((Collection) providerMethod.invoke(null));
                }
            } catch (IllegalAccessException e) {
                log.error("could not call {} on {} to get schema from it", providerDef.method(), schemaProvider.getSimpleName());
            } catch (InvocationTargetException e) {
                log.error("could not call {} on {} to get schema from it", providerDef.method(), schemaProvider.getSimpleName());
            } catch (NoSuchMethodException e) {
                log.error("Provider {} has no method named {}", schemaProvider.getSimpleName(), providerDef.method());
            }
        }

        return result;
    }

    public static void load() {
        //  Serves as a trigger to load this class and invoke its static section
    }
}
