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
 * Author: ikostrov
 * Created: 06.08.15 19:33
*/
package com.comcast.xconf.estbfirmware;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.xconf.MacAddressUtil;
import com.comcast.xconf.NamespacedList;
import com.comcast.xconf.StbContext;
import com.comcast.xconf.estbfirmware.legacy.MacRuleLegacyConverter;
import com.comcast.xconf.firmware.ApplicableAction;
import com.comcast.xconf.firmware.FirmwareRule;
import com.comcast.xconf.firmware.RuleAction;
import com.comcast.xconf.queries.beans.MacRuleBeanWrapper;
import com.comcast.xconf.search.PredicateManager;
import com.comcast.xconf.service.GenericNamespacedListLegacyService;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MacRuleService {

    private static final Logger log = LoggerFactory.getLogger(MacRuleService.class);

    @Autowired
    private ISimpleCachedDAO<String, FirmwareConfig> firmwareConfigDAO;

    @Autowired
    private ISimpleCachedDAO<String, FirmwareRule> firmwareRuleDao;

    @Autowired
    private GenericNamespacedListLegacyService genericNamespacedListLegacyService;

    @Autowired
    private PredicateManager predicateManager;

    public void save(MacRuleBean bean, String applicationType) throws ValidationException {
        if (StringUtils.isBlank(bean.getId())) {
            bean.setId(UUID.randomUUID().toString());
        }
        FirmwareRule macRule = convertMacRuleBeanToFirmwareRule(bean);
        if (StringUtils.isNotBlank(applicationType)) {
            macRule.setApplicationType(applicationType);
        }
        firmwareRuleDao.setOne(macRule.getId(), macRule);
    }

    public MacRuleBean getOne(String id) {
        FirmwareRule fr = firmwareRuleDao.getOne(id);
        if (fr != null) {
            return convertFirmwareRuleToMacRuleBean(fr);
        }
        return null;
    }

    public Set<MacRuleBeanWrapper> getByApplicationType(String applicationType) {
        List<Predicate<FirmwareRule>> predicates = new ArrayList<>();
        predicates.add(predicateManager.new FirmwareRuleByTemplatePredicate(TemplateNames.MAC_RULE));
        predicates.add(predicateManager.new ApplicationablePredicate<FirmwareRule>(applicationType));
        List<FirmwareRule> firmwareRules = Lists.newArrayList(Iterables.filter(firmwareRuleDao.getAll(), Predicates.and(predicates)));
        Set<MacRuleBeanWrapper> macRuleBeans = new TreeSet<>();
        for (FirmwareRule fwr : firmwareRules) {
            try {
                macRuleBeans.add(convertFirmwareRuleToMacRuleBean(fwr));
            } catch (RuntimeException e) {
                log.error("Could not convert", e);
            }
        }
        return macRuleBeans;
    }

    public Set<MacRuleBeanWrapper> getRulesWithMacCondition(String applicationType) {
        List<Predicate<FirmwareRule>> predicates = new ArrayList<>();
        predicates.add(predicateManager.new XRuleFreeArgPredicate<FirmwareRule>(StbContext.ESTB_MAC));
        predicates.add(predicateManager.new FirmwareRuleApplicableActionTypePredicate(ApplicableAction.Type.RULE.name()));
        predicates.add(predicateManager.new ApplicationablePredicate<FirmwareRule>(applicationType));
        List<FirmwareRule> firmwareRules = Lists.newArrayList(Iterables.filter(firmwareRuleDao.getAll(), Predicates.and(predicates)));
        Set<MacRuleBeanWrapper> macRuleBeans = new TreeSet<>();
        for (FirmwareRule firmwareRule : firmwareRules) {
            try {
                macRuleBeans.add(convertFirmwareRuleToMacRuleBean(firmwareRule));
            } catch (Exception e) {
                log.error("Could not convert", e);
            }
        }
        return macRuleBeans;
    }

    public void delete(String id) {
        firmwareRuleDao.deleteOne(id);
    }

    public Set<MacRuleBeanWrapper> searchMacRules(String macPart, String applicationType) {

        macPart = MacAddressUtil.removeNonAlphabeticSymbols(macPart);

        Set<MacRuleBeanWrapper> macRules = getRulesWithMacCondition(applicationType);
        for (Iterator<MacRuleBeanWrapper> iterator = macRules.iterator(); iterator.hasNext(); ) {
            MacRuleBeanWrapper macRuleBean = iterator.next();
            Set<String> macAddressesToSearch = new HashSet<>();
            if (StringUtils.isNotBlank(macRuleBean.getMacListRef())) {
                NamespacedList macList = genericNamespacedListLegacyService.getNamespacedList(macRuleBean.getMacListRef());
                if (macList != null && CollectionUtils.isNotEmpty(macList.getData())) {
                    macAddressesToSearch.addAll(macList.getData());
                }
            }
            if (CollectionUtils.isNotEmpty(macRuleBean.getMacList())) {
                macAddressesToSearch.addAll(macRuleBean.getMacList());
            }

            if (!isExistMacAddressInList(macAddressesToSearch, macPart)) {
                iterator.remove();
            }
        }

        return macRules;
    }

    public FirmwareRule convertMacRuleBeanToFirmwareRule(MacRuleBean bean) {
        return MacRuleLegacyConverter.convertMacRuleBeanToFirmwareRule(bean);
    }

    public MacRuleBeanWrapper convertFirmwareRuleToMacRuleBean(FirmwareRule firmwareRule) {
        MacRuleBeanWrapper macRuleBean = MacRuleLegacyConverter.convertFirmwareRuleToMacRuleBeanWrapper(firmwareRule);
        RuleAction action = (RuleAction) firmwareRule.getApplicableAction();
        if (action != null && StringUtils.isNotBlank(action.getConfigId())) {
            FirmwareConfig config = firmwareConfigDAO.getOne(action.getConfigId());
            macRuleBean.setFirmwareConfig(config);
            macRuleBean.setTargetedModelIds(config != null ? config.getSupportedModelIds() : new HashSet<String>());
        }
        return macRuleBean;
    }

    private boolean isExistMacAddressInList(Set<String> macAddresses, String macPart) {
        for (String macAddress : macAddresses) {
            if (macAddress.replaceAll(":", "").contains(macPart)) {
                return true;
            }
        }
        return false;
    }

    public MacRuleBeanWrapper getRuleWithMacConditionByName(String ruleName, String applicationType) {
        for (MacRuleBeanWrapper macRule : getRulesWithMacCondition(applicationType)) {
            if (ruleName.equals(macRule.getName())) {
                return macRule;
            }
        }
        return null;
    }

    public MacRuleBeanWrapper getMacRuleByName(String ruleName, String applicationType) {
        for (MacRuleBeanWrapper macRule : getByApplicationType(applicationType)) {
            if (ruleName.equals(macRule.getName())) {
                return macRule;
            }
        }
        return null;
    }
}
