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
 * Author: mdolina
 * Created: 2/21/16
 */
package com.comcast.xconf.admin.service.telemetry;

import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.xconf.admin.validator.telemetry.TelemetryRuleValidator;
import com.comcast.xconf.logupload.telemetry.TelemetryRule;
import com.comcast.xconf.permissions.PermissionService;
import com.comcast.xconf.permissions.TelemetryPermissionService;
import com.comcast.xconf.search.PredicateManager;
import com.comcast.xconf.search.SearchFields;
import com.comcast.xconf.shared.service.AbstractApplicationTypeAwareService;
import com.comcast.xconf.util.RuleUtil;
import com.comcast.xconf.validators.IValidator;
import com.google.common.base.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class TelemetryRuleService extends AbstractApplicationTypeAwareService<TelemetryRule> {

    @Autowired
    private ISimpleCachedDAO<String, TelemetryRule> telemetryRuleDAO;

    @Autowired
    private TelemetryRuleValidator telemetryRuleValidator;

    @Autowired
    private PredicateManager predicateManager;

    @Autowired
    private TelemetryPermissionService permissionService;

    @Override
    public PermissionService getPermissionService() {
        return permissionService;
    }

    @Override
    public ISimpleCachedDAO<String, TelemetryRule> getEntityDAO() {
        return telemetryRuleDAO;
    }

    @Override
    public IValidator<TelemetryRule> getValidator() {
        return telemetryRuleValidator;
    }

    @Override
    protected void normalizeOnSave(TelemetryRule entity) {
        RuleUtil.normalizeConditions(entity);
    }

    @Override
    protected List<Predicate<TelemetryRule>> getPredicatesByContext(Map<String, String> context) {
        List<Predicate<TelemetryRule>> predicates = new ArrayList<>();
        if (context == null || context.isEmpty()) {
            return predicates;
        }

        Predicate<TelemetryRule> xRulePredicate = predicateManager.getXRulePredicate(context);
        if (xRulePredicate != null) {
            predicates.add(xRulePredicate);
        }
        if (context.containsKey(SearchFields.NAME)) {
            predicates.add(predicateManager.new XRuleNamePredicate<TelemetryRule>(context.get(SearchFields.NAME)));
        }
        return predicates;
    }
}
