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
 * Author: ikostrov
 * Created: 28.08.15 19:06
*/
package com.comcast.xconf.queries.controllers;

import com.comcast.apps.hesperius.ruleengine.domain.additional.data.IpAddress;
import com.comcast.apps.hesperius.ruleengine.domain.additional.data.IpAddressGroup;
import com.comcast.apps.hesperius.ruleengine.domain.standard.StandardFreeArgType;
import com.comcast.apps.hesperius.ruleengine.domain.standard.StandardOperation;
import com.comcast.apps.hesperius.ruleengine.main.api.FixedArg;
import com.comcast.apps.hesperius.ruleengine.main.api.FreeArg;
import com.comcast.apps.hesperius.ruleengine.main.api.Operation;
import com.comcast.apps.hesperius.ruleengine.main.api.Relation;
import com.comcast.apps.hesperius.ruleengine.main.impl.Condition;
import com.comcast.apps.hesperius.ruleengine.main.impl.Rule;
import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.dao.ICompositeDAO;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.hesperius.dataaccess.core.util.CoreUtil;
import com.comcast.hydra.astyanax.data.IPersistable;
import com.comcast.xconf.*;
import com.comcast.xconf.converter.GenericNamespacedListsConverter;
import com.comcast.xconf.dcm.manager.web.FormulaDataObject;
import com.comcast.xconf.dcm.ruleengine.SettingsDAO;
import com.comcast.xconf.estbfirmware.*;
import com.comcast.xconf.estbfirmware.converter.PercentageBeanConverter;
import com.comcast.xconf.estbfirmware.factory.RuleFactory;
import com.comcast.xconf.firmware.*;
import com.comcast.xconf.firmware.FirmwareRule;
import com.comcast.xconf.logupload.*;
import com.comcast.xconf.logupload.settings.SettingProfile;
import com.comcast.xconf.logupload.settings.SettingRule;
import com.comcast.xconf.logupload.telemetry.PermanentTelemetryProfile;
import com.comcast.xconf.logupload.telemetry.TelemetryRule;
import com.comcast.xconf.queries.beans.DownloadLocationFilterWrapper;
import com.comcast.xconf.rfc.Feature;
import com.comcast.xconf.rfc.FeatureRule;
import com.comcast.xconf.rfc.FeatureSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.joda.time.LocalTime;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import java.util.*;

import static com.comcast.apps.hesperius.ruleengine.domain.standard.StandardOperation.IS;
import static com.comcast.xconf.estbfirmware.factory.RuleFactory.MODEL;
import static com.comcast.xconf.firmware.ApplicationType.STB;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:spring-servlet.xml",
        "classpath:spring-datastore.xml"})
public abstract class BaseQueriesControllerTest {

    @Autowired
    protected ISimpleCachedDAO<String, FirmwareConfig> firmwareConfigDAO;
    @Autowired
    protected ISimpleCachedDAO<String, Model> modelDAO;
    @Autowired
    protected ISimpleCachedDAO<String, com.comcast.xconf.firmware.FirmwareRule> firmwareRuleDao;
    @Autowired
    protected ISimpleCachedDAO<String, Environment> environmentDAO;
    @Autowired
    protected ISimpleCachedDAO<String, VodSettings> vodSettingsDAO;
    @Autowired
    protected ISimpleCachedDAO<String, DeviceSettings> deviceSettingsDAO;
    @Autowired
    protected ISimpleCachedDAO<String, LogUploadSettings> logUploadSettingsDAO;
    @Autowired
    protected ISimpleCachedDAO<String, SingletonFilterValue> singletonFilterValueDAO;
    @Autowired
    protected ISimpleCachedDAO<String, GenericNamespacedList> genericNamespacedListDAO;
    @Autowired
    protected ISimpleCachedDAO<String, DCMGenericRule> dcmRuleDAO;
    @Autowired
    protected PercentFilterService percentFilterService;
    @Autowired
    protected ISimpleCachedDAO<String, LogFile> logFileDAO;
    @Autowired
    protected ISimpleCachedDAO<String, LogFileList> logFileListDAO;
    @Autowired
    protected ISimpleCachedDAO<String, IpAddressGroupExtended> ipAddressGroupDAO;
    @Autowired
    protected ISimpleCachedDAO<String, FilterAction> filterActionDAO;
    @Autowired
    protected ICompositeDAO<String, LogFile> indexesLogFilesDAO;
    @Autowired
    protected ISimpleCachedDAO<String, FirmwareRuleTemplate> firmwareRuleTemplateDao;
    @Autowired
    protected ISimpleCachedDAO<String, Feature> featureDAO;
    @Autowired
    protected ISimpleCachedDAO<String, FeatureSet> featureSetDAO;
    @Autowired
    protected ISimpleCachedDAO<String, FeatureRule> featureRuleDAO;
    @Autowired
    protected PercentageBeanQueriesService percentageBeanQueriesService;
    @Autowired
    protected ISimpleCachedDAO<String, UploadRepository> uploadRepositoryDAO;
    @Autowired
    protected ISimpleCachedDAO<String, SettingProfile> settingProfileDao;
    @Autowired
    protected ISimpleCachedDAO<String, SettingRule> settingRuleDAO;
    @Autowired
    protected ISimpleCachedDAO<String, PermanentTelemetryProfile> permanentTelemetryDAO;
    @Autowired
    protected ISimpleCachedDAO<String, TelemetryRule> telemetryRuleDAO;

    @Autowired
    protected PercentageBeanConverter converter;

    @Autowired
    protected SettingsDAO settingsDAO;

    @Autowired
    protected EnvModelRuleService envModelRuleService;

    @Autowired
    protected BlockingFilterQueriesController blockingFilterController;

    @Autowired
    protected MacRuleService macRuleService;

    @Autowired
    protected RebootImmediatelyFilterService rebootImmediatelyFilterService;

    @Autowired
    protected WebApplicationContext wac;

    protected MockMvc mockMvc;

    protected String defaultModelId = "modelId";
    protected String defaultEnvironmentId = "environmentId";
    protected String defaultEnvModelId = "envModelId";
    protected String defaultIpFilterId = "ipFilterId";
    protected String defaultTimeFilterId = "timeFilterId";
    protected String defaultRebootImmediatelyFilterId = "rebootImmediatelyFilterId";
    protected String defaultFirmwareVersion = "firmwareVersion";
    protected String contextFirmwareVersion = "contextFirmwareVersion";
    protected String defaultIpRuleId = "ipRuleId";
    protected String defaultMacRuleId = "macRuleId";
    protected String defaultDownloadLocationFilterId = "dowloadLocationFilterId";
    protected String defaultIpListId = "ipListId";
    protected String defaultMacListId = "macListId";
    protected String defaultIpAddress = "1.1.1.1";
    protected String defaultIpv6Address = "::1";
    protected String defaultMacAddress = "11:11:11:11:11:11";
    protected String defaultHttpLocation = "httpLocation.com";
    protected String defaultHttpFullUrlLocation = "http://fullUrlLocation.com";
    protected String defaultFormulaId = "defaultFormulaObject";
    protected String defaultFirmwareConfigId = "firmwareConfigId";
    protected FirmwareConfig.DownloadProtocol defaultFirmwareDownloadProtocol = FirmwareConfig.DownloadProtocol.http;
    protected String defaultDeviceSettingName = "deviceSettingsName";
    protected String defaultLogUploadSettingName = "logUploadSettingsName";

    protected final static String APPLICATION_TYPE_PARAM = "applicationType";
    public static final String API_VERSION = "2";
    protected static final String WRONG_APPLICATION = "wrongVersion";

    @Before
    public void setup() {
        this.mockMvc = webAppContextSetup(this.wac).build();
    }

    protected ObjectMapper mapper = new ObjectMapper();

    @Before
    @After
    public void cleanData() throws NoSuchMethodException {
        deleteAllEntities();
    }

    protected void deleteAllEntities() {
        ArrayList<? extends ISimpleCachedDAO<String, ? extends IPersistable>> daoList = Lists.newArrayList(
                firmwareConfigDAO,
                modelDAO, ipAddressGroupDAO, environmentDAO,
                dcmRuleDAO, vodSettingsDAO,
                deviceSettingsDAO, logUploadSettingsDAO,
                genericNamespacedListDAO, logFileDAO, logFileListDAO,
                firmwareRuleDao, singletonFilterValueDAO,
                filterActionDAO, featureRuleDAO, featureDAO,
                uploadRepositoryDAO, settingProfileDao, settingRuleDAO,
                permanentTelemetryDAO, telemetryRuleDAO
        );
        for (ISimpleCachedDAO<String, ? extends IPersistable> dao : daoList) {
            for (String key : dao.asLoadingCache().asMap().keySet()) {
                dao.deleteOne(key);
            }
        }
    }

    protected void performPostRequestAndVerify(String path, Integer statusCode, Object obj) throws Exception {
        mockMvc.perform(
                post("/" + path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CoreUtil.toJSON(obj)))
                .andExpect(status().is(statusCode));
    }

    protected void performRequestAndVerifyResponse(String path, Object obj) throws Exception {
        String expectedContent = CoreUtil.toJSON(obj);
        mockMvc.perform(
                get("/" + path).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedContent)).andReturn();

        mockMvc.perform(
                get("/" + path).accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_XML)).andReturn();
    }

    protected void performRequestAndVerifyResponse(String path, String uriVariable, Object obj) throws Exception {
        String expectedContent = CoreUtil.toJSON(obj);
        mockMvc.perform(
                get("/" + path, uriVariable).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedContent));

        mockMvc.perform(
                get("/" + path, uriVariable).accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_XML));
    }

    protected ResultActions performPostRequest(final String path, final Object entity, HttpHeaders headers) throws Exception {
        final MockHttpServletRequestBuilder requestBuilder = post(path).contentType(MediaType.APPLICATION_JSON);
        if (headers != null) {
            requestBuilder.headers(headers);
        }
        if(entity != null) {
            requestBuilder.content(CoreUtil.toJSON(entity));
        }

        return mockMvc.perform(requestBuilder);
    }

    protected ResultActions performPostRequest(final String path, final Object entity) throws Exception {
        return performPostRequest(path, entity, null);
    }

    protected Model createDefaultModel() {
        return createModel(defaultModelId);
    }

    protected EnvModelRuleBean createDefaultEnvModelRuleBean() throws ValidationException {
        return createEnvModelRuleBean(defaultEnvModelId, defaultEnvironmentId, defaultModelId, defaultFirmwareVersion, defaultFirmwareDownloadProtocol);
    }

    protected EnvModelRuleBean createEnvModelRuleBean(String envModelId,
                                                      String environmentId,
                                                      String modelId,
                                                      String firmwareVersion,
                                                      FirmwareConfig.DownloadProtocol firmwareDownloadProtocol) throws ValidationException {
        EnvModelRuleBean result = new EnvModelRuleBean();
        result.setId(envModelId);
        result.setEnvironmentId(createAndSaveEnvironment(environmentId).getId());
        result.setModelId(createAndSaveModel(modelId).getId());
        result.setFirmwareConfig(createAndSaveFirmwareConfig(firmwareVersion, modelId, firmwareDownloadProtocol));
        result.setNoop(false);
        result.setName("envModelRuleName");

        return result;
    }

    protected EnvModelRuleBean createAndSaveEnvModelRuleBean(String envModelId,
                                                             String environmentId,
                                                             String modelId,
                                                             String firmwareVersion,
                                                             FirmwareConfig.DownloadProtocol firmwareDownloadProtocol,
                                                             String applicationType) throws ValidationException {
        EnvModelRuleBean result = createEnvModelRuleBean(envModelId, environmentId, modelId, firmwareVersion, firmwareDownloadProtocol);
        envModelRuleService.save(result, applicationType);

        return result;
    }

    protected FirmwareConfig createDefaultFirmwareConfig() throws ValidationException {
        return createFirmwareConfig(defaultFirmwareVersion, defaultModelId, defaultFirmwareDownloadProtocol);
    }

    protected FirmwareConfig createFirmwareConfig(String firmwareVersion, String modelId, FirmwareConfig.DownloadProtocol firmwareDownloadProtocol) throws ValidationException {
        FirmwareConfig firmwareConfig = new FirmwareConfig();
        firmwareConfig.setId(UUID.randomUUID().toString());
        firmwareConfig.setDescription("FirmwareDescription");
        firmwareConfig.setFirmwareFilename("FirmwareFilename");
        firmwareConfig.setFirmwareVersion(firmwareVersion);
        firmwareConfig.setFirmwareDownloadProtocol(firmwareDownloadProtocol);
        firmwareConfig.setApplicationType(STB);
        HashSet<String> supportedModels = new HashSet<>();
        supportedModels.add(createAndSaveModel(modelId).getId());
        firmwareConfig.setSupportedModelIds(supportedModels);

        return firmwareConfig;
    }

    protected FirmwareConfig createFirmwareConfig(String id) throws ValidationException {
        FirmwareConfig firmwareConfig = createFirmwareConfig("version", "MODEL_ID", FirmwareConfig.DownloadProtocol.http);
        firmwareConfig.setId(id);
        return firmwareConfig;
    }

    protected FirmwareConfig createAndSaveFirmwareConfig(String firmwareVersion, String modelId, FirmwareConfig.DownloadProtocol firmwareDownloadProtocol) throws ValidationException {
        FirmwareConfig config = createFirmwareConfig(firmwareVersion, modelId, firmwareDownloadProtocol);
        return save(config);
    }

    protected FirmwareConfig createAndSaveFirmwareConfig(String firmwareVersion, String modelId, FirmwareConfig.DownloadProtocol firmwareProtocol, String applicationType) throws ValidationException {
        FirmwareConfig firmwareConfig = createFirmwareConfig(firmwareVersion, modelId, firmwareProtocol);
        firmwareConfig.setApplicationType(applicationType);
        return save(firmwareConfig);
    }

    protected FirmwareConfig save(FirmwareConfig config) throws ValidationException {
        return firmwareConfigDAO.setOne(config.getId(), config);
    }

    protected IpAddressGroupExtended createDefaultIpAddressGroupExtended() {
        return createIpAddressGroupExtended(Collections.singleton(defaultIpAddress));
    }

    protected IpAddressGroupExtended createAndSaveDefaultIpAddressGroupExtended() throws ValidationException {
        IpAddressGroupExtended result = createDefaultIpAddressGroupExtended();
        GenericNamespacedList ipList = GenericNamespacedListsConverter.convertFromIpAddressGroupExtended(result);
        genericNamespacedListDAO.setOne(ipList.getId(), ipList);

        return result;
    }

    protected IpAddressGroupExtended createIpAddressGroupExtended(Set<String> stringIpAddresses) {
        return createIpAddressGroupExtended(defaultIpAddress, stringIpAddresses);
    }

    protected IpAddressGroupExtended createIpAddressGroupExtended(String name, Set<String> stringIpAddresses) {
        IpAddressGroupExtended ipAddressGroup = new IpAddressGroupExtended();
        ipAddressGroup.setId(name);
        ipAddressGroup.setName(name);
        Set<IpAddress> addresses = new HashSet<>();
        for (String ipAddress : stringIpAddresses) {
            addresses.add(new IpAddress(ipAddress));
        }
        ipAddressGroup.setIpAddresses(addresses);
        return ipAddressGroup;
    }

    protected IpAddressGroupExtended createAndSaveIpAddressGroupExtended(Set<String> stringIpAddresses) throws ValidationException {
        return createAndSaveIpAddressGroupExtended(defaultIpListId, stringIpAddresses);
    }

    protected IpAddressGroupExtended createAndSaveIpAddressGroupExtended(String name, Set<String> stringIpAddresses) throws ValidationException {
        IpAddressGroupExtended result = createIpAddressGroupExtended(name, stringIpAddresses);
        GenericNamespacedList ipList = GenericNamespacedListsConverter.convertFromIpAddressGroupExtended(result);
        genericNamespacedListDAO.setOne(ipList.getId(), ipList);

        return result;
    }

    protected Environment createEnvironment(String id) {
        Environment environment = new Environment();
        environment.setId(id);
        environment.setDescription("descr");
        return environment;
    }

    protected Environment createAndSaveEnvironment(String id) throws ValidationException {
        Environment environment = createEnvironment(id);
        environmentDAO.setOne(environment.getId(), environment);

        return environment;
    }

    protected IpRuleBean createIpRuleBean(String ipRuleId,
                                          String environmentId,
                                          String firmwareVersion,
                                          String modelId,
                                          FirmwareConfig.DownloadProtocol firmwareDownloadProtocol,
                                          Set<String> ipAddresses) throws ValidationException {
        IpRuleBean ipRuleBean = new IpRuleBean();
        ipRuleBean.setId(ipRuleId);
        ipRuleBean.setEnvironmentId(createAndSaveEnvironment(environmentId).getId());
        ipRuleBean.setFirmwareConfig(createAndSaveFirmwareConfig(firmwareVersion, modelId, firmwareDownloadProtocol));
        ipRuleBean.setIpAddressGroup(createAndSaveIpAddressGroupExtended(ipAddresses));
        ipRuleBean.setModelId(createModel(modelId).getId());
        ipRuleBean.setName("IpRuleName");
        ipRuleBean.setNoop(false);

        return ipRuleBean;
    }

    protected IpRuleBean createDefaultIpRuleBean() throws ValidationException {
        return createIpRuleBean(defaultIpRuleId, defaultEnvironmentId, defaultFirmwareVersion, defaultModelId, defaultFirmwareDownloadProtocol, Collections.singleton(defaultIpAddress));
    }

    protected Model createModel(String id) {
        Model model = new Model();
        model.setId(id);
        model.setDescription("ModelDescription");

        return model;
    }

    protected Model createAndSaveModel(String id) throws ValidationException {
        Model model = createModel(id);
        modelDAO.setOne(model.getId(), model);

        return model;
    }

    protected EstbFirmwareContext createDefaultContext() {
        return createContext(contextFirmwareVersion, defaultModelId, defaultEnvironmentId, defaultIpAddress, defaultMacAddress);
    }

    protected EstbFirmwareContext createContext(String firmwareVersion, String modelId, String environmentId, String ipAddress, String eStbMac) {
        EstbFirmwareContext context = new EstbFirmwareContext();
        context.setFirmwareVersion(firmwareVersion);
        context.setModel(modelId);
        context.setEnv(environmentId);
        context.setIpAddress(ipAddress);
        context.seteStbMac(eStbMac);

        return context;
    }

    protected NamespacedList createMacList(String id, Set<String> macAddresses) {
        NamespacedList result = new NamespacedList();
        result.setId(id);
        result.setData(macAddresses);

        return result;
    }

    protected NamespacedList createAndSaveMacList(String id, Set<String> macAddresses) throws ValidationException {
        NamespacedList result = createMacList(id, macAddresses);
        GenericNamespacedList macList = GenericNamespacedListsConverter.convertFromNamespacedList(result);
        genericNamespacedListDAO.setOne(macList.getId(), macList);

        return result;
    }

    protected MacRuleBean createMacRuleBean(String macRuleId,
                                            String firmwareVersion,
                                            String modelId,
                                            FirmwareConfig.DownloadProtocol firmwareDownloadProtocol,
                                            String macListId,
                                            Set<String> macAddresses) throws ValidationException {
        MacRuleBean result = new MacRuleBean();
        result.setId(macRuleId);
        result.setMacListRef(createAndSaveMacList(macListId, macAddresses).getId());
        result.setFirmwareConfig(createAndSaveFirmwareConfig(firmwareVersion, modelId, firmwareDownloadProtocol));
        result.setTargetedModelIds(Collections.singleton(createAndSaveModel(modelId).getId()));
        result.setName("macRuleName");

        return result;
    }

    protected MacRuleBean createDefaultMacRuleBean() throws ValidationException {
        return createMacRuleBean(defaultMacRuleId, defaultFirmwareVersion, defaultModelId, defaultFirmwareDownloadProtocol, "macListId", Collections.singleton(defaultMacAddress));
    }

    protected IpFilter createIpFilter(String id, String name, Set<String> ipAddresses) throws Exception {
        IpFilter result = new IpFilter();
        result.setId(id);
        result.setName(name);
        result.setIpAddressGroup(createAndSaveIpAddressGroupExtended(name, ipAddresses));

        return result;
    }

    protected IpFilter createDefaultIpFilter() throws Exception {
        return createIpFilter(defaultIpFilterId, "ipFilterName", Collections.singleton(defaultIpAddress));
    }

    protected TimeFilter createTimeFilter(String id,
                                          String startTime,
                                          String endTime,
                                          Boolean isLocalTime,
                                          Boolean neverBlockHttpDownload,
                                          Boolean neverBlockRebootDecoupled) {
        TimeFilter result = new TimeFilter();
        result.setId(id);
        result.setName("TimeFilterName");
        result.setStart(new LocalTime(startTime));
        result.setEnd(new LocalTime(endTime));
        result.setLocalTime(isLocalTime);
        result.setNeverBlockHttpDownload(neverBlockHttpDownload);
        result.setNeverBlockRebootDecoupled(neverBlockRebootDecoupled);

        return result;
    }

    protected TimeFilter createDefaultTimeFilter(String startTime, String endTime) {
        Boolean isLocalTime = false;
        Boolean neverBlockHttpDownload = false;
        Boolean neverBlockRebootDecoupled = false;

        return createTimeFilter(defaultTimeFilterId, startTime, endTime, isLocalTime, neverBlockHttpDownload, neverBlockRebootDecoupled);
    }

    protected RebootImmediatelyFilter createDefaultRebootImmediatelyFilter() throws Exception {
        RebootImmediatelyFilter result = new RebootImmediatelyFilter();
        result.setId(defaultRebootImmediatelyFilterId);
        result.setName("rebootImmediatelyFilterName");

        return result;
    }

    protected EnvModelPercentage createEnvModelPercentage(Boolean isActive,
                                                          Boolean isFirmwareCheckRequired,
                                                          Boolean rebootImmediately,
                                                          Set<String> firmwareVersions,
                                                          IpAddressGroup whiteList,
                                                          double percentage) throws Exception {
        EnvModelPercentage result = new EnvModelPercentage();
        result.setActive(isActive);
        result.setFirmwareCheckRequired(isFirmwareCheckRequired);
        result.setRebootImmediately(rebootImmediately);
        result.setFirmwareVersions(firmwareVersions);
        result.setWhitelist(whiteList);
        result.setPercentage(percentage);

        return result;
    }

    protected EnvModelPercentage createDefaultEnvModelPercentage(double percentage) throws Exception {
        return createEnvModelPercentage(true, false, false, Collections.singleton(defaultFirmwareVersion),
                null, percentage);
    }

    protected PercentFilterValue createPercentFilter(IpAddressGroupExtended ipAddressGroup, double percentage, Map<String, EnvModelPercentage> envModelPercentages) throws Exception {
        if (envModelPercentages == null) {
            envModelPercentages = Collections.emptyMap();
        }
        return new PercentFilterValue(ipAddressGroup, percentage, envModelPercentages);
    }

    protected DownloadLocationRoundRobinFilterValue createRoundRobinFilter(List<DownloadLocationRoundRobinFilterValue.Location> locations,
                                                                           List<DownloadLocationRoundRobinFilterValue.Location> ipv6Locations,
                                                                           List<Model> rogueModels,
                                                                           String httpLocation,
                                                                           String httpFullUrlLocation,
                                                                           Boolean neverUseHttp,
                                                                           String firmwareVersions) {
        DownloadLocationRoundRobinFilterValue result = new DownloadLocationRoundRobinFilterValue();
        result.setLocations(locations);
        result.setIpv6locations(ipv6Locations);
        result.setRogueModels(rogueModels);
        result.setHttpLocation(httpLocation);
        result.setHttpFullUrlLocation(httpFullUrlLocation);
        result.setNeverUseHttp(neverUseHttp);
        result.setFirmwareVersions(firmwareVersions);
        result.setApplicationType(STB);

        return result;
    }

    protected DownloadLocationRoundRobinFilterValue createDefaultRoundRobinFilter() throws Exception {
        return createRoundRobinFilter(
                            Collections.singletonList(createLocation(defaultIpAddress, 100)),
                            Collections.singletonList(createLocation(defaultIpv6Address, 100)),
                            Collections.singletonList(createAndSaveModel("rogueModel")),
                            defaultHttpLocation,
                            defaultHttpFullUrlLocation,
                            false,
                            contextFirmwareVersion);
    }

    protected DownloadLocationRoundRobinFilterValue.Location createLocation(String locationIp, double percentage) {
        DownloadLocationRoundRobinFilterValue.Location result = new DownloadLocationRoundRobinFilterValue.Location();
        result.setLocationIp(new IpAddress(locationIp));
        result.setPercentage(percentage);

        return result;
    }

    protected DownloadLocationFilterWrapper createDownloadLocationFilter(IpAddressGroup ipAddressGroup,
                                                                         Set<String> environments,
                                                                         Set<String> models,
                                                                         IpAddress firmwareLocation,
                                                                         IpAddress ipv6FirmwareLocation,
                                                                         String httpLocation,
                                                                         Boolean forceHttp,
                                                                         String id) {
        DownloadLocationFilterWrapper result = new DownloadLocationFilterWrapper();
        result.setIpAddressGroup(ipAddressGroup);
        result.setEnvironments(environments);
        result.setModels(models);
        result.setFirmwareLocation(firmwareLocation);
        result.setIpv6FirmwareLocation(ipv6FirmwareLocation);
        result.setHttpLocation(httpLocation);
        result.setForceHttp(forceHttp);
        result.setId(id);

        return result;
    }

    protected DownloadLocationFilterWrapper createDefaultDownloadLocationFilter() throws Exception {
        return createDownloadLocationFilter(
                createAndSaveIpAddressGroupExtended(Collections.singleton(defaultIpAddress)),
                Collections.singleton(createEnvironment(defaultEnvironmentId).getId()),
                Collections.singleton(createAndSaveModel(defaultModelId).getId()),
                new IpAddress(defaultIpAddress),
                new IpAddress(defaultIpv6Address),
                defaultHttpLocation,
                false,
                defaultDownloadLocationFilterId);
    }

    protected FormulaDataObject createDefaultFormula() {
        FormulaDataObject formula = new FormulaDataObject();
        formula.setId(defaultFormulaId);
        formula.setName("formulaName");
        formula.setPercentage(100);
        formula.setPriority(1);
        formula.setEnvList(Lists.newArrayList(defaultEnvironmentId.toUpperCase()));
        formula.setRuleExpression("env");
        return formula;
    }

    protected GenericNamespacedList createGenericNamespacedList(String name, String type, String data) {
        GenericNamespacedList namespacedList = new GenericNamespacedList(type);
        namespacedList.setId(name);
        namespacedList.setData(Sets.newHashSet(data.split(",")));
        return namespacedList;
    }

    public Rule createRule(Relation relation, FreeArg freeArg, Operation operation, String fixedArgValue) {
        Rule rule = new Rule();
        rule.setRelation(relation);
        rule.setCondition(createCondition(freeArg, operation, fixedArgValue));
        return rule;
    }

    public Condition createCondition(FreeArg freeArg, Operation operation, String fixedArgValue) {
        return new Condition(freeArg, operation, FixedArg.from(fixedArgValue));
    }

    public FirmwareRule createEnvModelFirmwareRule(String name, String firmwareConfigId, String envId, String modelId, String macListId) throws ValidationException {
        FirmwareRule envModelRule = new FirmwareRule();
        envModelRule.setId(UUID.randomUUID().toString());
        envModelRule.setName(name);
        envModelRule.setApplicableAction(createRuleAction(ApplicableAction.Type.RULE, firmwareConfigId));
        envModelRule.setType(TemplateNames.ENV_MODEL_RULE);
        envModelRule.setRule(createEnvModelRule(envId, modelId, macListId));
        return envModelRule;
    }

    public RuleAction createRuleAction(ApplicableAction.Type type, String firmwareConfigId) {
        RuleAction ruleAction = new RuleAction();
        ruleAction.setActionType(type);
        ruleAction.setConfigId(firmwareConfigId);
        ruleAction.setId(UUID.randomUUID().toString());
        return ruleAction;
    }

    public Rule createEnvModelRule() {
        Rule envModelRule = new Rule();
        List<Rule> compoundParts = new ArrayList<>();
        compoundParts.add(createRule(null, RuleFactory.ENV, StandardOperation.IS, "ENVID"));
        compoundParts.add(createRule(Relation.AND, RuleFactory.MODEL, StandardOperation.IS, "MODELID"));
        envModelRule.setCompoundParts(compoundParts);
        return envModelRule;
    }

    public Rule createEnvModelRule(String envId, String modelId, String namespacedListId) {
        Rule envModelRule = new Rule();
        List<Rule> compoundParts = new ArrayList<>();
        compoundParts.add(createRule(null, RuleFactory.ENV, StandardOperation.IS, envId));
        compoundParts.add(createRule(Relation.AND, RuleFactory.MODEL, StandardOperation.IS, modelId));
        compoundParts.add(createRule(Relation.AND, RuleFactory.MAC, RuleFactory.IN_LIST, namespacedListId));
        envModelRule.setCompoundParts(compoundParts);
        return envModelRule;
    }

    protected Rule createExistsRule(String tagName) {
        return createRule(null, new FreeArg(StandardFreeArgType.STRING, tagName), StandardOperation.EXISTS, null);
    }

    public static FirmwareRuleTemplate createFirmwareRuleTemplate(String id, Rule rule, ApplicableAction applicableAction) {
        FirmwareRuleTemplate template = new FirmwareRuleTemplate();
        template.setId(id);
        template.setRule(rule);
        template.setApplicableAction(applicableAction);
        return template;
    }

    public FirmwareRule createMacRuleWithDistribution(String defaultConfigId, RuleAction.ConfigEntry... entries) throws ValidationException {
        FirmwareRule firmwareRule = new FirmwareRule();
        firmwareRule.setId("MacRule");
        firmwareRule.setName("MacRule");
        firmwareRule.setType(TemplateNames.MAC_RULE);
        firmwareRule.setRule(RuleFactory.newMacRule(
                createAndSaveMacList(defaultMacListId, Collections.singleton(defaultMacAddress)).getId()));
        RuleAction ruleAction = new RuleAction();
        ruleAction.setConfigId(defaultConfigId);
        ruleAction.setConfigEntries(Arrays.asList(entries));
        firmwareRule.setApplicableAction(ruleAction);
        firmwareRuleDao.setOne(firmwareRule.getId(), firmwareRule);
        return firmwareRule;
    }

    protected PercentFilterValue createAndSavePercentFilter(String envModelRuleName,
                                                            double percentage,
                                                            String lastKnownGood,
                                                            String intermediateVersion,
                                                            double envModelPercent,
                                                            Set<String> firmwareVersions,
                                                            boolean isActive,
                                                            boolean isFirmwareCheckRequired,
                                                            boolean rebootImmediately,
                                                            String applicationType) throws ValidationException {
        PercentFilterValue percentFilter = new PercentFilterValue();
        IpAddressGroupExtended whitelist = createIpAddressGroupExtended(Sets.<String>newHashSet("1.1.1.1", "2.2.2.2"));

        EnvModelPercentage envModelPercentage = new EnvModelPercentage();
        envModelPercentage.setWhitelist(whitelist);
        envModelPercentage.setLastKnownGood(lastKnownGood);
        envModelPercentage.setIntermediateVersion(intermediateVersion);
        envModelPercentage.setFirmwareVersions(firmwareVersions);
        envModelPercentage.setPercentage(envModelPercent);
        envModelPercentage.setActive(isActive);
        envModelPercentage.setFirmwareCheckRequired(isFirmwareCheckRequired);
        envModelPercentage.setRebootImmediately(rebootImmediately);

        percentFilter.setPercentage(percentage);
        percentFilter.setWhitelist(whitelist);
        percentFilter.setEnvModelPercentages(Collections.singletonMap(envModelRuleName, envModelPercentage));

        percentFilterService.save(percentFilter, applicationType);

        return percentFilter;
    }

    protected FirmwareRule createDefaultLocationRule() throws Exception {
        FirmwareRuleTemplate firmwareRuleTemplate = createFirmwareRuleTemplate(
                TemplateNames.DOWNLOAD_LOCATION_FILTER,
                Rule.Builder.of(createCondition(RuleFactory.IP, RuleFactory.IN_LIST, "")).build(),
                createDefinePropertiesTemplateAction());
        firmwareRuleTemplateDao.setOne(firmwareRuleTemplate.getId(), firmwareRuleTemplate);

        GenericNamespacedList ipList = new GenericNamespacedList(GenericNamespacedListTypes.IP_LIST);
        ipList.setId("ipListId");
        ipList.setData(Sets.newHashSet(defaultIpAddress));
        genericNamespacedListDAO.setOne(ipList.getId(), ipList);

        FirmwareRule firmwareRule = new FirmwareRule();
        firmwareRule.setId(UUID.randomUUID().toString());
        firmwareRule.setName("locationFilter");
        firmwareRule.setType(firmwareRuleTemplate.getId());
        firmwareRule.setApplicableAction(createDefinePropertiesAction());
        firmwareRule.setRule(Rule.Builder.of(createCondition(RuleFactory.IP, RuleFactory.IN_LIST, ipList.getId())).build());

        return firmwareRule;
    }

    protected FirmwareRule createAndSaveDefaultDownloadLocationRule() throws Exception {
        FirmwareRule downloadLocationRule = createDefaultLocationRule();
        firmwareRuleDao.setOne(downloadLocationRule.getId(), downloadLocationRule);
        return downloadLocationRule;
    }

    protected DefinePropertiesTemplateAction createDefinePropertiesTemplateAction() {
        DefinePropertiesTemplateAction action = new DefinePropertiesTemplateAction();
        Map<String, DefinePropertiesTemplateAction.PropertyValue> properties = new HashMap<>();
        properties.put("firmwareLocation", new DefinePropertiesTemplateAction.PropertyValue());
        properties.put("firmwareDownloadProtocol", new DefinePropertiesTemplateAction.PropertyValue());
        properties.put("ipv6FirmwareLocation", new DefinePropertiesTemplateAction.PropertyValue());
        action.setProperties(properties);
        return action;
    }

    protected DefinePropertiesAction createDefinePropertiesAction() {
        DefinePropertiesAction action = new DefinePropertiesAction();
        Map<String, String> properties = new HashMap<>();
        properties.put("firmwareLocation", "http://1.1.1.1");
        properties.put("firmwareDownloadProtocol", "http");
        properties.put("ipv6FirmwareLocation", "");
        action.setProperties(properties);
        return action;
    }

    protected FirmwareRule createAndSaveDownloadLocationRuleWithInOperation() throws Exception {
        FirmwareRule downloadLocationRule = createDefaultLocationRule();
        downloadLocationRule.setRule(Rule.Builder.of(
                new Condition(RuleFactory.IP, StandardOperation.IN, FixedArg.from(Collections.singletonList(defaultIpAddress)))).build());
        firmwareRuleDao.setOne(downloadLocationRule.getId(), downloadLocationRule);
        return downloadLocationRule;
    }

    protected FirmwareRule createAndSaveDownloadLocationRuleWithIsOperation() throws Exception {
        FirmwareRule downloadLocationRule = createDefaultLocationRule();
        downloadLocationRule.setRule(Rule.Builder.of(
                new Condition(RuleFactory.IP, StandardOperation.IS, FixedArg.from(defaultIpAddress))).build());
        firmwareRuleDao.setOne(downloadLocationRule.getId(), downloadLocationRule);
        return downloadLocationRule;
    }

    protected PercentageBean createPercentageBean(String applicationType) throws ValidationException {
        PercentageBean percentageBean = new PercentageBean();
        percentageBean.setId("testId");
        percentageBean.setName("testName");
        percentageBean.setApplicationType(STB);
        percentageBean.setActive(true);
        percentageBean.setEnvironment(createAndSaveEnvironment("ENV_ID").getId());
        percentageBean.setModel(createAndSaveModel("MODEL_ID").getId());
        percentageBean.setFirmwareCheckRequired(true);
        GenericNamespacedList whitelist = createAndSaveGenericNamespacedList("percentageBeanWhitelist", GenericNamespacedListTypes.IP_LIST, "10.10.10.10");
        percentageBean.setWhitelist(whitelist.getId());
        FirmwareConfig firmwareConfig = createAndSaveFirmwareConfig("firmwareVersion", "MODEL_ID", FirmwareConfig.DownloadProtocol.http, applicationType);
        percentageBean.setFirmwareVersions(Sets.newHashSet(firmwareConfig.getFirmwareVersion()));
        percentageBean.setLastKnownGood(firmwareConfig.getId());
        percentageBean.setDistributions(Collections.singletonList(new RuleAction.ConfigEntry(firmwareConfig.getId(), 66.0)));
        percentageBean.setIntermediateVersion(firmwareConfig.getId());
        percentageBean.setApplicationType(applicationType);
        return percentageBean;
    }

    protected PercentageBean createPercentageBean(String name, String envId, String modelId, String whitelistId, String whitelistData, String firmwareVersion, String applicationType) throws ValidationException {
        PercentageBean percentageBean = new PercentageBean();
        percentageBean.setId(UUID.randomUUID().toString());
        percentageBean.setName(name);
        percentageBean.setActive(true);
        percentageBean.setEnvironment(createAndSaveEnvironment(envId).getId());
        percentageBean.setModel(createAndSaveModel(modelId).getId());
        percentageBean.setFirmwareCheckRequired(true);
        GenericNamespacedList whitelist = createAndSaveGenericNamespacedList(whitelistId, GenericNamespacedListTypes.IP_LIST, whitelistData);
        percentageBean.setWhitelist(whitelist.getId());
        FirmwareConfig firmwareConfig = createAndSaveFirmwareConfig(firmwareVersion, modelId, FirmwareConfig.DownloadProtocol.http, applicationType);
        percentageBean.setFirmwareVersions(Sets.newHashSet(firmwareConfig.getFirmwareVersion()));
        percentageBean.setLastKnownGood(firmwareConfig.getId());
        percentageBean.setDistributions(Collections.singletonList(new RuleAction.ConfigEntry(firmwareConfig.getId(), 66.0)));
        percentageBean.setIntermediateVersion(firmwareConfig.getId());
        percentageBean.setApplicationType(applicationType);
        return percentageBean;
    }

    protected PercentageBean createAndSavePercentageBean(String name, String envId, String modelId, String whitelistId, String whitelistData, String firmwareVersion, String applicationType) throws ValidationException {
        PercentageBean percentageBean = createPercentageBean(name, envId, modelId, whitelistId, whitelistData, firmwareVersion, applicationType);
        savePercentageBean(percentageBean);
        return percentageBean;
    }

    protected PercentageBean createAndSavePercentageBean() throws ValidationException {
        PercentageBean percentageBean = createPercentageBean(STB);
        savePercentageBean(percentageBean);
        return percentageBean;
    }

    protected List<PercentageBean> createAndSavePercentageBeans() throws ValidationException {
        List<PercentageBean> percentageBeans = Lists.newArrayList(
                createAndSavePercentageBean("bean1", "env1", "model1", "percentageBeanWhitelist1", "10.10.10.10", "firmwareVersion1", STB),
                createAndSavePercentageBean("bean2", "env2", "model2", "percentageBeanWhitelist2", "11.11.11.11", "firmwareVersion2", STB)
        );
        return percentageBeans;
    }

    protected GlobalPercentage createAndSaveGlobalPercentage() throws ValidationException {
        GlobalPercentage globalPercentage = createGlobalPercentage();
        FirmwareRule firmwareRule = converter.convertIntoRule(globalPercentage);
        firmwareRuleDao.setOne(firmwareRule.getId(), firmwareRule);
        return globalPercentage;
    }

    protected GlobalPercentage createGlobalPercentage() throws ValidationException {
        GlobalPercentage globalPercentage = new GlobalPercentage();
        GenericNamespacedList whitelist = createAndSaveGenericNamespacedList("globalPercentageWhitelist", GenericNamespacedListTypes.IP_LIST, "12.12.12.12");
        globalPercentage.setWhitelist(whitelist.getId());
        globalPercentage.setPercentage(100.0);
        return globalPercentage;
    }

    protected void savePercentageBean(PercentageBean percentageBean) throws ValidationException {
        FirmwareRule firmwareRule = converter.convertIntoRule(percentageBean);
        firmwareRuleDao.setOne(firmwareRule.getId(), firmwareRule);
    }

    protected GenericNamespacedList createAndSaveGenericNamespacedList(String id, String type, String data) throws ValidationException {
        GenericNamespacedList genericNamespacedList = createGenericNamespacedList(id, type, data);
        genericNamespacedListDAO.setOne(genericNamespacedList.getId(), genericNamespacedList);
        return genericNamespacedList;
    }

    protected FirmwareConfig createAndSaveFirmwareConfigByApplicationType(String applicationType) throws ValidationException {
        FirmwareConfig firmwareConfig = createFirmwareConfig(UUID.randomUUID().toString());
        firmwareConfig.setApplicationType(applicationType);
        firmwareConfigDAO.setOne(firmwareConfig.getId(), firmwareConfig);
        return firmwareConfig;
    }

    protected void performGetWithApplication(String url, String paramValue, Object object) throws Exception {
        mockMvc.perform(get(url)
                .accept(MediaType.APPLICATION_JSON)
                .param("version", API_VERSION)
                .param(APPLICATION_TYPE_PARAM, paramValue))
                .andExpect(status().isOk())
                .andExpect(content().json(CoreUtil.toJSON(object)));
    }

    protected void performPostWithApplication(String url, String paramValue, Object contentToSend, Object expectedObject) throws Exception {
        mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .param(APPLICATION_TYPE_PARAM, paramValue)
                .content(CoreUtil.toJSON(contentToSend)))
                .andExpect(status().isOk())
                .andExpect(content().string(CoreUtil.toJSON(expectedObject)));
    }

    protected void performPostWithWrongApplicationType(String url, Object content) throws Exception {
        mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(CoreUtil.toJSON(content))
                .param(APPLICATION_TYPE_PARAM, WRONG_APPLICATION))
                .andExpect(status().isBadRequest())
                .andExpect(errorMessageMatcher("ApplicationType " + WRONG_APPLICATION + " is not valid"));
    }

    protected ResultMatcher errorMessageMatcher(final String expectedMessage) {
        return new ResultMatcher() {
            @Override
            public void match(MvcResult mvcResult) throws Exception {
                assertEquals("\"" + expectedMessage + "\"", mvcResult.getResponse().getContentAsString());
            }
        };
    }

    protected void performGetWithWrongApplicationType(String url) throws Exception {
        mockMvc.perform(get(url)
                .accept(MediaType.APPLICATION_JSON)
                .param(APPLICATION_TYPE_PARAM, WRONG_APPLICATION))
                .andExpect(status().isBadRequest())
                .andExpect(errorMessageMatcher("ApplicationType " + WRONG_APPLICATION + " is not valid"));
    }

    protected DeviceSettings createDeviceSettings(String id) {
        DeviceSettings deviceSettings = new DeviceSettings();
        deviceSettings.setId(id);
        deviceSettings.setName(defaultDeviceSettingName);
        deviceSettings.setApplicationType(STB);
        deviceSettings.setSettingsAreActive(true);
        deviceSettings.setSchedule(createSchedule());
        deviceSettings.setCheckOnReboot(false);
        return deviceSettings;
    }

    protected LogUploadSettings createLogUploadSettings(String id, String uploadRepositoryId) {
        LogUploadSettings logUploadSettings = new LogUploadSettings();
        logUploadSettings.setId(id);
        logUploadSettings.setName(defaultLogUploadSettingName);
        logUploadSettings.setAreSettingsActive(true);
        logUploadSettings.setSchedule(createSchedule());
        logUploadSettings.setUploadRepositoryId(uploadRepositoryId);
        return logUploadSettings;
    }

    protected UploadRepository createUploadRepository() {
        UploadRepository uploadRepository = new UploadRepository();
        uploadRepository.setId(UUID.randomUUID().toString());
        uploadRepository.setName("uploadRepositoryName");
        uploadRepository.setDescription("description");
        uploadRepository.setApplicationType(STB);
        uploadRepository.setProtocol(UploadProtocol.HTTP);
        return uploadRepository;
    }

    protected Schedule createSchedule() {
        Schedule schedule = new Schedule();
        schedule.setExpression("1 1 * * *");
        return schedule;
    }

    protected PermanentTelemetryProfile createPermanentTelemetryProfile() {
        PermanentTelemetryProfile telemetryProfile = new PermanentTelemetryProfile();
        telemetryProfile.setId(UUID.randomUUID().toString());
        telemetryProfile.setName("telemetryProfileName");
        telemetryProfile.setApplicationType(STB);
        return telemetryProfile;
    }

    protected TelemetryRule createTelemetryRule(String profileId, Condition condition) {
        TelemetryRule telemetryRule = new TelemetryRule();
        telemetryRule.setId(UUID.randomUUID().toString());
        telemetryRule.setBoundTelemetryId(profileId);
        telemetryRule.setName("telemetryRuleName");
        telemetryRule.setApplicationType(STB);
        telemetryRule.setCondition(condition);
        return telemetryRule;
    }

    protected TelemetryRule createTelemetryRule(String name, String profileId, String applicationType) {
        TelemetryRule telemetryRule = createTelemetryRule(profileId, createCondition(MODEL, IS, defaultModelId));
        telemetryRule.setName(name);
        telemetryRule.setApplicationType(applicationType);
        return telemetryRule;
    }

    protected FirmwareRule createAndSaveTagFirmwareRule(FirmwareConfig firmwareConfig) throws ValidationException {
        Rule rule = createExistsRule("tagName1");
        FirmwareRuleTemplate tagTemplate = createFirmwareRuleTemplate("tagRuleTemplate", rule, createRuleAction(ApplicableAction.Type.RULE_TEMPLATE, firmwareConfig.getId()));
        firmwareRuleTemplateDao.setOne(tagTemplate.getId(), tagTemplate);
        FirmwareRule tagFirmwareRule = createAndSaveFirmwareRule("tagFirmwareRuleId", tagTemplate.getId(), createRuleAction(ApplicableAction.Type.RULE, firmwareConfig.getId()), rule);
        return tagFirmwareRule;
    }

    protected FirmwareRule createAndSavePartnerFirmwareRule(String partnerId, FirmwareConfig firmwareConfig) throws ValidationException {
        Rule partnerRule = createPartnerRule(partnerId);
        FirmwareRuleTemplate partnerTemplate = createFirmwareRuleTemplate("partnerTemplate", partnerRule, createRuleAction(ApplicableAction.Type.RULE_TEMPLATE, firmwareConfig.getId()));
        firmwareRuleTemplateDao.setOne(partnerTemplate.getId(), partnerTemplate);
        FirmwareRule partnerFirmwareRule = createAndSaveFirmwareRule("partnerFirmwareRuleId", partnerTemplate.getId(), createRuleAction(ApplicableAction.Type.RULE, firmwareConfig.getId()), partnerRule);
        return partnerFirmwareRule;
    }

    protected FirmwareRule createAndSaveFirmwareRule(String id, String templateId, ApplicableAction action, Rule rule) throws ValidationException {
        FirmwareRule firmwareRule = new FirmwareRule();
        firmwareRule.setId(id);
        firmwareRule.setName(id);
        firmwareRule.setActive(true);
        firmwareRule.setApplicableAction(action);
        firmwareRule.setType(templateId);
        firmwareRule.setRule(rule);

        firmwareRuleDao.setOne(firmwareRule.getId(), firmwareRule);
        return firmwareRule;
    }

    protected Rule createPartnerRule(String partnerId) {
        return Rule.Builder.of(new Condition(RuleFactory.PARTNER_ID,
                StandardOperation.IS,
                FixedArg.from(partnerId)))
                .build();
    }
}

