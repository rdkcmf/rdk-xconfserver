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

import com.comcast.hesperius.dataaccess.core.util.Archiver;
import com.comcast.hesperius.dataaccess.core.util.CoreUtil;
import com.netflix.astyanax.serializers.ComparatorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Serializer class to use in conjunction with Hector library.
 */
public class CompressingJsonSerializer<T> extends JsonSerializer<T> {

    private static final Logger logger = LoggerFactory.getLogger(CompressingJsonSerializer.class);
    private static final Archiver archiver = CoreUtil.createArchiver();

    public CompressingJsonSerializer(Class<T> serializableSubject) {
        super(serializableSubject);
    }

    /**
     * Translates object to {@link String} in JSON format and then encodes it to {@link java.nio.ByteBuffer}.
     */
    @Override
    public ByteBuffer toByteBuffer(T obj) {
        return archiver.compress(super.toByteBuffer(obj));
    }

    /**
     * Decodes {@link java.nio.ByteBuffer} to {@link String} and translates it to object using JSON format.
     */
    @Override
    public T fromByteBuffer(ByteBuffer byteBuffer) {
        try {
            return super.fromByteBuffer(archiver.decompress(byteBuffer));
        } catch (Archiver.DataFormatException e) {
            logger.error("Exception while trying to decompress entity from db response", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public ComparatorType getComparatorType() {
        return ComparatorType.BYTESTYPE;
    }
}
