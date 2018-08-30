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
 *  Created: 1:52 PM
 */
package com.comcast.xconf.search;

import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.xconf.*;
import com.comcast.xconf.estbfirmware.FirmwareConfig;
import com.comcast.xconf.firmware.*;
import com.comcast.xconf.logupload.DeviceSettings;
import com.comcast.xconf.logupload.LogUploadSettings;
import com.comcast.xconf.logupload.UploadRepository;
import com.comcast.xconf.logupload.VodSettings;
import com.comcast.xconf.logupload.settings.SettingProfile;
import com.comcast.xconf.logupload.telemetry.PermanentTelemetryProfile;
import com.comcast.xconf.rfc.Feature;
import com.comcast.xconf.rfc.FeatureRule;
import com.comcast.xconf.rfc.FeatureSet;
import com.comcast.xconf.service.GenericNamespacedListQueriesService;
import com.comcast.xconf.util.RuleUtil;
import com.google.common.base.Predicate;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.Map;

@Service
public class PredicateManager {

    @Autowired
    private ISimpleCachedDAO<String, FirmwareConfig> firmwareConfigDAO;

    @Autowired
    private GenericNamespacedListQueriesService genericNamespacedListQueriesService;

    @Autowired
    private ISimpleCachedDAO<String, Feature> featureDAO;

    private final Logger log = LoggerFactory.getLogger(PredicateManager.class);

    public class XEnvModelIdPredicate<T extends XEnvModel> implements Predicate<T> {
        private String name;

        public XEnvModelIdPredicate(String name) {
            this.name = name;
        }

        @Override
        public boolean apply(@Nullable T input) {
            return input != null && StringUtils.containsIgnoreCase(input.getId(), name);
        }
    }

    public class XEnvModelDescriptionPredicate<T extends XEnvModel> implements Predicate<T> {
        private String description;

        public XEnvModelDescriptionPredicate(String description) {
            this.description = description;
        }

        @Override
        public boolean apply(@Nullable XEnvModel input) {
            return StringUtils.containsIgnoreCase(input.getDescription(), description);
        }
    }

    public class FirmwareConfigModelPredicate implements Predicate<FirmwareConfig> {
        private String modelId;

        public FirmwareConfigModelPredicate(final String modelId) {
            this.modelId = modelId;
        }

        @Override
        public boolean apply(@Nullable FirmwareConfig input) {
            for (String supportedModel : input.getSupportedModelIds()) {
                if (supportedModel.contains(modelId.toUpperCase())) {
                    return true;
                }
            }
            return false;
        }
    }

    public class FirmwareConfigVersionPredicate implements Predicate<FirmwareConfig> {
        private String firmwareVersion;

        public FirmwareConfigVersionPredicate(final String firmwareVersion) {
            this.firmwareVersion = firmwareVersion;
        }

        @Override
        public boolean apply(@Nullable FirmwareConfig input) {
            return StringUtils.containsIgnoreCase(input.getFirmwareVersion(), firmwareVersion);
        }
    }

    public class FirmwareConfigDescriptionPredicate implements Predicate<FirmwareConfig> {
        private String description;

        public FirmwareConfigDescriptionPredicate(final String description) {
            this.description = description;
        }

        @Override
        public boolean apply(@Nullable FirmwareConfig input) {
            return StringUtils.containsIgnoreCase(input.getDescription(), description);
        }
    }

    public class FirmwareConfigApplicationTypePredicate implements Predicate<FirmwareConfig> {

        private String applicationType;

        public FirmwareConfigApplicationTypePredicate(String applicationType) {
            this.applicationType = applicationType;
        }

        @Override
        public boolean apply(@Nullable FirmwareConfig input) {
            return input != null && (ApplicationType.ALL.equals(applicationType) || StringUtils.containsIgnoreCase(ApplicationType.get(input.getApplicationType()), applicationType));
        }
    }

    public class XRuleNamePredicate<T extends XRule> implements Predicate<T>{
        private String name;

        public XRuleNamePredicate(final String name) {
            this.name = name;
        }

        @Override
        public boolean apply(@Nullable T input) {
            return StringUtils.containsIgnoreCase(input.getName(), name);
        }
    }

    public class XRuleFreeArgPredicate<T extends XRule> implements Predicate<T> {
        private String freeArgName;

        public XRuleFreeArgPredicate(final String freeArgName) {
            this.freeArgName = freeArgName;
        }

        @Override
        public boolean apply(@Nullable T input) {
            return input != null && RuleUtil.isExistConditionByFreeArgName(input.getRule(), freeArgName);
        }
    }

    public class XRuleFixedArgPredicate<T extends XRule> implements Predicate<T> {
        private String fixedArgValue;

        public XRuleFixedArgPredicate(final String fixedArgValue) {
            this.fixedArgValue = fixedArgValue;
        }

        @Override
        public boolean apply(@Nullable T input) {
            return input != null && RuleUtil.isExistConditionByFixedArgValue(input.getRule(), fixedArgValue);
        }
    }

    public class XRuleFreeAndFixedArgsPredicate<T extends XRule> implements Predicate<T> {

        private String freeArg;
        private String fixedArg;

        public XRuleFreeAndFixedArgsPredicate(String freeArg, String fixedArg) {
            this.freeArg = freeArg;
            this.fixedArg = fixedArg;
        }

        @Override
        public boolean apply(@Nullable T input) {
            return input != null
                    && input.getRule() != null
                    && RuleUtil.isExistConditionByFreeAndFixedArgParts(input.getRule(), freeArg, fixedArg);
        }
    }

    public class FirmwareRuleConfigPredicate implements Predicate<FirmwareRule> {
        private String firmwareConfigDescription;

        public FirmwareRuleConfigPredicate(final String firmwareConfigDescription) {
            this.firmwareConfigDescription = firmwareConfigDescription;
        }

        @Override
        public boolean apply(@Nullable FirmwareRule input) {
            if (input != null
                    && input.getApplicableAction() != null
                    && ApplicableAction.Type.RULE.equals(input.getApplicableAction().getActionType())) {
                String configId = ((RuleAction) input.getApplicableAction()).getConfigId();
                if (StringUtils.isNotBlank(configId)) {
                    FirmwareConfig ruleConfig = firmwareConfigDAO.getOne(configId);
                    return ruleConfig != null && StringUtils.containsIgnoreCase(ruleConfig.getDescription(), firmwareConfigDescription);
                }
            }
            return false;
        }
    }

    public class FirmwareRuleByTemplatePredicate implements Predicate<FirmwareRule> {

        private String templateId;

        public FirmwareRuleByTemplatePredicate(String templateId) {
            this.templateId = templateId;
        }

        @Override
        public boolean apply(@Nullable FirmwareRule input) {
            return StringUtils.equals(input.getType(), templateId);
        }
    }

    public class FirmwareRuleApplicableActionTypePredicate implements Predicate<FirmwareRule> {

        private ApplicableAction.Type type;

        public FirmwareRuleApplicableActionTypePredicate(String type) {
            try {
                this.type = ApplicableAction.Type.valueOf(type);
            } catch (IllegalArgumentException e) {
                log.error("Exception: " + e.getMessage());
                this.type = ApplicableAction.Type.RULE;
            }
        }

        @Override
        public boolean apply(@Nullable FirmwareRule input) {
            return type.equals(input.getApplicableAction().getActionType());
        }
    }

    public class FirmwareRuleApplicationTypePredicate implements Predicate<FirmwareRule> {

        private String applicationType;

        public FirmwareRuleApplicationTypePredicate(String deviceType) {
            this.applicationType = deviceType;
        }

        @Override
        public boolean apply(@Nullable FirmwareRule input) {
            return input != null && (ApplicationType.ALL.equals(applicationType) || StringUtils.containsIgnoreCase(ApplicationType.get(input.getApplicationType()), applicationType));
        }
    }

    public class FirmwareRuleTemplateApplicableActionTypePredicate implements Predicate<FirmwareRuleTemplate> {

        private ApplicableAction.Type type;

        public FirmwareRuleTemplateApplicableActionTypePredicate(String type) {
            try {
                this.type = ApplicableAction.Type.valueOf(type);
            } catch (IllegalArgumentException e) {
                log.error("Exception: " + e.getMessage());
                this.type = ApplicableAction.Type.RULE_TEMPLATE;
            }
        }

        @Override
        public boolean apply(@Nullable FirmwareRuleTemplate input) {
            return type.equals(input.getApplicableAction().getActionType());
        }
    }

    public class GenericNamespacedListIdPredicate implements Predicate<GenericNamespacedList> {
        private String id;

        public GenericNamespacedListIdPredicate(final String id) {
            this.id = id;
        }

        @Override
        public boolean apply(@Nullable GenericNamespacedList input) {
            return StringUtils.containsIgnoreCase(input.getId(), id);
        }
    }

    public class GenericNamespacedListTypePredicate implements Predicate<GenericNamespacedList> {

        private String type;

        public GenericNamespacedListTypePredicate(String type) {
            this.type = type;
        }

        @Override
        public boolean apply(@Nullable GenericNamespacedList input) {
            return StringUtils.equals(input.getTypeName(), type);
        }
    }

    public class GenericNamespacedListDataPredicate implements Predicate<GenericNamespacedList> {

        private String data;

        public GenericNamespacedListDataPredicate(String data) {
            this.data = data;
        }

        @Override
        public boolean apply(@Nullable GenericNamespacedList input) {
            if (StringUtils.isBlank(data)) {
                return true;
            }
            try {
                if (GenericNamespacedListTypes.IP_LIST.equals(input.getTypeName())) {
                    return genericNamespacedListQueriesService.isIpAddressHasIpPart(data, input.getData());
                } else if (GenericNamespacedListTypes.MAC_LIST.equals(input.getTypeName())) {
                    return genericNamespacedListQueriesService.isMacListHasMacPart(data, input.getData());
                }
            } catch (Exception e) {
                log.error("Exception: " + e.getMessage());
            }
            return false;
        }
    }

    public class PermanentProfileNamePredicate implements Predicate<PermanentTelemetryProfile> {
        private String name;

        public PermanentProfileNamePredicate(final String name) {
            this.name = name;
        }

        @Override
        public boolean apply(@Nullable PermanentTelemetryProfile input) {
            return StringUtils.containsIgnoreCase(input.getName(), name);
        }
    }

    public class SettingProfileNamePredicate implements Predicate<SettingProfile> {

        private String name;

        public SettingProfileNamePredicate(String name) {
            this.name = name;
        }

        @Override
        public boolean apply(@Nullable SettingProfile input) {
            return StringUtils.containsIgnoreCase(input.getSettingProfileId(), name);
        }
    }

    public class SettingProfileTypePredicate implements Predicate<SettingProfile> {

        private String type;

        public SettingProfileTypePredicate(String type) {
            this.type = type;
        }

        @Override
        public boolean apply(@Nullable SettingProfile input) {
            return StringUtils.containsIgnoreCase(input.getSettingType().name(), type);
        }
    }

    public class DeviceSettingNamePredicate implements Predicate<DeviceSettings> {
        private String name;

        public DeviceSettingNamePredicate(String name) {
            this.name = name;
        }

        @Override
        public boolean apply(@Nullable DeviceSettings input) {
            return StringUtils.containsIgnoreCase(input.getName(), name);
        }
    }

    public class LogUploadSettingsNamePredicate implements Predicate<LogUploadSettings> {

        private String name;

        public LogUploadSettingsNamePredicate(String name) {
            this.name = name;
        }

        @Override
        public boolean apply(@Nullable LogUploadSettings input) {
            return StringUtils.containsIgnoreCase(input.getName(), name);
        }
    }

    public class VodSettingsNamePredicate implements Predicate<VodSettings> {
        private String name;

        public VodSettingsNamePredicate(String name) {
            this.name = name;
        }

        @Override
        public boolean apply(@Nullable VodSettings input) {
            return StringUtils.containsIgnoreCase(input.getName(), name);
        }
    }

    public class UploadRepositoryNamePredicate implements Predicate<UploadRepository> {
        private String name;

        public UploadRepositoryNamePredicate(String name) {
            this.name = name;
        }

        @Override
        public boolean apply(@Nullable UploadRepository input) {
            return StringUtils.containsIgnoreCase(input.getName(), name);
        }
    }

    public class FeatureNamePredicate implements Predicate<Feature> {

        private String featureName;

        public FeatureNamePredicate(String featureName) {
            this.featureName = featureName;
        }

        @Override
        public boolean apply(@Nullable Feature input) {
            return StringUtils.containsIgnoreCase(input.getFeatureName(), featureName);
        }
    }

    public class FeatureDataKeyPredicate implements Predicate<Feature> {

        private String key;

        public FeatureDataKeyPredicate(String key) {
            this.key = key;
        }

        @Override
        public boolean apply(@Nullable Feature input) {
            if (input != null && MapUtils.isNotEmpty(input.getConfigData())) {
                for (String dataKey : input.getConfigData().keySet()) {
                    if (StringUtils.containsIgnoreCase(dataKey, key)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public class FeatureDataValuePredicate implements Predicate<Feature> {

        private String value;

        public FeatureDataValuePredicate(String value) {
            this.value = value;
        }

        @Override
        public boolean apply(@Nullable Feature input) {
            if (input != null && MapUtils.isNotEmpty(input.getConfigData())) {
                for (String dataValue : input.getConfigData().values()) {
                    if (StringUtils.containsIgnoreCase(dataValue, value)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public class FeatureDataKeyAndValuePredicate implements Predicate<Feature> {

        private String key;
        private String value;

        public FeatureDataKeyAndValuePredicate(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public boolean apply(@Nullable Feature input) {
            if (input != null && MapUtils.isNotEmpty(input.getConfigData())) {
                for (Map.Entry<String, String> dataEntry : input.getConfigData().entrySet()) {
                    if (StringUtils.containsIgnoreCase(dataEntry.getKey(), key) && StringUtils.containsIgnoreCase(dataEntry.getValue(), value)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public class FeatureSetNamePredicate implements Predicate<FeatureSet> {

        private String name;

        public FeatureSetNamePredicate(String name) {
            this.name = name;
        }

        @Override
        public boolean apply(@Nullable FeatureSet input) {
            return input != null && StringUtils.containsIgnoreCase(input.getName(), name);
        }
    }

    public class FeatureSetByFeatureNamePredicate implements Predicate<FeatureSet> {

        private String featureName;

        public FeatureSetByFeatureNamePredicate(String featureName) {
            this.featureName = featureName;
        }

        @Override
        public boolean apply(@Nullable FeatureSet input) {
            if (input != null && CollectionUtils.isNotEmpty(input.getFeatureIdList())) {
                for (String featureId : input.getFeatureIdList()) {
                    Feature feature = featureDAO.getOne(featureId);
                    if (feature != null && StringUtils.containsIgnoreCase(feature.getName(), featureName)) {
                        return true;
                    }
                }
                return false;
            }
            return true;
        }
    }

    public class FeatureRuleFeatureNamePredicate implements Predicate<FeatureRule> {

        String featureName;

        public FeatureRuleFeatureNamePredicate(String featureName) {
            this.featureName = featureName;
        }

        @Override
        public boolean apply(@Nullable FeatureRule input) {
            if (input != null && CollectionUtils.isNotEmpty(input.getFeatureIds())) {
                for (String featureId : input.getFeatureIds()) {
                    Feature feature = featureDAO.getOne(featureId);
                    if (feature != null && StringUtils.containsIgnoreCase(feature.getFeatureName(), featureName)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public class ApplicationablePredicate<T extends Applicationable> implements Predicate<T> {
        private String applicationType;

        public ApplicationablePredicate(String applicationType) {
            this.applicationType = applicationType;
        }

        @Override
        public boolean apply(@Nullable T input) {
            return ApplicationType.equals(applicationType, input.getApplicationType());
        }
    }

    public <T extends XRule> Predicate<T> getXRulePredicate(Map<String, String> context) {
        if (MapUtils.isEmpty(context)) {
            return null;
        }
        if (context.containsKey(SearchFields.FREE_ARG) && context.containsKey(SearchFields.FIXED_ARG)) {
            return new XRuleFreeAndFixedArgsPredicate<>(context.get(SearchFields.FREE_ARG), context.get(SearchFields.FIXED_ARG));
        } else if (context.containsKey(SearchFields.FREE_ARG)) {
            return new XRuleFreeArgPredicate<>(context.get(SearchFields.FREE_ARG));
        } else if (context.containsKey(SearchFields.FIXED_ARG)) {
            return new XRuleFixedArgPredicate<>(context.get(SearchFields.FIXED_ARG));
        }
        return null;
    }
}
