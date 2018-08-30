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
 * Author: Stanislav Menshykov
 * Created: 5/22/15  3:07 PM
 */
package com.comcast.xconf.admin.controller;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(MainController.URL_MAPPING)
public class MainController {
    public static final String URL_MAPPING = "/";

    private String INDEX_PAGE = "xconfindex";

    @Autowired
    private Environment environment;


    @RequestMapping
    public String showPage(final ModelMap model) {
        final String profile = environment.getDefaultProfiles()[0];
        model.put("profile", profile);
        model.put("utcTime", getUtcTime());

        return INDEX_PAGE;
    }

    private String getUtcTime() {
        DateTime time = new DateTime(DateTimeZone.UTC);
        return time.toString("YYY/M/dd HH:mm");
    }

}
