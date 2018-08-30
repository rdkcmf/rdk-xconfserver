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
 * Created: 13.08.15 19:52
*/
package com.comcast.xconf.estbfirmware;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.xconf.estbfirmware.converter.DownloadLocationFilterConverter;
import com.comcast.xconf.firmware.ApplicationType;
import com.comcast.xconf.firmware.FirmwareRule;
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
public class DownloadLocationFilterService {

    private static final Logger log = LoggerFactory.getLogger(DownloadLocationFilterService.class);

    @Autowired
    private ISimpleCachedDAO<String, FirmwareRule> firmwareRuleDao;

    @Autowired
    private DownloadLocationFilterConverter converter;

    @Autowired
    private PredicateManager predicateManager;

    public Set<DownloadLocationFilter> getByApplicationType(String applicationType) {
        List<Predicate<FirmwareRule>> predicates = new ArrayList<>();
        predicates.add(predicateManager.new FirmwareRuleByTemplatePredicate(TemplateNames.DOWNLOAD_LOCATION_FILTER));
        predicates.add(predicateManager.new ApplicationablePredicate<FirmwareRule>(applicationType));
        List<FirmwareRule> firmwareRules = Lists.newArrayList(Iterables.filter(firmwareRuleDao.getAll(), Predicates.and(predicates)));
        Set<DownloadLocationFilter> locationFilters = new TreeSet<>();
        for (FirmwareRule firmwareRule : firmwareRules) {
            try {
                locationFilters.add(convertFirmwareRuleToDownloadLocationFilter(firmwareRule));
            } catch (RuntimeException e) {
                log.error("Could not convert", e);
            }
        }
        return locationFilters;
    }

    public FirmwareRule convertDownloadLocationFilterToFirmwareRule(DownloadLocationFilter bean) {
        return converter.convert(bean);
    }

    public DownloadLocationFilter convertFirmwareRuleToDownloadLocationFilter(FirmwareRule firmwareRule) {
        return converter.convert(firmwareRule);
    }

    public DownloadLocationFilter getOneDwnLocationFilterFromDBById(String id) {
        FirmwareRule fr = firmwareRuleDao.getOne(id);
        if (fr != null) {
            return convertFirmwareRuleToDownloadLocationFilter(fr);
        }
        return null;
    }

    public DownloadLocationFilter getOneDwnLocationFilterFromDBByName(String name, String applicationType) {
        for (DownloadLocationFilter locationFilter : getByApplicationType(applicationType)) {
            if (locationFilter.getName().equalsIgnoreCase(name)) {
                return locationFilter;
            }
        }

        return null;
    }

    public void save(DownloadLocationFilter filter, String applicationType) throws ValidationException {
        if(StringUtils.isBlank(filter.getId())){
            filter.setId(UUID.randomUUID().toString());
        }
        FirmwareRule rule = convertDownloadLocationFilterToFirmwareRule(filter);
        rule.setApplicationType(ApplicationType.get(applicationType));
        firmwareRuleDao.setOne(rule.getId(), rule);
    }

    public void delete(String id) {
        firmwareRuleDao.deleteOne(id);
    }
}
