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
 * Created: 12/9/2016
*/
package com.comcast.xconf.validators;

import com.comcast.apps.hesperius.ruleengine.main.api.RuleValidationException;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.hesperius.dataaccess.core.exception.EntityConflictException;
import com.comcast.hesperius.dataaccess.core.exception.EntityNotFoundException;
import com.comcast.hesperius.dataaccess.core.exception.ValidationRuntimeException;
import com.comcast.xconf.estbfirmware.FirmwareConfig;
import com.comcast.xconf.estbfirmware.PercentageBean;
import com.comcast.xconf.firmware.ApplicationType;
import com.comcast.xconf.firmware.RuleAction;
import com.comcast.xconf.permissions.FirmwarePermissionService;
import com.comcast.xconf.permissions.PermissionHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class PercentageBeanValidator implements IValidator<PercentageBean> {

    @Autowired
    private ISimpleCachedDAO<String, FirmwareConfig> firmwareConfigDAO;

    @Autowired
    private FirmwarePermissionService permissionService;

    @Override
    public void validate(PercentageBean percentage) {
        if (StringUtils.isBlank(percentage.getName())) {
            throw new ValidationRuntimeException("Name could not be blank");
        }
        if (StringUtils.isBlank(percentage.getEnvironment())) {
            throw new ValidationRuntimeException("Environment could not be blank");
        }
        if (StringUtils.isBlank(percentage.getModel())) {
            throw new ValidationRuntimeException("Model could not be blank");
        }
        validateApplicationType(percentage);
        Set<String> firmwareVersions = percentage.getFirmwareVersions();
        if (percentage.isFirmwareCheckRequired() && CollectionUtils.isEmpty(firmwareVersions)) {
            throw new ValidationRuntimeException("Please select at least one version or disable firmware check");
        }
        float totalPercentage = 0;
        for (RuleAction.ConfigEntry entry : percentage.getDistributions()) {
            if (String.valueOf(entry.getPercentage()).contains("-")) {
                throw new ValidationRuntimeException("Percent filter contains negative value");
            }
            if (entry.getPercentage() < 0 || entry.getPercentage() > 100) {
                throw new ValidationRuntimeException("Percentage should be within [0, 100]");
            }
            FirmwareConfig config = firmwareConfigDAO.getOne(entry.getConfigId(), false);
            if (config == null) {
                throw new EntityNotFoundException("FirmwareConfig with id " + entry.getConfigId() + " does not exist");
            }
            if (!firmwareVersions.contains(config.getFirmwareVersion()) && percentage.isFirmwareCheckRequired()) {
                throw new ValidationRuntimeException("Distribution version should be selected in MinCheck list");
            }
            validateFirmwareConfigApplicationType(config.getApplicationType(), percentage.getApplicationType());
            totalPercentage += entry.getPercentage();
        }
        if (totalPercentage > 100) {
            throw new ValidationRuntimeException("Distribution total percentage > 100");
        }

        checkDistributionDuplicates(percentage.getDistributions());


        String lastKnownGoodConfigId = percentage.getLastKnownGood();
        if (StringUtils.isNotBlank(lastKnownGoodConfigId)) {
            FirmwareConfig lkgConfig = firmwareConfigDAO.getOne(lastKnownGoodConfigId, false);
            if (lkgConfig == null) {
                throw new EntityNotFoundException("LastKnownGood: config with id " + lastKnownGoodConfigId + " does not exist");
            }
            validateFirmwareConfigApplicationType(lkgConfig.getApplicationType(), percentage.getApplicationType());
            if (!percentage.getFirmwareVersions().contains(lkgConfig.getFirmwareVersion())) {
                throw new ValidationRuntimeException("LastKnownGood should be selected in min check list");
            }
            if (Math.abs(totalPercentage - 100.0) < 1.0e-8) {
                throw new ValidationRuntimeException("Can't set LastKnownGood when percentage=100");
            }
        }
        if (percentage.isActive() && CollectionUtils.isNotEmpty(percentage.getDistributions()) && totalPercentage < 100 && StringUtils.isBlank(lastKnownGoodConfigId)) {
            throw new ValidationRuntimeException("LastKnownGood is required when percentage < 100");
        }
        String intermediateVersionConfigId = percentage.getIntermediateVersion();
        if (StringUtils.isNotBlank(intermediateVersionConfigId)) {
            FirmwareConfig intermediateConfig = firmwareConfigDAO.getOne(intermediateVersionConfigId, false);
            if (intermediateConfig == null) {
                throw new EntityNotFoundException("IntermediateVersion: config with id " + intermediateVersionConfigId + " does not exist");
            }
            validateFirmwareConfigApplicationType(intermediateConfig.getApplicationType(), percentage.getApplicationType());
            if (!percentage.isFirmwareCheckRequired()) {
                throw new ValidationRuntimeException("Can't set IntermediateVersion when firmware check is disabled");
            }
        }
    }

    @Override
    public void validateAll(PercentageBean entity, Iterable<PercentageBean> existingEntities) {
        String entityEnvironment = entity.getEnvironment();
        String entityModel = entity.getModel();
        for (PercentageBean old : existingEntities) {
            if (entity.getId().equals(old.getId())) {
                continue;
            }
            if (StringUtils.equalsIgnoreCase(entity.getName(), old.getName())) {
                throw new EntityConflictException("This name " + entity.getName() + " is already used");
            }
            if (StringUtils.equalsIgnoreCase(entityEnvironment, old.getEnvironment()) &&
                    StringUtils.equalsIgnoreCase(entityModel, old.getModel())) {
                throw new EntityConflictException("PercentageBean already exists with such env/model pair: "
                        + entityEnvironment + "/" + entityModel);
            }
        }
    }

    private void checkDistributionDuplicates(List<RuleAction.ConfigEntry> distributions) {
        Set<String> firmwareConfigIds = new HashSet<>();
        for (RuleAction.ConfigEntry configEntry : distributions) {
            firmwareConfigIds.add(configEntry.getConfigId());
        }
        if (firmwareConfigIds.size() != distributions.size()) {
            throw new ValidationRuntimeException("Distributions contain duplicates");
        }
    }

    public void validateApplicationType(PercentageBean percentageBean) {
        PermissionHelper.validateWrite(permissionService, percentageBean.getApplicationType());
    }

    private void validateFirmwareConfigApplicationType(String configApplicationType, String beanApplicationType) {
        if (!ApplicationType.equals(configApplicationType, beanApplicationType)) {
            throw new ValidationRuntimeException("ApplicationTypes of FirmwareConfig and PercentageBean do not match");
        }
    }
}
