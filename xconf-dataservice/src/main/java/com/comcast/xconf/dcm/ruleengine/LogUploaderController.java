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
package com.comcast.xconf.dcm.ruleengine;

import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.xconf.ApiVersionUtils;
import com.comcast.xconf.RequestUtil;
import com.comcast.xconf.estbfirmware.evaluation.EvaluationResult;
import com.comcast.xconf.firmware.ApplicationType;
import com.comcast.xconf.logupload.DCMGenericRule;
import com.comcast.xconf.logupload.LogUploaderContext;
import com.comcast.xconf.logupload.Settings;
import com.comcast.xconf.logupload.settings.SettingProfile;
import com.comcast.xconf.logupload.settings.SettingRule;
import com.comcast.xconf.logupload.telemetry.PermanentTelemetryProfile;
import com.comcast.xconf.logupload.telemetry.TelemetryProfile;
import com.comcast.xconf.logupload.telemetry.TelemetryRule;
import com.google.common.base.Joiner;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Controller
@RequestMapping(value = "/loguploader")
public class LogUploaderController {

    private static final Logger log = LoggerFactory.getLogger(LogUploaderController.class);

    @Autowired
    private LogUploadRuleBase ruleBase;
    @Autowired
    private TelemetryProfileService telemetryProfileService;
    @Autowired
    private SettingsProfileService settingProfileService;
    @Autowired
    private ISimpleCachedDAO<String, DCMGenericRule> dcmRuleDAO;

    @RequestMapping(value = "/getSettings", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getSettings(HttpServletRequest request,
                                      @RequestParam(value = "checkNow", required = false) Boolean checkNow,
                                      @RequestParam(value = "version", required = false) String apiVersion,
                                      @RequestParam(value = "settingType", required = false) Set<String> settingTypes,
                                      @RequestParam Map<String, String> params) {

        final LogUploaderContext context = new LogUploaderContext(params);
        context.setApplication(ApplicationType.STB);

        return evaluateSettings(request, checkNow, apiVersion, settingTypes, context);
    }

    @RequestMapping(value = "/getSettings/{applicationType}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity getSettingsByApplicationType(HttpServletRequest request,
                                           @PathVariable String applicationType,
                                           @RequestParam(value = "checkNow", required = false) Boolean checkNow,
                                           @RequestParam(value = "version", required = false) String apiVersion,
                                           @RequestParam(value = "settingType", required = false) Set<String> settingTypes,
                                           @RequestParam Map<String, String> params) {

        final LogUploaderContext context = new LogUploaderContext(params);
        context.setApplication(applicationType);

        return evaluateSettings(request, checkNow, apiVersion, settingTypes, context);
    }

    private ResponseEntity evaluateSettings(HttpServletRequest request, Boolean checkNow, String apiVersion, Set<String> settingTypes, LogUploaderContext context) {
        if (context.getEnv() != null) {
            context.setEnv(context.getEnv().toUpperCase());
        }

        String ipAddress = RequestUtil.findValidIpAddress(request, context.getEstbIP());
        context.setEstbIP(ipAddress);

        RequestUtil.normalizeContext(context);

        final Map<String, String> contextProps = context.getProperties();

        if (checkNow != null && checkNow) return getTelemetryProfile(contextProps);

        Settings result = ruleBase.eval(context);

        TelemetryRule telemetryRule = null;
        if (result != null) {
            telemetryRule = telemetryProfileService.getTelemetryRuleForContext(contextProps);

            PermanentTelemetryProfile permanentTelemetryProfile = telemetryProfileService.getPermanentProfileByTelemetryRule(telemetryRule);
            result.setTelemetryProfile(permanentTelemetryProfile);
            result.setUploadImmediately(context.getUploadImmediately());

            cleanupLusUploadRepository(result, apiVersion);
        }

        Set<SettingRule> settingRules = null;
        if (ApiVersionUtils.greaterOrEqual(apiVersion, 2.1f) && CollectionUtils.isNotEmpty(settingTypes)) {
            Set<SettingProfile> settingProfiles = new HashSet<>();
            settingRules = new HashSet<>();

            for (String settingType : settingTypes) {
                SettingRule settingRule = settingProfileService.getSettingRuleByTypeForContext(settingType, contextProps);
                SettingProfile settingProfile = settingProfileService.getSettingProfileBySettingRule(settingRule);

                if (settingProfile != null) {
                    settingProfiles.add(settingProfile);
                    settingRules.add(settingRule);
                }
            }

            if (result == null && CollectionUtils.isNotEmpty(settingProfiles)) {
                result = new Settings();
            }
            if (result != null) {
                result.setSettingProfiles(settingProfiles);
            }
        }

        if (result != null) {
            logResultSettings(result, telemetryRule, settingRules);

            return new ResponseEntity<>(result, HttpStatus.OK);
        }

        if (log.isDebugEnabled()) {
            log.debug("returning 404: settings not found");
        }
        return new ResponseEntity<>("<h2>404 NOT FOUND</h2><div>settings not found</div>", HttpStatus.NOT_FOUND);
    }

    private static void cleanupLusUploadRepository(Settings settings, String apiVersion) {
        if (ApiVersionUtils.greaterOrEqual(apiVersion, 2)) {
            settings.setLusUploadRepositoryURL(null);
        } else {
            settings.setLusUploadRepositoryUploadProtocol(null);
            settings.setLusUploadRepositoryURLNew(null);
        }
    }

    private ResponseEntity getTelemetryProfile (final Map<String,String> context) {
        TelemetryProfile profile = telemetryProfileService.getTelemetryForContext(context);
        if (profile != null) {
            return new ResponseEntity<>(profile, HttpStatus.OK);
        } else {
            return new ResponseEntity<>("<h2>404 NOT FOUND</h2><div> telemetry profile not found</div>", HttpStatus.NOT_FOUND);
        }
    }

    private void logResultSettings(Settings settings, TelemetryRule telemetryRule, Set<SettingRule> settingRules) {
        List<String> ruleNames = new ArrayList<>();
        for(String ruleId: settings.getRuleIDs()) {
            DCMGenericRule dcmRule = dcmRuleDAO.getOne(ruleId);
            if (dcmRule != null && StringUtils.isNotBlank(dcmRule.getName())) {
                ruleNames.add(dcmRule.getName());
            }
        }

        List<String> settingRuleNames = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(settingRules)) {
            for (SettingRule settingRule : settingRules) {
                settingRuleNames.add(settingRule.getName());
            }
        }

        log.info("AppliedRules: formulaNames={}, telemetryRuleName={}, settingRuleName={}",
                CollectionUtils.isNotEmpty(ruleNames) ? Joiner.on(",").join(ruleNames) : EvaluationResult.DefaultValue.NOMATCH,
                telemetryRule != null ? telemetryRule.getName() : EvaluationResult.DefaultValue.NOMATCH,
                CollectionUtils.isNotEmpty(settingRuleNames) ? Joiner.on(",").join(settingRuleNames) : EvaluationResult.DefaultValue.NOMATCH);
    }
}
