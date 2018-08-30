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
package com.comcast.hesperius.dataaccess.core.dao.mapper;

import com.comcast.hesperius.data.annotation.CF;
import com.comcast.hesperius.dataaccess.core.dao.util.CFPersistenceDefinition;
import com.comcast.hesperius.dataaccess.core.dao.util.DataUtils;
import com.comcast.hesperius.dataaccess.core.util.Archiver;
import com.comcast.hesperius.dataaccess.core.util.CoreUtil;
import com.comcast.hydra.astyanax.data.IPersistable;
import com.comcast.hydra.astyanax.util.ReflectionUtils;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.serializers.ByteBufferSerializer;
import com.netflix.astyanax.serializers.SerializerTypeInferer;

import java.nio.ByteBuffer;

/**
 * RowMapper that is used in {@link com.comcast.hesperius.dataaccess.core.dao.RowMappingPartialDAO}.
 * Includes logic to handle all modes of persistence which are described in {@link CF} annotation.
 *
 * @param <T> persistable object type
 */
public class CommonRowMapper<T extends IPersistable> extends ReflectionSimpleMapper<T> {
    private static final Archiver archiver = CoreUtil.createArchiver();
    private final CFPersistenceDefinition cfDef;
    private final Class<T> entityClass;

    public CommonRowMapper(Class<T> entityClass, CFPersistenceDefinition persistenceDef) {
        super(entityClass);

        if (persistenceDef != null) {
            this.cfDef = persistenceDef;
        } else if (entityClass.isAnnotationPresent(CF.class)) {
            this.cfDef = CFPersistenceDefinition.fromAnnotation(entityClass.getAnnotation(CF.class));
        } else {
            throw new IllegalStateException("Either PersistenceDefinition must be passed or entity class must have CF annotation applied to " + entityClass.getCanonicalName());
        }
        this.entityClass = entityClass;
    }

    @Override
    public void mapToMutation(T obj, ColumnListMutation<String> mutation) {
        if (cfDef.marshalingPolicy == CF.MarshalingPolicy.WHOLE) {
            String strValue = cfDef.complexObjectMedium == CF.Medium.JSON
                    ? CoreUtil.toJSON(obj)
                    : "xml marshalling not supported anymore";
            if (cfDef.compressionPolicy == CF.CompressionPolicy.NONE) {
                mutation.putColumn(cfDef.defaultColumnName, strValue, ttl());

            } else {
                ByteBuffer data = ByteBuffer.wrap(strValue.getBytes(Charsets.UTF_8));
                data = archiver.compress(data);
                // cfDef.compressionChunkSize() * 1024 - convert from kilobytes to bytes
                ByteBuffer[] splitData = archiver.split(data, cfDef.compressionChunkSize * 1024);
                mutation.putColumn(cfDef.defaultColumnName + "_parts_count", splitData.length, ttl());
                for (int i = 0; i < splitData.length; i++) {
                    mutation.putColumn(cfDef.defaultColumnName + "_part_" + i, splitData[i], ByteBufferSerializer.get(), ttl());
                }
            }
        } else {    //  CF.MarshalingPolicy.PER_FIELD
            for (String col : ReflectionUtils.getColumnNamesFromFields(obj.getClass())) {
                Object value = DataUtils.invokeGetter(obj, col);
                if (value == null)
                    continue;
                mutation.putColumn(col, value, SerializerTypeInferer.getSerializer(value), ttl());
            }
        }
    }

    @Override
    public T mapFromColumnList(ColumnList<String> columnList, T obj) {
        Preconditions.checkNotNull(obj);
        if (cfDef.marshalingPolicy.equals(CF.MarshalingPolicy.PER_FIELD)) {
            super.mapFromColumnList(columnList, obj);
            return obj;
        } else {
            final String counterColumnName = cfDef.defaultColumnName + "_parts_count";
            if (columnList.getColumnByName(counterColumnName) == null) { // value is not compressed
                return cfDef.complexObjectMedium == CF.Medium.JSON
                        ? CoreUtil.fromJSON(entityClass, columnList.getStringValue(cfDef.defaultColumnName, null))
                        : null; //TODO comeup with json alternative
            } else { // value is compressed
                int partsCount = columnList.getIntegerValue(counterColumnName, 1);
                ByteBuffer[] splitData = new ByteBuffer[partsCount];
                for (int i = 0; i < partsCount; i++) {
                    splitData[i] = columnList.getByteBufferValue(cfDef.defaultColumnName.concat("_part_").concat(String.valueOf(i)), null);
                }
                final ByteBuffer compressed = archiver.join(splitData);
                try {
                    final ByteBuffer data = archiver.decompress(compressed);
                    final String strData = Charsets.UTF_8.decode(data).toString();
                    return cfDef.complexObjectMedium == CF.Medium.JSON
                            ? CoreUtil.fromJSON(entityClass, strData)
                            : null; //TODO comeup with json alternative
                } catch (Archiver.DataFormatException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    private final Integer ttl() {
        return cfDef.ttl != 0 ? cfDef.ttl : null;
    }
}
