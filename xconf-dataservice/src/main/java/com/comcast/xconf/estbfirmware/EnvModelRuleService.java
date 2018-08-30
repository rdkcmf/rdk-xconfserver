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
 * Created: 13.08.15 19:59
*/
package com.comcast.xconf.estbfirmware;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.xconf.estbfirmware.converter.NgRuleConverter;
import com.comcast.xconf.estbfirmware.legacy.EnvModelRuleLegacyConverter;
import com.comcast.xconf.firmware.FirmwareRule;
import com.comcast.xconf.firmware.RuleAction;
import com.comcast.xconf.search.PredicateManager;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EnvModelRuleService {

    private static final Logger log = LoggerFactory.getLogger(EnvModelRuleService.class);

    @Autowired
    private NgRuleConverter ngRuleConverter;

    @Autowired
    private ISimpleCachedDAO<String, FirmwareConfig> firmwareConfigDAO;

    @Autowired
    private ISimpleCachedDAO<String, FirmwareRule> firmwareRuleDao;

    @Autowired
    private PredicateManager predicateManager;

    public Set<EnvModelRuleBean> getByApplicationType(String applicationType) {
        List<FirmwareRule> allRules = firmwareRuleDao.getAll();
        List<Predicate<FirmwareRule>> predicates = new ArrayList<>();
        predicates.add(predicateManager.new FirmwareRuleByTemplatePredicate(TemplateNames.ENV_MODEL_RULE));
        predicates.add(predicateManager.new ApplicationablePredicate<FirmwareRule>(applicationType));
        allRules = Lists.newArrayList(Iterables.filter(allRules, Predicates.and(predicates)));
        Set<EnvModelRuleBean> envModelRuleBeans = new TreeSet<>();

        for (FirmwareRule firmwareRule : allRules) {
            envModelRuleBeans.add(convertFirmwareRuleToEnvModelRuleBean(firmwareRule));
        }
        return envModelRuleBeans;
    }

    public List<EnvModelRuleBean> getAll() {
        List<FirmwareRule> envModelRules = firmwareRuleDao.getAll(predicateManager.new FirmwareRuleByTemplatePredicate(TemplateNames.ENV_MODEL_RULE));
        List<EnvModelRuleBean> envModelRuleBeans = new ArrayList<>();
        for (FirmwareRule envModelRule : envModelRules) {
            envModelRuleBeans.add(convertFirmwareRuleToEnvModelRuleBean(envModelRule));
        }
        return envModelRuleBeans;
    }

    public void save(EnvModelRuleBean bean, String applicationType) throws ValidationException {
        if (bean.getId() == null) {
            String id = UUID.randomUUID().toString();
            bean.setId(id);
        }
        FirmwareRule envModelRule = convertModelRuleBeanToFirmwareRule(bean);
        if (StringUtils.isNotBlank(applicationType)) {
            envModelRule.setApplicationType(applicationType);
        }
        firmwareRuleDao.setOne(envModelRule.getId(), envModelRule);
    }

    public void delete(String id) {
        firmwareRuleDao.deleteOne(id);
    }

    public EnvModelRuleBean convertFirmwareRuleToEnvModelRuleBean(FirmwareRule firmwareRule) {
        EnvModelRuleBean bean = EnvModelRuleLegacyConverter.convertFirmwareRuleToEnvModelRuleBean(ngRuleConverter.convertNew(firmwareRule));
        RuleAction action = (RuleAction) firmwareRule.getApplicableAction();
        if (StringUtils.isNotBlank(action.getConfigId())) {
            bean.setFirmwareConfig(firmwareConfigDAO.getOne(action.getConfigId()));
        }

        return bean;
    }

    public FirmwareRule convertModelRuleBeanToFirmwareRule(EnvModelRuleBean bean) {
        return ngRuleConverter.convertOld(EnvModelRuleLegacyConverter.convertModelRuleBeanToFirmwareRule(bean));
    }

    public EnvModelRuleBean getOne(String id) {
        FirmwareRule fr = firmwareRuleDao.getOne(id);
        if (fr != null) {
            return convertFirmwareRuleToEnvModelRuleBean(fr);
        }
        return null;
    }

    public EnvModelRuleBean getOneByName(String name, String applicationType) {
        for (EnvModelRuleBean envModelRule : getByApplicationType(applicationType)) {
            if (envModelRule.getName().equalsIgnoreCase(name)) {
                return envModelRule;
            }
        }
        return null;
    }

    public EnvModelRuleBean getOneByEnvModel(String model, String environment, String applicationType) {
        for (EnvModelRuleBean envModelRule : getByApplicationType(applicationType)) {
            if (StringUtils.equalsIgnoreCase(envModelRule.getEnvironmentId(), environment)
                    && StringUtils.equalsIgnoreCase(envModelRule.getModelId(), model)) {
                return envModelRule;
            }
        }
        return null;
    }

    public FirmwareConfig nullifyUnwantedFields(FirmwareConfig config) {
        if (config != null) {
            config.setUpdated(null);
            config.setFirmwareDownloadProtocol(null);
            config.setRebootImmediately(null);
        }

        return config;
    }

    public boolean isExistEnvModelRule(EnvModelRuleBean envModelRule, String applicationType) {
        if (envModelRule != null && envModelRule.getEnvironmentId() != null && envModelRule.getModelId() != null) {
            EnvModelRuleBean one = getOneByEnvModel(envModelRule.getModelId(), envModelRule.getEnvironmentId(), applicationType);
            return one != null;
        }
        return false;
    }

}
