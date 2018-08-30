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

import java.util.Arrays;

public class ColumnFamilySchemaDefinition {
    private String columnFamilyName;
    private String keyValidationClass; // row key
    private String comparatorType;     // column name
    private Index[] indexes;           // cassandra secondary indexes

    // APPDS-356    compactionStrategy, caching, readRepairChance, minCompactionThreshold, maxCompactionThreshold,
    // replicateOnWrite are moved to ColumnFamilyConfiguration for using in service-config.json.
    // For Cassandra version 1.1 and upper - parameters are marked as deprecated - see at http://www.datastax.com/docs/1.1/operations/tuning
    // Deprecated parameters are: "rowCacheSize", "keyCacheSize", "rowCacheSavePeriodInSeconds" and "keyCacheSavePeriodInSeconds". "caching" parameter should be used instead.

    private double readRepairChance = ColumnFamilyDefinitionDefaults.DEFAULT_READ_REPAIR_CHANCE;
    private int gcGraceSeconds = ColumnFamilyDefinitionDefaults.DEFAULT_GC_GRACE_SECONDS;

    public String getColumnFamilyName() {
        return columnFamilyName;
    }

    public void setColumnFamilyName(String columnFamilyName) {
        this.columnFamilyName = columnFamilyName;
    }

    public String getKeyValidationClass() {
        return keyValidationClass;
    }

    public void setKeyValidationClass(String keyValidationClass) {
        this.keyValidationClass = keyValidationClass;
    }

    public String getComparatorType() {
        return comparatorType;
    }

    public void setComparatorType(String comparatorType) {
        this.comparatorType = comparatorType;
    }

    public Index[] getIndexes() {
        return Arrays.copyOf(indexes, indexes.length);
    }

    public void setIndexes(Index[] indexes) {
        this.indexes = Arrays.copyOf(indexes, indexes.length);
    }

    public double getReadRepairChance() {
        return readRepairChance;
    }

    public void setReadRepairChance(double readRepairChance) {
        this.readRepairChance = readRepairChance;
    }

    public int getGcGraceSeconds() {
        return gcGraceSeconds;
    }

    public void setGcGraceSeconds(int gcGraceSeconds) {
        this.gcGraceSeconds = gcGraceSeconds;
    }

    public static class Index {
        private String columnName;
        private String validationClass;

        public String getColumnName() {
            return columnName;
        }

        public void setColumnName(String columnName) {
            this.columnName = columnName;
        }

        public String getValidationClass() {
            return validationClass;
        }

        public void setValidationClass(String validationClass) {
            this.validationClass = validationClass;
        }
    }
}
