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
 * Created: 12/14/16  12:00 PM
 */
package com.comcast.xconf.admin.controller.rfc;

import com.comcast.hesperius.dataaccess.core.util.CoreUtil;
import com.comcast.xconf.admin.controller.AbstractControllerTest;
import com.comcast.xconf.admin.controller.ExportFileNames;
import com.comcast.xconf.admin.controller.rfc.feature.FeatureController;
import com.comcast.xconf.admin.utils.TestDataBuilder;
import com.comcast.xconf.rfc.Feature;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

public class FeatureControllerTest extends AbstractControllerTest<Feature> {

    @Override
    public String getUrlMapping() {
        return FeatureController.URL_MAPPING;
    }

    @Override
    public Feature createEntity() throws Exception {
        return TestDataBuilder.createFeature();
    }

    @Override
    public Feature updateEntity(Feature feature) throws Exception {
        return TestDataBuilder.modifyFeature(feature, "new-" + feature.getId());
    }

    @Override
    public void assertEntity(ResultActions resultActions, Object feature) throws Exception {
        resultActions.andExpect(content().json(CoreUtil.toJSON(feature)));
    }

    @Override
    public String getOneEntityExportName() {
        return ExportFileNames.FEATURE.getName();
    }

    @Override
    public String getAllEntitiesExportName() {
        return ExportFileNames.ALL_FEATURES.getName();
    }

}
