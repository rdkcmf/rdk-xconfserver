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
 * Author: Igor Kostrov
 * Created: 3/17/2016
*/
package com.comcast.xconf.admin.service.setting;

import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.hesperius.dataaccess.core.exception.EntityConflictException;
import com.comcast.xconf.admin.validator.setting.SettingProfileValidator;
import com.comcast.xconf.logupload.settings.SettingProfile;
import com.comcast.xconf.logupload.settings.SettingRule;
import com.comcast.xconf.permissions.DcmPermissionService;
import com.comcast.xconf.permissions.PermissionService;
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
public class SettingProfileService extends AbstractApplicationTypeAwareService<SettingProfile> {

    @Autowired
    private ISimpleCachedDAO<String, SettingRule> settingRuleDAO;

    @Autowired
    private ISimpleCachedDAO<String, SettingProfile> settingProfileDao;

    @Autowired
    private SettingProfileValidator validator;

    @Autowired
    private PredicateManager predicateManager;

    @Autowired
    private DcmPermissionService permissionService;

    @Override
    public PermissionService getPermissionService() {
        return permissionService;
    }

    @Override
    public ISimpleCachedDAO<String, SettingProfile> getEntityDAO() {
        return settingProfileDao;
    }

    @Override
    public IValidator<SettingProfile> getValidator() {
        return validator;
    }

    @Override
    protected void validateUsage(String id) {
        Iterable<SettingRule> all = Optional.presentInstances(settingRuleDAO.asLoadingCache().asMap().values());
        for (SettingRule rule : all) {
            if (StringUtils.equals(rule.getBoundSettingId(), id)) {
                throw new EntityConflictException("Can't delete profile as it's used in setting rule: " + rule.getName());
            }
        }
    }

    @Override
    protected List<Predicate<SettingProfile>> getPredicatesByContext(Map<String, String> context) {
        List<Predicate<SettingProfile>> predicates = new ArrayList<>();
        if (context.containsKey(SearchFields.NAME)) {
            predicates.add(predicateManager.new SettingProfileNamePredicate(context.get(SearchFields.NAME)));
        }
        if (context.containsKey(SearchFields.TYPE)) {
            predicates.add(predicateManager.new SettingProfileTypePredicate(context.get(SearchFields.TYPE)));
        }
        return predicates;
    }
}
