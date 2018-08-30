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
 * Created: 2/11/16  7:32 PM
 */
package com.comcast.hesperius.dataaccess.support.cache;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.cache.support.dao.ChangedKeysProcessingDAO;
import com.comcast.hesperius.dataaccess.core.config.ConfigurationProvider;
import com.comcast.hesperius.dataaccess.core.config.DataServiceConfiguration;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.hesperius.dataaccess.core.util.CoreUtil;
import com.comcast.hesperius.dataaccess.support.BaseTest;
import com.comcast.hesperius.dataaccess.support.DatastoreTestContext;
import com.comcast.hesperius.dataaccess.support.WebTestContext;
import com.comcast.hesperius.dataaccess.support.ServiceTestContext;
import com.comcast.hesperius.dataaccess.support.domain.SampleEntity;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ChangedKeysServiceTest extends BaseTest {
    private ChangedKeysProcessingDAO changedKeysDao;
    private final long changedKeysTimeWindowSize;
    private final int NUMBER_OF_ENTITIES = 5;

    @Autowired
    private ISimpleCachedDAO<String, SampleEntity> sampleEntitiesDao;


    public ChangedKeysServiceTest() {
        final DataServiceConfiguration.CacheConfiguration cacheConfig =
                ConfigurationProvider.getConfiguration().getCacheConfiguration();
        changedKeysTimeWindowSize = cacheConfig.getChangedKeysTimeWindowSize();
        changedKeysDao = new ChangedKeysProcessingDAO(StringUtils.capitalize(getProjectName()), changedKeysTimeWindowSize);
    }

    @Test
    public void testChanges() throws Exception {
        final long startTime = DateTime.now(DateTimeZone.UTC).getMillis();
        createEntities(NUMBER_OF_ENTITIES);
        final long endTime = DateTime.now(DateTimeZone.UTC).getMillis();

        getMockMvc().perform(
                get("/ChangedKeys/" + startTime + "-" + endTime)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(NUMBER_OF_ENTITIES)));
    }

    private void createEntities(int numberOfEntities) throws ValidationException {
        for (int i = 0; i < numberOfEntities; i++ ) {
            final SampleEntity sampleEntity = new SampleEntity();
            sampleEntity.setId("test_" + i);

            sampleEntitiesDao.setOne(sampleEntity.getId(), sampleEntity);
        }
    }

    private String getProjectName() {
        final String projectName = CoreUtil.dsconfig.getDomainClassesBasePackage();
        return projectName.substring(projectName.lastIndexOf(".") + 1);
    }
}
