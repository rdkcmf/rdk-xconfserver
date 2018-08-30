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
 * Author: Yury Stagit
 * Created: 11/06/16  12:00 PM
 */
package com.comcast.xconf.admin.validator.rfc;

import com.comcast.apps.hesperius.ruleengine.domain.standard.StandardOperation;
import com.comcast.apps.hesperius.ruleengine.main.api.Operation;
import com.comcast.hesperius.dataaccess.core.exception.EntityNotFoundException;
import com.comcast.hesperius.dataaccess.core.exception.ValidationRuntimeException;
import com.comcast.xconf.admin.service.rfc.FeatureService;
import com.comcast.xconf.permissions.DcmPermissionService;
import com.comcast.xconf.permissions.PermissionHelper;
import com.comcast.xconf.rfc.FeatureRule;
import com.comcast.xconf.shared.validator.BaseRuleValidator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FeatureRuleValidator extends BaseRuleValidator<FeatureRule> {

    @Autowired
    private FeatureService featureService;

    @Autowired
    private DcmPermissionService permissionService;

    private static final Logger log = LoggerFactory.getLogger(FeatureRuleValidator.class);

    @Override
    public void validate(FeatureRule entity) {
        super.validate(entity);
        if (StringUtils.isBlank(entity.getName())) {
            throw new ValidationRuntimeException("Feature Rule name is blank");
        }
        if (CollectionUtils.isEmpty(entity.getFeatureIds())) {
            throw new ValidationRuntimeException("Features should be specified and be up to 10 items");
        } else {
            for (String featureId : entity.getFeatureIds()) {
                try {
                    featureService.getOne(featureId);
                } catch (EntityNotFoundException e) {
                    log.error("Exception ", e);
                }
            }
        }
        validateApplicationType(entity);
    }

    private void validateApplicationType(FeatureRule featureRule) {
        PermissionHelper.validateWrite(permissionService, featureRule.getApplicationType());
    }

    @Override
    public List<Operation> getAllowedOperations() {
        List<Operation> operations = super.getAllowedOperations();
        operations.add(StandardOperation.EXISTS);
        return operations;
    }
}
