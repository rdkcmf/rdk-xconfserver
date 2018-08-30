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
 * Author: rdolomansky
 * Created: 2/11/16  12:58 PM
 */
package com.comcast.hesperius.dataaccess.support.cache;

import com.comcast.hesperius.dataaccess.core.cache.support.dao.ChangedKeysProcessingDAO;
import com.comcast.hesperius.dataaccess.core.cache.support.data.ChangedData;
import com.comcast.hesperius.dataaccess.core.config.ConfigurationProvider;
import com.comcast.hesperius.dataaccess.core.config.DataServiceConfiguration;
import com.comcast.hesperius.dataaccess.core.util.CoreUtil;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Iterator;
import java.util.List;

@Service
@RequestMapping(ChangedKeysService.URL_MAPPING)
public class ChangedKeysService {
    public static final String URL_MAPPING = "/ChangedKeys";

    private ChangedKeysProcessingDAO changedKeysDao;
    private final long changedKeysTimeWindowSize;

    public ChangedKeysService() {
        final DataServiceConfiguration.CacheConfiguration cacheConfig =
                ConfigurationProvider.getConfiguration().getCacheConfiguration();
        changedKeysTimeWindowSize = cacheConfig.getChangedKeysTimeWindowSize();
        changedKeysDao = new ChangedKeysProcessingDAO(StringUtils.capitalize(getProjectName()), changedKeysTimeWindowSize);
    }


    @RequestMapping(value = "/{tickStart}-{tickEnd}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<ChangedData> getChanges(@PathVariable long tickStart, @PathVariable long tickEnd) {
        final Iterator<ChangedData> changes = changedKeysDao.getIteratedChangedKeysForTick(tickStart, tickEnd);
        return Lists.newArrayList(changes);
    }

    private String getProjectName() {
        final String projectName = CoreUtil.dsconfig.getDomainClassesBasePackage();
        return projectName.substring(projectName.lastIndexOf(".") + 1);
    }
}