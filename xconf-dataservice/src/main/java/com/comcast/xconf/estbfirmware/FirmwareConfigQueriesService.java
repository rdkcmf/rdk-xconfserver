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

import com.comcast.apps.hesperius.ruleengine.main.impl.Condition;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.hesperius.dataaccess.core.exception.EntityNotFoundException;
import com.comcast.xconf.estbfirmware.factory.RuleFactory;
import com.comcast.xconf.firmware.ApplicationType;
import com.comcast.xconf.firmware.FirmwareRule;
import com.comcast.xconf.search.PredicateManager;
import com.comcast.xconf.util.RuleUtil;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Service
public class FirmwareConfigQueriesService {

    @Autowired
    private ISimpleCachedDAO<String, FirmwareConfig> firmwareConfigDAO;

    @Autowired
    private ISimpleCachedDAO<String, FirmwareRule> firmwareRuleDao;

    @Autowired
    private PredicateManager predicateManager;

    public List<FirmwareConfig> getAll() {
        return firmwareConfigDAO.getAll();
    }

    public FirmwareConfig getById(String id) {
        FirmwareConfig firmwareConfig = firmwareConfigDAO.getOne(id);
        if (firmwareConfig == null) {
            throw new EntityNotFoundException("FirmwareConfig with id does not exist");
        }
        return firmwareConfig;
    }

    public List<FirmwareConfig> getFilteredFirmwareConfigsByModelIds(Set<String> modelIds, String applicationType) {
        if (CollectionUtils.isEmpty(modelIds)) {
            return null;
        }
        List<Predicate<FirmwareConfig>> predicates = new ArrayList<>();
        predicates.add(new FirmwareConfig.TargetedModelsFilter(modelIds));
        predicates.add(predicateManager.new ApplicationablePredicate<FirmwareConfig>(applicationType));

        return Lists.newArrayList(Iterables.filter(firmwareConfigDAO.getAll(), Predicates.and(predicates)));
    }


    public boolean isValidFirmwareConfigByModelIds(Set<String> modelIds, FirmwareConfig firmwareConfig, String applicationType) {
        if (modelIds != null && CollectionUtils.isNotEmpty(modelIds)
                && getFilteredFirmwareConfigsByModelIds(modelIds, applicationType).contains(firmwareConfig)) {
            return true;
        }
        return false;
    }

    public boolean validateName(FirmwareConfig config, String applicationType) {
        for (FirmwareConfig existedFirmwareConfig : firmwareConfigDAO.getAll()) {
            if (existedFirmwareConfig.getDescription().equalsIgnoreCase(config.getDescription())
                    && ApplicationType.equals(existedFirmwareConfig.getApplicationType(), applicationType)) {
                return false;
            }
        }
        return true;
    }

    public Set<FirmwareConfig> getFirmwareVersions(String envModelName) {
        Set<FirmwareConfig> versions = new HashSet<>();
        String model = null;
        List<FirmwareRule> all = firmwareRuleDao.getAll();
        for (FirmwareRule rule : all) {
            if (TemplateNames.ENV_MODEL_RULE.equals(rule.getType()) && rule.getName().equals(envModelName)) {
                model = extractModel(rule);
            }
        }

        for (FirmwareConfig config : firmwareConfigDAO.getAll()) {
            Set<String> supportedModels = config.getSupportedModelIds();
            if (model != null && supportedModels != null
                    && supportedModels.contains(model)) {
                versions.add(config);
            }
        }
        return versions;
    }

    private String extractModel(FirmwareRule rule) {
        for (Condition condition : RuleUtil.getAllConditions(rule.getRule())) {
            if (RuleFactory.MODEL.equals(condition.getFreeArg())) {
                return (String) condition.getFixedArg().getValue();
            }
        }
        return null;
    }

    public List<FirmwareConfig> getByApplicationType(final String applicationType) {
        return Lists.newArrayList(Iterables.filter(firmwareConfigDAO.getAll(), new Predicate<FirmwareConfig>() {
            @Override
            public boolean apply(@Nullable FirmwareConfig input) {
                return input != null && ApplicationType.equals(applicationType, input.getApplicationType());
            }
        }));
    }
}
