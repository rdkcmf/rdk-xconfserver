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
 * Copyright 2011 Netflix
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

/*
 * Author: obaturynskyi
 * Created: 18.05.2015  19:50
 */
package com.comcast.hesperius.dataaccess.support.astyanax;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.apache.cassandra.service.CassandraDaemon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

public class EmbCassandra {
    private static final Logger logger = LoggerFactory.getLogger(EmbCassandra.class);

    public static final int DEFAULT_PORT = 9180;
    public static final int DEFAULT_STORAGE_PORT = 9001;

    private final CassandraDaemon cassandra;
    private final File dataDir;

    public EmbCassandra() throws IOException {
        this(createTempDir(), "TestCluster", DEFAULT_PORT, DEFAULT_STORAGE_PORT);
    }

    public EmbCassandra(String dataDir) throws IOException {
        this(new File(dataDir), "TestCluster", DEFAULT_PORT, DEFAULT_STORAGE_PORT);
    }

    public EmbCassandra(File dataDir) throws IOException {
        this(dataDir, "TestCluster", DEFAULT_PORT, DEFAULT_STORAGE_PORT);
    }

    private static File createTempDir() {
        File tempDir = Files.createTempDir();
        tempDir.deleteOnExit();
        return tempDir;
    }

    public EmbCassandra(File dataDir, String clusterName, int port, int storagePort) throws IOException {
        logger.info("Starting cassandra in dir " + dataDir);
        this.dataDir = dataDir;
        dataDir.mkdirs();

        URL templateUrl = EmbCassandra.class.getClassLoader().getResource("cassandra-template.yaml");
        Preconditions.checkNotNull(templateUrl, "Cassandra config template is null");
        String baseFile = Resources.toString(templateUrl, Charset.defaultCharset());

        String newFile = baseFile.replace("$DIR$", dataDir.getPath());
        newFile = newFile.replace("$PORT$", Integer.toString(port));
        newFile = newFile.replace("$STORAGE_PORT$", Integer.toString(storagePort));
        newFile = newFile.replace("$CLUSTER$", clusterName);

        File configFile = new File(dataDir, "cassandra.yaml");
        Files.write(newFile, configFile, Charset.defaultCharset());

        logger.info("Cassandra config file: " + configFile.getPath());
        System.setProperty("cassandra.config", "file:" + configFile.getPath());

        try {
            cassandra = new CassandraDaemon();
            cassandra.init(null);
        } catch (IOException e) {
            logger.error("Error initializing embedded cassandra", e);
            throw e;
        }

        logger.info("Started cassandra deamon");
    }

    public void start() {
        cassandra.start();
    }

    public void stop() {
        cassandra.deactivate();
    }
}
