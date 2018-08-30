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
 * Author: phoenix
 * Created: 09/01/2014  15:14
 */
package com.comcast.hesperius.dataaccess.core.dao.util;

import com.comcast.hesperius.data.annotation.CF;

public class CFPersistenceDefinition {
    /**
     * column family name
     */
    public final String cfName;

    /**
     * row key type
     */
    public final Class<?> keyType;

    /**
     * if MarshalingPolicy.WHOLE then persistable object should be written to single column using one of {@link com.comcast.hesperius.data.annotation.CF.Medium} formats
     */
    public final CF.MarshalingPolicy marshalingPolicy;

    /**
     * requires MarshalingPolicy.WHOLE, otherwise should be ignored
     */
    public final CF.Medium complexObjectMedium;

    /**
     * requires MarshalingPolicy.WHOLE, otherwise should be ignored
     */
    public final CF.CompressionPolicy compressionPolicy;

    /**
     * measured in kilobytes
     */
    public final int compressionChunkSize;

    /**
     * ttl for columns this definition produces in seconds, 0 - columns do not expire
     */
    public final int ttl;

    public final String defaultColumnName;

    public CFPersistenceDefinition(String cfName,
                                   Class<?> keyType,
                                   CF.MarshalingPolicy marshalingPolicy,
                                   CF.Medium complexObjectMedium,
                                   CF.CompressionPolicy compressionPolicy,
                                   int compressionChunkSize,
                                   int ttl,
                                   String defaultColumnName) {
        this.cfName = cfName;
        this.keyType = keyType;
        this.marshalingPolicy = marshalingPolicy;
        this.complexObjectMedium = complexObjectMedium;
        this.compressionPolicy = compressionPolicy;
        this.compressionChunkSize = compressionChunkSize;
        this.ttl = ttl;
        this.defaultColumnName = defaultColumnName;
    }

    public CFPersistenceDefinition(String cfName,
                                   Class<?> keyType,
                                   CF.MarshalingPolicy marshalingPolicy,
                                   CF.Medium complexObjectMedium) {
        this(cfName,
                keyType,
                marshalingPolicy,
                complexObjectMedium,
                CF.CompressionPolicy.NONE,
                0,
                0,
                "data");
    }

    public static class Builder {
        private String cfName;
        private Class<?> keyType;
        private CF.MarshalingPolicy marshalingPolicy;
        private CF.Medium complexObjectMedium;
        private CF.CompressionPolicy compressionPolicy = CF.CompressionPolicy.NONE;
        private int compressionChunkSize = 0;
        private int ttl = 0;
        private String defaultColumnName = "data";

        public Builder setCfName(String cfName) {
            this.cfName = cfName;
            return this;
        }

        public Builder setKeyType(Class<?> keyType) {
            this.keyType = keyType;
            return this;
        }

        public Builder setMarshalingPolicy(CF.MarshalingPolicy marshalingPolicy) {
            this.marshalingPolicy = marshalingPolicy;
            return this;
        }

        public Builder setComplexObjectMedium(CF.Medium complexObjectMedium) {
            this.complexObjectMedium = complexObjectMedium;
            return this;
        }

        public Builder setCompressionPolicy(CF.CompressionPolicy compressionPolicy) {
            this.compressionPolicy = compressionPolicy;
            return this;
        }

        public Builder setCompressionChunkSize(int compressionChunkSize) {
            this.compressionChunkSize = compressionChunkSize;
            return this;
        }

        public Builder setTtl(int ttl) {
            this.ttl = ttl;
            return this;
        }

        public Builder setDefaultColumnName(String defaultColumnName) {
            this.defaultColumnName = defaultColumnName;
            return this;
        }

        public CFPersistenceDefinition createPersistenceDefinition() {
            return new CFPersistenceDefinition(cfName, keyType, marshalingPolicy, complexObjectMedium, compressionPolicy, compressionChunkSize, ttl, defaultColumnName);
        }
    }

    public static Builder builderFromAnnotation(CF cfDef) {
        return new Builder()
                .setCfName(cfDef.cfName())
                .setKeyType(cfDef.keyType())
                .setMarshalingPolicy(cfDef.marshaling())
                .setComplexObjectMedium(cfDef.complexObjectMedium())
                .setCompressionPolicy(cfDef.compressionPolicy())
                .setCompressionChunkSize(cfDef.compressionChunkSize())
                .setTtl(cfDef.ttl())
                .setDefaultColumnName(cfDef.defaultColumnName());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static CFPersistenceDefinition fromAnnotation(final CF cfdef) {
        return builderFromAnnotation(cfdef).createPersistenceDefinition();
    }
}
