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

import org.apache.cassandra.config.CFMetaData;

/**
 * Default properties for creating ColumnFamily.
 * See http://www.datastax.com/docs/1.1/configuration/storage_configuration for details.
 */
public interface ColumnFamilyDefinitionDefaults {

    // APPDS-356 Deprecated parameters are: "rowCacheSize", "keyCacheSize", "rowCacheSavePeriodInSeconds" and "keyCacheSavePeriodInSeconds". "caching" parameter should be used instead.
    // see at http://www.datastax.com/docs/1.1/operations/tuning
    CFMetaData.Caching DEFAULT_CACHING = CFMetaData.Caching.KEYS_ONLY;
    String DEFAULT_COMPACTION_STRATEGY = "SizeTieredCompactionStrategy";
    double DEFAULT_READ_REPAIR_CHANCE = 1.0;
    boolean DEFAULT_REPLICATE_ON_WRITE = true;
    int DEFAULT_GC_GRACE_SECONDS = 864000;
    int DEFAULT_MIN_COMPACTION_THRESHOLD = 4;
    int DEFAULT_MAX_COMPACTION_THRESHOLD = 32;
}
