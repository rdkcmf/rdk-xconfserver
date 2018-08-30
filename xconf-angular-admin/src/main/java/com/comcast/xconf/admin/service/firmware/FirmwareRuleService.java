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
 * Author: rdolomansky
 * Created: 1/20/16  11:45 AM
 */
package com.comcast.xconf.admin.service.firmware;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.hesperius.dataaccess.core.exception.EntityNotFoundException;
import com.comcast.xconf.admin.validator.firmware.FirmwareRuleValidator;
import com.comcast.xconf.firmware.ApplicableAction;
import com.comcast.xconf.firmware.FirmwareRule;
import com.comcast.xconf.firmware.FirmwareRuleTemplate;
import com.comcast.xconf.permissions.FirmwarePermissionService;
import com.comcast.xconf.permissions.PermissionService;
import com.comcast.xconf.search.PredicateManager;
import com.comcast.xconf.search.SearchFields;
import com.comcast.xconf.shared.service.AbstractApplicationTypeAwareService;
import com.comcast.xconf.util.RuleUtil;
import com.comcast.xconf.validators.IValidator;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.*;

@Component
public class FirmwareRuleService extends AbstractApplicationTypeAwareService<FirmwareRule> {

    @Autowired
    private ISimpleCachedDAO<String, FirmwareRule> firmwareRuleDao;

    @Autowired
    private ISimpleCachedDAO<String, FirmwareRuleTemplate> firmwareRuleTemplateDao;

    @Autowired
    private FirmwareRuleValidator firmwareRuleValidator;

    @Autowired
    private PredicateManager predicateManager;

    @Autowired
    private FirmwarePermissionService permissionService;

    private static final Logger log = LoggerFactory.getLogger(FirmwareConfigService.class);

    public List<FirmwareRule> getAllByType(final ApplicableAction.Type type) {
        return filterByType(getAll(), type);
    }

    @Override
    public ISimpleCachedDAO<String, FirmwareRule> getEntityDAO() {
        return firmwareRuleDao;
    }

    @Override
    public IValidator<FirmwareRule> getValidator() {
        return firmwareRuleValidator;
    }

    @Override
    public void normalizeOnSave(FirmwareRule firmwareRule) {
        if(firmwareRule != null && firmwareRule.getRule() != null) {
            RuleUtil.normalizeConditions(firmwareRule.getRule());
        }
    }

    @Override
    public PermissionService getPermissionService() {
        return permissionService;
    }

    public List<FirmwareRule> filterByTemplate(final List<FirmwareRule> firmwareRules, final String templateId) {
        List<Predicate<FirmwareRule>> predicates = new ArrayList<>();
        predicates.add(predicateManager.new FirmwareRuleByTemplatePredicate(templateId));
        predicates.add(predicateManager.new FirmwareRuleApplicationTypePredicate(permissionService.getReadApplication()));
        //predicates.add(createEditableTemplatePredicate());
        return Lists.newArrayList(Iterables.filter(firmwareRules, Predicates.and(predicates)));
    }

    public List<FirmwareRule> filterByTemplate(List<FirmwareRule> firmwareRules, String templateId, String applicationType) {
        List<Predicate<FirmwareRule>> predicates = new ArrayList();
        predicates.add(predicateManager.new FirmwareRuleByTemplatePredicate(templateId));
        predicates.add(predicateManager.new FirmwareRuleApplicationTypePredicate(applicationType));
        return Lists.newArrayList(Iterables.filter(firmwareRules, Predicates.and(predicates)));
    }

    public List<FirmwareRule> filterByType(final List<FirmwareRule> firmwareRules, final ApplicableAction.Type type) {
        List<Predicate<FirmwareRule>> predicates = new ArrayList<>();
        predicates.add(createEditableTemplatePredicate());
        predicates.add(predicateManager.new FirmwareRuleApplicableActionTypePredicate(type.name()));
        return Lists.newArrayList(Iterables.filter(firmwareRules, Predicates.and(predicates)));
    }

    public List<FirmwareRule> filterByType(ApplicableAction.Type actionType, String applicationType) {
        List<Predicate<FirmwareRule>> predicates = new ArrayList<>();
        predicates.add(createEditableTemplatePredicate());
        predicates.add(predicateManager.new FirmwareRuleApplicableActionTypePredicate(actionType.name()));
        predicates.add(predicateManager.new FirmwareRuleApplicationTypePredicate(applicationType));
        return Lists.newArrayList(Iterables.filter(getAll(), Predicates.and(predicates)));
    }

    public Set<String> getFirmwareRuleNames(String templateId) {
        List<FirmwareRule> firmwareRulesByTemplate = filterByTemplate(getAll(), templateId);
        return FluentIterable.from(firmwareRulesByTemplate)
                .transform(new Function<FirmwareRule, String>() {
                    @Nullable
                    @Override
                    public String apply(@Nullable FirmwareRule input) {
                        return input.getName();
                    }
                }).toSet();
    }

    public Map<String, String> getIdToNameMap(String type) {
        final Map<String, String> nameMap = new HashMap<>();
        for (FirmwareRule rule : getAll()) {
            if (rule.getType().equals(type)) {
                nameMap.put(rule.getId(), rule.getName());
            }
        }

        return nameMap;
    }

    @Override
    public void validateOnSave(FirmwareRule firmwareRule) {
        getValidator().validate(firmwareRule);
        getValidator().validateAll(firmwareRule, filterByTemplate(getAll(), firmwareRule.getType()));
    }

    @Override
    protected List<Predicate<FirmwareRule>> getPredicatesByContext(Map<String, String> context) {
        List<Predicate<FirmwareRule>> predicates = new ArrayList<>();

        if (context == null) {
            return predicates;
        }

        Predicate<FirmwareRule> xRulePredicate = predicateManager.getXRulePredicate(context);
        if (xRulePredicate != null) {
            predicates.add(xRulePredicate);
        }
        if (context.containsKey(SearchFields.NAME)) {
            predicates.add(predicateManager.new XRuleNamePredicate<FirmwareRule>(context.get(SearchFields.NAME)));
        }
        if (context.containsKey(SearchFields.FIRMWARE_VERSION)) {
            predicates.add(predicateManager.new FirmwareRuleConfigPredicate(context.get(SearchFields.FIRMWARE_VERSION)));
        }
        if (context.containsKey(SearchFields.TEMPLATE_ID)) {
            predicates.add(predicateManager.new FirmwareRuleByTemplatePredicate(context.get(SearchFields.TEMPLATE_ID)));
        }
        if (StringUtils.isNotBlank(context.get(SearchFields.APPLICATION_TYPE))) {
            predicates.add(predicateManager.new ApplicationablePredicate<FirmwareRule>(context.get(SearchFields.APPLICATION_TYPE)));
        } else {
            predicates.add(predicateManager.new ApplicationablePredicate<FirmwareRule>(permissionService.getReadApplication()));
        }

        return predicates;
    }

    @Override
    public void beforeUpdating(FirmwareRule firmwareRule) throws ValidationException {
        String id = firmwareRule.getId();
        if (StringUtils.isBlank(id)) {
            throw new ValidationException("FirmwareRule id is empty");
        }
        FirmwareRule existedFirmwareRule = getOne(id);
        if (existedFirmwareRule == null) {
            throw new EntityNotFoundException("FirmwareRule with id: " + id + " does not exist");
        }
    }

    private Predicate<FirmwareRule> createEditableTemplatePredicate() {
        return new Predicate<FirmwareRule>() {
            @Override
            public boolean apply(@Nullable FirmwareRule input) {
                if (input != null && StringUtils.isNotBlank(input.getTemplateId())) {
                    FirmwareRuleTemplate template = firmwareRuleTemplateDao.getOne(input.getTemplateId());
                    return template == null || template.isEditable();
                }
                return false;
            }
        };
    }
}