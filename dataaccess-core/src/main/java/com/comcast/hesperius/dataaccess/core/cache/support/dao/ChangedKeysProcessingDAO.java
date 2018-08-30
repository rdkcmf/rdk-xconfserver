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
package com.comcast.hesperius.dataaccess.core.cache.support.dao;

import com.comcast.hesperius.data.annotation.ListingCF;
import com.comcast.hesperius.dataaccess.core.cache.TimeUUIDUtils;
import com.comcast.hesperius.dataaccess.core.cache.support.data.ChangedData;
import com.comcast.hesperius.dataaccess.core.dao.ListingDAO;
import com.comcast.hesperius.dataaccess.core.dao.query.*;
import com.comcast.hesperius.dataaccess.core.dao.query.impl.ColumnRangeImpl;
import com.comcast.hesperius.dataaccess.core.dao.util.JsonSerializer;
import com.comcast.hesperius.dataaccess.core.util.bean.BeanUtils;
import com.comcast.hydra.astyanax.util.PersistableFactory;
import com.netflix.astyanax.serializers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * DAO for {@link com.comcast.hesperius.dataaccess.core.cache.support.data.ChangedData}. Helps to deal with cache. In addition to standard methods of {@link com.comcast.hesperius.dataaccess.core.dao.CompositeDAO}
 * provides convenient methods to read {@link com.comcast.hesperius.dataaccess.core.cache.support.data.ChangedData} for specified time window.
 *
 * @author PBura
 */
public class ChangedKeysProcessingDAO extends ListingDAO<Long, UUID, ChangedData> {
    private final Logger log = LoggerFactory.getLogger(ChangedKeysProcessingDAO.class);
    private final long changedKeysTimeWindowSize;
    private static final ListingCF LISTING_CF_ANNOTATION = ChangedData.class.getAnnotation(ListingCF.class);
    private static final String COLUMN_NAME = "columnName";

    public ChangedKeysProcessingDAO(final String cfname, long changedKeysTimeWindowSize) {
        super(cfname.concat(LISTING_CF_ANNOTATION.cfName()), LongSerializer.get(), new JsonSerializer<>(ChangedData.class),
                BeanUtils.<ChangedData, UUID>getOrCreateBeanProperty(ChangedData.class, COLUMN_NAME),
                new PersistableFactory<>(ChangedData.class), LISTING_CF_ANNOTATION.ttl());
        this.changedKeysTimeWindowSize = changedKeysTimeWindowSize;
    }

    /**
     * @param tickStart start timestamp of data that should be read, inclusive
     * @param tickEnd   end timestamp of data that should be read, exclusive
     * @return iterator over retrieved objects
     */
    public Iterator<ChangedData> getIteratedChangedKeysForTick(final long tickStart, final long tickEnd) {
        long currentRowKey = tickStart - (tickStart % changedKeysTimeWindowSize);
        final long endRowKey = tickEnd - (tickEnd % changedKeysTimeWindowSize);

        final UUID startUuid = TimeUUIDUtils.createQueryUUID(tickStart);

        final Map<Long, ColumnRange<UUID>> ranges = new HashMap<>();
        ranges.put(currentRowKey, buildColumnRange(startUuid, null));
        currentRowKey += changedKeysTimeWindowSize;
        while(currentRowKey <= endRowKey) {
            ranges.put(currentRowKey, buildColumnRange(null, null));
            currentRowKey += changedKeysTimeWindowSize;
        }
        log.info("Getting changed keys for tick {} - {} @ {}", tickStart, tickEnd, buildLogForRanges(ranges));
        return getRange(ranges).iterator();
    }

    private ColumnRange<UUID> buildColumnRange(final UUID startColumn, final UUID endColumn) {
        return new ColumnRangeImpl<UUID>().startColumn(startColumn).endColumn(endColumn);
    }

    private String buildLogForRanges(final Map<Long, ColumnRange<UUID>> ranges) {
        final StringBuilder result = new StringBuilder();
        final Iterator<Map.Entry<Long, ColumnRange<UUID>>> rangesIterator = ranges.entrySet().iterator();
        while (rangesIterator.hasNext()) {
            final Map.Entry<Long, ColumnRange<UUID>> entry = rangesIterator.next();
            final ColumnRange<UUID> columnRange = entry.getValue();
            result
                    .append("Row Key: ").append(entry.getKey()).append("; ")
                    .append("Start Column Name: ").append(columnRange.getStartColumnName());
            if (rangesIterator.hasNext()) {
                result.append(" @ ");
            }
        }
        return result.toString();
    }

}
