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
package com.comcast.xconf.admin.service.telemetry;

import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.hesperius.dataaccess.core.exception.EntityConflictException;
import com.comcast.xconf.admin.validator.telemetry.TelemetryProfileValidator;
import com.comcast.xconf.logupload.telemetry.PermanentTelemetryProfile;
import com.comcast.xconf.logupload.telemetry.TelemetryRule;
import com.comcast.xconf.permissions.PermissionService;
import com.comcast.xconf.permissions.TelemetryPermissionService;
import com.comcast.xconf.search.PredicateManager;
import com.comcast.xconf.search.SearchFields;
import com.comcast.xconf.shared.service.AbstractApplicationTypeAwareService;
import com.comcast.xconf.validators.IValidator;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PermanentTelemetryProfileService extends AbstractApplicationTypeAwareService<PermanentTelemetryProfile> {

    @Autowired
    private ISimpleCachedDAO<String, PermanentTelemetryProfile> permanentTelemetryDAO;

    @Autowired
    private ISimpleCachedDAO<String, TelemetryRule> telemetryRuleDAO;

    @Autowired
    private TelemetryProfileValidator validator;

    @Autowired
    private PredicateManager predicateManager;

    @Autowired
    private TelemetryPermissionService permissionService;

    @Override
    public PermissionService getPermissionService() {
        return permissionService;
    }

    @Override
    public ISimpleCachedDAO<String, PermanentTelemetryProfile> getEntityDAO() {
        return permanentTelemetryDAO;
    }

    @Override
    public IValidator<PermanentTelemetryProfile> getValidator() {
        return validator;
    }

    @Override
    protected void validateUsage(String id) {
        Iterable<TelemetryRule> all = Optional.presentInstances(telemetryRuleDAO.asLoadingCache().asMap().values());
        for (TelemetryRule rule : all) {
            if (StringUtils.equals(rule.getBoundTelemetryId(), id)) {
                throw new EntityConflictException("Can't delete profile as it's used in telemetry rule: " + rule.getName());
            }
        }
    }

    @Override
    protected List<Predicate<PermanentTelemetryProfile>> getPredicatesByContext(Map<String, String> context) {
        List<Predicate<PermanentTelemetryProfile>> predicates = new ArrayList<>();
        if (context == null || context.isEmpty()) {
            return predicates;
        }
        if (context.containsKey(SearchFields.NAME)) {
            predicates.add(predicateManager.new PermanentProfileNamePredicate(context.get(SearchFields.NAME)));
        }
        return predicates;
    }
}
