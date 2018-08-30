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
package com.comcast.hesperius.data.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for persistable objects.
 * @author PBura
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CF {
    /**
     * column family name
     */
    public String cfName();

    /**
     * row key type
     */
    public Class<?> keyType() default String.class;

    public String comparatorTypeAlias() default "";

    /**
     * if MarshalingPolicy.WHOLE then persistable object should be written to single column using one of {@link Medium} formats
     */
    public MarshalingPolicy marshaling() default MarshalingPolicy.WHOLE;

    /**
     * requires MarshalingPolicy.WHOLE, otherwise should be ignored
     */
    public Medium complexObjectMedium() default Medium.JSON;

    /**
     * in common case is used if MarshalingPolicy.WHOLE and persistable object is written to single column using one of {@link Medium} formats
     */
    public String defaultColumnName() default "data";

    /**
     * requires MarshalingPolicy.WHOLE, otherwise should be ignored
     */
    public CompressionPolicy compressionPolicy() default CompressionPolicy.NONE;

    /**
     * measured in kilobytes
     */
    public int compressionChunkSize() default 0;

    /**
     * ttl for columns this definition produces in seconds, 0 - columns do not expire
     */
    public int ttl() default 0;

    public enum MarshalingPolicy {
        WHOLE,
        PER_FIELD
    }

    public enum Medium {
        @Deprecated
        XML,
        JSON
    }

    public enum CompressionPolicy {
        COMPRESS_AND_SPLIT,
        NONE
    }
}
