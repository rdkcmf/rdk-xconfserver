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
import com.comcast.xconf.Environment;
import com.comcast.xconf.XRule;
import com.comcast.xconf.admin.service.dcm.FormulaService;
import com.comcast.xconf.admin.service.firmware.FirmwareRuleService;
import com.comcast.xconf.admin.service.firmware.FirmwareRuleTemplateService;
import com.comcast.xconf.admin.service.setting.SettingRuleService;
import com.comcast.xconf.admin.service.telemetry.TelemetryRuleService;
import com.comcast.xconf.estbfirmware.factory.RuleFactory;
import com.comcast.xconf.search.PredicateManager;
import com.comcast.xconf.search.SearchFields;
import com.comcast.xconf.shared.service.AbstractService;
import com.comcast.xconf.util.RuleUtil;
import com.comcast.xconf.validators.IValidator;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
public class EnvironmentService extends AbstractService<Environment> {

    @Autowired
    private FirmwareRuleService firmwareRuleService;

    @Autowired
    private FirmwareRuleTemplateService firmwareRuleTemplateService;

    @Autowired
    private FormulaService formulaService;

    @Autowired
    private TelemetryRuleService telemetryRuleService;

    @Autowired
    private SettingRuleService settingRuleService;

    @Autowired
    private ISimpleCachedDAO<String, Environment> environmentDAO;

    @Autowired
    private PredicateManager predicateManager;

    @Override
    public ISimpleCachedDAO<String, Environment> getEntityDAO() {
        return environmentDAO;
    }

    @Override
    public IValidator<Environment> getValidator() {
        return new IValidator<Environment>() {
            @Override
            public void validate(Environment entity) {}

            @Override
            public void validateAll(Environment entity, Iterable<Environment> existingEntities) {}
        };
    }

    @Override
    protected List<Predicate<Environment>> getPredicatesByContext(Map<String, String> context) {
        List<Predicate<Environment>> predicates = new ArrayList<>();
        if (context == null || context.isEmpty()) {
            return predicates;
        }
        if (context.containsKey(SearchFields.ID)) {
            predicates.add(predicateManager.new XEnvModelIdPredicate<Environment>(context.get(SearchFields.ID)));
        }
        if (context.containsKey(SearchFields.DESCRIPTION)) {
            predicates.add(predicateManager.new XEnvModelDescriptionPredicate<Environment>(context.get(SearchFields.DESCRIPTION)));
        }
        return predicates;
    }

    @Override
    protected void validateUsage(String id) {
        List<? extends AbstractService<? extends XRule>> services = Lists.newArrayList(firmwareRuleService, firmwareRuleTemplateService, telemetryRuleService, settingRuleService, formulaService);
        for (AbstractService<? extends XRule> service : services) {
            for (XRule xRule : service.getAll()) {
                if (RuleUtil.isExistConditionByFreeArgAndFixedArg(xRule.getRule(), RuleFactory.ENV.getName(), id)) {
                    throw new EntityConflictException("Environment " + id + " is used by " + xRule.getRuleType() + " " + xRule.getName());
                }
            }
        }
    }
}
