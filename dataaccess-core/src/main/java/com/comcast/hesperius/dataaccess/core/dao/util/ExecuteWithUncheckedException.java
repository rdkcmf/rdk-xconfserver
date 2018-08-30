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

import com.comcast.hesperius.dataaccess.core.AstyanaxException;
import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.astyanax.Cluster;
import com.netflix.astyanax.Execution;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.BadRequestException;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.ddl.ColumnFamilyDefinition;
import com.netflix.astyanax.ddl.KeyspaceDefinition;
import com.netflix.astyanax.ddl.SchemaChangeResult;
import com.netflix.astyanax.partitioner.Partitioner;
import com.netflix.astyanax.recipes.reader.AllRowsReader;

/**
 * Class executes Cassandra operations. On exception wraps it to unchecked exception and rethrows.
 */
public class ExecuteWithUncheckedException {

    public static <R> R execute(Execution<R> execution) {
        try {
            return execution.execute().getResult();
        } catch (ConnectionException ex) {
            throw new AstyanaxException(ex);
        }
    }

    public static <R> ListenableFuture<OperationResult<R>> executeAsync(Execution<R> execution) {
        try {
            return execution.executeAsync();
        } catch (ConnectionException ex) {
            throw new AstyanaxException(ex);
        }
    }

    public static Boolean execute(AllRowsReader<?, ?> allRowsReader) {
        try {
            return allRowsReader.call();
        } catch(ConnectionException ex) {
            throw new AstyanaxException(ex);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @return KeyspaceDefinition if keyspace exists, null otherwise
     */
    public static KeyspaceDefinition describeKeyspace(Keyspace keyspace) {
        try {
            return keyspace.describeKeyspace();
        } catch (BadRequestException ex) { // workaround for https://github.com/Netflix/core/issues/382
            return null;
        } catch (ConnectionException ex) {
            throw new AstyanaxException(ex);
        }
    }

    public static Partitioner getPartitioner(Keyspace keyspace) {
        try {
            return keyspace.getPartitioner();
        } catch (ConnectionException ex) {
            throw new AstyanaxException(ex);
        }
    }

    public static void truncateColumnFamily(Keyspace keyspace, String columnFamilyName) {
        try {
            keyspace.truncateColumnFamily(columnFamilyName);
        } catch (ConnectionException ex) {
            throw new AstyanaxException(ex);
        }
    }

    // TODO we will probably get rid of Cluster, and Keyspace.createKeyspace() will be used. see https://www.teamccp.com/jira/browse/APPDS-335
    public static OperationResult<SchemaChangeResult> addKeyspace(Cluster cluster, KeyspaceDefinition keyspaceDefinition) {
        try {
            return cluster.addKeyspace(keyspaceDefinition);
        } catch (ConnectionException ex) {
            throw new AstyanaxException(ex);
        }
    }

    // TODO we will probably get rid of Cluster, and Keyspace.createColumnFamily() will be used. see https://www.teamccp.com/jira/browse/APPDS-335
    public static OperationResult<SchemaChangeResult> addColumnFamily(Cluster cluster, ColumnFamilyDefinition columnFamilyDefinition) {
        try {
            return cluster.addColumnFamily(columnFamilyDefinition);
        } catch (ConnectionException ex) {
            throw new AstyanaxException(ex);
        }
    }

    // TODO we will probably get rid of Cluster, and Keyspace.updateColumnFamily() will be used. see https://www.teamccp.com/jira/browse/APPDS-335
    public static OperationResult<SchemaChangeResult> updateColumnFamily(Cluster cluster, ColumnFamilyDefinition columnFamilyDefinition) {
        try {
            return cluster.updateColumnFamily(columnFamilyDefinition);
        } catch (ConnectionException ex) {
            throw new AstyanaxException(ex);
        }
    }

}
