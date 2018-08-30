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
 * <p>
 * Author: mdolina
 * Created: 6/23/17  12:31 PM
 */

package com.comcast.xconf.permissions;

public class Permissions {

    public static final String READ_COMMON_XHOME = "read-common-xhome";
    public static final String READ_COMMON_STB = "read-common-stb";
    public static final String READ_COMMON_ALL = "read-common-*";

    public static final String WRITE_COMMON_XHOME = "write-common-xhome";
    public static final String WRITE_COMMON_STB = "write-common-stb";
    public static final String WRITE_COMMON_ALL = "write-common-*";

    public static final String READ_DCM_XHOME = "read-dcm-xhome";
    public static final String READ_DCM_STB = "read-dcm-stb";
    public static final String READ_DCM_ALL = "read-dcm-*";

    public static final String WRITE_DCM_XHOME = "write-dcm-xhome";
    public static final String WRITE_DCM_STB = "write-dcm-stb";
    public static final String WRITE_DCM_ALL = "write-dcm-*";

    public static final String READ_FIRMWARE_XHOME = "read-firmware-xhome";
    public static final String READ_FIRMWARE_STB = "read-firmware-stb";
    public static final String READ_FIRMWARE_ALL = "read-firmware-*";

    public static final String WRITE_FIRMWARE_XHOME = "write-firmware-xhome";
    public static final String WRITE_FIRMWARE_STB = "write-firmware-stb";
    public static final String WRITE_FIRMWARE_ALL = "write-firmware-*";

    public static final String READ_TELEMETRY_XHOME = "read-telemetry-xhome";
    public static final String READ_TELEMETRY_STB = "read-telemetry-stb";
    public static final String READ_TELEMETRY_ALL = "read-telemetry-*";

    public static final String WRITE_TELEMETRY_XHOME = "write-telemetry-xhome";
    public static final String WRITE_TELEMETRY_STB = "write-telemetry-stb";
    public static final String WRITE_TELEMETRY_ALL = "write-telemetry-*";

    public static final String READ_FIRMWARE_RULE_TEMPLATE_XHOME = "read-firmware-rule-template-xhome";
    public static final String READ_FIRMWARE_RULE_TEMPLATE_STB = "read-firmware-rule-template-stb";
    public static final String READ_FIRMWARE_RULE_TEMPLATE_ALL = "read-firmware-rule-template-*";

    public static final String WRITE_FIRMWARE_RULE_TEMPLATE_XHOME = "write-firmware-rule-template-xhome";
    public static final String WRITE_FIRMWARE_RULE_TEMPLATE_STB = "write-firmware-rule-template-stb";
    public static final String WRITE_FIRMWARE_RULE_TEMPLATE_ALL = "write-firmware-rule-template-*";
}