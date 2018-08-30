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
 */

/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.comcast.hesperius.dataaccess.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.netflix.astyanax.connectionpool.ConnectionPoolMonitor;
import com.netflix.astyanax.connectionpool.Host;
import com.netflix.astyanax.connectionpool.HostConnectionPool;
import com.netflix.astyanax.connectionpool.HostStats;
import com.netflix.astyanax.connectionpool.exceptions.BadRequestException;
import com.netflix.astyanax.connectionpool.exceptions.HostDownException;
import com.netflix.astyanax.connectionpool.exceptions.InterruptedOperationException;
import com.netflix.astyanax.connectionpool.exceptions.NoAvailableHostsException;
import com.netflix.astyanax.connectionpool.exceptions.NotFoundException;
import com.netflix.astyanax.connectionpool.exceptions.OperationTimeoutException;
import com.netflix.astyanax.connectionpool.exceptions.PoolTimeoutException;
import com.netflix.astyanax.connectionpool.exceptions.TimeoutException;
import com.netflix.astyanax.connectionpool.exceptions.TransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author: Alexander Pletnev
 */
@XmlRootElement(name = "ConnectionPoolMonitor")
@XmlAccessorType(XmlAccessType.NONE)
public class CustomConnectionPoolMonitor implements ConnectionPoolMonitor {

    private static Logger LOG = LoggerFactory.getLogger(CustomConnectionPoolMonitor.class);

    private static CustomConnectionPoolMonitor INSTANCE = new CustomConnectionPoolMonitor();

    public static CustomConnectionPoolMonitor getInstance() {
        return INSTANCE;
    }

    @XmlElement
    private Hosts hosts = new Hosts();
    @XmlElement
    private Connections connections = new Connections();
    @XmlElement
    private Operations operations = new Operations();

    private AtomicLong badRequestCount = new AtomicLong();
    private AtomicLong notFoundCounter = new AtomicLong();
    private Long startTime = Calendar.getInstance().getTime().getTime();

    private void trackError(Host host, Exception reason) {
        if (reason instanceof PoolTimeoutException) {
            operations.getPoolExhasted().incrementAndGet();
        } else if (reason instanceof TimeoutException) {
            operations.getSocketTimeout().incrementAndGet();
        } else if (reason instanceof OperationTimeoutException) {
            operations.getOperationTimeout().incrementAndGet();
        } else if (reason instanceof BadRequestException) {
            this.badRequestCount.incrementAndGet();
        } else if (reason instanceof NoAvailableHostsException) {
            operations.getNoHosts().incrementAndGet();
        } else if (reason instanceof InterruptedOperationException) {
            operations.getInterrupted().incrementAndGet();
        } else if (reason instanceof HostDownException) {
            hosts.getHostDown().incrementAndGet();
        } else if (reason instanceof TransportException) {
            operations.getTransportError().incrementAndGet();
        } else {
            LOG.error(reason.toString(), reason);
            operations.getUnknownError().incrementAndGet();
        }
    }

    @Override
    public void incOperationFailure(Host host, Exception reason) {
        if (reason instanceof NotFoundException) {
            this.notFoundCounter.incrementAndGet();
            return;
        }

        operations.getOperationFailureCount().incrementAndGet();
        trackError(host, reason);
    }

    public long getOperationFailureCount() {
        return operations.getOperationFailureCount().get();
    }

    @Override
    public void incOperationSuccess(Host host, long latency) {
        operations.getOperationSuccess().incrementAndGet();
    }


    public long getOperationSuccessCount() {
        return operations.getOperationSuccess().get();
    }

    @Override
    public void incConnectionCreated(Host host) {
        connections.getConnectionCreate().incrementAndGet();
    }


    public long getConnectionCreatedCount() {
        return connections.getConnectionCreate().get();
    }

    @Override
    public void incConnectionClosed(Host host, Exception reason) {
        connections.getConnectionClosed().incrementAndGet();
    }


    public long getConnectionClosedCount() {
        return connections.getConnectionClosed().get();
    }

    @Override
    public void incConnectionCreateFailed(Host host, Exception reason) {
        connections.getConnectionCreateFailure().incrementAndGet();
    }


    public long getConnectionCreateFailedCount() {
        return connections.getConnectionCreateFailure().get();
    }

    @Override
    public void incConnectionBorrowed(Host host, long delay) {
        connections.getConnectionBorrow().incrementAndGet();
    }


    public long getConnectionBorrowedCount() {
        return connections.getConnectionBorrow().get();
    }

    @Override
    public void incConnectionReturned(Host host) {
        connections.getConnectionReturn().incrementAndGet();
    }


    @Override
    public long getConnectionReturnedCount() {
        return connections.getConnectionReturn().get();
    }


    @Override
    public long getPoolExhaustedTimeoutCount() {
        return operations.getPoolExhasted().get();
    }

    @Override
    public long getSocketTimeoutCount() {
        return operations.getSocketTimeout().get();
    }


    @Override
    public long getOperationTimeoutCount() {
        return operations.getOperationTimeout().get();
    }

    @Override
    public void incFailover(Host host, Exception reason) {
        operations.getOperationFailover().incrementAndGet();
        trackError(host, reason);
    }

    @Override
    public long getFailoverCount() {
        return operations.getOperationFailover().get();
    }

    @Override
    public void onHostAdded(Host host, HostConnectionPool<?> pool) {
        LOG.info("AddHost: " + host.getHostName());
        hosts.getHostAdded().incrementAndGet();
    }

    @Override
    public long getHostAddedCount() {
        return hosts.getHostAdded().get();
    }

    @Override
    public void onHostRemoved(Host host) {
        LOG.info("RemoveHost: " + host.getHostName());
        hosts.getHostRemoved().incrementAndGet();
    }

    @Override
    public long getHostRemovedCount() {
        return hosts.getHostRemoved().get();
    }

    @Override
    public void onHostDown(Host host, Exception reason) {
        hosts.getHostDown().incrementAndGet();
    }

    @Override
    public long getHostDownCount() {
        return hosts.getHostDown().get();
    }

    @Override
    public void onHostReactivated(Host host, HostConnectionPool<?> pool) {
        LOG.info("Reactivating " + host.getHostName());
        hosts.getHostReactivated().incrementAndGet();
    }

    @Override
    public long getNoHostCount() {
        return operations.getNoHosts().get();
    }

    @Override
    public long getUnknownErrorCount() {
        return operations.getUnknownError().get();
    }

    @Override
    public long getInterruptedCount() {
        return operations.getInterrupted().get();
    }

    @Override
    public long getTransportErrorCount() {
        return operations.getTransportError().get();
    }

    @Override
    @XmlElement(name = "BadRequest")
    public long getBadRequestCount() {
        return this.badRequestCount.get();
    }


    @XmlElement(name = "BusyConnections")
    public long getNumBusyConnections() {
        return connections.getConnectionBorrow().get() - connections.getConnectionReturn().get();
    }


    @XmlElement(name = "OpenConnections")
    public long getNumOpenConnections() {
        return connections.getConnectionCreate().get() - connections.getConnectionClosed().get();
    }

    @Override
    public long notFoundCount() {
        return this.notFoundCounter.get();
    }

    @Override
    @XmlElement(name = "HostCount")
    public long getHostCount() {
        return hosts.getHostAdded().get() - hosts.getHostRemoved().get();
    }

    @Override
    @XmlElement(name = "HostActiveCount")
    public long getHostActiveCount() {
        return hosts.getHostAdded().get() - hosts.getHostRemoved().get() - hosts.getHostReactivated().get() - hosts.getHostDown().get();
    }

    @JsonIgnore
    @Override
    public Map<Host, HostStats> getHostStats() {
        throw new UnsupportedOperationException("Not supported");
    }

    @XmlElement(name = "UptimeMinutes")
    public Long getUptime() {
        Long currTime = Calendar.getInstance().getTime().getTime();
        Long uptime = currTime - startTime;
        return uptime / (60 * 1000);
    }
}

class Connections {
    Connections() {
    }

    private AtomicLong connectionCreateCount = new AtomicLong();
    private AtomicLong connectionClosedCount = new AtomicLong();
    private AtomicLong connectionCreateFailureCount = new AtomicLong();
    private AtomicLong connectionBorrowCount = new AtomicLong();
    private AtomicLong connectionReturnCount = new AtomicLong();


    AtomicLong getConnectionCreate() {
        return connectionCreateCount;
    }

    @XmlElement(name = "Create")
    Long getConnectionCreateValue() {
        return connectionCreateCount.get();
    }

    AtomicLong getConnectionClosed() {
        return connectionClosedCount;
    }

    @XmlElement(name = "Closed")
    Long getConnectionClosedValue() {
        return connectionClosedCount.get();
    }

    AtomicLong getConnectionCreateFailure() {
        return connectionCreateFailureCount;
    }

    @XmlElement(name = "CreateFailure")
    Long getConnectionCreateFailureValue() {
        return connectionCreateFailureCount.get();
    }

    AtomicLong getConnectionBorrow() {
        return connectionBorrowCount;
    }

    @XmlElement(name = "Borrow")
    Long getConnectionBorrowValue() {
        return connectionBorrowCount.get();
    }

    AtomicLong getConnectionReturn() {
        return connectionReturnCount;
    }

    @XmlElement(name = "Return")
    Long getConnectionReturnValue() {
        return connectionReturnCount.get();
    }
}

class Operations {
    Operations() {
    }

    private AtomicLong operationFailureCount = new AtomicLong();
    private AtomicLong operationSuccessCount = new AtomicLong();
    private AtomicLong operationFailoverCount = new AtomicLong();
    private AtomicLong operationTimeoutCount = new AtomicLong();
    private AtomicLong socketTimeoutCount = new AtomicLong();
    private AtomicLong noHostsCount = new AtomicLong();
    private AtomicLong unknownErrorCount = new AtomicLong();
    private AtomicLong interruptedCount = new AtomicLong();
    private AtomicLong poolExhastedCount = new AtomicLong();
    private AtomicLong transportErrorCount = new AtomicLong();

    AtomicLong getOperationFailureCount() {
        return operationFailureCount;
    }

    @XmlElement(name = "Failure")
    Long getOperationFailureCountValue() {
        return operationFailureCount.get();
    }

    AtomicLong getOperationSuccess() {
        return operationSuccessCount;
    }

    @XmlElement(name = "Success")
    Long getOperationSuccessValue() {
        return operationSuccessCount.get();
    }

    AtomicLong getOperationFailover() {
        return operationFailoverCount;
    }

    @XmlElement(name = "Failover")
    Long getOperationFailoverValue() {
        return operationFailoverCount.get();
    }

    AtomicLong getOperationTimeout() {
        return operationTimeoutCount;
    }

    @XmlElement(name = "Timeout")
    Long getOperationTimeoutValue() {
        return operationTimeoutCount.get();
    }

    AtomicLong getSocketTimeout() {
        return socketTimeoutCount;
    }

    @XmlElement(name = "SocketTimeout")
    Long getSocketTimeoutValue() {
        return socketTimeoutCount.get();
    }

    AtomicLong getNoHosts() {
        return noHostsCount;
    }

    @XmlElement(name = "NoHosts")
    Long getNoHostsValue() {
        return noHostsCount.get();
    }

    AtomicLong getUnknownError() {
        return unknownErrorCount;
    }

    @XmlElement(name = "UnknownError")
    Long getUnknownErrorValue() {
        return unknownErrorCount.get();
    }

    AtomicLong getInterrupted() {
        return interruptedCount;
    }

    @XmlElement(name = "Interrupted")
    Long getInterruptedValue() {
        return interruptedCount.get();
    }


    AtomicLong getPoolExhasted() {
        return poolExhastedCount;
    }

    @XmlElement(name = "PoolExhasted")
    Long getPoolExhastedValue() {
        return poolExhastedCount.get();
    }

    AtomicLong getTransportError() {
        return transportErrorCount;
    }

    @XmlElement(name = "TransportError")
    Long getTransportErrorValue() {
        return transportErrorCount.get();
    }
}

class Hosts {
    private AtomicLong hostAddedCount = new AtomicLong();
    private AtomicLong hostRemovedCount = new AtomicLong();
    private AtomicLong hostDownCount = new AtomicLong();
    private AtomicLong hostReactivatedCount = new AtomicLong();

    AtomicLong getHostAdded() {
        return hostAddedCount;
    }

    @XmlElement(name = "Added")
    Long getHostAddedValue() {
        return hostAddedCount.get();
    }

    AtomicLong getHostRemoved() {
        return hostRemovedCount;
    }

    @XmlElement(name = "Removed")
    Long getHostRemovedValue() {
        return hostRemovedCount.get();
    }

    AtomicLong getHostDown() {
        return hostDownCount;
    }

    @XmlElement(name = "Down")
    Long getHostDownValue() {
        return hostDownCount.get();
    }

    AtomicLong getHostReactivated() {
        return hostReactivatedCount;
    }

    @XmlElement(name = "Reactivated")
    Long getHostReactivatedValue() {
        return hostReactivatedCount.get();
    }
}
