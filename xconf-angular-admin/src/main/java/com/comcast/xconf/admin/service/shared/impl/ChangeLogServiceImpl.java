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
package com.comcast.xconf.admin.service.shared.impl;

import com.comcast.hesperius.dataaccess.core.cache.CacheManager;
import com.comcast.hesperius.dataaccess.core.cache.support.dao.ChangedKeysProcessingDAO;
import com.comcast.hesperius.dataaccess.core.cache.support.data.ChangedData;
import com.comcast.xconf.admin.controller.shared.ChangeLogController;
import com.comcast.xconf.admin.service.shared.ChangeLogService;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ChangeLogServiceImpl implements ChangeLogService {
    private final ChangedKeysProcessingDAO changeLogDao;

    @Autowired
    public ChangeLogServiceImpl(final ChangedKeysProcessingDAO changeLogDao) {
        this.changeLogDao = changeLogDao;
    }

    @Override
    public Map<Long, List<ChangeLogController.Change>> getChangeLog() {
        //period of time to group changed data in result map
        final long intervalStep = 60 * 60 * 1000;
        final Map<Long, List<ChangeLogController.Change>> result =  new HashMap<>();
        boolean lastDataChunk = false;

        final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        final Long currentTime = calendar.getTimeInMillis();
        //get timestamp of the beginning of the day
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Long startOfInterval = calendar.getTimeInMillis();
        Long endOfInterval = startOfInterval;

        while(!lastDataChunk) {
            startOfInterval = endOfInterval;
            endOfInterval = startOfInterval + intervalStep;
            if(endOfInterval >= currentTime) {
                endOfInterval = currentTime + intervalStep;
                lastDataChunk = true;
            }
            final PeekingIterator<ChangedData> changedKeysIterator = Iterators.peekingIterator(getIteratorForPeriod(startOfInterval, endOfInterval));
            final List<ChangeLogController.Change> listOfChanges = new ArrayList<>();
            while(changedKeysIterator.hasNext()) {
                final ChangedData data = changedKeysIterator.next();
                final List<Class<?>> typeParams = Lists.newArrayList(CacheManager.findTypeParamsByDAOId(data.getDAOid()));
                if(typeParams.size() > 1) {
                    final String cfName = typeParams.get(1).getSimpleName();
                    listOfChanges.add(new ChangeLogController.Change(data.getChangedKey(), data.getOperation().name(), cfName));
                }
            }

            result.put(startOfInterval, listOfChanges);
        }
        return result;
    }

    private Iterator<ChangedData> getIteratorForPeriod(final long start, final long end) {
        return changeLogDao.getIteratedChangedKeysForTick(start, end);
    }
}
