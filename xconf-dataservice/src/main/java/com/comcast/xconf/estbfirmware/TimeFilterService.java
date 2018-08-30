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
 * Created: 01.09.15 16:33
*/
package com.comcast.xconf.estbfirmware;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.xconf.estbfirmware.converter.TimeFilterConverter;
import com.comcast.xconf.firmware.ApplicationType;
import com.comcast.xconf.firmware.FirmwareRule;
import com.comcast.xconf.search.PredicateManager;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TimeFilterService {

    @Autowired
    private TimeFilterConverter timeFilterConverter;

    @Autowired
    private ISimpleCachedDAO<String, FirmwareRule> firmwareRuleDao;

    @Autowired
    private PredicateManager predicateManager;

    public void save(TimeFilter filter, String applicationType) throws ValidationException {
        if (StringUtils.isBlank(filter.getId())) {
            String id = UUID.randomUUID().toString();
            filter.setId(id);
        }
        FirmwareRule rule = timeFilterConverter.convert(filter);
        if (StringUtils.isNotBlank(applicationType)) {
            rule.setApplicationType(applicationType);
        }
        firmwareRuleDao.setOne(rule.getId(), rule);
    }

    public void delete(String id) {
        firmwareRuleDao.deleteOne(id);
    }

    public TimeFilter getOneTimeFilterFromDB(String id) {
        TimeFilter timeFilter = null;
        FirmwareRule fr = firmwareRuleDao.getOne(id);
        if (fr != null) {
            timeFilter = convertToFilter(fr);
        }
        return timeFilter;
    }

    public TimeFilter getByName(String name, String applicationType) {
        for (FirmwareRule firmwareRule : firmwareRuleDao.getAll()) {
            if (TemplateNames.TIME_FILTER.equals(firmwareRule.getType())
                    && firmwareRule.getName().equalsIgnoreCase(name)
                    && ApplicationType.equals(applicationType, firmwareRule.getApplicationType())) {
                return convertToFilter(firmwareRule);
            }
        }
        return null;
    }

    public Set<TimeFilter> getByApplicationType(String applicationType) {
        List<Predicate<FirmwareRule>> predicates = new ArrayList<>();
        predicates.add(predicateManager.new FirmwareRuleByTemplatePredicate(TemplateNames.TIME_FILTER));
        predicates.add(predicateManager.new ApplicationablePredicate<FirmwareRule>(applicationType));
        List<FirmwareRule> allRules = Lists.newArrayList(Iterables.filter(firmwareRuleDao.getAll(), Predicates.and(predicates)));
        Set<TimeFilter> timeFilters = new TreeSet<>();
        for (FirmwareRule fwr : allRules) {
            if (TemplateNames.TIME_FILTER.equals(fwr.getType())) {
                timeFilters.add(convertToFilter(fwr));
            }
        }
        return timeFilters;
    }

    private TimeFilter convertToFilter(FirmwareRule rule) {
        return timeFilterConverter.convert(rule);
    }

}
