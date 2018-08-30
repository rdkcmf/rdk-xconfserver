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
package com.comcast.xconf.admin.controller.telemetry;

import com.comcast.hesperius.dataaccess.core.util.CoreUtil;
import com.comcast.xconf.admin.controller.BaseControllerTest;
import com.comcast.xconf.firmware.ApplicationType;
import com.comcast.xconf.logupload.UploadProtocol;
import com.comcast.xconf.logupload.telemetry.PermanentTelemetryProfile;
import com.comcast.xconf.search.SearchFields;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class PermanentProfileControllerTest extends BaseControllerTest{

    @Test
    public void createUpdateProfile() throws Exception {
        PermanentTelemetryProfile telemetryProfile = createTelemetryProfile();

        mockMvc.perform(post("/telemetry/profile").contentType(MediaType.APPLICATION_JSON)
                .content(CoreUtil.toJSON(telemetryProfile)))
                .andExpect(status().isCreated())
                .andExpect(content().json(CoreUtil.toJSON(telemetryProfile)));
    }

    @Test
    public void exportOne() throws Exception {
        PermanentTelemetryProfile telemetryProfile = createTelemetryProfile();
        permanentTelemetryDAO.setOne(telemetryProfile.getId(), telemetryProfile);

        performExportRequestAndVerifyResponse("/telemetry/profile/" + telemetryProfile.getId(), Lists.newArrayList(telemetryProfile), ApplicationType.STB);
    }

    @Test
    public void exportAll() throws Exception {
        PermanentTelemetryProfile profile1 = saveTelemetryProfile(createTelemetryProfile("id1", "a"));
        PermanentTelemetryProfile profile2 = saveTelemetryProfile(createTelemetryProfile("id2", "b"));
        List<PermanentTelemetryProfile> expectedResult = Arrays.asList(profile1, profile2);

        performExportRequestAndVerifyResponse("/telemetry/profile", expectedResult, ApplicationType.STB);
    }

    @Test
    public void update() throws Exception {
        PermanentTelemetryProfile telemetryProfile = createTelemetryProfile();
        permanentTelemetryDAO.setOne(telemetryProfile.getId(), telemetryProfile);
        telemetryProfile.setUploadRepository("http://changedurl.com");
        telemetryProfile.setUploadProtocol(UploadProtocol.HTTP);

        mockMvc.perform(put("/telemetry/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CoreUtil.toJSON(telemetryProfile)))
                .andExpect(status().isOk());
    }

    @Test
    public void getAllTelemetryProfiles() throws Exception {
        PermanentTelemetryProfile telemetryProfile = createTelemetryProfile();
        permanentTelemetryDAO.setOne(telemetryProfile.getId(), telemetryProfile);

        mockMvc.perform(get("/telemetry/profile/" + telemetryProfile.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(CoreUtil.toJSON(telemetryProfile)));

        mockMvc.perform(get("/telemetry/profile")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(CoreUtil.toJSON(Lists.newArrayList(telemetryProfile))));
    }

    @Test
    public void getTelemetryProfiles() throws Exception {
        PermanentTelemetryProfile profile1 = saveTelemetryProfile(createTelemetryProfile("id1", "a"));
        PermanentTelemetryProfile profile2 = saveTelemetryProfile(createTelemetryProfile("id2", "b"));
        PermanentTelemetryProfile profile3 = saveTelemetryProfile(createTelemetryProfile("id3", "c"));
        String expectedNumberOfItems = "3";
        List<PermanentTelemetryProfile> expectedResult = Arrays.asList(profile1, profile2);

        MockHttpServletResponse response = performGetRequestAndVerifyResponse("/telemetry/profile/page",
                new HashMap<String, String>(){{
                    put("pageNumber", "1");
                    put("pageSize", "2");
                }}, expectedResult).andReturn().getResponse();

        final Object actualNumberOfItems = response.getHeaderValue("numberOfItems");
        assertEquals(expectedNumberOfItems, actualNumberOfItems);
    }

    @Test
    public void deleteTelemetryProfile() throws Exception {
        PermanentTelemetryProfile telemetryProfile = createTelemetryProfile();
        permanentTelemetryDAO.setOne(telemetryProfile.getId(), telemetryProfile);

        mockMvc.perform(delete("/telemetry/profile/" + telemetryProfile.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/telemetry/profile/" + telemetryProfile.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void checkSorting() throws Exception {
        List<PermanentTelemetryProfile> profiles = new ArrayList<>();
        for(int i = 0; i < 3; i++) {
            PermanentTelemetryProfile profile = changeProfileIdAndName(createTelemetryProfile(), "profileId" + i, "profileName" + i);
            permanentTelemetryDAO.setOne(profile.getId(), profile);
            profiles.add(profile);
        }

        mockMvc.perform(get("/telemetry/profile"))
                .andExpect(status().isOk())
                .andExpect(content().string(CoreUtil.toJSON(profiles)));
    }

    @Test
    public void searchTelemetryProfile() throws Exception {
        PermanentTelemetryProfile profile1 = saveTelemetryProfile(createTelemetryProfile("id1", "Profile123"));
        PermanentTelemetryProfile profile2 = saveTelemetryProfile(createTelemetryProfile("id2", "Profile456"));

        List<PermanentTelemetryProfile> expectedResult = Arrays.asList(profile1);

        mockMvc.perform(post("/telemetry/profile/filtered")
                .contentType(MediaType.APPLICATION_JSON)
                .param("pageSize", "10")
                .param("pageNumber", "1")
                .content(CoreUtil.toJSON(Collections.singletonMap(SearchFields.NAME, profile1.getName()))))
                .andExpect(status().isOk())
                .andExpect(content().string(CoreUtil.toJSON(expectedResult)));

        expectedResult = Arrays.asList(profile2);

        mockMvc.perform(post("/telemetry/profile/filtered")
                .contentType(MediaType.APPLICATION_JSON)
                .param("pageSize", "10")
                .param("pageNumber", "1")
                .content(CoreUtil.toJSON(Collections.singletonMap(SearchFields.NAME, "456"))))
                .andExpect(status().isOk())
                .andExpect(content().string(CoreUtil.toJSON(expectedResult)));

        expectedResult = Arrays.asList(profile1, profile2);

        mockMvc.perform(post("/telemetry/profile/filtered")
                .contentType(MediaType.APPLICATION_JSON)
                .param("pageSize", "10")
                .param("pageNumber", "1")
                .content(CoreUtil.toJSON(Collections.singletonMap(SearchFields.NAME, "profile"))))
                .andExpect(status().isOk())
                .andExpect(content().string(CoreUtil.toJSON(expectedResult)));
    }

    private PermanentTelemetryProfile changeProfileIdAndName(PermanentTelemetryProfile profile, String id, String name) {
        profile.setId(id);
        profile.setName(name);
        return profile;
    }
}