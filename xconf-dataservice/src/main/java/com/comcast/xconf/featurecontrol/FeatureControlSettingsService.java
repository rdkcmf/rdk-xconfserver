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
package com.comcast.xconf.featurecontrol;

import com.comcast.xconf.RequestUtil;
import com.comcast.xconf.logupload.LogUploaderContext;
import com.comcast.xconf.rfc.FeatureControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;

@Service
public class FeatureControlSettingsService {

    protected static final Logger log = LoggerFactory.getLogger(FeatureControlSettingsService.class);

    @Autowired
    private FeatureControlRuleBase featureControlRuleBase;

    protected ResponseEntity getFeatureSettings(HttpServletRequest request, Map<String, String> context, String applicationType) {
        String ipAddress = RequestUtil.findValidIpAddress(request, context.get(LogUploaderContext.ESTB_IP));
        context.put(LogUploaderContext.ESTB_IP, ipAddress);

        context = featureControlRuleBase.normalizeContext(context);

        normalizeContext(context);

        FeatureControl featureControl = featureControlRuleBase.eval(context, applicationType);
        if (featureControl == null) {
            return new ResponseEntity<>("NO MATCH", HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(Collections.singletonMap("featureControl", featureControl), HttpStatus.OK);
    }

    protected void normalizeContext(Map<String, String> context) {
        // Do nothing.
    }

}
