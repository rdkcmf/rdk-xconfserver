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
 * Created: 31.08.15 21:34
*/
package com.comcast.xconf.estbfirmware;

import com.comcast.apps.hesperius.ruleengine.main.impl.Rule;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.xconf.Environment;
import com.comcast.xconf.dcm.converter.FormulaRuleBuilder;
import com.comcast.xconf.logupload.DCMGenericRule;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class EnvironmentQueriesService {

    @Autowired
    private ISimpleCachedDAO<String, Environment> environmentDAO;

    @Autowired
    private IpRuleService ipRuleService;

    @Autowired
    private EnvModelRuleService envModelRuleService;

    @Autowired
    private RebootImmediatelyFilterService rebootImmediatelyFilterService;

    @Autowired
    private ISimpleCachedDAO<String, DCMGenericRule> dcmRuleDAO;

    public String checkUsage(String id) {

        List<String> formulasWithEnvironment = new ArrayList<>();
        for (DCMGenericRule dcmRule : dcmRuleDAO.getAll()) {
            List<Rule> rules = new ArrayList<Rule>();
            rules.add(dcmRule);
            while (!rules.isEmpty()) {
                Rule rule = rules.remove(0);
                if (rule.getCondition() != null
                        && rule.getCondition().getFreeArg() != null
                        && FormulaRuleBuilder.PROP_ENV.equals(rule.getCondition().getFreeArg().getName())) {
                    Object fixedArg = rule.getCondition().getFixedArg().getValue();
                    if (fixedArg instanceof Collection) {
                        for (Object fixedArgItem : (Collection) fixedArg) {
                            if (fixedArgItem.equals(id)) {
                                formulasWithEnvironment.add(dcmRule.getName());
                            }
                        }
                    } else if (fixedArg.equals(id)) {
                        formulasWithEnvironment.add(dcmRule.getName());
                    }
                }
                if (rule.getCompoundParts() != null) {
                    rules.addAll(rule.getCompoundParts());
                }
            }
        }

        if (CollectionUtils.isNotEmpty(formulasWithEnvironment)) {
            return "Log upload management formulas: " + StringUtils.join(formulasWithEnvironment, ", ");
        }

        for (IpRuleBean rule : ipRuleService.getAllIpRulesFromDB()) {
            if (id.equals(rule.getEnvironmentId())) {
                return "Ip rule: " + rule.getName();
            }
        }
        for (EnvModelRuleBean rule : envModelRuleService.getAll()) {
            if (id.equals(rule.getEnvironmentId())) {
                return "Environment/Model rule: " + rule.getName();
            }
        }
        for (RebootImmediatelyFilter filter : rebootImmediatelyFilterService.getAllRebootFiltersFromDB()) {
            if (filter.getEnvironments() != null && filter.getEnvironments().contains(id)) {
                return "Reboot immediately filter: " + filter.getName();
            }
        }
        return null;
    }

    public boolean isExistEnvironment(String envId) {
        if (StringUtils.isNotBlank(envId)) {
            return environmentDAO.getOne(envId) != null;
        }
        return false;
    }

}
