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
 * Author: Yury Stagit
 * Created: 11/06/16  12:00 PM
 */
package com.comcast.xconf.admin.controller.rfc.featurerule;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.xconf.admin.controller.ExportFileNames;
import com.comcast.xconf.admin.service.rfc.FeatureRuleService;
import com.comcast.xconf.rfc.FeatureRule;
import com.comcast.xconf.shared.controller.ApplicationTypeAwayController;
import com.comcast.xconf.shared.service.AbstractApplicationTypeAwareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(FeatureRuleController.URL_MAPPING)
public class FeatureRuleController extends ApplicationTypeAwayController<FeatureRule> {
    public static final String URL_MAPPING = "rfc/featurerule";

    @Autowired
    private FeatureRuleService featureRuleService;

    @Override
    public String getOneEntityExportName() {
        return ExportFileNames.FEATURE_RULE.getName();
    }

    @Override
    public String getAllEntitiesExportName() {
        return ExportFileNames.ALL_FEATURE_RUlES.getName();
    }

    @Override
    public AbstractApplicationTypeAwareService<FeatureRule> getService() {
        return featureRuleService;
    }

    @RequestMapping(value="/{id}/priority/{newPriority}", method = RequestMethod.POST)
    public ResponseEntity changePriorities(@PathVariable final String id, @PathVariable final Integer newPriority) throws ValidationException {
        return new ResponseEntity<>(featureRuleService.changePriorities(id, newPriority), HttpStatus.OK);
    }


    @RequestMapping(value = "/size", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getFeatureRulesSize() {
        return Integer.toString(getService().getAll().size());
    }
}
