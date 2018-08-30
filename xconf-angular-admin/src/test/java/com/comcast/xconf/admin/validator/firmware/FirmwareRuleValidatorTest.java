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
package com.comcast.xconf.admin.validator.firmware;

import com.comcast.apps.hesperius.ruleengine.domain.additional.data.Time;
import com.comcast.apps.hesperius.ruleengine.main.impl.Rule;
import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.xconf.BaseIntegrationTest;
import com.comcast.xconf.estbfirmware.FirmwareConfig;
import com.comcast.xconf.estbfirmware.TemplateNames;
import com.comcast.xconf.estbfirmware.factory.RuleFactory;
import com.comcast.xconf.estbfirmware.factory.TemplateFactory;
import com.comcast.xconf.firmware.*;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.HashSet;

public class FirmwareRuleValidatorTest extends BaseIntegrationTest {
    public final static String CONFIG = "firmwareConfigId";
    public final static String IP = "121.0.0.1";
    public final static String MODEL = "X1";
    public final static String ENVIRONMENT = "QA";
    public final static String MAC_ADDRESS = "00:A0:C9:14:C8:29";

    @Autowired
    private FirmwareRuleValidator validator;

    @Autowired
    private TemplateFactory templateFactory;

    @Before
    public void setUp() throws Exception {
        firmwareConfigDAO.setOne(CONFIG, getFirmwareConfig());
    }

    private FirmwareConfig getFirmwareConfig() {
        FirmwareConfig config = new FirmwareConfig();
        config.setId(CONFIG);
        config.setFirmwareLocation("firmwareLocation");
        config.setFirmwareFilename("firmwareFilename");
        return config;
    }

    // --- MAC RULE ---------------------------------------------------------------------------------------------------
    @Test
    public void testValidMacRule() throws ValidationException {
        createTemplate(templateFactory.createMacRuleTemplate());
        final FirmwareRule firmwareRule = doFirmwareRuleWrapper(
                RuleFactory.newMacRule(MAC_ADDRESS),
                TemplateNames.MAC_RULE,
                new RuleAction(CONFIG));
        validator.validate(firmwareRule);
    }


    @Test
    public void testInvalidMacRule() throws ValidationException {
        createTemplate(templateFactory.createMacRuleTemplate());
        FirmwareRule firmwareRule = doFirmwareRuleWrapper(
                RuleFactory.newMacRule("123"),
                TemplateNames.MAC_RULE,
                new RuleAction(CONFIG));
        validateFirmwareRule("eStbMac is not valid: 123", firmwareRule);
    }

    // --- IP RULE ----------------------------------------------------------------------------------------------------

    @Test
    public void testValidIpRule() throws ValidationException {
        createTemplate(templateFactory.createIpRuleTemplate());
        FirmwareRule firmwareRule = doFirmwareRuleWrapper(
                RuleFactory.newIpRule(IP, ENVIRONMENT, MODEL),
                TemplateNames.IP_RULE,
                new RuleAction(CONFIG));
        validator.validate(firmwareRule);
    }

    @Test
    public void testIpRuleWithInvalidEnvironment() {
        FirmwareRule firmwareRule = doFirmwareRuleWrapper(
                RuleFactory.newIpRule(IP, "", MODEL),
                TemplateNames.IP_RULE,
                new RuleAction(CONFIG));
        validateFirmwareRule("env is empty", firmwareRule);
    }

    @Test
    public void testIpRuleWithInvalidIp() throws ValidationException {
        createTemplate(templateFactory.createIpRuleTemplate());
        FirmwareRule firmwareRule = doFirmwareRuleWrapper(
                RuleFactory.newIpRule("test", ENVIRONMENT, MODEL),
                TemplateNames.IP_RULE,
                new RuleAction(CONFIG));
        validateFirmwareRule("ipAddress is not valid: test", firmwareRule);
    }

    // --- ENV MODEL RULE ---------------------------------------------------------------------------------------------

    @Test
    public void testValidEnvModelRule() throws ValidationException {
        createTemplate(templateFactory.createEnvModelRuleTemplate());
        FirmwareRule firmwareRule = doFirmwareRuleWrapper(
                RuleFactory.newEnvModelRule(ENVIRONMENT, MODEL),
                TemplateNames.ENV_MODEL_RULE,
                new RuleAction(CONFIG));
        validator.validate(firmwareRule);
    }

    // --- TIME FILTER ------------------------------------------------------------------------------------------------

    @Test
    public void testValidTimeFilter() throws ValidationException {
        createTemplate(templateFactory.createTimeFilterTemplate());
        FirmwareRule firmwareRule = doFirmwareRuleWrapper(
                RuleFactory.newTimeFilter(
                        false, false, false, ENVIRONMENT, MODEL, null, new Time("00:00"), new Time("00:00")
                ),
                TemplateNames.TIME_FILTER,
                new BlockingFilterAction());
        validator.validate(firmwareRule);
    }

    @Test
    public void testTimeFilterWithInvalidTimeGTECondition() throws ValidationException {
        createTemplate(templateFactory.createTimeFilterTemplate());
        final Rule timeFilterRule = RuleFactory.newTimeFilter(
                false, false, false, ENVIRONMENT, MODEL, null, new Time("00:00"), new Time("00:00"));
        timeFilterRule.getCompoundParts().remove(1); // (time GTE 00:00:00)
        FirmwareRule firmwareRule = doFirmwareRuleWrapper(timeFilterRule, TemplateNames.TIME_FILTER, new BlockingFilterAction());
        validateFirmwareRule("time with GTE operation is required", firmwareRule);
    }

    @Test
    public void testTimeFilterWithInvalidTimeLTECondition() throws ValidationException {
        createTemplate(templateFactory.createTimeFilterTemplate());
        final Rule timeFilterRule = RuleFactory.newTimeFilter(
                false, false, false, ENVIRONMENT, MODEL, null, new Time("00:00"), new Time("00:00"));
        timeFilterRule.getCompoundParts().remove(2); // (time LTE 00:00:00)
        FirmwareRule firmwareRule = doFirmwareRuleWrapper(timeFilterRule, TemplateNames.TIME_FILTER, new BlockingFilterAction());
        validateFirmwareRule("time with LTE operation is required", firmwareRule);
    }

    // --- REBOOT IMMEDIATELY FILTER ----------------------------------------------------------------------------------

    @Test
    public void testValidRebootImmediatelyFilter() throws ValidationException {
        createTemplate(templateFactory.createRiFilterTemplate());
        FirmwareRule firmwareRule = doFirmwareRuleWrapper(
                RuleFactory.newRiFilter(
                        Sets.newHashSet(IP), Sets.newHashSet(MAC_ADDRESS), Sets.newHashSet(ENVIRONMENT),
                        Sets.newHashSet(MODEL)
                ),
                TemplateNames.REBOOT_IMMEDIATELY_FILTER,
                new DefinePropertiesAction(Collections.singletonMap("rebootImmediately", "false")));
        validator.validate(firmwareRule);
    }

    @Test
    public void testInvalidRebootImmediatelyFilter() throws ValidationException {
        createTemplate(templateFactory.createRiFilterTemplate());
        FirmwareRule firmwareRule = doFirmwareRuleWrapper(
                RuleFactory.newRiFilter(
                        new HashSet<String>(), new HashSet<String>(), Sets.newHashSet(ENVIRONMENT),
                        new HashSet<String>()
                ),
                TemplateNames.REBOOT_IMMEDIATELY_FILTER,
                new DefinePropertiesAction());
        validateFirmwareRule("Need to set ipAddress OR eStbMac OR env AND model", firmwareRule);
    }

    // --- MIN VERSION CHECK ------------------------------------------------------------------------------------------

    @Test
    public void testValidMinVersionCheck() throws ValidationException {
        createTemplate(templateFactory.createMinCheckRiTemplate());
        FirmwareRule firmwareRule = doFirmwareRuleWrapper(
                RuleFactory.newMinVersionCheckRule(ENVIRONMENT, MODEL, Sets.newHashSet("version_1")),
                TemplateNames.MIN_CHECK_RI,
                new DefinePropertiesAction());
        validator.validate(firmwareRule);
    }

    @Test
    public void testMinVersionCheckWithEmptyVersionList() throws ValidationException {
        createTemplate(templateFactory.createMinCheckRiTemplate());
        FirmwareRule firmwareRule = doFirmwareRuleWrapper(
                RuleFactory.newMinVersionCheckRule(ENVIRONMENT, MODEL, Sets.<String>newHashSet()),
                TemplateNames.MIN_CHECK_RI,
                new DefinePropertiesAction());
        validateFirmwareRule("firmwareVersion is empty", firmwareRule);
    }

    @Test
    public void testMinVersionCheckWithoutModelCondition() throws ValidationException {
        createTemplate(templateFactory.createMinCheckRiTemplate());
        final Rule minVersionCheck = RuleFactory.newMinVersionCheckRule(ENVIRONMENT, MODEL,
                Sets.newHashSet("version_1"));
        minVersionCheck.getCompoundParts().remove(1);
        FirmwareRule firmwareRule = doFirmwareRuleWrapper(minVersionCheck, TemplateNames.MIN_CHECK_RI, new DefinePropertiesAction());
        validateFirmwareRule("model field is required", firmwareRule);
    }

    // --- GLOBAL PERCENT  --------------------------------------------------------------------------------------------

    @Test
    public void testValidGlobalPercent() throws ValidationException {
        createTemplate(templateFactory.createGlobalPercentTemplate());
        FirmwareRule firmwareRule = doFirmwareRuleWrapper(
                RuleFactory.newGlobalPercentFilter(new Double("55"), IP), TemplateNames.GLOBAL_PERCENT, new BlockingFilterAction());
        validator.validate(firmwareRule);
    }

    @Test
    public void testGlobalPercentWithInvalidPercent() {
        final Rule globalPercentFilter = RuleFactory.newGlobalPercentFilter(new Double("101"), IP);
        FirmwareRule firmwareRule = doFirmwareRuleWrapper(globalPercentFilter, TemplateNames.GLOBAL_PERCENT, new BlockingFilterAction());
        validateFirmwareRule("eStbMac is not valid; 0.0 < value < 100.0", firmwareRule);
    }

    // --- IP FILTER --------------------------------------------------------------------------------------------------

    @Test
    public void testValidIpFilter() throws ValidationException {
        createTemplate(templateFactory.createIpFilterTemplate());
        FirmwareRule firmwareRule = doFirmwareRuleWrapper(
                RuleFactory.newIpFilter(IP), TemplateNames.IP_FILTER, new BlockingFilterAction());
        validator.validate(firmwareRule);
    }

    @Test
    public void testInvalidIpFilter() throws ValidationException {
        createTemplate(templateFactory.createIpFilterTemplate());
        FirmwareRule firmwareRule = doFirmwareRuleWrapper(
                RuleFactory.newIpFilter("test"), TemplateNames.IP_FILTER, new BlockingFilterAction());
        validateFirmwareRule("ipAddress is not valid: test", firmwareRule);
    }


    // --- Utility methods --------------------------------------------------------------------------------------------

    private void validateFirmwareRule(final String expected, final FirmwareRule firmwareRule) {
        try {
            validator.validate(firmwareRule);
        } catch (Exception e) {
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    private FirmwareRule doFirmwareRuleWrapper(final Rule rule, final String type, ApplicableAction applicableAction) {
        final FirmwareRule firmwareRule = new FirmwareRule();
        firmwareRule.setRule(rule);
        firmwareRule.setType(type);
        firmwareRule.setApplicableAction(applicableAction);
        firmwareRule.setApplicationType(ApplicationType.STB);
        return firmwareRule;
    }

    private void createTemplate(FirmwareRuleTemplate template) throws ValidationException {
        firmwareRuleTemplateDao.setOne(template.getId(), template);
    }
}
