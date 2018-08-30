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

import com.comcast.apps.healthcheck.HealthCheckUtils;
import com.comcast.apps.healthcheck.IHeartBeat;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * User: ikostrov
 * Date: 04.12.14
 * Time: 21:33
 */
public class ConnectionChecker implements IHeartBeat {

    private static final Logger log = LoggerFactory.getLogger(ConnectionChecker.class);

    /**
     * Connection state is verified based on 'keyspace.describeKeyspace' method.
     * @return Ok or Failure
     */
    @Override
    public String checkHeartBeat() {
        Set<Keyspace> set = new HashSet<>(BaseDAO.keyspaces.values());
        for (Keyspace keyspace : set) {
            try {
                keyspace.describeKeyspace();
            } catch (ConnectionException e) {
                log.error("Cassandra is down: couldn't describe keyspace: ", e.getMessage());
                return HealthCheckUtils.FAILURE;
            }
        }
        return HealthCheckUtils.OK;
    }
}
