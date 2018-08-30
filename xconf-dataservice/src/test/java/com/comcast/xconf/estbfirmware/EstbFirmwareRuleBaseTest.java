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
package com.comcast.xconf.estbfirmware;

import com.comcast.apps.hesperius.ruleengine.RuleEngine;
import com.comcast.apps.hesperius.ruleengine.domain.additional.data.IpAddress;
import com.comcast.apps.hesperius.ruleengine.domain.additional.data.Time;
import com.comcast.apps.hesperius.ruleengine.main.api.IRuleProcessor;
import com.comcast.apps.hesperius.ruleengine.main.impl.Condition;
import com.comcast.apps.hesperius.ruleengine.main.impl.Rule;
import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.xconf.GenericNamespacedList;
import com.comcast.xconf.IpAddressGroupExtended;
import com.comcast.xconf.converter.GenericNamespacedListsConverter;
import com.comcast.xconf.firmware.ApplicationType;
import com.comcast.xconf.queries.beans.PercentFilterWrapper;
import com.google.common.collect.Sets;
import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.*;

/**
 * User: ikostrov
 * Date: 26.08.14
 * Time: 15:57
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:spring-servlet.xml",
        "classpath:spring-datastore.xml" })
@WebAppConfiguration
public class EstbFirmwareRuleBaseTest {

    public static final String ENV = "QA";
    public static final String MODEL = "X1";
    public static final IpAddress IPv4_LOCATION = new IpAddress("1.2.3.4");
    public static final String MIN_VERSION = "OldFirmwareVersion";
    public static final String NEW_VERSION = "NewFirmwareVersion";
    public static final String IP_ADDRESS_GROUP_ID = "ipAddressGroupId";

    @Autowired
    private ISimpleCachedDAO<String, FirmwareConfig> firmwareConfigDAO;
    @Autowired
    private EnvModelRuleService envModelRuleService;
    @Autowired
    private IpRuleService ipRuleService;
    @Autowired
    private PercentFilterService percentFilterService;
    @Autowired
    private EstbFirmwareRuleBase ruleBase;
    @Autowired
    protected ISimpleCachedDAO<String, GenericNamespacedList> genericNamespacedListDAO;

    private String configID = "ConfigID";

    @Test
    public void testTimeFilter() throws Exception {
        // start time is before end
        assertTimeRange("09:00", "15:00", 10, true);
        assertTimeRange("09:00", "15:00", 8, false);
        assertTimeRange("09:00", "15:00", 20, false);

        // start time is after end
        assertTimeRange("15:00", "09:00", 20, true);
        assertTimeRange("15:00", "09:00", 8, true);
        assertTimeRange("15:00", "09:00", 10, false);

        // env-model whitelist is exception for time filter even if it's in range
        assertEnvModelWhitelist("QA", "PR150BNC", false);
        assertEnvModelWhitelist("Test", "PR150BNC", true);
        assertEnvModelWhitelist("QA", "Test", true);
    }

    private void assertTimeRange(String start, String end, int hourOfDay, boolean matched) {
        Time startTime = new Time(start);
        Time endTime = new Time(end);

        EstbFirmwareContext context = new EstbFirmwareContext();
        LocalDateTime time = new LocalDateTime(2014, 8, 26, hourOfDay, 0);
        context.setTime(time);

        Rule rule = FirmwareRule.newTimeFilter(true, true, false, null, null, null, startTime, endTime);
        IRuleProcessor<Condition, Rule> processor = RuleEngine.getRuleProcessor();
        FirmwareRule matchedRule = (FirmwareRule)processor.find(Arrays.asList(rule), context.getProperties());

        if (matched) {
            String message = "Context should match TimeFilter. Start: " + start + ". End: " + end + ". Time:  " + time;
            Assert.assertNotNull(message, matchedRule);
        } else {
            String message = "Context should not match TimeFilter. Start: " + start + ". End: " + end + ". Time:  " + time;
            Assert.assertNull(message, matchedRule);
        }
    }

    private void assertEnvModelWhitelist(String env, String model, boolean matched) {
        Time startTime = new Time("09:00");
        Time endTime = new Time("15:00");
        int hourOfDay = 10;

        EstbFirmwareContext context = new EstbFirmwareContext();
        LocalDateTime time = new LocalDateTime(2014, 8, 26, hourOfDay, 0);
        context.setTime(time);
        context.setEnv(env);
        context.setModel(model);

        Rule rule = FirmwareRule.newTimeFilter(true, true, false, "QA", "PR150BNC", null, startTime, endTime);
        IRuleProcessor<Condition, Rule> processor = RuleEngine.getRuleProcessor();
        FirmwareRule matchedRule = (FirmwareRule)processor.find(Arrays.asList(rule), context.getProperties());

        if (matched) {
            String message = "EnvModel whitelist(QA-PR150BNC) should NOT match and TimeFilter must be applied. Env: " + env + ". Model: " + model;
            Assert.assertNotNull(message, matchedRule);
        } else {
            String message = "EnvModel whitelist(QA-PR150BNC) should match and TimeFilter not applied. Env: " + env + ". Model: " + model;
            Assert.assertNull(message, matchedRule);
        }
    }

    @Test
    public void testHasMinimumFirmware() throws Exception {
        String ruleName = "EnvModelRule";
        EstbFirmwareContext context = getContext(MIN_VERSION);
        FirmwareConfig config = createConfig();

        // check no matched rule
        Assert.assertTrue(ruleBase.hasMinimumFirmware(context));

        // check noop rule
        createEnvModelRule(ruleName, null);
        Assert.assertTrue(ruleBase.hasMinimumFirmware(context));

        createEnvModelRule(ruleName, config);
        savePercentFilter(ruleName, true);
        Assert.assertTrue(ruleBase.hasMinimumFirmware(context));

        context = getContext("unknown");
        Assert.assertFalse(ruleBase.hasMinimumFirmware(context));

        savePercentFilter(ruleName, false);
        context = getContext(MIN_VERSION);
        Assert.assertTrue(ruleBase.hasMinimumFirmware(context));

        context = getContext("unknown");
        createIpRule("IpRuleID", config);
        Assert.assertTrue(ruleBase.hasMinimumFirmware(context));
    }

    private EstbFirmwareContext getContext(String version) {
        EstbFirmwareContext context = new EstbFirmwareContext();
        context.seteStbMac("11:22:33:44:55:66");
        context.setIpAddress(IPv4_LOCATION.toString());
        context.setEnv(ENV);
        context.setModel(MODEL);
        context.setFirmwareVersion(version);
        return context;
    }

    private IpRuleBean createIpRule(String ruleName, FirmwareConfig config) throws ValidationException {
        IpRuleBean rule = new IpRuleBean();
        IpAddressGroupExtended ipAddressGroup = new IpAddressGroupExtended();
        ipAddressGroup.setId(IP_ADDRESS_GROUP_ID);
        ipAddressGroup.setName(IP_ADDRESS_GROUP_ID);
        ipAddressGroup.setIpAddresses(Sets.newHashSet(IPv4_LOCATION));
        GenericNamespacedList ipList = GenericNamespacedListsConverter.convertFromIpAddressGroupExtended(ipAddressGroup);
        genericNamespacedListDAO.setOne(ipList.getId(), ipList);
        rule.setId(ruleName);
        rule.setName(ruleName);
        rule.setIpAddressGroup(ipAddressGroup);
        rule.setEnvironmentId(ENV);
        rule.setModelId(MODEL);
        rule.setFirmwareConfig(config);
        ipRuleService.save(rule, ApplicationType.STB);
        return rule;
    }

    private EnvModelRuleBean createEnvModelRule(String ruleName, FirmwareConfig config) throws Exception {
        EnvModelRuleBean bean = new EnvModelRuleBean();
        bean.setId(ruleName);
        bean.setName(ruleName);
        bean.setEnvironmentId(ENV);
        bean.setModelId(MODEL);
        bean.setFirmwareConfig(config);
        envModelRuleService.save(bean, ApplicationType.STB);
        return bean;
    }

    private void savePercentFilter(String ruleName, boolean active) throws ValidationException {
        PercentFilterValue filter = new PercentFilterValue();
        Map<String, EnvModelPercentage> envModelPercentages = new HashMap<>();
        EnvModelPercentage envModelPercentage = new EnvModelPercentage();
        envModelPercentage.setPercentage(100.0);
        envModelPercentage.setActive(active);
        envModelPercentage.setFirmwareCheckRequired(true);
        envModelPercentage.setLastKnownGood(configID);
        envModelPercentage.setFirmwareVersions(new HashSet<>(Collections.singleton(MIN_VERSION)));
        envModelPercentages.put(ruleName, envModelPercentage);
        filter.setEnvModelPercentages(envModelPercentages);
        percentFilterService.save(new PercentFilterWrapper(filter), ApplicationType.STB);
    }

    private FirmwareConfig createConfig() throws ValidationException {
        FirmwareConfig config = new FirmwareConfig();
        config.setId(configID);
        config.setFirmwareVersion(NEW_VERSION);
        firmwareConfigDAO.setOne(config.getId(), config);
        return config;
    }
}
