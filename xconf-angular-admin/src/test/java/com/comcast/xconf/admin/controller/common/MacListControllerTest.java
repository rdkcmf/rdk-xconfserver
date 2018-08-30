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
package com.comcast.xconf.admin.controller.common;

import com.comcast.apps.hesperius.ruleengine.main.api.FixedArg;
import com.comcast.apps.hesperius.ruleengine.main.impl.Condition;
import com.comcast.hesperius.dataaccess.core.exception.EntityConflictException;
import com.comcast.hesperius.dataaccess.core.util.CoreUtil;
import com.comcast.hydra.astyanax.data.XMLPersistable;
import com.comcast.xconf.GenericNamespacedList;
import com.comcast.xconf.GenericNamespacedListTypes;
import com.comcast.xconf.StbContext;
import com.comcast.xconf.admin.controller.BaseControllerTest;
import com.comcast.xconf.estbfirmware.TemplateNames;
import com.comcast.xconf.estbfirmware.factory.RuleFactory;
import com.comcast.xconf.firmware.FirmwareRule;
import com.comcast.xconf.firmware.FirmwareRuleTemplate;
import com.comcast.xconf.logupload.DCMGenericRule;
import com.comcast.xconf.logupload.settings.SettingProfile;
import com.comcast.xconf.logupload.settings.SettingRule;
import com.comcast.xconf.logupload.telemetry.TelemetryRule;
import com.comcast.xconf.queries.QueriesHelper;
import com.comcast.xconf.search.SearchFields;
import com.comcast.xconf.util.RuleUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultActions;

import java.util.*;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class MacListControllerTest extends BaseControllerTest {

    @Test
    public void getList() throws Exception {
        GenericNamespacedList macList = createMacList();
        genericNamespacedListDAO.setOne(macList.getId(), macList);

        mockMvc.perform(get("/genericnamespacedlist/"))
                .andExpect(status().isOk())
                .andExpect(content().json(CoreUtil.toJSON(Lists.newArrayList(macList))));
    }

    @Test
    public void getLists() throws Exception {
        GenericNamespacedList list1 = saveGenericList(createMacList("id1"));
        GenericNamespacedList list2 = saveGenericList(createMacList("id2"));
        GenericNamespacedList list3 = saveGenericList(createMacList("id3"));
        String expectedNumberOfItems = "3";
        List<GenericNamespacedList> expectedResult = Arrays.asList(list1, list2, list3);

        MockHttpServletResponse response = mockMvc.perform(post("/genericnamespacedlist/filtered")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CoreUtil.toJSON(new HashMap<String, String>()))
                .param("pageNumber", "1")
                .param("pageSize", "10")
                .param("type", GenericNamespacedListTypes.MAC_LIST))
                .andExpect(status().isOk())
                .andExpect(content().json(CoreUtil.toJSON(expectedResult)))
                .andReturn().getResponse();

        final Object actualNumberOfItems = response.getHeaderValue("numberOfItems");
        assertEquals(expectedNumberOfItems, actualNumberOfItems);
    }

    @Test
    public void getById() throws Exception {
        GenericNamespacedList macList = createMacList();
        genericNamespacedListDAO.setOne(macList.getId(), macList);

        mockMvc.perform(get("/genericnamespacedlist/" + macList.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json(CoreUtil.toJSON(macList)));
    }

    @Test
    public void getByType() throws Exception {
        GenericNamespacedList macList = createMacList();
        genericNamespacedListDAO.setOne(macList.getId(), macList);
        GenericNamespacedList ipList = createIpList();
        genericNamespacedListDAO.setOne(ipList.getId(), ipList);

        Map<String, String> searchContext = Collections.singletonMap(SearchFields.TYPE, GenericNamespacedListTypes.MAC_LIST);

        mockMvc.perform(post("/genericnamespacedlist/filtered")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CoreUtil.toJSON(searchContext))
                .param("pageNumber", "1")
                .param("pageSize", "10")
                .param("type", GenericNamespacedListTypes.MAC_LIST))
                .andExpect(status().isOk())
                .andExpect(content().json(CoreUtil.toJSON(Collections.singletonList(macList))));
    }

    @Test
    public void getListIdsByType() throws Exception {
        GenericNamespacedList macList = createMacList();
        genericNamespacedListDAO.setOne(macList.getId(), macList);
        GenericNamespacedList ipList = createIpList();
        genericNamespacedListDAO.setOne(ipList.getId(), ipList);

        mockMvc.perform(get("/genericnamespacedlist/" + GenericNamespacedListTypes.MAC_LIST + "/ids"))
                .andExpect(status().isOk())
                .andExpect(content().json(CoreUtil.toJSON(Lists.newArrayList(macList.getId()))));
    }

    @Test
    public void createList() throws Exception {
        GenericNamespacedList macList = createMacList();

        mockMvc.perform(post("/genericnamespacedlist")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CoreUtil.toJSON(macList)))
                .andExpect(status().isCreated())
                .andExpect(content().json(CoreUtil.toJSON(macList)));

        assertEquals(macList, genericNamespacedListDAO.getOne(macList.getId()));
    }

    @Test
    public void createListWithDuplicateMacs() throws Exception {
        GenericNamespacedList macList1 = createMacList();
        genericNamespacedListDAO.setOne(macList1.getId(), macList1);

        GenericNamespacedList macList2 = createMacList();
        macList2.setId("macListId");

        String errorMessage = mockMvc.perform(post("/genericnamespacedlist")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CoreUtil.toJSON(macList2)))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException().getMessage();

        assertEquals("MAC addresses are used in another lists: [AA:AA:AA:AA:AA:AA] in macList", errorMessage);
    }

    @Test
    public void createListWithLowerCaseDuplicateMacs() throws Exception {
        GenericNamespacedList macList1 = createMacList();
        genericNamespacedListDAO.setOne(macList1.getId(), macList1);

        GenericNamespacedList macList2 = createMacList();
        macList2.setData(Collections.singleton("aa:aa:aa:aa:aa:aa"));
        macList2.setId("macListId");

        String errorMessage = mockMvc.perform(post("/genericnamespacedlist")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CoreUtil.toJSON(macList2)))
                .andExpect(status().isConflict())
                .andReturn().getResolvedException().getMessage();

        assertEquals("MAC addresses are used in another lists: [AA:AA:AA:AA:AA:AA] in macList", errorMessage);
    }

    @Test
    public void updateList() throws Exception {
        GenericNamespacedList macList = createMacList();
        genericNamespacedListDAO.setOne(macList.getId(), macList);
        macList.setData(Sets.newHashSet("BB:BB:BB:BB:BB:BB"));

        mockMvc.perform(put("/genericnamespacedlist/" + macList.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(CoreUtil.toJSON(macList)))
                .andExpect(status().isOk())
                .andExpect(content().json(CoreUtil.toJSON(QueriesHelper.nullifyUnwantedFields(macList))));

        assertEquals(QueriesHelper.nullifyUnwantedFields(macList), QueriesHelper.nullifyUnwantedFields(genericNamespacedListDAO.getOne(macList.getId())));
    }

    @Test
    public void deleteList() throws Exception {
        GenericNamespacedList macList = createMacList();
        genericNamespacedListDAO.setOne(macList.getId(), macList);

        mockMvc.perform(delete("/genericnamespacedlist/" + macList.getId()))
                .andExpect(status().isNoContent());
        assertNull(genericNamespacedListDAO.getOne(macList.getId()));
    }

    @Test
    public void unableToDeleteListBecauseTelemetryRuleIsUsingIt() throws Exception {
        GenericNamespacedList macList = saveGenericList(createMacList());
        String listId = macList.getId();
        TelemetryRule rule = saveTelemetryRule(createTelemetryRule(createCondition(listId, StbContext.ESTB_MAC, RuleFactory.IN_LIST)));

        ResultActions resultActions = mockMvc.perform(delete("/genericnamespacedlist/" + macList.getId()));

        assertException(resultActions, EntityConflictException.class, "List is used by TelemetryRule " + rule.getName());
    }

    @Test
    public void unableToDeleteListBecauseFormulaIsUsingIt() throws Exception {
        GenericNamespacedList macList = saveGenericList(createMacList());
        String listId = macList.getId();
        DCMGenericRule rule = saveFormula(createFormula(createCondition(listId, StbContext.ESTB_MAC, RuleFactory.IN_LIST)));

        ResultActions resultActions = mockMvc.perform(delete("/genericnamespacedlist/" + macList.getId()));

        assertException(resultActions, EntityConflictException.class, "List is used by Formula " + rule.getName());
    }

    @Test
    public void unableToDeleteListBecauseFirmwareRuleIsUsingIt() throws Exception {
        GenericNamespacedList macList = saveGenericList(createMacList());
        String listId = macList.getId();
        FirmwareRule rule = saveFirmwareRule(createFirmwareRule(createCondition(listId, StbContext.ESTB_MAC, RuleFactory.IN_LIST), TemplateNames.MAC_RULE));

        ResultActions resultActions = mockMvc.perform(delete("/genericnamespacedlist/" + macList.getId()));

        assertException(resultActions, EntityConflictException.class, "List is used by FirmwareRule " + rule.getName());
    }

    @Test
    public void exportOne() throws Exception {
        GenericNamespacedList macList = createMacList();
        genericNamespacedListDAO.setOne(macList.getId(), macList);

        MockHttpServletResponse response = mockMvc.perform(get("/genericnamespacedlist/" + macList.getId())
                .param("export", "export"))
                .andExpect(status().isOk())
                .andExpect(content().json(CoreUtil.toJSON(Lists.newArrayList(macList))))
                .andReturn().getResponse();
        assertEquals(Sets.newHashSet("Content-Disposition", "Content-Type"), response.getHeaderNames());
    }

    @Test
    public void exportAll() throws Exception {
        GenericNamespacedList macList = createMacList();
        genericNamespacedListDAO.setOne(macList.getId(), macList);

        MockHttpServletResponse response = mockMvc.perform(get("/genericnamespacedlist/all/" + GenericNamespacedListTypes.MAC_LIST)
                .param("export", "export"))
                .andExpect(status().isOk())
                .andExpect(content().json(CoreUtil.toJSON(Lists.newArrayList(QueriesHelper.nullifyUnwantedFields(macList)))))
                .andReturn().getResponse();

        assertEquals(Sets.newHashSet("Content-Disposition", "Content-Type"), response.getHeaderNames());
    }

    @Test
    public void checkSorting() throws Exception {
        List<XMLPersistable> namespacedLists = new ArrayList<>();
        for(int i = 0; i < 3; i++) {
            GenericNamespacedList macList = changeNamespacedListId(createMacList(), "macListId" + i);
            genericNamespacedListDAO.setOne(macList.getId(), macList);
            namespacedLists.add(QueriesHelper.nullifyUnwantedFields(macList));
        }

        mockMvc.perform(post("/genericnamespacedlist/filtered")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CoreUtil.toJSON(new HashMap<String, String>()))
                .param("type", GenericNamespacedListTypes.MAC_LIST)
                .param("pageNumber", "1")
                .param("pageSize", "10")
        ).andExpect(status().isOk())
                .andExpect(content().string(CoreUtil.toJSON(namespacedLists)));
    }

    @Test
    public void searchByContext() throws Exception {
        GenericNamespacedList macList = createMacList();
        genericNamespacedListDAO.setOne(macList.getId(), macList);
        GenericNamespacedList macList1 = createMacList();
        macList1.setId("testId2");
        macList1.setData(Sets.newHashSet("AA:AA:CD:CD:CD:CD"));
        genericNamespacedListDAO.setOne(macList1.getId(), macList1);

        Map<String, String> context = new HashMap<>();
        context.put(SearchFields.DATA, "AA:AA");

        mockMvc.perform(post("/genericnamespacedlist/filtered")
                .contentType(MediaType.APPLICATION_JSON).content(CoreUtil.toJSON(context))
                .param("type", macList.getTypeName())
                .param("pageSize", "1")
                .param("pageNumber", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json(CoreUtil.toJSON(Lists.newArrayList(macList))));

        //search by name
        context.clear();
        context.put(SearchFields.NAME, "mac");

        mockMvc.perform(post("/genericnamespacedlist/filtered")
                .contentType(MediaType.APPLICATION_JSON)
                .param("type", macList.getTypeName()).content(CoreUtil.toJSON(context))
                .param("pageSize", "1")
                .param("pageNumber", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json(CoreUtil.toJSON(Lists.newArrayList(macList))));

        //search by name and data
        context.put(SearchFields.DATA, "CD:CD");
        context.put(SearchFields.NAME, "test");

        mockMvc.perform(post("/genericnamespacedlist/filtered")
                .contentType(MediaType.APPLICATION_JSON)
                .param("type", macList.getTypeName()).content(CoreUtil.toJSON(context))
                .param("pageSize", "1")
                .param("pageNumber", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json(CoreUtil.toJSON(Lists.newArrayList(macList1))));

        //search by name and data, returns two macs
        context.put(SearchFields.DATA, "AA");
        context.put(SearchFields.NAME, "t");
        mockMvc.perform(post("/genericnamespacedlist/filtered")
                .contentType(MediaType.APPLICATION_JSON).content(CoreUtil.toJSON(context))
                .param("type", macList.getTypeName())
                .param("pageSize", "2")
                .param("pageNumber", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json(CoreUtil.toJSON(Lists.newArrayList(macList, macList1))));
    }

    @Test
    public void renameMacList() throws Exception {
        GenericNamespacedList macList = createMacList();
        genericNamespacedListDAO.setOne(macList.getId(), macList);

        FirmwareRuleTemplate macRuleTemplate = createMacRuleTemplate(macList.getId());
        firmwareRuleTemplateDao.setOne(macRuleTemplate.getId(), macRuleTemplate);

        FirmwareRule macRule = createMacRule("macRuleName");
        firmwareRuleDao.setOne(macRule.getId(), macRule);

        SettingProfile settingProfile = createSettingProfile("settingProfileName");
        settingProfileDao.setOne(settingProfile.getId(), settingProfile);

        SettingRule settingRule = createSettingIpRule("settingRuleName", settingProfile.getId(), macList.getId());
        settingRuleDAO.setOne(settingRule.getId(), settingRule);

        TelemetryRule telemetryRule = createTelemetryMacRule("telemetryRuleName", macList.getId());
        telemetryRuleDAO.setOne(telemetryRule.getId(), telemetryRule);

        DCMGenericRule dcmRule = createFormula(new Condition(RuleFactory.MAC, RuleFactory.IN_LIST, FixedArg.from(macList.getId())));
        dcmRuleDAO.setOne(dcmRule.getId(), dcmRule);

        String newMacListId = "newId";

        String savedIpListStr = mockMvc.perform(put("/genericnamespacedlist/" + newMacListId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(CoreUtil.toJSON(macList)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        GenericNamespacedList savedIpList = mapper.readValue(savedIpListStr, GenericNamespacedList.class);
        savedIpList.setUpdated(null);
        macList.setId(newMacListId);

        assertEquals(macList, savedIpList);
        assertTrue(RuleUtil.isExistConditionByFixedArgValue(telemetryRuleDAO.getOne(telemetryRule.getId()), newMacListId));
        assertTrue(RuleUtil.isExistConditionByFixedArgValue(settingRuleDAO.getOne(settingRule.getId()).getRule(), newMacListId));
        assertTrue(RuleUtil.isExistConditionByFixedArgValue(dcmRuleDAO.getOne(dcmRule.getId()), newMacListId));
        assertTrue(RuleUtil.isExistConditionByFixedArgValue(firmwareRuleTemplateDao.getOne(macRuleTemplate.getId()).getRule(), newMacListId));
    }
}
