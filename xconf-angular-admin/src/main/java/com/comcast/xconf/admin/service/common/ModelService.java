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
 *  Created: 4:59 PM
 */
package com.comcast.xconf.admin.service.common;

import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.hesperius.dataaccess.core.exception.EntityConflictException;
import com.comcast.xconf.XRule;
import com.comcast.xconf.admin.service.dcm.FormulaService;
import com.comcast.xconf.admin.service.firmware.FirmwareConfigService;
import com.comcast.xconf.admin.service.firmware.FirmwareRuleService;
import com.comcast.xconf.admin.service.firmware.FirmwareRuleTemplateService;
import com.comcast.xconf.admin.service.firmware.RoundRobinFilterService;
import com.comcast.xconf.admin.service.setting.SettingRuleService;
import com.comcast.xconf.admin.service.telemetry.TelemetryRuleService;
import com.comcast.xconf.estbfirmware.DownloadLocationRoundRobinFilterValue;
import com.comcast.xconf.estbfirmware.FirmwareConfig;
import com.comcast.xconf.estbfirmware.Model;
import com.comcast.xconf.estbfirmware.factory.RuleFactory;
import com.comcast.xconf.firmware.ApplicationType;
import com.comcast.xconf.search.PredicateManager;
import com.comcast.xconf.search.SearchFields;
import com.comcast.xconf.shared.service.AbstractService;
import com.comcast.xconf.util.RuleUtil;
import com.comcast.xconf.validators.IValidator;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ModelService extends AbstractService<Model> {

    @Autowired
    private ISimpleCachedDAO<String, Model> modelDAO;

    @Autowired
    private FirmwareRuleService firmwareRuleService;

    @Autowired
    private FirmwareRuleTemplateService firmwareRuleTemplateService;

    @Autowired
    private TelemetryRuleService telemetryRuleService;

    @Autowired
    private FormulaService formulaService;

    @Autowired
    private SettingRuleService settingRuleService;

    @Autowired
    private FirmwareConfigService firmwareConfigService;

    @Autowired
    private RoundRobinFilterService roundRobinFilterService;

    @Autowired
    private PredicateManager predicateManager;

    @Override
    public ISimpleCachedDAO<String, Model> getEntityDAO() {
        return modelDAO;
    }

    @Override
    public IValidator<Model> getValidator() {
        return new IValidator<Model>() {
            @Override
            public void validate(Model entity) {}

            @Override
            public void validateAll(Model entity, Iterable<Model> existingEntities) {}
        };
    }

    @Override
    protected List<Predicate<Model>> getPredicatesByContext(Map<String, String> context) {
        List<Predicate<Model>> predicates = new ArrayList<>();
        if (context == null || context.isEmpty()) {
            return predicates;
        }
        if (context.containsKey(SearchFields.ID)) {
            predicates.add(predicateManager.new XEnvModelIdPredicate<Model>(context.get(SearchFields.ID)));
        }
        if (context.containsKey(SearchFields.DESCRIPTION)) {
            predicates.add(predicateManager.new XEnvModelDescriptionPredicate<Model>(context.get(SearchFields.DESCRIPTION)));
        }
        return predicates;
    }

    @Override
    public void validateUsage(String id) {
        List<? extends AbstractService<? extends XRule>> services = Lists.newArrayList(firmwareRuleService, firmwareRuleTemplateService, formulaService, telemetryRuleService, settingRuleService);
        for (AbstractService<? extends XRule> service : services) {
            for (XRule xRule : service.getAll()) {
                if (RuleUtil.isExistConditionByFreeArgAndFixedArg(xRule.getRule(), RuleFactory.MODEL.getName(), id)) {
                    throw new EntityConflictException("Model " + id + " is used by " + xRule.getRuleType() + " " + xRule.getName());
                }
            }
        }

        for (FirmwareConfig firmwareConfig : firmwareConfigService.getAll()) {
            if (CollectionUtils.isNotEmpty(firmwareConfig.getSupportedModelIds()) && firmwareConfig.getSupportedModelIds().contains(id)) {
                throw new EntityConflictException("Model " + id + " is used by FirmwareConfig " + firmwareConfig.getDescription());
            }
        }

        List<DownloadLocationRoundRobinFilterValue> roundRobinFilters = roundRobinFilterService.getAllRoundRobinFilters();
        for (DownloadLocationRoundRobinFilterValue roundRobinFilter : roundRobinFilters) {
            if (roundRobinFilter != null && CollectionUtils.isNotEmpty(roundRobinFilter.getRogueModels())) {
                for (Model model : roundRobinFilter.getRogueModels()) {
                    if (model.getId().equalsIgnoreCase(id)) {
                        throw new EntityConflictException("Model " + id + " is used by " + ApplicationType.get(roundRobinFilter.getApplicationType()) + " RoundRobinFilter");
                    }
                }
            }
        }
    }
}
