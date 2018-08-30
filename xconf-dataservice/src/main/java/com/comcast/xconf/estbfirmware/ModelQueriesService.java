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
 * Created: 31.08.15 17:00
*/
package com.comcast.xconf.estbfirmware;

import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ModelQueriesService {

    @Autowired
    private ISimpleCachedDAO<String, FirmwareConfig> firmwareConfigDAO;

    @Autowired
    private IpRuleService ipRuleService;

    @Autowired
    private EnvModelRuleService envModelRuleService;

    @Autowired
    private RebootImmediatelyFilterService rebootImmediatelyFilterService;

    @Autowired
    private ISimpleCachedDAO<String, SingletonFilterValue> singletonFilterValueDAO;

    @Autowired
    private ISimpleCachedDAO<String, Model> modelDAO;

    public String checkUsage(String id) {
        for (FirmwareConfig firmware : firmwareConfigDAO.getAll()) {
            if (firmware.getSupportedModelIds() != null && firmware.getSupportedModelIds().contains(id)) {
                return "Firmware configuration : " + firmware.getDescription();
            }
        }
        for (IpRuleBean rule : ipRuleService.getAllIpRulesFromDB()) {
            if (id.equals(rule.getModelId())) {
                return "Ip rule : " + rule.getName();
            }
        }
        for (EnvModelRuleBean rule : envModelRuleService.getAll()) {
            if (id.equals(rule.getModelId())) {
                return "Environment/Model rule : " + rule.getName();
            }
        }
        for (RebootImmediatelyFilter filter : rebootImmediatelyFilterService.getAllRebootFiltersFromDB()) {
            if (filter.getModels() != null && filter.getModels().contains(id)) {
                return "Reboot immediately filter : " + filter.getName();
            }
        }

        DownloadLocationRoundRobinFilterValue filter = (DownloadLocationRoundRobinFilterValue) singletonFilterValueDAO.getOne(DownloadLocationRoundRobinFilterValue.SINGLETON_ID);
        if (filter != null) {
            for (Model model : filter.getRogueModels()) {
                if (id.equals(model.getId())) {
                    return "Firmware download location filter in Rogue model list";
                }
            }
        }
        return null;
    }

    public boolean isExistModel(String modelId) {
        if (StringUtils.isNotBlank(modelId)) {
            return modelDAO.getOne(modelId) != null;
        }
        return false;
    }
}
