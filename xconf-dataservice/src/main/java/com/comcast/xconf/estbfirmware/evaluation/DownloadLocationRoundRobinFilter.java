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
 * Author: slavrenyuk
 * Created: 6/13/14
 */
package com.comcast.xconf.estbfirmware.evaluation;

import com.comcast.xconf.estbfirmware.*;
import org.apache.commons.lang.StringUtils;

import java.util.Random;

public class DownloadLocationRoundRobinFilter {

    private static final Random random = new Random();

    /**
     * @return true if filter is applied, false otherwise
     */
    public static boolean filter(FirmwareConfigFacade firmwareConfig, DownloadLocationRoundRobinFilterValue filterValue, EstbFirmwareContext.Converted context) {
        String model = context.getModel();
        Model modelObject = new Model(model, null);
        String firmwareVersion = context.getFirmwareVersion();
        boolean supportsFullHttpUrl = context.getCapabilities().contains(Capabilities.supportsFullHttpUrl);
        if (!filterValue.isNeverUseHttp() && context.getCapabilities().contains(Capabilities.RCDL)
                && !StringUtils.isBlank(model) && !filterValue.getRogueModels().contains(modelObject)
                && !StringUtils.isBlank(filterValue.getHttpLocation())
                && !StringUtils.isBlank(filterValue.getHttpFullUrlLocation())) {

            boolean thisVersionOk = true;
            String firmwareVersions = filterValue.getFirmwareVersions();

            if (StringUtils.isNotBlank(firmwareVersions)) {
                thisVersionOk = (firmwareVersion != null && containsVersion(firmwareVersions, firmwareVersion));
            }

            if (thisVersionOk) {
                firmwareConfig.setFirmwareDownloadProtocol(FirmwareConfig.DownloadProtocol.http);
                if(supportsFullHttpUrl) {
                    firmwareConfig.setFirmwareLocation(filterValue.getHttpFullUrlLocation());
                } else {
                    firmwareConfig.setFirmwareLocation(filterValue.getHttpLocation());
                }
                return true;
            }
        }

        firmwareConfig.setFirmwareDownloadProtocol(FirmwareConfig.DownloadProtocol.tftp);
        boolean isIPv4LocationApplied = setupIPv4Location(firmwareConfig, filterValue);
        boolean isIPv6LocationApplied = setupIPv6Location(firmwareConfig, filterValue);

        return isIPv4LocationApplied || isIPv6LocationApplied;
    }

    public static boolean setupIPv6Location(FirmwareConfigFacade firmwareConfig, DownloadLocationRoundRobinFilterValue filterValue) {
        double rand = random.nextDouble();
        double limit = 0.0;
        boolean isApplied = false;
        if (filterValue.getIpv6locations() != null) {
            for (DownloadLocationRoundRobinFilterValue.Location location : filterValue.getIpv6locations()) {
                limit += location.getPercentage() / 100.00;
                if (rand < limit) {
                    firmwareConfig.setIpv6FirmwareLocation(location.getLocationIp().toString());
                    isApplied = true;
                    break;
                }
            }
        }
        return isApplied;
    }

    public static boolean setupIPv4Location(FirmwareConfigFacade firmwareConfig, DownloadLocationRoundRobinFilterValue filterValue) {
        double rand = random.nextDouble();
        double limit = 0.0;
        boolean isApplied = false;
        for (DownloadLocationRoundRobinFilterValue.Location location : filterValue.getLocations()) {
            limit += location.getPercentage() / 100.00;
            if (rand < limit) {
                firmwareConfig.setFirmwareLocation(location.getLocationIp().toString());
                isApplied = true;
                break;
            }
        }
        return isApplied;
    }

    public static boolean containsVersion(String firmwareVersions, String contextVersion) {
        String[] split = firmwareVersions.split("\\s+");
        for (String s : split) {
            if (contextVersion.equals(s)) {
                return true;
            }
        }
        return false;
    }

}
