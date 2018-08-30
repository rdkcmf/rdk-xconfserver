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
 * Created: 01.09.15 16:44
*/
package com.comcast.xconf.estbfirmware;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.xconf.estbfirmware.converter.NgRuleConverter;
import com.comcast.xconf.estbfirmware.legacy.IpFilterLegacyConverter;
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
public class IpFilterService {

    @Autowired
    private NgRuleConverter ngRuleConverter;

    @Autowired
    private IpFilterLegacyConverter ipFilterLegacyConverter;

    @Autowired
    private ISimpleCachedDAO<String, FirmwareRule> firmwareRuleDao;

    @Autowired
    private PredicateManager predicateManager;

    public IpFilter getOneIpFilterFromDB(String id){
        IpFilter ipFilter = null;
        FirmwareRule fr = firmwareRuleDao.getOne(id);
        if(fr != null){
            ipFilter = convertFirmwareRuleToIpFilter(fr);
        }
        return ipFilter;
    }

    public IpFilter getIpFilterByName(String name, String applicationType) {
        for (IpFilter ipFilter : getByApplicationType(applicationType)) {
            if (StringUtils.equals(ipFilter.getName(), name)) {
                return ipFilter;
            }
        }
        return null;
    }

    public Set<IpFilter> getAllIpFiltersFromDB() {
        List<FirmwareRule> allRules = firmwareRuleDao.getAll();
        Set<IpFilter> ipFilters = new TreeSet<IpFilter>();

        for (FirmwareRule fwr : allRules) {
            if (TemplateNames.IP_FILTER.equals(fwr.getType())) {
                ipFilters.add(convertFirmwareRuleToIpFilter(fwr));
            }
        }
        return ipFilters;
    }

    public Set<IpFilter> getByApplicationType(String applicationType) {
        List<Predicate<FirmwareRule>> predicates = new ArrayList<>();
        predicates.add(predicateManager.new FirmwareRuleByTemplatePredicate(TemplateNames.IP_FILTER));
        predicates.add(predicateManager.new ApplicationablePredicate<FirmwareRule>(applicationType));
        List<FirmwareRule> allRules = Lists.newArrayList(Iterables.filter(firmwareRuleDao.getAll(), Predicates.and(predicates)));

        Set<IpFilter> ipFilters = new TreeSet<IpFilter>();
        for (FirmwareRule firmwareRule : allRules) {
            ipFilters.add(convertFirmwareRuleToIpFilter(firmwareRule));
        }
        return ipFilters;
    }

    public void save(IpFilter filter, String applicationType) throws ValidationException {
        if (StringUtils.isBlank(filter.getId())) {
            filter.setId(UUID.randomUUID().toString());
        }

        FirmwareRule rule = convertIpFilterToFirmwareRule(filter);
        if (StringUtils.isNotBlank(applicationType)) {
            rule.setApplicationType(applicationType);
        }
        firmwareRuleDao.setOne(rule.getId(), rule);
    }

    public void delete(String id) {
        firmwareRuleDao.deleteOne(id);
    }

    public IpFilter convertFirmwareRuleToIpFilter(FirmwareRule firmwareRule){

        return ipFilterLegacyConverter.convertFirmwareRuleToIpFilter(ngRuleConverter.convertNew(firmwareRule));
    }

    public FirmwareRule convertIpFilterToFirmwareRule(IpFilter ipFilter){

        return ngRuleConverter.convertOld(ipFilterLegacyConverter.convertIpFilterToFirmwareRule(ipFilter));
    }

}
