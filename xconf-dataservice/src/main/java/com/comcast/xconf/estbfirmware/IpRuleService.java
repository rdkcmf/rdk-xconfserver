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
 * Created: 13.08.15 19:41
*/
package com.comcast.xconf.estbfirmware;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.xconf.estbfirmware.converter.NgRuleConverter;
import com.comcast.xconf.estbfirmware.legacy.IpRuleLegacyConverter;
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
public class IpRuleService {

    private static final Logger log = LoggerFactory.getLogger(IpRuleService.class);

    @Autowired
    private NgRuleConverter ngRuleConverter;

    @Autowired
    private IpRuleLegacyConverter ipRuleLegacyConverter;

    @Autowired
    private ISimpleCachedDAO<String, FirmwareConfig> firmwareConfigDAO;

    @Autowired
    private ISimpleCachedDAO<String, FirmwareRule> firmwareRuleDao;

    @Autowired
    private PredicateManager predicateManager;

    public Set<IpRuleBean> getAllIpRulesFromDB() {
        List<FirmwareRule> allRules = firmwareRuleDao.getAll();
        Set<IpRuleBean> ipRuleBeans = new TreeSet<>();

        for (FirmwareRule fwr : allRules) {
            if (TemplateNames.IP_RULE.equals(fwr.getType())) {
                try {
                    ipRuleBeans.add(convertFirmwareRuleToIpRuleBean(fwr));
                } catch (RuntimeException e) {
                    log.error("Could not convert", e);
                }
            }
        }
        return ipRuleBeans;
    }

    public Set<IpRuleBean> getByApplicationType(String applicationType) {
        List<Predicate<FirmwareRule>> predicates = new ArrayList<>();
        predicates.add(predicateManager.new FirmwareRuleByTemplatePredicate(TemplateNames.IP_RULE));
        predicates.add(predicateManager.new ApplicationablePredicate<FirmwareRule>(applicationType));
        List<FirmwareRule> firmwareRules = Lists.newArrayList(Iterables.filter(firmwareRuleDao.getAll(), Predicates.and(predicates)));
        Set<IpRuleBean> ipRuleBeans = new HashSet<>();
        for (FirmwareRule firmwareRule : firmwareRules) {
            try {
                ipRuleBeans.add(convertFirmwareRuleToIpRuleBean(firmwareRule));
            } catch (RuntimeException e) {
                log.error("Could not convert", e);
            }
        }

        return ipRuleBeans;
    }

    public FirmwareRule save(IpRuleBean bean, String applicationType) throws ValidationException {
        if (StringUtils.isBlank(bean.getId())) {
            bean.setId(UUID.randomUUID().toString());
        }
        FirmwareRule ipRule = convertIpRuleBeanToFirmwareRule(bean);
        if (StringUtils.isNotBlank(applicationType)) {
            ipRule.setApplicationType(applicationType);
        }
        firmwareRuleDao.setOne(ipRule.getId(), ipRule);
        return ipRule;
    }

    public void delete(String id) {
        firmwareRuleDao.deleteOne(id);
    }

    public IpRuleBean getOne(String id) {
        FirmwareRule fr = firmwareRuleDao.getOne(id);
        if (fr != null) {
            return convertFirmwareRuleToIpRuleBean(fr);
        }
        return null;
    }

    public FirmwareRule convertIpRuleBeanToFirmwareRule(IpRuleBean bean) {
        return ngRuleConverter.convertOld(ipRuleLegacyConverter.convertIpRuleBeanToFirmwareRule(bean));
    }

    public IpRuleBean convertFirmwareRuleToIpRuleBean(FirmwareRule firmwareRule) {
        IpRuleBean bean = ipRuleLegacyConverter.convertFirmwareRuleToIpRuleBean(ngRuleConverter.convertNew(firmwareRule));
        RuleAction action = (RuleAction) firmwareRule.getApplicableAction();
        if (StringUtils.isNotBlank(action.getConfigId())) {
            bean.setFirmwareConfig(firmwareConfigDAO.getOne(action.getConfigId()));
        }

        return bean;
    }

    public FirmwareConfig nullifyUnwantedFields(FirmwareConfig config) {
        if (config != null) {
            config.setUpdated(null);
            config.setFirmwareDownloadProtocol(null);
            config.setRebootImmediately(null);
        }

        return config;
    }

}
