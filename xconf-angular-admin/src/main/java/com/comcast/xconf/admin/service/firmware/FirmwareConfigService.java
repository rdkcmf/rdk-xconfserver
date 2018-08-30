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
 * Created: 2/17/16  11:24 AM
 */
package com.comcast.xconf.admin.service.firmware;

import com.comcast.apps.hesperius.ruleengine.main.impl.Condition;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.hesperius.dataaccess.core.exception.EntityConflictException;
import com.comcast.xconf.admin.validator.firmware.FirmwareConfigValidator;
import com.comcast.xconf.estbfirmware.FirmwareConfig;
import com.comcast.xconf.estbfirmware.TemplateNames;
import com.comcast.xconf.estbfirmware.factory.RuleFactory;
import com.comcast.xconf.firmware.FirmwareRule;
import com.comcast.xconf.firmware.RuleAction;
import com.comcast.xconf.permissions.FirmwarePermissionService;
import com.comcast.xconf.permissions.PermissionService;
import com.comcast.xconf.search.PredicateManager;
import com.comcast.xconf.search.SearchFields;
import com.comcast.xconf.shared.service.AbstractApplicationTypeAwareService;
import com.comcast.xconf.util.RuleUtil;
import com.comcast.xconf.validators.IValidator;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FirmwareConfigService extends AbstractApplicationTypeAwareService<FirmwareConfig> {

    @Autowired
    private ISimpleCachedDAO<String, FirmwareConfig> firmwareConfigDAO;

    @Autowired
    private FirmwareConfigValidator firmwareConfigValidator;

    @Autowired
    private FirmwareRuleService firmwareRuleService;

    @Autowired
    private PredicateManager predicateManager;

    @Autowired
    private FirmwarePermissionService permissionService;

    @Override
    public ISimpleCachedDAO getEntityDAO() {
        return firmwareConfigDAO;
    }

    @Override
    public IValidator getValidator() {
        return firmwareConfigValidator;
    }

    @Override
    public void normalizeOnSave(FirmwareConfig firmwareConfig) {
        normalizeModelIds(firmwareConfig);
    }

    @Override
    public PermissionService getPermissionService() {
        return permissionService;
    }

    public List<FirmwareConfig> getFirmwareConfigsByModel(String modelId) {
        List<FirmwareConfig> result = new ArrayList<>();
        if (!StringUtils.isBlank(modelId)) {
            for (FirmwareConfig firmware : getAll()) {
                if (firmware.getSupportedModelIds().contains(modelId)) {
                    result.add(firmware);
                }
            }
        }

        return result;
    }

    public List<FirmwareConfig> getFirmwareConfigBySupportedModels(Set<String> modelIds) {
        if (CollectionUtils.isNotEmpty(modelIds)) {
            List<Predicate<FirmwareConfig>> predicates = new ArrayList<>();
            predicates.add(new FirmwareConfig.TargetedModelsFilter(modelIds));
            predicates.add(predicateManager.new FirmwareConfigApplicationTypePredicate(permissionService.getReadApplication()));
            return firmwareConfigDAO.getAll(Predicates.and(predicates));
        }

        return Collections.emptyList();
    }

    public Set<FirmwareConfig> getSupportedConfigsByEnvModelRuleName(String envModelName) {
        Set<FirmwareConfig> versions = new HashSet<>();
        String model = null;
        List<FirmwareRule> firmwareRules = firmwareRuleService.getAll();
        for (FirmwareRule rule : firmwareRules) {
            if (TemplateNames.ENV_MODEL_RULE.equals(rule.getType()) && rule.getName().equals(envModelName)) {
                model = extractModel(rule);
            }
        }

        for (FirmwareConfig config : getAll()) {
            Set<String> supportedModels = config.getSupportedModelIds();
            if (model != null && supportedModels != null && supportedModels.contains(model)) {
                versions.add(config);
            }
        }
        return versions;
    }

    public Map<String, Set<String>> verifyIfFirmwareVersionsExistsByList(String modelId, Set<String> firmwareVersions) {
        List<FirmwareConfig> firmwareConfigsByModel = getFirmwareConfigsByModel(modelId);
        Set<String> existedVersions = new HashSet<>();
        Set<String> notExistedVersions = new HashSet<>();

        for (String version : firmwareVersions) {
            if(containsVersion(firmwareConfigsByModel, version)) {
                existedVersions.add(version);
            } else {
                notExistedVersions.add(version);
            }
        }

        Map<String, Set<String>> firmwareVersionMap = new HashedMap();
        firmwareVersionMap.put("existedVersions", existedVersions);
        firmwareVersionMap.put("notExistedVersions", notExistedVersions);
        return firmwareVersionMap;
    }

    private boolean containsVersion(List<FirmwareConfig> configs, String version) {
        for (FirmwareConfig config : configs) {
            if (config.getFirmwareVersion().equals(version)) {
                return true;
            }
        }
        return false;
    }

    private String extractModel(FirmwareRule rule) {
        for (Condition condition : RuleUtil.getAllConditions(rule.getRule())) {
            if (RuleFactory.MODEL.equals(condition.getFreeArg())) {
                return (String) condition.getFixedArg().getValue();
            }
        }
        return null;
    }

    private void normalizeModelIds(FirmwareConfig config) {
        Set<String> normalizedModelIds = new HashSet<>();
        for (String modelId : config.getSupportedModelIds()) {
            normalizedModelIds.add(modelId.toUpperCase());
        }

        config.setSupportedModelIds(normalizedModelIds);
    }

    @Override
    public void validateUsage(String id) {
        if (StringUtils.isNotBlank(id)) {
            FirmwareConfig firmwareConfig = getOne(id);
            for (FirmwareRule entity : firmwareRuleService.getAll()) {
                if (configUsedInAction(firmwareConfig, entity)) {
                    throw new EntityConflictException("FirmwareConfig is used by " + entity.getName() + " firmware rule");
                }
            }
        }
    }

    public boolean configUsedInAction(FirmwareConfig firmwareConfig, FirmwareRule rule) {
        String id = firmwareConfig.getId();
        if (rule != null && rule.getApplicableAction() != null && (rule.getApplicableAction() instanceof RuleAction)) {
            RuleAction action = (RuleAction) rule.getApplicableAction();
            if (id.equals(action.getConfigId())) {
                return true;
            }
            List<RuleAction.ConfigEntry> configEntries = action.getConfigEntries();
            if (configEntries != null) {
                for (RuleAction.ConfigEntry entry : configEntries) {
                    if (id.equals(entry.getConfigId())) {
                        return true;
                    }
                }
            }
            if (StringUtils.equals(id, action.getIntermediateVersion())
                    || CollectionUtils.isNotEmpty(action.getFirmwareVersions()) && action.getFirmwareVersions().contains(firmwareConfig.getFirmwareVersion())) {
                return true;
            }
        }
        return false;
    }

    public FirmwareConfig getFirmwareConfigByEnvModelRuleName(String envModelRuleName) {
        List<FirmwareRule> firmwareRules = firmwareRuleService.getAll();
        for (FirmwareRule rule : firmwareRules) {
            if (TemplateNames.ENV_MODEL_RULE.equals(rule.getType())
                    && rule.getName().equals(envModelRuleName)
                    && rule.getApplicableAction() instanceof RuleAction) {
                RuleAction ruleAction = (RuleAction) rule.getApplicableAction();
                if (StringUtils.isNotBlank(ruleAction.getConfigId())) {
                    return firmwareConfigDAO.getOne(ruleAction.getConfigId());
                }
            }
        }
        return null;
    }

    @Override
    protected List<Predicate<FirmwareConfig>> getPredicatesByContext(Map<String, String> context) {
        List<Predicate<FirmwareConfig>> predicates = new ArrayList<>();

        if (context == null) {
            return predicates;
        }

        if (context.containsKey(SearchFields.MODEL)) {
            predicates.add(predicateManager.new FirmwareConfigModelPredicate(context.get(SearchFields.MODEL)));
        }
        if (context.containsKey(SearchFields.FIRMWARE_VERSION)) {
            predicates.add(predicateManager.new FirmwareConfigVersionPredicate(context.get(SearchFields.FIRMWARE_VERSION)));
        }
        if (context.containsKey(SearchFields.DESCRIPTION)) {
            predicates.add(predicateManager.new FirmwareConfigDescriptionPredicate(context.get(SearchFields.DESCRIPTION)));
        }
        if (StringUtils.isNotBlank(context.get(SearchFields.APPLICATION_TYPE))) {
            predicates.add(predicateManager.new ApplicationablePredicate<FirmwareConfig>(context.get(SearchFields.APPLICATION_TYPE)));
        } else {
            predicates.add(predicateManager.new ApplicationablePredicate<FirmwareConfig>(permissionService.getReadApplication()));
        }

        return predicates;
    }

    public Map<String, FirmwareConfig> getFirmwareConfigMap() {
        Map<String, FirmwareConfig> firmwareConfigs = new HashMap<>();
        for (FirmwareConfig firmwareConfig : firmwareConfigDAO.getAll()) {
            firmwareConfigs.put(firmwareConfig.getId(), firmwareConfig);
        }
        return firmwareConfigs;
    }
}
