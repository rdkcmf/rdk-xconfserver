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
 */
package com.comcast.xconf.admin.controller.telemetry;

import com.comcast.xconf.admin.controller.ExportFileNames;
import com.comcast.xconf.admin.core.Utils;
import com.comcast.xconf.admin.service.telemetry.TelemetryRuleService;
import com.comcast.xconf.logupload.telemetry.TelemetryRule;
import com.comcast.xconf.shared.controller.AbstractController;
import com.comcast.xconf.shared.controller.ApplicationTypeAwayController;
import com.comcast.xconf.shared.service.AbstractApplicationTypeAwareService;
import com.comcast.xconf.shared.service.AbstractService;
import com.comcast.xconf.shared.service.CrudService;
import com.comcast.xconf.shared.utils.PageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/telemetry/rule")
public class TargetingRuleController extends ApplicationTypeAwayController<TelemetryRule> {
    private static final Logger log = LoggerFactory.getLogger(TargetingRuleController.class);

    @Autowired
    private TelemetryRuleService telemetryRuleService;

    @Override
    public String getOneEntityExportName() {
        return ExportFileNames.TELEMETRY_RULE.getName();
    }

    @Override
    public String getAllEntitiesExportName() {
        return ExportFileNames.ALL_TELEMETRY_RULES.getName();
    }

    @Override
    public AbstractApplicationTypeAwareService<TelemetryRule> getService() {
        return telemetryRuleService;
    }
}
