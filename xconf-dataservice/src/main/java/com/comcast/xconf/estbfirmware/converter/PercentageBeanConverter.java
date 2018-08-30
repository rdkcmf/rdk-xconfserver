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
 * Created: 1/27/2017
*/
package com.comcast.xconf.estbfirmware.converter;

import com.comcast.apps.hesperius.ruleengine.domain.additional.data.IpAddressGroup;
import com.comcast.apps.hesperius.ruleengine.main.impl.Condition;
import com.comcast.apps.hesperius.ruleengine.main.impl.Rule;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.xconf.estbfirmware.*;
import com.comcast.xconf.estbfirmware.factory.RuleFactory;
import com.comcast.xconf.firmware.ApplicationType;
import com.comcast.xconf.firmware.BlockingFilterAction;
import com.comcast.xconf.firmware.FirmwareRule;
import com.comcast.xconf.firmware.RuleAction;
import com.comcast.xconf.util.RuleUtil;
import com.google.common.base.Optional;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.comcast.apps.hesperius.ruleengine.domain.standard.StandardOperation.IS;
import static com.comcast.apps.hesperius.ruleengine.domain.standard.StandardOperation.PERCENT;

@Component
public class PercentageBeanConverter {

    @Autowired
    private ISimpleCachedDAO<String, FirmwareRule> firmwareRuleDao;

    public PercentageBean convertIntoBean(FirmwareRule firmwareRule) {
        PercentageBean percentageBean = new PercentageBean();
        parseEnvModelRule(percentageBean, firmwareRule);
        if (firmwareRule.getApplicableAction() != null) {
            parseRuleAction(percentageBean, (RuleAction)firmwareRule.getApplicableAction());
        }
        return percentageBean;
    }

    private void parseEnvModelRule(PercentageBean bean, FirmwareRule envModelRule) {
        String model = null;
        String environment = null;
        for (Condition condition : RuleUtil.getAllConditions(envModelRule.getRule())) {
            if (IS.equals(condition.getOperation()) && RuleFactory.MODEL.equals(condition.getFreeArg())) {
                model = String.valueOf(condition.getFixedArg().getValue());
            } else if (IS.equals(condition.getOperation()) && RuleFactory.ENV.equals(condition.getFreeArg())) {
                environment = String.valueOf(condition.getFixedArg().getValue());
            }
        }
        bean.setId(envModelRule.getId());
        bean.setName(envModelRule.getName());
        bean.setEnvironment(environment);
        bean.setModel(model);
        bean.setApplicationType(envModelRule.getApplicationType());
    }

    private void parseRuleAction(PercentageBean bean, RuleAction action) {
        bean.setActive(action.isActive());
        bean.setWhitelist(action.getWhitelist());
        bean.setRebootImmediately(action.isRebootImmediately());
        bean.setFirmwareCheckRequired(action.isFirmwareCheckRequired());
        bean.setFirmwareVersions(action.getFirmwareVersions());
        bean.setIntermediateVersion(action.getIntermediateVersion());
        bean.setDistributions(action.getConfigEntries());
        bean.setLastKnownGood(action.getConfigId());
    }

    public FirmwareRule convertIntoRule(PercentageBean bean) {
        Rule rule = RuleFactory.newEnvModelRule(bean.getEnvironment(), bean.getModel());
        RuleAction action = new RuleAction();
        action.setActive(bean.isActive());
        action.setFirmwareCheckRequired(bean.isFirmwareCheckRequired());
        action.setRebootImmediately(bean.isRebootImmediately());
        action.setWhitelist(bean.getWhitelist());
        action.setIntermediateVersion(bean.getIntermediateVersion());
        action.setConfigId(bean.getLastKnownGood());
        action.setConfigEntries(bean.getDistributions());
        action.setFirmwareVersions(bean.getFirmwareVersions());
        FirmwareRule firmwareRule = RuleFactory.newFirmwareRule(bean.getId(), bean.getName(), TemplateNames.ENV_MODEL_RULE, rule, action, true);
        firmwareRule.setApplicationType(bean.getApplicationType());
        return firmwareRule;
    }

    public List<PercentageBean> convertIntoBeans(PercentFilterValue percentFilterValue) {
        List<PercentageBean> result = new ArrayList<>();
        Map<String, EnvModelPercentage> envModelPercentages = percentFilterValue.getEnvModelPercentages();
        for (String ruleName : envModelPercentages.keySet()) {
            EnvModelPercentage envModelPercentage = envModelPercentages.get(ruleName);
            FirmwareRule envModelRule = getEnvModelRuleByName(ruleName);
            if (envModelRule != null) {
                result.add(migrateIntoPercentageBean(envModelPercentage, envModelRule));
            }
        }
        return result;
    }

    public FirmwareRule convertIntoRule(GlobalPercentage percentage) {
        BigDecimal hundredPercent = new BigDecimal(100);
        BigDecimal incomingPercentage = new BigDecimal(percentage.getPercentage());
        FirmwareRule globalPercentFilterRule = newGlobalPercentFilter(RuleFactory.newGlobalPercentFilter(hundredPercent.subtract(incomingPercentage).doubleValue(), percentage.getWhitelist()));
        globalPercentFilterRule.setApplicationType(percentage.getApplicationType());
        return globalPercentFilterRule;
    }

    public PercentageBean migrateIntoPercentageBean(EnvModelPercentage envModelPercentage, FirmwareRule firmwareRule) {
        PercentageBean percentageBean = new PercentageBean();
        if (firmwareRule != null) {

            parseEnvModelRule(percentageBean, firmwareRule);

            RuleAction ruleAction = (RuleAction) firmwareRule.getApplicableAction();
            if (ruleAction != null && CollectionUtils.isNotEmpty(ruleAction.getConfigEntries())) {
                percentageBean.setDistributions(ruleAction.getConfigEntries());
            } else if (ruleAction != null && StringUtils.isNotBlank(ruleAction.getConfigId())) {
                RuleAction.ConfigEntry configEntry = new RuleAction.ConfigEntry(ruleAction.getConfigId(), envModelPercentage.getPercentage());
                percentageBean.setDistributions(Collections.singletonList(configEntry));
            }
        }

        percentageBean.setWhitelist(getWhitelistName(envModelPercentage.getWhitelist()));
        percentageBean.setActive(envModelPercentage.isActive());
        percentageBean.setLastKnownGood(envModelPercentage.getLastKnownGood());
        percentageBean.setIntermediateVersion(envModelPercentage.getIntermediateVersion());
        percentageBean.setRebootImmediately(envModelPercentage.isRebootImmediately());
        percentageBean.setFirmwareCheckRequired(envModelPercentage.isFirmwareCheckRequired());
        percentageBean.setFirmwareVersions(envModelPercentage.getFirmwareVersions());
        percentageBean.setApplicationType(ApplicationType.get(firmwareRule.getApplicationType()));

        return percentageBean;
    }

    public FirmwareRule convertIntoGlobalPercentage(PercentFilterValue percentFilterValue, String applicationType) {
        double percentage = percentFilterValue.getPercentage();
        BigDecimal hundredPercentage = new BigDecimal(100);
        String whitelistName = getWhitelistName(percentFilterValue.getWhitelist());
        if (StringUtils.isBlank(whitelistName) && percentage == 100.0) {
            return null;
        }
        String applicationTypeToSave = ApplicationType.get(applicationType);
        FirmwareRule globalPercentFirmwareRule = newGlobalPercentFilter(RuleFactory.newGlobalPercentFilter(hundredPercentage.subtract(new BigDecimal(percentage)).doubleValue(), whitelistName));
        globalPercentFirmwareRule.setId(getGlobalPercentId(applicationTypeToSave));
        globalPercentFirmwareRule.setApplicationType(applicationType);
        return globalPercentFirmwareRule;
    }

    public GlobalPercentage convertIntoGlobalPercentage(FirmwareRule rule) {
        GlobalPercentage result = new GlobalPercentage();
        BigDecimal hunderdPercent = new BigDecimal(100);
        for (Condition condition : RuleUtil.getAllConditions(rule.getRule())) {
            if (PERCENT.equals(condition.getOperation())) {
                Object value = condition.getFixedArg().getValue();
                result.setPercentage(hunderdPercent.subtract(new BigDecimal(value.toString())).doubleValue());
            } else if (RuleFactory.IN_LIST.equals(condition.getOperation()) && RuleFactory.IP.equals(condition.getFreeArg())){
                String groupId = (String) condition.getFixedArg().getValue();
                result.setWhitelist(groupId);
            }
        }
        result.setApplicationType(rule.getApplicationType());
        return result;
    }

    public FirmwareRule newGlobalPercentFilter(Rule rule) {
        FirmwareRule firmwareRule = new FirmwareRule();
        firmwareRule.setId(TemplateNames.GLOBAL_PERCENT);
        firmwareRule.setType(TemplateNames.GLOBAL_PERCENT);
        firmwareRule.setName(TemplateNames.GLOBAL_PERCENT);
        firmwareRule.setRule(rule);
        firmwareRule.setApplicableAction(new BlockingFilterAction());
        return firmwareRule;
    }

    public FirmwareRule newEnvModelRule(String id, String name, Rule rule, String config, List<RuleAction.ConfigEntry> distributions, String whitelist, boolean alwaysReturnConfig) {
        RuleAction action = new RuleAction(config);
        action.setActive(alwaysReturnConfig);
        action.setConfigEntries(distributions);
        action.setWhitelist(whitelist);
        return RuleFactory.newFirmwareRule(id, name, TemplateNames.ENV_MODEL_RULE, rule, action, true); // always active
    }

    private String getWhitelistName(IpAddressGroup ipAddressGroup) {
        return ipAddressGroup != null ? ipAddressGroup.getName() : null;
    }

    private FirmwareRule getEnvModelRuleByName(String name) {
        Iterable<FirmwareRule> firmwareRules = Optional.presentInstances(firmwareRuleDao.asLoadingCache().asMap().values());
        for (FirmwareRule rule : firmwareRules) {
            if (TemplateNames.ENV_MODEL_RULE.equals(rule.getType()) && StringUtils.equals(rule.getName(), name)) {
                return rule;
            }
        }
        return null;
    }

    private String getGlobalPercentId(String applicationType) {
        if (ApplicationType.equals(ApplicationType.STB, applicationType)) {
            return TemplateNames.GLOBAL_PERCENT;
        }
        return applicationType.toUpperCase() + "_" + TemplateNames.GLOBAL_PERCENT;
    }

}
