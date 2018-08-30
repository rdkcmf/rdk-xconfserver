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
package com.comcast.xconf.admin.service.rfc;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.hesperius.dataaccess.core.exception.EntityNotFoundException;
import com.comcast.xconf.admin.validator.rfc.FeatureRuleValidator;
import com.comcast.xconf.permissions.DcmPermissionService;
import com.comcast.xconf.permissions.PermissionService;
import com.comcast.xconf.priority.PriorityUtils;
import com.comcast.xconf.rfc.FeatureRule;
import com.comcast.xconf.search.PredicateManager;
import com.comcast.xconf.search.SearchFields;
import com.comcast.xconf.shared.service.AbstractApplicationTypeAwareService;
import com.comcast.xconf.util.RuleUtil;
import com.comcast.xconf.validators.IValidator;
import com.google.common.base.Predicate;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class FeatureRuleService extends AbstractApplicationTypeAwareService<FeatureRule> {

    @Autowired
    private ISimpleCachedDAO<String, FeatureRule> featureRuleDAO;

    @Autowired
    private FeatureRuleValidator featureRuleValidator;

    @Autowired
    private PredicateManager predicateManager;

    @Autowired
    private DcmPermissionService permissionService;

    private static Logger log = LoggerFactory.getLogger(FeatureService.class);

    @Override
    public PermissionService getPermissionService() {
        return permissionService;
    }

    @Override
    public ISimpleCachedDAO<String, FeatureRule> getEntityDAO() {
        return featureRuleDAO;
    }

    @Override
    public IValidator<FeatureRule> getValidator() {
        return featureRuleValidator;
    }

    @Override
    protected List<Predicate<FeatureRule>> getPredicatesByContext(Map<String, String> context) {
        List<Predicate<FeatureRule>> predicates = new ArrayList<>();
        if (MapUtils.isEmpty(context)) {
            return predicates;
        }

        Predicate<FeatureRule> xRulePredicate = predicateManager.getXRulePredicate(context);
        if (xRulePredicate != null) {
            predicates.add(xRulePredicate);
        }
        if (context.containsKey(SearchFields.NAME)) {
            predicates.add(predicateManager.new XRuleNamePredicate<FeatureRule>(context.get(SearchFields.NAME)));
        }
        if (context.containsKey(SearchFields.FEATURE)) {
            predicates.add(predicateManager.new FeatureRuleFeatureNamePredicate(context.get(SearchFields.FEATURE)));
        }
        return predicates;
    }

    public List<FeatureRule> changePriorities(String featureRuleId, Integer newPriority) throws ValidationException {
        final FeatureRule featureRuleToUpdate = getEntityDAO().getOne(featureRuleId);
        if (featureRuleToUpdate == null) {
            throw new EntityNotFoundException("Formula with id " + featureRuleId + " does not exist");
        }

        List<FeatureRule> reorganizedFeatureRules = PriorityUtils.updatePriorities(getAll(), featureRuleToUpdate.getPriority(), newPriority);
        saveAll(reorganizedFeatureRules);

        return reorganizedFeatureRules;
    }

    private void saveAll(List<FeatureRule> featureRules) throws ValidationException {
        for (FeatureRule featureRule : featureRules) {
            getEntityDAO().setOne(featureRule.getId(), featureRule);
        }
    }

    @Override
    public FeatureRule create(FeatureRule featureRule) throws ValidationException {
        beforeCreating(featureRule);
        beforeSaving(featureRule);
        saveAll(PriorityUtils.addNewItemAndReorganize(featureRule, getAll()));
        return featureRule;
    }

    @Override
    public FeatureRule update(FeatureRule featureRule) throws ValidationException {
        beforeUpdating(featureRule);
        beforeSaving(featureRule);
        FeatureRule featureRuleToUpdate = getEntityDAO().getOne(featureRule.getId());
        saveAll(PriorityUtils.updateItemByPriorityAndReorganize(featureRule, getAll(), featureRuleToUpdate.getPriority()));
        return featureRule;
    }

    @Override
    public FeatureRule delete(final String id) throws ValidationException {
        FeatureRule removedFeatureRule = super.delete(id);
        try {
            saveAll(PriorityUtils.packPriorities(getAll()));
        } catch (ValidationException e) {
            log.error("Failed to save all" + e);
        }
        return removedFeatureRule;
    }

    @Override
    public void normalizeOnSave(FeatureRule featureRule) {
        if(featureRule != null && featureRule.getRule() != null) {
            RuleUtil.normalizeConditions(featureRule.getRule());
        }
    }
}