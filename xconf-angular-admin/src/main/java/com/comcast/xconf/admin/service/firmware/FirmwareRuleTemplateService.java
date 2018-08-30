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
 * Author: Stanislav Menshykov
 * Created: 18.01.16  18:34
 */
package com.comcast.xconf.admin.service.firmware;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.hesperius.dataaccess.core.exception.EntityConflictException;
import com.comcast.hesperius.dataaccess.core.exception.EntityExistsException;
import com.comcast.hesperius.dataaccess.core.exception.EntityNotFoundException;
import com.comcast.xconf.admin.validator.firmware.FirmwareRuleTemplateValidator;
import com.comcast.xconf.firmware.ApplicableAction;
import com.comcast.xconf.firmware.ApplicationType;
import com.comcast.xconf.firmware.FirmwareRule;
import com.comcast.xconf.firmware.FirmwareRuleTemplate;
import com.comcast.xconf.importing.OverwritePrioritizableWrapperComparator;
import com.comcast.xconf.importing.OverwriteWrapper;
import com.comcast.xconf.priority.PriorityUtils;
import com.comcast.xconf.search.PredicateManager;
import com.comcast.xconf.search.SearchFields;
import com.comcast.xconf.shared.service.AbstractService;
import com.comcast.xconf.util.RuleUtil;
import com.comcast.xconf.validators.IValidator;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.*;

@Component
public class FirmwareRuleTemplateService extends AbstractService<FirmwareRuleTemplate> {

    @Autowired
    private ISimpleCachedDAO<String, FirmwareRuleTemplate> firmwareRuleTemplateDao;

    @Autowired
    private FirmwareRuleService firmwareRuleService;

    @Autowired
    private FirmwareRuleTemplateValidator firmwareRuleTemplateValidator;

    @Autowired
    private PredicateManager predicateManager;

    @Override
    public FirmwareRuleTemplate save(FirmwareRuleTemplate template) throws ValidationException {
        List<FirmwareRuleTemplate> templatesOfCurrentType = getTemplatesByType(template.getApplicableAction().getActionType(), firmwareRuleTemplateDao.getAll());
        saveAll(PriorityUtils.addNewItemAndReorganize(template, templatesOfCurrentType));

        return template;
    }

    @Override
    public FirmwareRuleTemplate create(FirmwareRuleTemplate template) throws ValidationException {
        final String id = template.getId();
        if (id != null && firmwareRuleTemplateDao.getOne(id) != null) {
            throw new EntityExistsException("FirmwareRuleTemplate with id " + id + " already exists");
        }
        firmwareRuleTemplateValidator.validate(template);
        List<FirmwareRuleTemplate> templatesOfCurrentType = getTemplatesByType(template.getApplicableAction().getActionType(), firmwareRuleTemplateDao.getAll());
        firmwareRuleTemplateValidator.validateAll(template, templatesOfCurrentType);
        saveAll(PriorityUtils.addNewItemAndReorganize(template, templatesOfCurrentType));

        return template;
    }

    @Override
    public FirmwareRuleTemplate update(FirmwareRuleTemplate template) throws ValidationException {
        FirmwareRuleTemplate templateToUpdate = firmwareRuleTemplateDao.getOne(template.getId());
        if (templateToUpdate == null) {
            throw  new EntityNotFoundException("FirmwareRuleTemplate " + template.getId() + " doesn't exist");
        }
        firmwareRuleTemplateValidator.validate(template);
        List<FirmwareRuleTemplate> templatesOfCurrentType = getTemplatesByType(template.getApplicableAction().getActionType(), firmwareRuleTemplateDao.getAll());
        firmwareRuleTemplateValidator.validateAll(templateToUpdate, templatesOfCurrentType);
        saveAll(PriorityUtils.updateItemByPriorityAndReorganize(template, templatesOfCurrentType, templateToUpdate.getPriority()));

        return template;
    }

    @Override
    public FirmwareRuleTemplate delete(String id) throws ValidationException {
        FirmwareRuleTemplate templateToDelete = firmwareRuleTemplateDao.getOne(id);
        super.delete(id);

        saveAll(PriorityUtils.packPriorities(getTemplatesByType(templateToDelete.getApplicableAction().getActionType(),
                firmwareRuleTemplateDao.getAll())));

        return templateToDelete;
    }

    @Override
    public ISimpleCachedDAO<String, FirmwareRuleTemplate> getEntityDAO() {
        return firmwareRuleTemplateDao;
    }

    @Override
    public IValidator<FirmwareRuleTemplate> getValidator() {
        return firmwareRuleTemplateValidator;
    }

    public Map<String, List<String>> importTemplates(List<OverwriteWrapper<FirmwareRuleTemplate>> wrappedTemplates) throws ValidationException {
        final List<String> failedToImport = new ArrayList<>();
        final List<String> successfulImportIds = new ArrayList<>();
        Collections.sort(wrappedTemplates, new OverwritePrioritizableWrapperComparator());
        for (OverwriteWrapper<FirmwareRuleTemplate> wrappedTemplate : wrappedTemplates) {
            FirmwareRuleTemplate currentTemplate = wrappedTemplate.getEntity();
            String currentTemplateId = currentTemplate.getId();
            if (wrappedTemplate.getOverwrite()) {
                try {
                    update(currentTemplate);
                    successfulImportIds.add(currentTemplateId);
                } catch (RuntimeException e) {
                    failedToImport.add(e.getMessage());
                }
            } else {
                try {
                    create(currentTemplate);
                    successfulImportIds.add(currentTemplateId);
                } catch (RuntimeException e) {
                    failedToImport.add(e.getMessage());
                }
            }
        }

        return new HashMap<String, List<String>>(){{
            put("success", successfulImportIds);
            put("failure", failedToImport);
        }};
    }

    public void saveAll(List<FirmwareRuleTemplate> templateList) throws ValidationException {
        for (FirmwareRuleTemplate template : templateList) {
            firmwareRuleTemplateDao.setOne(template.getId(), template);
        }
    }

    private List<FirmwareRuleTemplate> getTemplatesByType(final ApplicableAction.Type type, List<FirmwareRuleTemplate> templates) {
        return Lists.newArrayList(Iterables.filter(templates, new Predicate<FirmwareRuleTemplate>() {
            @Override
            public boolean apply(FirmwareRuleTemplate input) {
                return input.getApplicableAction() != null && input.getApplicableAction().getActionType() == type;
            }
        }));
    }

    @Override
    protected void validateUsage(String id) {
        FirmwareRuleTemplate template = getOne(id);
        List<FirmwareRule> firmwareRulesByTemplate = firmwareRuleService.filterByTemplate(firmwareRuleService.getEntityDAO().getAll(), template.getId(), ApplicationType.ALL);
        if (CollectionUtils.isNotEmpty(firmwareRulesByTemplate)) {
            Set<String> ruleNames = getFirwmareRuleNames(firmwareRulesByTemplate);
            throw new EntityConflictException("Template " + template.getId() + " is used by rules: " + Joiner.on(", ").join(ruleNames));
        }
    }

    public List<FirmwareRuleTemplate> getByTypeAndEditableOption(ApplicableAction.Type type, boolean isEditable) {
        List<Predicate<FirmwareRuleTemplate>> predicates = Lists.newArrayList(createByTypePredicate(type), createEditablePredicate(isEditable));
        return Lists.newArrayList(Iterables.filter(getAll(), Predicates.and(predicates)));
    }

    public List<String> getTemplateIds(final ApplicableAction.Type type) {
        return FluentIterable
                .from(getAll())
                .filter(new Predicate<FirmwareRuleTemplate>() {
                    @Override
                    public boolean apply(FirmwareRuleTemplate input) {
                        return input != null
                                && input.getApplicableAction().getActionType() == type
                                && input.isEditable();
                    }
                }).transform(new Function<FirmwareRuleTemplate, String>() {
                    @Nullable
                    @Override
                    public String apply(FirmwareRuleTemplate input) {
                        return input.getId();
                    }
                }).toList();
    }

    public List<FirmwareRuleTemplate> changePriorities(String templateId, Integer newPriority) throws ValidationException {
        final FirmwareRuleTemplate templateToUpdate = getOne(templateId);
        if (templateToUpdate == null) {
            throw new EntityNotFoundException("FirmwareTemplate with id " + templateId + " does not exist");
        }
        List<FirmwareRuleTemplate> templatesOfCurrentType = getTemplatesByType(templateToUpdate.getApplicableAction().getActionType(), getAll());
        List<FirmwareRuleTemplate> reorganizedTemplates = PriorityUtils.updatePriorities(templatesOfCurrentType, templateToUpdate.getPriority(), newPriority);
        saveAll(reorganizedTemplates);
        return reorganizedTemplates;
    }

    @Override
    public List<Predicate<FirmwareRuleTemplate>> getPredicatesByContext(Map<String, String> context) {
        List<Predicate<FirmwareRuleTemplate>> predicates = new ArrayList<>();
        if (context == null || context.isEmpty()) {
            return predicates;
        }

        Predicate<FirmwareRuleTemplate> xRulePredicate = predicateManager.getXRulePredicate(context);
        if (xRulePredicate != null) {
            predicates.add(xRulePredicate);
        }
        if (context.containsKey(SearchFields.NAME)) {
            predicates.add(predicateManager.new XRuleNamePredicate<FirmwareRuleTemplate>(context.get(SearchFields.NAME)));
        }
        return predicates;
    }

    @Override
    protected void normalizeOnSave(FirmwareRuleTemplate firmwareRuleTemplate) {
        if (firmwareRuleTemplate != null) {
            RuleUtil.normalizeConditions(firmwareRuleTemplate.getRule());
        }
    }

    private Predicate<FirmwareRuleTemplate> createEditablePredicate(final boolean isEditable) {
        return new Predicate<FirmwareRuleTemplate>() {
            @Override
            public boolean apply(@Nullable FirmwareRuleTemplate input) {
                return input.isEditable() == isEditable;
            }
        };
    }

    private Predicate<FirmwareRuleTemplate> createByTypePredicate(final ApplicableAction.Type type) {
        return new Predicate<FirmwareRuleTemplate>() {
            @Override
            public boolean apply(FirmwareRuleTemplate input) {
                return input.getApplicableAction() != null && input.getApplicableAction().getActionType() == type;
            }
        };
    }

    private Set<String> getFirwmareRuleNames(List<FirmwareRule> firmwareRules) {
        Set<String> names = new HashSet<>();
        for (FirmwareRule firmwareRule : firmwareRules) {
            names.add(firmwareRule.getName());
        }
        return names;
    }

}
