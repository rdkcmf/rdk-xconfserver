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
 * Created: 11/3/2017
*/
package com.comcast.xconf.admin.controller.firmware;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.xconf.admin.controller.BaseControllerTest;
import com.comcast.xconf.estbfirmware.PercentageBean;
import com.comcast.xconf.firmware.ApplicationType;
import org.junit.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.comcast.xconf.firmware.ApplicationType.XHOME;

public class PercentageBeanControllerTest extends BaseControllerTest {

    @Test
    public void createBeanWithWrongApplicationType() throws Exception {
        PercentageBean percentageBean = createPercentageBean();
        String errorMsg = String.format("Current application type %s doesn't match with entity application type: %s", XHOME, percentageBean.getApplicationType());
        performPostRequest("percentfilter/percentageBean", xhomeCookie, percentageBean)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("message").value(errorMsg));
    }

    private PercentageBean createPercentageBean() throws ValidationException {
        PercentageBean percentageBean = new PercentageBean();
        percentageBean.setId("testId");
        percentageBean.setName("testName");
        percentageBean.setModel("testModel");
        percentageBean.setEnvironment("testEnvironment");
        percentageBean.setApplicationType(ApplicationType.STB);
        percentageBean.setActive(true);
        return percentageBean;
    }
}
