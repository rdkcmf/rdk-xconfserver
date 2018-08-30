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
 * Created: 11/04/16  12:00 PM
 */

package com.comcast.xconf.admin.controller.rfc.featureset;

import com.comcast.xconf.admin.controller.ExportFileNames;
import com.comcast.xconf.admin.service.rfc.FeatureSetService;
import com.comcast.xconf.rfc.FeatureSet;
import com.comcast.xconf.shared.controller.AbstractController;
import com.comcast.xconf.shared.service.AbstractService;
import com.comcast.xconf.shared.service.CrudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("rfc/featureset")
public class FeatureSetController extends AbstractController<FeatureSet> {

    @Autowired
    private FeatureSetService featureSetService;

    @Override
    public String getOneEntityExportName() {
        return ExportFileNames.FEATURE_SET.getName();
    }

    @Override
    public String getAllEntitiesExportName() {
        return ExportFileNames.ALL_FEATURE_SETS.getName();
    }

    @Override
    public AbstractService<FeatureSet> getService() {
        return featureSetService;
    }
}
