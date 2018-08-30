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
package com.comcast.hesperius.dataaccess.core.jmx.config;

import com.comcast.hesperius.dataaccess.core.config.DSConfig;
import com.comcast.hesperius.dataaccess.core.config.Property;
import com.comcast.hesperius.dataaccess.core.config.Source;

/**
 * Default configuration for JMX facility
 * disabled by default and with jmx search package defaulted to widest possible
 */

@Source("hydraJmxProperties.properties")
public interface JMXConfig extends DSConfig {

    @Property(key = "hydra.jmx.enable", defaultValue = "false")
    public String isJmxEnabled();

    @Property(key = "hydra.app.name", defaultValue = "hydraApplication")
    public String getAppName();

    @Property(key = "hydra.app.jmx.package", defaultValue = "com.comcast.hydra")
    public String getAppJMXPackage();
}
