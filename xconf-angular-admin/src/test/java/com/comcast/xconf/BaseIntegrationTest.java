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
 * Author: Stanislav Menshykov
 * Created: 2/10/16  3:54 PM
 */
package com.comcast.xconf;


import com.comcast.hesperius.dataaccess.core.cache.support.dao.ChangedKeysProcessingDAO;
import com.comcast.hesperius.dataaccess.core.dao.ICompositeDAO;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.hydra.astyanax.data.IPersistable;
import com.comcast.xconf.admin.contextconfig.TestContextConfig;
import com.comcast.xconf.estbfirmware.FirmwareConfig;
import com.comcast.xconf.estbfirmware.Model;
import com.comcast.xconf.estbfirmware.SingletonFilterValue;
import com.comcast.xconf.firmware.FirmwareRule;
import com.comcast.xconf.firmware.FirmwareRuleTemplate;
import com.comcast.xconf.logupload.*;
import com.comcast.xconf.logupload.settings.SettingProfile;
import com.comcast.xconf.logupload.settings.SettingRule;
import com.comcast.xconf.logupload.telemetry.PermanentTelemetryProfile;
import com.comcast.xconf.logupload.telemetry.TelemetryRule;
import com.comcast.xconf.permissions.DcmPermissionService;
import com.comcast.xconf.permissions.FirmwarePermissionService;
import com.comcast.xconf.permissions.TelemetryPermissionService;
import com.comcast.xconf.rfc.FeatureRule;
import com.comcast.xconf.service.GenericNamespacedListQueriesService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestContextConfig.class})
public class BaseIntegrationTest {

    public static final HashSet<String> STB_PERMISSIONS = Sets.newHashSet("read-common", "write-common", "read-firmware-stb", "write-firmware-stb", "read-dcm-stb", "write-dcm-stb", "read-telemetry-stb", "write-telemetry-stb");
    public static final HashSet<String> XHOME_PERMISSIONS = Sets.newHashSet("read-firmware-xhome", "write-firmware-xhome", "read-dcm-xhome", "write-dcm-xhome", "read-telemetry-xhome", "write-telemetry-xhome");

    @Autowired
    protected WebApplicationContext wac;

    protected MockMvc mockMvc;

    @Autowired
    protected ISimpleCachedDAO<String, Model> modelDAO;

    @Autowired
    protected ISimpleCachedDAO<String, Environment> environmentDAO;

    @Autowired
    protected ISimpleCachedDAO<String, LogFile> logFileDAO;

    @Autowired
    protected ISimpleCachedDAO<String, LogFilesGroup> logFilesGroupDAO;

    @Autowired
    protected ISimpleCachedDAO<String, DCMGenericRule> dcmRuleDAO;

    @Autowired
    protected ISimpleCachedDAO<String, UploadRepository> uploadRepositoryDAO;

    @Autowired
    protected ISimpleCachedDAO<String, LogUploadSettings> logUploadSettingsDAO;

    @Autowired
    protected ISimpleCachedDAO<String, DeviceSettings> deviceSettingsDAO;

    @Autowired
    protected ISimpleCachedDAO<String, FirmwareRuleTemplate> firmwareRuleTemplateDao;

    @Autowired
    protected ISimpleCachedDAO<String, FirmwareConfig> firmwareConfigDAO;

    @Autowired
    protected ISimpleCachedDAO<String, FirmwareRule> firmwareRuleDao;

    @Autowired
    protected ISimpleCachedDAO<String, VodSettings> vodSettingsDAO;

    @Autowired
    protected ISimpleCachedDAO<String, SettingProfile> settingProfileDao;

    @Autowired
    protected ISimpleCachedDAO<String, SettingRule> settingRuleDAO;

    @Autowired
    protected ISimpleCachedDAO<String, PermanentTelemetryProfile> permanentTelemetryDAO;

    @Autowired
    protected ISimpleCachedDAO<String, TelemetryRule> telemetryRuleDAO;

    @Autowired
    protected ISimpleCachedDAO<String, FeatureRule> featureRuleDAO;

    @Autowired
    protected ISimpleCachedDAO<String, GenericNamespacedList> genericNamespacedListDAO;

    @Autowired
    protected ISimpleCachedDAO<String, SingletonFilterValue> singletonFilterValueDAO;

    @Autowired
    protected ICompositeDAO<String, LogFile> indexesLogFilesDAO;

    @Autowired
    protected GenericNamespacedListQueriesService genericNamespacedListQueriesService;

    @Autowired
    protected ChangedKeysProcessingDAO changeLogDao;

    @Autowired
    @Deprecated
    protected ISimpleCachedDAO<String, Formula> formulaDAO;

    @Autowired
    @Deprecated
    protected ISimpleCachedDAO<String, IpAddressGroupExtended> ipAddressGroupDAO;

    @Autowired
    @Deprecated
    protected ISimpleCachedDAO<String, NamespacedList> namespacedListDAO;

    @Autowired
    @Deprecated
    protected ISimpleCachedDAO<String, com.comcast.xconf.estbfirmware.FirmwareRule> firmwareRuleDAO;

    @Autowired
    protected FirmwarePermissionService firmwarePermissionService;

    @Autowired
    protected DcmPermissionService dcmPermissionService;

    @Autowired
    private TelemetryPermissionService telemetryPermissionService;

    protected ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setup() {
        this.mockMvc = webAppContextSetup(this.wac).build();
        when(firmwarePermissionService.getPermissions()).thenReturn(STB_PERMISSIONS);
        when(dcmPermissionService.getPermissions()).thenReturn(STB_PERMISSIONS);
        when(telemetryPermissionService.getPermissions()).thenReturn(STB_PERMISSIONS);

    }

    @Before
    @After
    public void cleanData() throws NoSuchMethodException {
        List<? extends ISimpleCachedDAO<String, ? extends IPersistable>> daoList = Arrays.asList(
                modelDAO, environmentDAO, logFileDAO, logFilesGroupDAO, dcmRuleDAO, uploadRepositoryDAO,
                logUploadSettingsDAO, deviceSettingsDAO, firmwareRuleTemplateDao, firmwareConfigDAO,
                firmwareRuleDao, vodSettingsDAO, permanentTelemetryDAO, telemetryRuleDAO, featureRuleDAO, genericNamespacedListDAO,
                singletonFilterValueDAO, settingProfileDao, settingRuleDAO, formulaDAO, ipAddressGroupDAO,
                namespacedListDAO, firmwareRuleDAO
        );
        for (ISimpleCachedDAO<String, ? extends IPersistable> dao : daoList) {
            for (String key : dao.asLoadingCache().asMap().keySet()) {
                dao.deleteOne(key);
            }
        }
    }
}
