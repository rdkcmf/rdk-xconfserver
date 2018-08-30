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
 * Created: 11/04/16  12:00 PM
 */
package com.comcast.xconf.admin.service.rfc;

import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.xconf.admin.validator.rfc.FeatureSetValidator;
import com.comcast.xconf.permissions.DcmPermissionService;
import com.comcast.xconf.permissions.PermissionService;
import com.comcast.xconf.rfc.FeatureRule;
import com.comcast.xconf.rfc.FeatureSet;
import com.comcast.xconf.search.PredicateManager;
import com.comcast.xconf.search.SearchFields;
import com.comcast.xconf.shared.service.AbstractApplicationTypeAwareService;
import com.comcast.xconf.validators.IValidator;
import com.google.common.base.Predicate;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class FeatureSetService extends AbstractApplicationTypeAwareService<FeatureSet> {

    @Autowired
    private FeatureSetValidator featureSetValidator;

    @Autowired
    private ISimpleCachedDAO<String, FeatureSet> featureSetDAO;

    @Autowired
    private ISimpleCachedDAO<String, FeatureRule> featureRuleDAO;

    @Autowired
    private PredicateManager predicateManager;

    @Autowired
    private DcmPermissionService permissionService;

    @Override
    public ISimpleCachedDAO<String, FeatureSet> getEntityDAO() {
        return featureSetDAO;
    }

    @Override
    public IValidator<FeatureSet> getValidator() {
        return featureSetValidator;
    }

    @Override
    protected List<Predicate<FeatureSet>> getPredicatesByContext(Map<String, String> context) {
        List<Predicate<FeatureSet>> predicates = new ArrayList<>();
        if (MapUtils.isEmpty(context)) {
            return predicates;
        }

        if (context.containsKey(SearchFields.NAME)) {
            predicates.add(predicateManager.new FeatureSetNamePredicate(context.get(SearchFields.NAME)));
        }
        if (context.containsKey(SearchFields.FEATURE)) {
            predicates.add(predicateManager.new FeatureSetByFeatureNamePredicate(context.get(SearchFields.FEATURE)));
        }
        return predicates;
    }

    @Override
    protected PermissionService getPermissionService() {
        return permissionService;
    }
}
