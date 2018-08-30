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
 * Created: 4/18/16  4:25 PM
 */
package com.comcast.hesperius.dataaccess.support.services;

import com.comcast.hesperius.dataaccess.support.BaseTest;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DataServiceInfoTest extends BaseTest {

    @Test
    public void testGetVersion() throws Exception {
        getMockMvc().perform(
                get("/version")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("projectName", Matchers.hasToString("CORE SUPPORT")))
                .andExpect(jsonPath("projectVersion", Matchers.hasToString("2.41.23-SNAPSHOT")))
                .andExpect(jsonPath("gitBuildTime", Matchers.hasToString("4/15/2016 3:49 PM")))
                .andExpect(jsonPath("gitCommitId", Matchers.hasToString("2b0f9631cfce388e3c791d0dba8e475fd97fad3b")))
                .andExpect(jsonPath("gitCommitTime", Matchers.hasToString("Thu Apr 14 19:51:37 2016 +0300")))
                .andExpect(jsonPath("gitBranch", Matchers.hasToString("develop")));

    }

    @Test
    public void testGetConfig() throws Exception {
        getMockMvc().perform(
                get("/config")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("domainClassesBasePackage", Matchers.hasToString("com.comcast.hesperius.dataaccess.support")));
    }

    @Test
    public void testCheckHeartBeat() throws Exception {
        getMockMvc().perform(
                get("/heartBeat")
                        .accept(MediaType.TEXT_PLAIN_VALUE)
        )
                .andExpect(status().isOk())
                .andExpect(content().string(DataServiceInfo.OK));
    }

    @Test
    public void testGetStatus() throws Exception {
        getMockMvc().perform(
                get("/status")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk());
    }

    @Test
    public void testGetStatistics() throws Exception {
        getMockMvc().perform(
                get("/statistics")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("cacheMap", Matchers.hasKey("SampleEntities")));
    }

    @Test
    public void testRefreshCf() throws Exception {
        final String cfName = "SampleEntities";
        getMockMvc().perform(
                get("/refresh/" + cfName)
                        .accept(MediaType.TEXT_PLAIN_VALUE)
        )
                .andExpect(status().isOk())
                .andExpect(content().string(DataServiceInfo.OK));
    }

    @Test
    public void testRefreshAll() throws Exception {
        getMockMvc().perform(
                get("/refreshAll")
                        .accept(MediaType.TEXT_PLAIN_VALUE)
        )
                .andExpect(status().isOk())
                .andExpect(content().string(DataServiceInfo.OK));
    }

}
