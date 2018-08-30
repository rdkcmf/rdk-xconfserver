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
 * Author: Alexander Binkovsky
 * Created: 10/8/14  12:38 PM
 */
package com.comcast.hesperius.dataaccess.core.dao;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.util.bean.BeanProperty;
import com.comcast.hydra.astyanax.data.IPersistable;
import com.google.common.base.Predicate;
import com.netflix.astyanax.Serializer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class RotatingListingDAO<K, T extends IPersistable> extends ListingDAO<K, String, T> {
    private static final String DEFAULT_PREFIX = "XCONF";
    private String prefix;
    private Byte bounds;

    public RotatingListingDAO(String columnFamilyName, Serializer<K> keySerializer, Serializer<T> valueSerializer,
                              BeanProperty<T, String> columnNameProperty, IPersistable.Factory<T> factory, int ttl, Byte bounds) {
        super(columnFamilyName, keySerializer, valueSerializer, columnNameProperty, factory, ttl);
        this.bounds = bounds;

        try {
            prefix = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            prefix = DEFAULT_PREFIX;
        }
    }

    @Override
    public T setOne(K rowKey, T obj) throws ValidationException {
        obj.setId(getCurrentId(rowKey));
        return super.setOne(rowKey, obj);
    }

    @Override
    public List<T> getAll(K rowKey, Predicate<T> filter) {
        List<T> result = super.getAll(rowKey, filter);
        Collections.sort(result, COMPARATOR_BY_UPDATE_DESCENDING);

        return result;
    }

    private String getCurrentId(K rowKey) {
        Byte count  = getCountFromCassandra(rowKey);
        count = (count == null || count == 1) ? bounds : (byte)(count - 1);
        return numberToColumnName(count);
    }

    private Byte getCountFromCassandra(K rowKey) {
        List<T> logs = execute(query()
                .getRow(rowKey)
                .withColumnSlice(getColumnNames()));

        if (!logs.isEmpty()) {
            Collections.sort(logs, COMPARATOR_BY_UPDATE_DESCENDING);
            return columnNameToNumber(logs.get(0).getId());
        }

        return null;
    }

    private List<String> getColumnNames() {
        List<String> columnNames = new ArrayList<String>(bounds);
        for (byte i = 1; i <= bounds; i++) {
            columnNames.add(numberToColumnName(i));
        }

        return columnNames;
    }

    private String numberToColumnName(byte number) {
        return prefix + "_" + Byte.toString(number);
    }

    private byte columnNameToNumber(String columnName) {
        return Byte.parseByte(columnName.substring((prefix + "_").length()));
    }

    private final Comparator<T> COMPARATOR_BY_UPDATE_DESCENDING = new Comparator<T>() {
        @Override
        public int compare(T o1, T o2) {
            return (o2.getUpdated() != null && o1.getUpdated() != null) ? o2.getUpdated().compareTo(o1.getUpdated()) : 0;
        }
    };
}
