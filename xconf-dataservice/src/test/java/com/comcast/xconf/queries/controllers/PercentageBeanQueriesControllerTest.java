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
 *  Author: mdolina
 *  Created: 4:42 PM
 */
package com.comcast.xconf.queries.controllers;

import com.comcast.hesperius.dataaccess.core.util.CoreUtil;
import com.comcast.xconf.estbfirmware.FirmwareConfig;
import com.comcast.xconf.estbfirmware.Model;
import com.comcast.xconf.estbfirmware.PercentageBean;
import com.comcast.xconf.estbfirmware.PercentageBeanQueriesHelper;
import com.comcast.xconf.firmware.RuleAction;
import com.comcast.xconf.queries.QueriesHelper;
import com.comcast.xconf.queries.QueryConstants;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.*;

import static com.comcast.xconf.firmware.ApplicationType.STB;
import static com.comcast.xconf.firmware.ApplicationType.XHOME;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PercentageBeanQueriesControllerTest extends BaseQueriesControllerTest {

    @Autowired
    private PercentageBeanQueriesHelper helper;

    @Test
    public void getAll() throws Exception {
        PercentageBean percentageBean = createPercentageBean(STB);
        savePercentageBean(percentageBean);
        QueriesHelper.nullifyUnwantedFields(percentageBean);
        String expectedJson = CoreUtil.toJSON(Lists.newArrayList(helper.replaceConfigIdWithFirmwareVersion(percentageBean)));

        mockMvc.perform(
                get("/" + QueryConstants.QUERIES_PERCENTAGE_BEAN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));
    }

    @Test
    public void getAllByApplicationType() throws Exception {
        Map<String, PercentageBean> percentageBeans = createAndSavePercentageBeans(STB, XHOME);

        String url = "/" + QueryConstants.QUERIES_PERCENTAGE_BEAN;
        PercentageBean stbPercentageBean = helper.replaceConfigIdWithFirmwareVersion(percentageBeans.get(STB));
        List<PercentageBean> expectedResult = Lists.newArrayList(QueriesHelper.nullifyUnwantedFields(stbPercentageBean));
        performGetWithApplication(url, "", expectedResult);

        PercentageBean xhomePercentageBean = helper.replaceConfigIdWithFirmwareVersion(percentageBeans.get(XHOME));
        List<PercentageBean> xhomePercentageBeans = Lists.newArrayList(QueriesHelper.nullifyUnwantedFields(xhomePercentageBean));

        performGetWithApplication(url, XHOME, xhomePercentageBeans);
    }

    @Test
    public void getById() throws Exception {
        PercentageBean percentageBean = createPercentageBean(STB);
        savePercentageBean(percentageBean);
        QueriesHelper.nullifyUnwantedFields(percentageBean);
        String expectedJson = CoreUtil.toJSON(helper.replaceConfigIdWithFirmwareVersion(percentageBean));

        mockMvc.perform(
                get("/" + QueryConstants.QUERIES_PERCENTAGE_BEAN + "/" + percentageBean.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));
    }

    @Test
    public void getByWrongId() throws Exception {
        PercentageBean percentageBean = createPercentageBean(STB);
        savePercentageBean(percentageBean);
        mockMvc.perform(
                get("/" + QueryConstants.QUERIES_PERCENTAGE_BEAN + "/wrongId")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void update() throws Exception {
        PercentageBean percentageBean = createPercentageBean(STB);
        savePercentageBean(percentageBean);

        percentageBean.setActive(false);
        Model model = createAndSaveModel("MODEL_ID2");
        percentageBean.setModel(model.getId());

        mockMvc.perform(put("/" + QueryConstants.UPDATES_PERCENTAGE_BEAN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(CoreUtil.toJSON(percentageBean)))
                .andExpect(status().isOk())
                .andExpect(content().json(CoreUtil.toJSON(percentageBean)));
    }

    @Test
    public void updateWithApplicationType() throws Exception {
        PercentageBean percentageBean = createPercentageBean(XHOME);
        savePercentageBean(percentageBean);

        String newModelId = "MODEL_ID2";
        Model model = createAndSaveModel(newModelId);
        percentageBean.setModel(model.getId());

        mockMvc.perform(put("/" + QueryConstants.UPDATES_PERCENTAGE_BEAN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(CoreUtil.toJSON(percentageBean))
                .param("applicationType", XHOME))
                .andExpect(status().isOk());

        assertEquals(newModelId, percentageBeanQueriesService.getOne(percentageBean.getId()).getModel());
    }

    @Test
    public void create() throws Exception {
        PercentageBean percentageBean = createPercentageBean(STB);

        mockMvc.perform(post("/" + QueryConstants.UPDATES_PERCENTAGE_BEAN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(CoreUtil.toJSON(percentageBean)))
                .andExpect(status().isOk())
                .andExpect(content().json(CoreUtil.toJSON(percentageBean)));
    }

    @Test
    public void createPercentageBeanWithApplicationType() throws Exception {
        PercentageBean percentageBean = createPercentageBean(XHOME);

        String url = "/" + QueryConstants.UPDATES_PERCENTAGE_BEAN;
        PercentageBean expectedResult = CoreUtil.clone(percentageBean);
        expectedResult.setApplicationType(XHOME);
        performPostWithApplication(url, XHOME, percentageBean, expectedResult);
    }

    @Test
    public void deleteById() throws Exception {
        PercentageBean percentageBean = createPercentageBean(STB);
        savePercentageBean(percentageBean);

        mockMvc.perform(
                delete("/" + QueryConstants.DELETES_PERCENTAGE_BEAN + "/" + percentageBean.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CoreUtil.toJSON(percentageBean)))
                .andExpect(status().isNoContent());
    }

    @Test
    public void getPercentageBeanWhitelistField() throws Exception {
        createAndSavePercentageBeans();
        createAndSaveGlobalPercentage();
        String fieldToSearch = "whitelist";
        Map<String, HashSet<String>> expectedResponse = Collections.singletonMap(fieldToSearch, Sets.newHashSet("percentageBeanWhitelist1", "percentageBeanWhitelist2", "globalPercentageWhitelist"));
        mockMvc.perform(get("/" + QueryConstants.QUERIES_PERCENTAGE_BEAN).param("field", fieldToSearch))
                .andExpect(status().isOk())
                .andExpect(content().json(CoreUtil.toJSON(expectedResponse)));
    }

    @Test
    public void getPercentageBeanFirmwareVersionField() throws Exception {
        createAndSavePercentageBeans();
        String fieldToSearch = "firmwareVersions";
        Map<String, HashSet<String>> expectedResponse = Collections.singletonMap(fieldToSearch, Sets.newHashSet("firmwareVersion1", "firmwareVersion2"));
        mockMvc.perform(get("/" + QueryConstants.QUERIES_PERCENTAGE_BEAN).param("field", fieldToSearch))
                .andExpect(status().isOk())
                .andExpect(content().json(CoreUtil.toJSON(expectedResponse)));
    }

    @Test
    public void verifyDuplicateChecking() throws Exception {
        Map<String, PercentageBean> percentageBeans = createAndSavePercentageBeans(STB, XHOME);
        PercentageBean newPercentageBean = CoreUtil.clone(percentageBeans.get(XHOME));
        newPercentageBean.setId(null);
        newPercentageBean.setName("newPercentageBean");
        mockMvc.perform(post("/" + QueryConstants.UPDATES_PERCENTAGE_BEAN)
                .contentType(MediaType.APPLICATION_JSON)
                .param(APPLICATION_TYPE_PARAM, XHOME)
                .content(CoreUtil.toJSON(newPercentageBean)))
                .andExpect(status().isConflict())
                .andExpect(errorMessageMatcher("PercentageBean already exists with such env/model pair: " + newPercentageBean.getEnvironment() + "/" + newPercentageBean.getModel()));
    }

    private Map<String, PercentageBean> createAndSavePercentageBeans(String stbName, String xhomeName) throws Exception {
        Map<String, PercentageBean> percentageBeans = new HashMap<>();
        PercentageBean percentageBean1 = createPercentageBean(stbName, stbName, stbName, defaultIpListId, defaultIpAddress, defaultFirmwareVersion, STB);
        percentageBean1.setId(UUID.randomUUID().toString());
        percentageBeanQueriesService.create(percentageBean1);
        percentageBeans.put(STB, percentageBean1);

        PercentageBean percentageBean2 = createPercentageBean(xhomeName, xhomeName, xhomeName, defaultIpListId, defaultIpAddress, defaultFirmwareVersion, XHOME);
        percentageBean2.setId(UUID.randomUUID().toString());
        FirmwareConfig firmwareConfig = createAndSaveFirmwareConfigByApplicationType(XHOME);
        percentageBean2.setDistributions(Collections.singletonList(new RuleAction.ConfigEntry(firmwareConfig.getId(), 60.0)));
        percentageBean2.getFirmwareVersions().add(firmwareConfig.getFirmwareVersion());
        percentageBeanQueriesService.create(percentageBean2);
        percentageBeans.put(XHOME, percentageBean2);

        return percentageBeans;
    }
}
