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
 *  Created: 12/8/15 4:53 PM
 */
package com.comcast.xconf;

import com.comcast.hesperius.dataaccess.core.cache.CacheManager;
import com.comcast.hesperius.dataaccess.core.util.CoreUtil;
import com.comcast.xconf.admin.controller.RestUpdateDelete403FilterTest;
import com.comcast.xconf.admin.controller.common.EnvironmentControllerTest;
import com.comcast.xconf.admin.controller.common.IpListControllerTest;
import com.comcast.xconf.admin.controller.common.MacListControllerTest;
import com.comcast.xconf.admin.controller.common.ModelControllerTest;
import com.comcast.xconf.admin.controller.dcm.*;
import com.comcast.xconf.admin.controller.firmware.*;
import com.comcast.xconf.admin.controller.rfc.FeatureControllerTest;
import com.comcast.xconf.admin.controller.rfc.FeatureRuleControllerTest;
import com.comcast.xconf.admin.controller.setting.SettingProfileControllerTest;
import com.comcast.xconf.admin.controller.setting.SettingRuleControllerTest;
import com.comcast.xconf.admin.controller.setting.SettingTestPageControllerTest;
import com.comcast.xconf.admin.controller.shared.ChangeLogControllerTest;
import com.comcast.xconf.admin.controller.shared.StatisticsControllerTest;
import com.comcast.xconf.admin.controller.telemetry.PermanentProfileControllerTest;
import com.comcast.xconf.admin.controller.telemetry.TargetingRuleControllerTest;
import com.comcast.xconf.admin.validator.firmware.ApplicableActionValidatorTest;
import com.comcast.xconf.admin.validator.firmware.BaseRuleValidatorTest;
import com.comcast.xconf.admin.validator.firmware.FirmwareRuleValidatorTest;
import com.comcast.xconf.admin.validator.firmware.TemplateConsistencyValidatorTest;
import com.comcast.xconf.dcm.ruleengine.TelemetryProfileService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        // controller/common package
        EnvironmentControllerTest.class,
        IpListControllerTest.class,
        MacListControllerTest.class,
        ModelControllerTest.class,
        // controller/dcm
        DeviceSettingsControllerTest.class,
        FormulaQueryControllerTest.class,
        LogUploadSettingsControllerTest.class,
        UploadRepositoryControllerTest.class,
        VodSettingsControllerTest.class,
        // controller/firmware
        FirmwareConfigControllerTest.class,
        FirmwareRuleControllerTest.class,
        FirmwareRuleTemplateControllerTest.class,
        PercentageBeanControllerTest.class,
        PercentFilterControllerTest.class,
        RoundRobinFilterControllerTest.class,
        //rfc
        FeatureControllerTest.class,
        FeatureRuleControllerTest.class,
        // controller/settings
        SettingProfileControllerTest.class,
        SettingRuleControllerTest.class,
        SettingTestPageControllerTest.class,
        // controller/shared
        StatisticsControllerTest.class,
        ChangeLogControllerTest.class,
        // controller/telemetry
        PermanentProfileControllerTest.class,
        TargetingRuleControllerTest.class,
        // filter
        RestUpdateDelete403FilterTest.class,
        // validator/firmware
        ApplicableActionValidatorTest.class,
        BaseRuleValidatorTest.class,
        FirmwareRuleValidatorTest.class,
        TemplateConsistencyValidatorTest.class,
})
public class CompleteTestSuite {

    private static EmbCassandra embCassandra;

    public static final long telemetryProfileServiceExpireTimeMs = 1000L;

    @BeforeClass
    public static void startEmbeddedCassandra() throws IOException {
        embCassandra = new EmbCassandra();
        embCassandra.start();
        CacheManager.initCaches(CoreUtil.CF_DEFINITIONS);
        TelemetryProfileService.expireTime = telemetryProfileServiceExpireTimeMs;
    }

    @AfterClass
    public static void stopEmbeddedCassandra() throws IOException, InterruptedException {
        if (embCassandra != null) {
            embCassandra.stop();
        }
    }
}
