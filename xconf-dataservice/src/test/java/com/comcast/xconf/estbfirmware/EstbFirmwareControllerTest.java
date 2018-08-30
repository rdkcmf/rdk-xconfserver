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
 * <p/>
 * Author: Stanislav Menshykov
 * Created: 12/3/15  2:02 PM
 */
package com.comcast.xconf.estbfirmware;

import com.comcast.apps.hesperius.ruleengine.domain.additional.data.IpAddress;
import com.comcast.apps.hesperius.ruleengine.domain.additional.data.IpAddressGroup;
import com.comcast.hesperius.dataaccess.core.util.CoreUtil;
import com.comcast.xconf.ConfigNames;
import com.comcast.xconf.IpAddressGroupExtended;
import com.comcast.xconf.estbfirmware.converter.TimeFilterConverter;
import com.comcast.xconf.estbfirmware.factory.RuleFactory;
import com.comcast.xconf.estbfirmware.factory.TemplateFactory;
import com.comcast.xconf.estbfirmware.util.LogsCompatibilityUtils;
import com.comcast.xconf.firmware.ApplicationType;
import com.comcast.xconf.firmware.DefinePropertiesAction;
import com.comcast.xconf.firmware.FirmwareRule;
import com.comcast.xconf.firmware.FirmwareRuleTemplate;
import com.comcast.xconf.queries.QueryConstants;
import com.comcast.xconf.queries.beans.DownloadLocationFilterWrapper;
import com.comcast.xconf.queries.beans.PercentFilterWrapper;
import com.comcast.xconf.queries.beans.TimeFilterWrapper;
import com.comcast.xconf.queries.controllers.BaseQueriesControllerTest;
import com.google.common.collect.Lists;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class EstbFirmwareControllerTest extends BaseQueriesControllerTest {

    @Autowired
    private IpRuleService ipRuleService;
    @Autowired
    private IpFilterService ipFilterService;
    @Autowired
    private EnvModelRuleService envModelRuleService;
    @Autowired
    private TimeFilterConverter timeFilterConverter;
    @Autowired
    private EstbFirmwareLogger estbFirmwareLogger;
    @Autowired
    private TemplateFactory templateFactory;

    @Test
    public void getConfigForIpRuleWithNoFilters() throws Exception {
        createAndSaveDefaultIpRuleBean();

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", createDefaultContext())).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void getConfigForMacRuleWithNoFilters() throws Exception {
        saveMacRuleBean(createDefaultMacRuleBean());

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", createDefaultContext())).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void getConfigForMacRuleWithNoFiltersAndContextVersionSameAsDefault() throws Exception {
        saveMacRuleBean(createDefaultMacRuleBean());

        EstbFirmwareContext defaultContext = createDefaultContext();
        defaultContext.setFirmwareVersion(defaultFirmwareVersion);
        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", defaultContext)).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void getConfigForEnvModelRuleWithNoFilters() throws Exception {
        createAndSaveDefaultEnvModelRuleBean();

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", createDefaultContext())).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void resultIsBlockedByIpFilter() throws Exception {
        IpRuleBean ipRuleBean = createAndSaveDefaultIpRuleBean();
        IpFilter ipFilter = createDefaultIpFilter();
        saveIpFilter(ipFilter);
        EstbFirmwareContext context = createDefaultContext();

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        verifyExplanation(ipRuleService.convertIpRuleBeanToFirmwareRule(ipRuleBean),
                ipRuleBean.getFirmwareConfig(), ipFilterService.convertIpFilterToFirmwareRule(ipFilter), actualResult);
    }

    @Test
    public void resultIsNotBlocked_WhenIpFilterNotInRange() throws Exception {
        String notDefaultIpAddress = "2.2.2.2";
        createAndSaveDefaultIpRuleBean();
        saveIpFilter(createIpFilter(defaultIpFilterId, "filterName", Collections.singleton(notDefaultIpAddress)));
        EstbFirmwareContext context = createDefaultContext();

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void resultIsNotBlocked_WhenIpFilterInBypassFilters() throws Exception {
        createAndSaveDefaultIpRuleBean();
        saveIpFilter(createDefaultIpFilter());
        EstbFirmwareContext context = createDefaultContext();
        context.setBypassFilters(TemplateNames.IP_FILTER);

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void resultIsBlockedByTimeFilter() throws Exception {
        EstbFirmwareContext context = createContextWithTime(12);
        IpRuleBean ipRuleBean = createAndSaveDefaultIpRuleBean();
        TimeFilter timeFilter = createAndSaveTimeFilterFrom9to15();

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        verifyExplanation(ipRuleService.convertIpRuleBeanToFirmwareRule(ipRuleBean),
                ipRuleBean.getFirmwareConfig(), timeFilterConverter.convert(timeFilter), actualResult);
    }

    @Test
    public void resultIsNotBlocked_WhenTimeFilterNotInRange() throws Exception {
        EstbFirmwareContext context = createContextWithTime(8);
        createAndSaveDefaultIpRuleBean();
        createAndSaveTimeFilterFrom9to15();

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context))
                .andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void resultIsNotBlocked_WhenTimeFilterInBypassFilters() throws Exception {
        EstbFirmwareContext context = createContextWithTime(12);
        context.setBypassFilters(TemplateNames.TIME_FILTER);
        createAndSaveDefaultIpRuleBean();
        createAndSaveTimeFilterFrom9to15();

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void resultIsNotBlocked_WhenTimeFilterNeverBlockRebootDecoupledIsTrue() throws Exception {
        EstbFirmwareContext context = createContextWithTime(12);
        context.setCapabilities(Collections.singletonList(Capabilities.rebootDecoupled.toString()));
        IpRuleBean ipRuleBean = createAndSaveDefaultIpRuleBean();
        TimeFilter timeFilter = createTimeFilterFrom9to15();
        timeFilter.setNeverBlockRebootDecoupled(true);
        saveTimeFilter(timeFilter);

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = ipRuleBean.getFirmwareConfig();
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void resultIsNotBlocked_WhenTimeFilterNeverBlockHttpDownloadIsTrue() throws Exception {
        EstbFirmwareContext context = createContextWithTime(12);
        createAndSaveDefaultIpRuleBean();
        TimeFilter timeFilter = createTimeFilterFrom9to15();
        timeFilter.setNeverBlockHttpDownload(true);
        saveTimeFilter(timeFilter);

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void resultIsBlocked_WhenTimeFilterNeverBlockHttpDownloadIsTrueButDownloadProtocolIsTftp() throws Exception {
        EstbFirmwareContext context = createContextWithTime(12);
        defaultFirmwareDownloadProtocol = FirmwareConfig.DownloadProtocol.tftp;
        IpRuleBean ipRuleBean = createAndSaveDefaultIpRuleBean();
        TimeFilter timeFilter = createTimeFilterFrom9to15();
        timeFilter.setNeverBlockHttpDownload(true);
        saveTimeFilter(timeFilter);

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        verifyExplanation(ipRuleService.convertIpRuleBeanToFirmwareRule(ipRuleBean),
                ipRuleBean.getFirmwareConfig(), timeFilterConverter.convert(timeFilter), actualResult);
    }

    @Test
    public void resultIsNotBlocked_WhenTimeFilterTimeIsLocal() throws Exception {
        EstbFirmwareContext context = createContextWithTime(12);
        createAndSaveDefaultIpRuleBean();
        TimeFilter timeFilter = createTimeFilterFrom9to15();
        timeFilter.setLocalTime(true);
        saveTimeFilter(timeFilter);

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void resultIsNotBlocked_WhenTimeFilterIsUTCButContextTimeIsLocal() throws Exception {
        EstbFirmwareContext context = createContextWithTime(12);
        context.setTimeZoneOffset("-01:00");
        createAndSaveDefaultIpRuleBean();
        TimeFilter timeFilter = createTimeFilterFrom9to15();
        saveTimeFilter(timeFilter);

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void resultIsBlocked_WhenTimeFilterAndContextTimeBothInLocalFormat() throws Exception {
        EstbFirmwareContext context = createContextWithTime(12);
        context.setTimeZoneOffset("-01:00");
        IpRuleBean ipRuleBean = createAndSaveDefaultIpRuleBean();
        TimeFilter timeFilter = createTimeFilterFrom9to15();
        timeFilter.setLocalTime(true);
        saveTimeFilter(timeFilter);

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        verifyExplanation(ipRuleService.convertIpRuleBeanToFirmwareRule(ipRuleBean),
                ipRuleBean.getFirmwareConfig(), timeFilterConverter.convert(timeFilter), actualResult);
    }

    @Test
    public void resultIsNotBlocked_WhenContextIsInTimeFilterEnvModelWhiteList() throws Exception {
        EstbFirmwareContext context = createContextWithTime(12);
        createAndSaveDefaultIpRuleBean();
        TimeFilter timeFilter = createTimeFilterFrom9to15();
        timeFilter.setEnvModelWhitelist(createAndSaveEnvModelRuleBean(defaultEnvModelId, defaultEnvironmentId, defaultModelId,
                defaultFirmwareVersion, defaultFirmwareDownloadProtocol, ApplicationType.STB));
        saveTimeFilter(timeFilter);

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void resultIsNotBlocked_WhenContextIsInTimeFilterIpWhiteList() throws Exception {
        EstbFirmwareContext context = createContextWithTime(12);
        createAndSaveDefaultIpRuleBean();
        TimeFilter timeFilter = createTimeFilterFrom9to15();
        timeFilter.setIpWhitelist(createAndSaveIpAddressGroupExtended(Collections.singleton(defaultIpAddress)));
        saveTimeFilter(timeFilter);

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void configIsSetToRebootImmediatelyByContextForceFilter() throws Exception {
        EstbFirmwareContext context = createDefaultContext();
        context.setForceFilters(TemplateNames.REBOOT_IMMEDIATELY_FILTER);
        createAndSaveDefaultIpRuleBean();

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        expectedResult.setRebootImmediately(true);
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void configIsSetToRebootImmediatelyByRebootImmediatelyFilter_WhenMacsAreEqual() throws Exception {
        RebootImmediatelyFilter rebootImmediatelyFilter = createDefaultRebootImmediatelyFilter();
        rebootImmediatelyFilter.setMacAddresses(defaultMacAddress);

        verifyRebootImmediatelyValueInConfig(true, rebootImmediatelyFilter);
    }

    @Test
    public void configIsSetToRebootImmediatelyByRebootImmediatelyFilter_WhenIpIsInRange() throws Exception {
        RebootImmediatelyFilter rebootImmediatelyFilter = createDefaultRebootImmediatelyFilter();
        rebootImmediatelyFilter.setIpAddressGroups(Collections.singleton(createDefaultIpAddressGroup()));

        verifyRebootImmediatelyValueInConfig(true, rebootImmediatelyFilter);
    }

    @Test
    public void configIsSetToRebootImmediatelyByRebootImmediatelyFilter_WhenModelsAreEqual() throws Exception {
        RebootImmediatelyFilter rebootImmediatelyFilter = createDefaultRebootImmediatelyFilter();
        rebootImmediatelyFilter.setModels(Collections.singleton(createAndSaveModel(defaultModelId).getId()));

        verifyRebootImmediatelyValueInConfig(true, rebootImmediatelyFilter);
    }

    @Test
    public void configIsSetToRebootImmediatelyByRebootImmediatelyFilter_WhenEnvironmentsAreEqual() throws Exception {
        RebootImmediatelyFilter rebootImmediatelyFilter = createDefaultRebootImmediatelyFilter();
        rebootImmediatelyFilter.setEnvironments(Collections.singleton(createAndSaveEnvironment(defaultEnvironmentId).getId()));

        verifyRebootImmediatelyValueInConfig(true, rebootImmediatelyFilter);
    }

    @Test
    public void configIsNotSetToRebootImmediately_WhenFilterIsEmpty() throws Exception {
        RebootImmediatelyFilter rebootImmediatelyFilter = createDefaultRebootImmediatelyFilter();

        verifyRebootImmediatelyValueInConfig(false, rebootImmediatelyFilter);
    }

    @Test
    public void resultIsBlocked_WhenPercentFilterPercentageIs0AndWhiteListIsNull() throws Exception {
        IpRuleBean ipRuleBean = createAndSaveDefaultIpRuleBean();
        PercentFilterValue percentFilterValue = createPercentFilter(null, 0, null);
        savePercentFilter(percentFilterValue);
        EstbFirmwareContext context = createDefaultContext();

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        verifyExplanation(ipRuleService.convertIpRuleBeanToFirmwareRule(ipRuleBean),
                ipRuleBean.getFirmwareConfig(), percentFilterValue, actualResult);
    }

    @Test
    public void resultNotBlocked_WhenPercentFilterPercentageIs0AndWhiteListIsNullButPercentFilterIsInBypassFilters() throws Exception {
        IpRuleBean ipRuleBean = createAndSaveDefaultIpRuleBean();
        savePercentFilter(createPercentFilter(null, 0, null));
        EstbFirmwareContext context = createDefaultContext();
        context.setBypassFilters(EstbFirmwareRuleBase.PERCENT_FILTER_NAME);

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        verifyFirmwareConfig(ipRuleBean.getFirmwareConfig(), actualResult);
    }

    @Test
    public void resultIsBlocked_WhenPercentFilterPercentageIs0AndContextIpIsNotInWhiteList() throws Exception {
        String ipNotInWhiteList = "99.99.99.99";
        IpRuleBean ipRuleBean = createAndSaveDefaultIpRuleBean();
        PercentFilterValue percentFilterValue = createPercentFilter(
                createAndSaveIpAddressGroupExtended(ipNotInWhiteList, Collections.singleton(ipNotInWhiteList)),
                0, null);
        savePercentFilter(percentFilterValue);
        EstbFirmwareContext context = createDefaultContext();

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        verifyExplanation(ipRuleService.convertIpRuleBeanToFirmwareRule(ipRuleBean),
                ipRuleBean.getFirmwareConfig(), percentFilterValue, actualResult);
    }

    @Test
    public void resultIsNotBlocked_WhenPercentFilterPercentageIs100() throws Exception {
        IpRuleBean ipRuleBean = createAndSaveDefaultIpRuleBean();
        savePercentFilter(createPercentFilter(null, 100, null));
        EstbFirmwareContext context = createDefaultContext();

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        verifyFirmwareConfig(ipRuleBean.getFirmwareConfig(), actualResult);
    }

    @Test
    public void resultIsNotBlocked_WhenContextIpIsInPercentFilterWhiteList() throws Exception {
        IpRuleBean ipRuleBean = createAndSaveDefaultIpRuleBean();
        savePercentFilter(createPercentFilter(createAndSaveIpAddressGroupExtended(Collections.singleton(defaultIpAddress)), 100, null));
        EstbFirmwareContext context = createDefaultContext();

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        verifyFirmwareConfig(ipRuleBean.getFirmwareConfig(), actualResult);
    }

    @Test
    public void resultIsBlocked_WhenEnvModelPercentageIs0AndWhiteListIsNull() throws Exception {
        EnvModelRuleBean envModelRuleBean = createAndSaveDefaultEnvModelRuleBean();
        PercentFilterValue percentFilterValue = createPercentFilter(null, 100,
                Collections.singletonMap(envModelRuleBean.getName(), createDefaultEnvModelPercentage(0)));
        savePercentFilter(percentFilterValue);
        EstbFirmwareContext context = createDefaultContext();

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        verifyExplanationForDistributionRule(envModelRuleService.convertModelRuleBeanToFirmwareRule(envModelRuleBean), actualResult);
    }

    @Test
    public void resultIsBlocked_WhenEnvModelPercentageIs0AndContextIpIsNotInWhiteList() throws Exception {
        String ipNotInWhiteList = "99.99.99.99";
        EnvModelRuleBean envModelRuleBean = createAndSaveDefaultEnvModelRuleBean();
        EnvModelPercentage envModelPercentage = createDefaultEnvModelPercentage(0);
        envModelPercentage.setWhitelist(createAndSaveIpAddressGroupExtended(Collections.singleton(ipNotInWhiteList)));
        PercentFilterValue percentFilterValue = createPercentFilter(null, 100,
                Collections.singletonMap(envModelRuleBean.getName(), envModelPercentage));
        savePercentFilter(percentFilterValue);
        EstbFirmwareContext context = createDefaultContext();

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        verifyExplanationForDistributionRule(envModelRuleService.convertModelRuleBeanToFirmwareRule(envModelRuleBean), actualResult);
    }

    @Test
    public void resultIsNotBlocked_WhenEnvModelPercentageIs100() throws Exception {
        String ipNotInWhiteList = "99.99.99.99";
        EnvModelRuleBean envModelRuleBean = createAndSaveDefaultEnvModelRuleBean();
        EnvModelPercentage envModelPercentage = createDefaultEnvModelPercentage(100);
        envModelPercentage.setWhitelist(createAndSaveIpAddressGroupExtended(Collections.singleton(ipNotInWhiteList)));
        savePercentFilter(createPercentFilter(null, 0, Collections.singletonMap(envModelRuleBean.getName(), envModelPercentage)));
        EstbFirmwareContext context = createDefaultContext();

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        verifyFirmwareConfig(envModelRuleBean.getFirmwareConfig(), actualResult);
    }

    @Test
    public void resultIsNotBlocked_WhenContextIpIsInEnvModelPercentageWhiteList() throws Exception {
        EnvModelRuleBean envModelRuleBean = createAndSaveDefaultEnvModelRuleBean();
        EnvModelPercentage envModelPercentage = createDefaultEnvModelPercentage(0);
        IpAddressGroupExtended whitelist = createAndSaveIpAddressGroupExtended(Collections.singleton(defaultIpAddress));
        envModelPercentage.setWhitelist(whitelist);
        savePercentFilter(createPercentFilter(whitelist, 0, Collections.singletonMap(envModelRuleBean.getName(), envModelPercentage)));
        EstbFirmwareContext context = createDefaultContext();

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        verifyFirmwareConfig(envModelRuleBean.getFirmwareConfig(), actualResult);
    }

    @Test
    public void contextIsSetToRebootImmediatelyByPercentFilter() throws Exception {
        String someFirmwareVersionDifferentFromVersionInContext = "firmwareVersion42";
        EnvModelRuleBean envModelRuleBean = createAndSaveDefaultEnvModelRuleBean();
        EnvModelPercentage envModelPercentage = createDefaultEnvModelPercentage(0);
        envModelPercentage.setRebootImmediately(true);
        envModelPercentage.setFirmwareCheckRequired(true);
        envModelPercentage.setFirmwareVersions(Collections.singleton(someFirmwareVersionDifferentFromVersionInContext));
        savePercentFilter(createPercentFilter(null, 0, Collections.singletonMap(envModelRuleBean.getName(), envModelPercentage)));
        EstbFirmwareContext context = createDefaultContext();

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        expectedResult.setRebootImmediately(true);
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void resultIsNotBlockedByTimeFilter_WhenPercentFilterAddsTimeFilterIntoBypassFilters() throws Exception {
        String someFirmwareVersionDifferentFromVersionInContext = "firmwareVersion42";
        EstbFirmwareContext context = createContextWithTime(12);
        TimeFilter timeFilter = createTimeFilterFrom9to15();
        saveTimeFilter(timeFilter);
        EnvModelRuleBean envModelRuleBean = createAndSaveDefaultEnvModelRuleBean();
        EnvModelPercentage envModelPercentage = createDefaultEnvModelPercentage(0);
        envModelPercentage.setFirmwareCheckRequired(true);
        envModelPercentage.setFirmwareVersions(Collections.singleton(someFirmwareVersionDifferentFromVersionInContext));
        savePercentFilter(createPercentFilter(null, 0, Collections.singletonMap(envModelRuleBean.getName(), envModelPercentage)));

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        verifyFirmwareConfig(envModelRuleBean.getFirmwareConfig(), actualResult);
    }

    @Test
    public void roundRobinFilterSetsFullUrlHttpLocationAndFirmwareDownloadProtocolInConfig() throws Exception {
        defaultFirmwareDownloadProtocol = FirmwareConfig.DownloadProtocol.tftp;
        EstbFirmwareContext context = createDefaultContext();
        context.setCapabilities(Lists.newArrayList(
                Capabilities.supportsFullHttpUrl.name(),
                Capabilities.RCDL.name()));
        createAndSaveDefaultIpRuleBean();
        createAndSaveDefaultRoundRobinFilter();

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        expectedResult.setFirmwareDownloadProtocol(FirmwareConfig.DownloadProtocol.http);
        expectedResult.setFirmwareLocation(defaultHttpFullUrlLocation);
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void roundRobinFilterSetsHttpLocationAndFirmwareDownloadProtocolInConfig() throws Exception {
        defaultFirmwareDownloadProtocol = FirmwareConfig.DownloadProtocol.tftp;
        EstbFirmwareContext context = createDefaultContext();
        context.setCapabilities(Collections.singletonList(Capabilities.RCDL.name()));
        createAndSaveDefaultIpRuleBean();
        createAndSaveDefaultRoundRobinFilter();

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        expectedResult.setFirmwareDownloadProtocol(FirmwareConfig.DownloadProtocol.http);
        expectedResult.setFirmwareLocation(defaultHttpLocation);
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void roundRobinFilterDoesNotSetHttpLocation_WhenFirmwareVersionIsNotInFilterFirmwareVersionsList() throws Exception {
        String anotherFirmwareVersion = "firmwareVersionWhichIsNotInFilter";
        DownloadLocationRoundRobinFilterValue roundRobinFilterValue = createDefaultRoundRobinFilter();
        EstbFirmwareContext context = createDefaultContext();
        context.setFirmwareVersion(anotherFirmwareVersion);
        context.setCapabilities(Collections.singletonList(Capabilities.RCDL.name()));

        verifyRoundRobinFilterDoesNotSetHttpLocation(roundRobinFilterValue, context);
    }

    @Test
    public void roundRobinFilterDoesNotSetHttpLocation_WhenContextDoesNotContainRCDLCapability() throws Exception {
        EstbFirmwareContext context = createDefaultContext();
        DownloadLocationRoundRobinFilterValue roundRobinFilterValue = createDefaultRoundRobinFilter();

        verifyRoundRobinFilterDoesNotSetHttpLocation(roundRobinFilterValue, context);
    }

    @Test
    public void roundRobinFilterDoesNotSetHttpLocation_WhenFilterNeverUseHttpIsTrue() throws Exception {
        EstbFirmwareContext context = createDefaultContext();
        context.setCapabilities(Lists.newArrayList(Collections.singletonList(Capabilities.RCDL.name())));
        DownloadLocationRoundRobinFilterValue roundRobinFilterValue = createDefaultRoundRobinFilter();
        roundRobinFilterValue.setNeverUseHttp(true);

        verifyRoundRobinFilterDoesNotSetHttpLocation(roundRobinFilterValue, context);
    }

    @Test
    public void roundRobinFilterDoesNotSetHttpLocation_WhenContextModelIsInFilterRogueModelsList() throws Exception {
        EstbFirmwareContext context = createDefaultContext();
        context.setCapabilities(Lists.newArrayList(Collections.singletonList(Capabilities.RCDL.name())));
        DownloadLocationRoundRobinFilterValue roundRobinFilterValue = createDefaultRoundRobinFilter();
        roundRobinFilterValue.setRogueModels(Collections.singletonList(createDefaultModel()));

        verifyRoundRobinFilterDoesNotSetHttpLocation(roundRobinFilterValue, context);
    }

    @Test
    public void roundRobinFilterDoesNotSetHttpLocation_WhenFilterHttpLocationIsBlank() throws Exception {
        EstbFirmwareContext context = createDefaultContext();
        context.setCapabilities(Lists.newArrayList(Collections.singletonList(Capabilities.RCDL.name())));
        DownloadLocationRoundRobinFilterValue roundRobinFilterValue = createDefaultRoundRobinFilter();
        roundRobinFilterValue.setHttpLocation("");

        verifyRoundRobinFilterDoesNotSetHttpLocation(roundRobinFilterValue, context);
    }

    @Test
    public void roundRobinFilterSetsIPv4FirmwareLocation() throws Exception {
        EstbFirmwareContext context = createDefaultContext();
        createAndSaveDefaultIpRuleBean();
        DownloadLocationRoundRobinFilterValue roundRobinFilterValue = createDefaultRoundRobinFilter();
        roundRobinFilterValue.setIpv6locations(null);
        saveRoundRobinFilter(roundRobinFilterValue);

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        expectedResult.setFirmwareDownloadProtocol(FirmwareConfig.DownloadProtocol.tftp);
        expectedResult.setFirmwareLocation(defaultIpAddress);
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void roundRobinFilterSetsBothIPv4AndIPv6FirmwareLocations() throws Exception {
        EstbFirmwareContext context = createDefaultContext();
        createAndSaveDefaultIpRuleBean();
        createAndSaveDefaultRoundRobinFilter();

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        expectedResult.setFirmwareDownloadProtocol(FirmwareConfig.DownloadProtocol.tftp);
        expectedResult.setFirmwareLocation(defaultIpAddress);
        expectedResult.setIpv6FirmwareLocation(defaultIpv6Address);
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void downloadLocationFilterSetsTftpLocationInConfig_WhenDownloadProtocolIsTftpAndForceHttpIsFalseAndFirmwareLocationIsNotEmpty() throws Exception {
        defaultFirmwareDownloadProtocol = FirmwareConfig.DownloadProtocol.tftp;
        DownloadLocationFilter locationFilter = createDefaultDownloadLocationFilter();
        locationFilter.setHttpLocation(null);

        verifyDownloadLocationfilterSetsTftpLocationInConfig(locationFilter);
    }

    @Test
    public void downloadLocationFilterSetsTftpLocationAndFirmwareDownloadProtocolInConfig_WhenFilterHttpLocationIsNull() throws Exception {
        DownloadLocationFilter locationFilter = createDefaultDownloadLocationFilter();
        locationFilter.setHttpLocation(null);

        verifyDownloadLocationfilterSetsTftpLocationInConfig(locationFilter);
    }

    @Test
    public void downloadLocationFilterSetsIPv6FirmwareLocationFromRoundRobinFilter_WhenItsOwnIPv6FirmwareLocationIsNull() throws Exception {
        String ipv6FirmwareLocationIpForRoundRobinFilter = "66::66";
        EstbFirmwareContext context = createDefaultContext();
        createAndSaveDefaultIpRuleBean();
        DownloadLocationRoundRobinFilterValue downloadLocationRoundRobinFilterValue = createDefaultRoundRobinFilter();
        downloadLocationRoundRobinFilterValue.setIpv6locations(Collections.singletonList(createLocation(ipv6FirmwareLocationIpForRoundRobinFilter, 100)));
        saveRoundRobinFilter(downloadLocationRoundRobinFilterValue);
        DownloadLocationFilter locationFilter = createDefaultDownloadLocationFilter();
        locationFilter.setHttpLocation(null);
        locationFilter.setIpv6FirmwareLocation(null);
        saveDownloadLocationFilter(locationFilter);

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        expectedResult.setFirmwareDownloadProtocol(FirmwareConfig.DownloadProtocol.tftp);
        expectedResult.setFirmwareLocation(defaultIpAddress);
        expectedResult.setIpv6FirmwareLocation(ipv6FirmwareLocationIpForRoundRobinFilter);
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void downloadLocationFilterSetsHttpLocationAndFirmwareDownloadProtocolInConfig_WhenForceHttpIsTrue() throws Exception {
        defaultFirmwareDownloadProtocol = FirmwareConfig.DownloadProtocol.tftp;
        DownloadLocationFilter locationFilter = createDefaultDownloadLocationFilter();
        locationFilter.setForceHttp(true);

        verifyDownloadLocationFilterSetsHttpLocationInConfig(locationFilter);
    }

    @Test
    public void downloadLocationFilterSetsHttpLocationInConfig_WhenFirmwareDownloadProtocolIsHttp() throws Exception {
        DownloadLocationFilter locationFilter = createDefaultDownloadLocationFilter();
        locationFilter.setFirmwareLocation(null);
        locationFilter.setIpv6FirmwareLocation(null);

        verifyDownloadLocationFilterSetsHttpLocationInConfig(locationFilter);
    }

    @Test
    public void downloadLocationFilterSetsHttpLocationAndFirmwareDownloadProtocolInConfig_WhenFilterFirmwareLocationIsNull() throws Exception {
        defaultFirmwareDownloadProtocol = FirmwareConfig.DownloadProtocol.tftp;
        DownloadLocationFilter locationFilter = createDefaultDownloadLocationFilter();
        locationFilter.setFirmwareLocation(null);
        locationFilter.setIpv6FirmwareLocation(null);

        verifyDownloadLocationFilterSetsHttpLocationInConfig(locationFilter);
    }

    @Test
    public void downloadLocationFilterSetsHttpLocationAndSetsToNullIPv6FirmwareLocationWhichWasSetByRoundRobinFilterPreviously() throws Exception {
        EstbFirmwareContext context = createDefaultContext();
        createAndSaveDefaultIpRuleBean();
        createAndSaveDefaultRoundRobinFilter();
        DownloadLocationFilterWrapper locationFilter = createDefaultDownloadLocationFilter();
        locationFilter.setFirmwareLocation(null);
        locationFilter.setIpv6FirmwareLocation(null);
        saveDownloadLocationFilter(locationFilter);

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        expectedResult.setFirmwareLocation(defaultHttpLocation);
        expectedResult.setIpv6FirmwareLocation(null);
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void noMatchingRuleWasFound() throws Exception {
        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", createDefaultContext())).andReturn().getResponse().getContentAsString();

        assertTrue(actualResult.contains(getExplanationForNoMatchedRules()));
    }

    @Test
    public void noopRuleWasFound() throws Exception {
        IpRuleBean ipRuleBean = createDefaultIpRuleBean();
        ipRuleBean.setFirmwareConfig(null);
        saveIpRuleBean(ipRuleBean);

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", createDefaultContext())).andReturn().getResponse().getContentAsString();

        assertTrue(actualResult.contains(getExplanationForNoopRule(ipRuleService.convertIpRuleBeanToFirmwareRule(ipRuleBean))));
    }

    @Test
    public void macRuleHasHighestPriorityWhenMatchingRulesWithDifferentTypes() throws Exception {
        createAndSaveIpRuleBean("firmwareConfigFileNameForIpRule");
        MacRuleBean macRuleBean = createAndSaveMacRuleBean("firmwareConfigFileNameForMacRule");
        createAndSaveEnvModelRuleBean("firmwareConfigFileNameForEnvModelRule");

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", createDefaultContext())).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = macRuleBean.getFirmwareConfig();
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void macRuleHasHighestPriorityWhenMatchingRulesWithDifferentTypes2() throws Exception {
        MacRuleBean macRuleBean = createAndSaveMacRuleBean("firmwareConfigFileNameForMacRule");
        createAndSaveEnvModelRuleBean("firmwareConfigFileNameForEnvModelRule");

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", createDefaultContext())).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = macRuleBean.getFirmwareConfig();
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void macRuleHasHighestPriorityWhenMatchingRulesWithDifferentTypes3() throws Exception {
        createAndSaveIpRuleBean("firmwareConfigFileNameForIpRule");
        MacRuleBean macRuleBean = createAndSaveMacRuleBean("firmwareConfigFileNameForMacRule");

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", createDefaultContext())).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = macRuleBean.getFirmwareConfig();
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void ipRuleHasSecondPriorityWhenMatchingRulesWithDifferentTypes() throws Exception {
        IpRuleBean ipRuleBean = createAndSaveIpRuleBean("firmwareConfigFileNameForIpRule");
        createAndSaveEnvModelRuleBean("firmwareConfigFileNameForEnvModelRule");

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", createDefaultContext())).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = ipRuleBean.getFirmwareConfig();
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void ipRuleHasSecondPriorityWhenMatchingRulesWithDifferentTypes2() throws Exception {
        createAndSaveEnvModelRuleBean("firmwareConfigFileNameForEnvModelRule");
        IpRuleBean ipRuleBean = createAndSaveIpRuleBean("firmwareConfigFileNameForIpRule");

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", createDefaultContext())).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = ipRuleBean.getFirmwareConfig();
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    @Test
    public void estbMacIsEmpty_Throws500ByDefault() throws Exception {
        mockMvc.perform(get("/xconf/swu/stb").param("test", "test"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("\"eStbMac should be specified\""));
    }

    @Test
    public void estbMacIsEmpty_Throws500WhenVersionLessThan2() throws Exception {
        mockMvc.perform(
                get("/xconf/swu/stb").param("test", "test").param("version", "1.2"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("\"eStbMac should be specified\""));
    }

    @Test
    public void estbMacIsEmpty_Throws400WhenVersionGreaterThan2() throws Exception {
        mockMvc.perform(
                get("/xconf/swu/stb").param("test", "test").param("version", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("\"eStbMac should be specified\""));
    }

    @Test
    public void getBseConfig() throws Exception {
        IpRuleBean ipRuleBean = createAndSaveIpRuleBean("ipRuleFirmwareConfig");
        DownloadLocationFilter downloadLocationFilter = createDefaultDownloadLocationFilter();
        downloadLocationFilter.setHttpLocation("http://1.1.1.1");
        downloadLocationFilter.setForceHttp(true);
        downloadLocationFilter.setIpv6FirmwareLocation(null);
        saveDownloadLocationFilter(downloadLocationFilter);
        createAndSaveDefaultRoundRobinFilter();

        verifyBseResponse(ipRuleBean);
    }

    @Test
    public void getBseConfigIfIpAddressHasInListOperation() throws Exception {
        IpRuleBean ipRuleBean = createAndSaveIpRuleBean("ipRuleFirmwareConfig");
        createAndSaveDefaultDownloadLocationRule();
        createAndSaveDefaultRoundRobinFilter();

        verifyBseResponse(ipRuleBean);
    }

    @Test
    public void getBseConfigIfIpAddressHasInOperation() throws Exception {
        IpRuleBean ipRuleBean = createAndSaveIpRuleBean("ipRuleFirmwareConfig");
        createAndSaveDownloadLocationRuleWithInOperation();
        createAndSaveDefaultRoundRobinFilter();

        verifyBseResponse(ipRuleBean);
    }

    @Test
    public void getBseConfigIfIpAddressHasIsOperation() throws Exception {
        IpRuleBean ipRuleBean = createAndSaveIpRuleBean("ipRuleFirmwareConfig");
        createAndSaveDownloadLocationRuleWithIsOperation();
        createAndSaveDefaultRoundRobinFilter();

        verifyBseResponse(ipRuleBean);
    }

    @Test
    public void getBseConfigIfIpAddressIsWrong() throws Exception {
        createAndSaveIpRuleBean("ipRuleFirmwareConfig");
        createAndSaveDefaultDownloadLocationRule();
        createAndSaveDefaultRoundRobinFilter();

        mockMvc.perform(
                get("/xconf/swu/bse")
                        .param("ipAddress", "10.10.10.10"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getConfigWhenPartnerCaseInsensitive() throws Exception {
        FirmwareConfig firmwareConfig = createDefaultFirmwareConfig();
        save(firmwareConfig);
        String partnerId = "PARTNERID";
        FirmwareRule partnerFirmwareRule = createAndSavePartnerFirmwareRule(partnerId, firmwareConfig);
        EstbFirmwareContext context = createDefaultContext();
        context.setPartnerId("partnerId");

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();
        verifyFirmwareConfig(firmwareConfig, actualResult);

        context.setPartnerId("PartnerId");
        actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();
        verifyFirmwareConfig(firmwareConfig, actualResult);

        firmwareRuleTemplateDao.deleteOne(partnerFirmwareRule.getType());
    }

    public void definePropertiesAreAppliedFromRuleWithHigherPriority() throws Exception {
        createAndSaveIpRuleBean("firmwareConfigFileNameForIpRule");

        String highPriorityLocation = "highPriorityLocation";
        createAndSaveDefinePropertyTemplateAndRule("HighPriorityTemplate", 10, highPriorityLocation);
        createAndSaveDefinePropertyTemplateAndRule("LowerPriorityTemplate", 11, "lowPriorityLocation");

        String path = "$." + ConfigNames.FIRMWARE_LOCATION;
        mockMvc.perform(postContext("/xconf/swu/stb", createDefaultContext()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(path).value(highPriorityLocation));
    }

    private void verifyBseResponse(IpRuleBean ipRuleBean) throws Exception {
        BseConfiguration bseConfig = new BseConfiguration();
        bseConfig.setProtocol(FirmwareConfig.DownloadProtocol.http.toString());
        bseConfig.setLocation("http://1.1.1.1");
        bseConfig.setModelConfigurations(Collections.singletonList(new BseConfiguration.ModelFirmwareConfiguration(
                ipRuleBean.getModelId(), ipRuleBean.getFirmwareConfig().getFirmwareFilename(), ipRuleBean.getFirmwareConfig().getFirmwareVersion())));

        mockMvc.perform(
                get("/xconf/swu/bse")
                        .param("ipAddress", defaultIpAddress))
                .andExpect(status().isOk())
                .andExpect(content().json(CoreUtil.toJSON(bseConfig)));
    }

    private void verifyDownloadLocationFilterSetsHttpLocationInConfig(DownloadLocationFilter locationFilter) throws Exception {
        EstbFirmwareContext context = createDefaultContext();
        createAndSaveDefaultIpRuleBean();
        saveDownloadLocationFilter(locationFilter);

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        expectedResult.setFirmwareDownloadProtocol(FirmwareConfig.DownloadProtocol.http);
        expectedResult.setFirmwareLocation(defaultHttpLocation);
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    private void verifyDownloadLocationfilterSetsTftpLocationInConfig(DownloadLocationFilter locationFilter) throws Exception {
        EstbFirmwareContext context = createDefaultContext();
        createAndSaveDefaultIpRuleBean();
        saveDownloadLocationFilter(locationFilter);

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        expectedResult.setFirmwareDownloadProtocol(FirmwareConfig.DownloadProtocol.tftp);
        expectedResult.setFirmwareLocation(defaultIpAddress);
        expectedResult.setIpv6FirmwareLocation(defaultIpv6Address);
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    private void verifyRoundRobinFilterDoesNotSetHttpLocation(DownloadLocationRoundRobinFilterValue roundRobinFilterValue, EstbFirmwareContext context) throws Exception {
        createAndSaveDefaultIpRuleBean();
        saveRoundRobinFilter(roundRobinFilterValue);

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        expectedResult.setFirmwareDownloadProtocol(FirmwareConfig.DownloadProtocol.tftp);
        expectedResult.setFirmwareLocation(defaultIpAddress);
        expectedResult.setIpv6FirmwareLocation(defaultIpv6Address);
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    private static MockHttpServletRequestBuilder postContext(String url, EstbFirmwareContext context) throws Exception {
        MockHttpServletRequestBuilder form = post(url).characterEncoding("UTF-8").contentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = context.getContext();
        for (String key : map.keySet()) {
            List<String> values = map.get(key);
            for (String value : values) {
                form.param(key, value);
            }
        }

        return form;
    }

    private void nullifyRedundantFirmwareConfigFieldsBeforeAssert(FirmwareConfig config) {
        config.setId(null);
        config.setDescription(null);
        config.setSupportedModelIds(null);
        config.setUpdated(null);
        config.setApplicationType(null);
    }

    private IpRuleBean createAndSaveIpRuleBean(String firmwareConfigFilename) throws Exception {
        String firmwareConfigId = "ipRuleFirmwareConfigId";
        IpRuleBean ipRuleBean = createDefaultIpRuleBean();
        FirmwareConfig firmwareConfigForIpRule = createDefaultFirmwareConfig();
        firmwareConfigForIpRule.setId(firmwareConfigId);
        firmwareConfigForIpRule.setFirmwareFilename(firmwareConfigFilename);
        firmwareConfigDAO.setOne(firmwareConfigForIpRule.getId(), firmwareConfigForIpRule);
        ipRuleBean.setFirmwareConfig(firmwareConfigForIpRule);
        saveIpRuleBean(ipRuleBean);

        return ipRuleBean;
    }

    private MacRuleBean createAndSaveMacRuleBean(String firmwareFilename) throws Exception {
        String firmwareConfigId = "macRuleFirmwareConfigId";
        MacRuleBean macRuleBean = createDefaultMacRuleBean();
        FirmwareConfig firmwareConfigForMacRule = createDefaultFirmwareConfig();
        firmwareConfigForMacRule.setId(firmwareConfigId);
        firmwareConfigForMacRule.setFirmwareFilename(firmwareFilename);
        firmwareConfigDAO.setOne(firmwareConfigForMacRule.getId(), firmwareConfigForMacRule);
        macRuleBean.setFirmwareConfig(firmwareConfigForMacRule);
        saveMacRuleBean(macRuleBean);

        return macRuleBean;
    }

    private EnvModelRuleBean createAndSaveEnvModelRuleBean(String firmwareConfigFilename) throws Exception {
        String firmwareConfigId = "envModelRuleFirmwareConfigId";
        EnvModelRuleBean envModelRuleBean = createDefaultEnvModelRuleBean();
        FirmwareConfig firmwareConfigForEnvModelRule = createDefaultFirmwareConfig();
        firmwareConfigForEnvModelRule.setIpv6FirmwareLocation(firmwareConfigId);
        firmwareConfigForEnvModelRule.setFirmwareFilename(firmwareConfigFilename);
        firmwareConfigDAO.setOne(firmwareConfigForEnvModelRule.getId(), firmwareConfigForEnvModelRule);
        envModelRuleBean.setFirmwareConfig(firmwareConfigForEnvModelRule);
        saveEnvModelRuleBean(envModelRuleBean);

        return envModelRuleBean;
    }

    private FirmwareRuleTemplate createAndSaveDefinePropertyTemplateAndRule(String templateId, int priority, String propertyValue) throws Exception {
        FirmwareRuleTemplate template = templateFactory.createDownloadLocationTemplate();
        template.setId(templateId);
        template.setPriority(priority);
        firmwareRuleTemplateDao.setOne(template.getId(), template);

        FirmwareRule rule = new FirmwareRule();
        rule.setId(UUID.randomUUID().toString());
        rule.setType(templateId);
        rule.setRule(RuleFactory.newDownloadLocationFilter(defaultIpListId));
        rule.setApplicableAction(new DefinePropertiesAction(Collections.singletonMap(ConfigNames.FIRMWARE_LOCATION, propertyValue)));
        firmwareRuleDao.setOne(rule.getId(), rule);
        return template;
    }

    private String getExplanationForFilter(Object filter) {
        String filterStr = "";
        if (filter instanceof FirmwareRule) {
            filterStr = estbFirmwareLogger.toString((FirmwareRule) filter);
        } else if (filter instanceof PercentFilterValue) {
            // PercentFilter now is split into multiple rules so filter output would be different
            //filterStr = estbFirmwareLogger.toString((PercentFilterValue) filter);
        } else if (filter instanceof DownloadLocationRoundRobinFilterValue) {
            filterStr = LogsCompatibilityUtils.getRuleIdInfo(filter) + " " + ((SingletonFilterValue) filter).getId();
        }

        return "was blocked/modified by filter " + filterStr;
    }

    private String getExplanationForNoMatchedRules() {
        return "did not match any rule.";
    }

    private String getExplanationForNoopRule(FirmwareRule rule) {
        StringBuilder result = new StringBuilder();
        result.append("matched NO OP ")
                .append(getExplanationForRule(rule))
                .append("\n received NO config.");

        return CoreUtil.toJSON(result.toString()).substring(1, result.length() - 1);
    }

    private String getExplanationForRule(FirmwareRule rule) {
        return rule.getType() + " " + rule.getId() + ": " + rule.getName();
    }

    private String getExplanationForConfig(FirmwareConfigFacade config) {
        int lengthOfClassNameWithIdentityHash = FirmwareConfigFacade.class.getName().length() + 9;
        String withoutIdentityHash = config.toString().substring(lengthOfClassNameWithIdentityHash);

        return CoreUtil.toJSON(withoutIdentityHash).substring(1, withoutIdentityHash.length() - 1);
    }

    private void verifyFirmwareConfig(FirmwareConfig expectedConfig, String actualResult) throws Exception {
        nullifyRedundantFirmwareConfigFieldsBeforeAssert(expectedConfig);
        JSONAssert.assertEquals(CoreUtil.toJSON(expectedConfig), actualResult, true);
    }

    private void verifyExplanationForDistributionRule(FirmwareRule expectedRule, String actualResult) {
        assertTrue(actualResult.contains(getExplanationForRule(expectedRule)));
        assertTrue(actualResult.contains("and blocked by Distribution percent in RuleAction"));
    }

    private void verifyExplanation(FirmwareRule expectedRule, FirmwareConfig expectedConfig, Object expectedFilter, String actualResult) {
        assertTrue(actualResult.contains(getExplanationForRule(expectedRule)));
        assertTrue(actualResult.contains(getExplanationForConfig(new FirmwareConfigFacade(expectedConfig))));
        assertTrue(actualResult.contains(getExplanationForFilter(expectedFilter)));
    }

    private EstbFirmwareContext createContextWithTime(Integer hour) {
        EstbFirmwareContext result = createDefaultContext();
        result.setTime(new LocalDateTime(2016, 1, 1, hour, 0));

        return result;
    }

    private TimeFilter createTimeFilterFrom9to15() {
        return createDefaultTimeFilter("9", "15");
    }

    private void verifyRebootImmediatelyValueInConfig(Boolean expectedValue, RebootImmediatelyFilter rebootImmediatelyFilter) throws Exception {
        createAndSaveDefaultIpRuleBean();
        EstbFirmwareContext context = createDefaultContext();
        saveRebootImmediatelyFilter(rebootImmediatelyFilter);

        String actualResult = mockMvc.perform(postContext("/xconf/swu/stb", context)).andReturn().getResponse().getContentAsString();

        FirmwareConfig expectedResult = createDefaultFirmwareConfig();
        expectedResult.setRebootImmediately(expectedValue);
        verifyFirmwareConfig(expectedResult, actualResult);
    }

    private TimeFilter createAndSaveTimeFilterFrom9to15() throws Exception {
        TimeFilter timeFilter = createTimeFilterFrom9to15();
        saveTimeFilter(timeFilter);

        return timeFilter;
    }

    private IpAddressGroup createDefaultIpAddressGroup() {
        IpAddressGroup result = new IpAddressGroup();
        result.setId(defaultIpListId);
        result.setName(defaultIpListId);
        result.setIpAddresses(Collections.singleton(new IpAddress(defaultIpAddress)));

        return result;
    }

    private void saveTimeFilter(TimeFilter timeFilter) throws Exception {
        performPostRequest("/" + QueryConstants.UPDATE_FILTERS_TIME, new TimeFilterWrapper(timeFilter));
    }

    private void savePercentFilter(PercentFilterValue percentFilterValue) throws Exception {
        performPostRequest("/" + QueryConstants.UPDATE_FILTERS_PERCENT, new PercentFilterWrapper(percentFilterValue));
    }

    private DownloadLocationRoundRobinFilterValue createAndSaveDefaultRoundRobinFilter() throws Exception {
        DownloadLocationRoundRobinFilterValue roundRobinFilterValue = createDefaultRoundRobinFilter();
        saveRoundRobinFilter(roundRobinFilterValue);

        return roundRobinFilterValue;
    }

    private void saveRoundRobinFilter(DownloadLocationRoundRobinFilterValue roundRobinFilterValue) throws Exception {
        performPostRequest("/" + QueryConstants.UPDATE_FILTERS_DOWNLOADLOCATION, roundRobinFilterValue);
    }

    private EnvModelRuleBean createAndSaveDefaultEnvModelRuleBean() throws Exception {
        EnvModelRuleBean envModelRuleBean = createDefaultEnvModelRuleBean();
        saveEnvModelRuleBean(envModelRuleBean);

        return envModelRuleBean;
    }

    private void saveEnvModelRuleBean(EnvModelRuleBean envModelRuleBean) throws Exception {
        performPostRequest("/" + QueryConstants.UPDATE_RULES_ENV_MODEL, envModelRuleBean);
    }

    private void saveMacRuleBean(MacRuleBean macRuleBean) throws Exception {
        performPostRequest("/" + QueryConstants.UPDATE_RULES_MAC, macRuleBean);
    }

    private void saveIpFilter(IpFilter ipFilter) throws Exception {
        performPostRequest("/" + QueryConstants.UPDATE_FILTERS_IPS, ipFilter);
    }

    private void saveDownloadLocationFilter(DownloadLocationFilter locationFilter) throws Exception {
        performPostRequest("/" + QueryConstants.UPDATE_FILTERS_LOCATION, locationFilter);
    }

    private IpRuleBean createAndSaveDefaultIpRuleBean() throws Exception {
        IpRuleBean ipRuleBean = createDefaultIpRuleBean();
        saveIpRuleBean(ipRuleBean);

        return ipRuleBean;
    }

    private void saveIpRuleBean(IpRuleBean ipRuleBean) throws Exception {
        performPostRequest("/" + QueryConstants.UPDATE_RULES_IPS, ipRuleBean);
    }

    private void saveRebootImmediatelyFilter(RebootImmediatelyFilter rebootImmediatelyFilter) throws Exception {
        performPostRequest("/" + QueryConstants.UPDATES_FILTERS_RI, rebootImmediatelyFilter);
    }
}
