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
 *  Created: 3:58 PM
 */
package com.comcast.xconf.admin.validator.firmware;

import com.comcast.apps.hesperius.ruleengine.domain.standard.StandardOperation;
import com.comcast.apps.hesperius.ruleengine.main.api.Operation;
import com.comcast.apps.hesperius.ruleengine.main.api.RuleValidationException;
import com.comcast.apps.hesperius.ruleengine.main.impl.Condition;
import com.comcast.xconf.estbfirmware.factory.RuleFactory;
import com.comcast.xconf.firmware.ApplicableAction;
import com.comcast.xconf.firmware.DefinePropertiesTemplateAction;
import com.comcast.xconf.firmware.FirmwareRuleTemplate;
import com.comcast.xconf.shared.validator.BaseRuleValidator;
import com.comcast.xconf.util.RuleUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class FirmwareRuleTemplateValidator extends BaseRuleValidator<FirmwareRuleTemplate> {

    @Override
    public void validate(FirmwareRuleTemplate template) {
        validateRule(template);
    }

    private void validateRule(FirmwareRuleTemplate template) {
        List<Condition> conditions = RuleUtil.getAllConditions(template.getRule());
        if (CollectionUtils.isEmpty(conditions)) {
            throw new RuleValidationException("FirmwareRuleTemplate " + template.getId() + " should have as minimum one condition");
        }

        for (final Condition condition : conditions) {
            checkOperationName(condition);
        }

        validateProperties(template.getApplicableAction());
    }

    private void validateProperties(ApplicableAction applicableAction) {
        ApplicableAction.Type type = applicableAction.getActionType();
        if (ApplicableAction.Type.DEFINE_PROPERTIES_TEMPLATE.equals(type)) {
            Map<String, DefinePropertiesTemplateAction.PropertyValue> properties = ((DefinePropertiesTemplateAction) applicableAction).getProperties();
            for (Map.Entry<String, DefinePropertiesTemplateAction.PropertyValue> entry : properties.entrySet()) {
                if (StringUtils.isBlank(entry.getKey())) {
                    throw new RuleValidationException("Properties key is blank");
                }
            }
        }
    }

    @Override
    public List<Operation> getAllowedOperations() {
        List<Operation> operations = super.getAllowedOperations();
        operations.add(StandardOperation.IN);
        operations.add(StandardOperation.GTE);
        operations.add(StandardOperation.LTE);
        operations.add(StandardOperation.EXISTS);
        operations.add(RuleFactory.MATCH);
        return operations;
    }
}
