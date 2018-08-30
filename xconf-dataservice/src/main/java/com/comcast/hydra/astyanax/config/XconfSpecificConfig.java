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
package com.comcast.hydra.astyanax.config;

import com.comcast.hesperius.dataaccess.core.config.SpecificConfigBean;

/**
 * Defines Xconf DataService specific section
 * User: ikostrov
 * Date: 16.10.14
 */
@SpecificConfigBean
public class XconfSpecificConfig {

    private String authKey;

    private Integer maxConnections;

    private Integer maxConnectionsPerRoute;

    private Integer requestTimeoutInMs;

    private Integer connectionTimeoutInMs;

    private Integer socketTimeoutInMs;

    private String haProxyHeaderName;

    private boolean enableUpdateDeleteAPI = true; // enabled by default for legacy purposes

    public String getHaProxyHeaderName() {
        return haProxyHeaderName;
    }

    public void setHaProxyHeaderName(String haProxyHeaderName) {
        this.haProxyHeaderName = haProxyHeaderName;
    }

    public boolean isEnableUpdateDeleteAPI() {
        return enableUpdateDeleteAPI;
    }

    public void setEnableUpdateDeleteAPI(boolean enableUpdateDeleteAPI) {
        this.enableUpdateDeleteAPI = enableUpdateDeleteAPI;
    }

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public Integer getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }

    public Integer getMaxConnectionsPerRoute() {
        return maxConnectionsPerRoute;
    }

    public void setMaxConnectionsPerRoute(Integer maxConnectionsPerRoute) {
        this.maxConnectionsPerRoute = maxConnectionsPerRoute;
    }

    public Integer getRequestTimeoutInMs() {
        return requestTimeoutInMs;
    }

    public void setRequestTimeoutInMs(Integer requestTimeoutInMs) {
        this.requestTimeoutInMs = requestTimeoutInMs;
    }

    public Integer getConnectionTimeoutInMs() {
        return connectionTimeoutInMs;
    }

    public void setConnectionTimeoutInMs(Integer connectionTimeoutInMs) {
        this.connectionTimeoutInMs = connectionTimeoutInMs;
    }

    public Integer getSocketTimeoutInMs() {
        return socketTimeoutInMs;
    }

    public void setSocketTimeoutInMs(Integer socketTimeoutInMs) {
        this.socketTimeoutInMs = socketTimeoutInMs;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("XconfSpecificConfig{");
        sb.append(", authKey='").append(authKey).append('\'');
        sb.append(", maxConnections=").append(maxConnections);
        sb.append(", maxConnectionsPerRoute=").append(maxConnectionsPerRoute);
        sb.append(", requestTimeoutInMs=").append(requestTimeoutInMs);
        sb.append(", connectionTimeoutInMs=").append(connectionTimeoutInMs);
        sb.append(", socketTimeoutInMs=").append(socketTimeoutInMs);
        sb.append(", haProxyHeaderName='").append(haProxyHeaderName).append('\'');
        sb.append(", enableUpdateDeleteAPI=").append(enableUpdateDeleteAPI);
        sb.append('}');
        return sb.toString();
    }
}
