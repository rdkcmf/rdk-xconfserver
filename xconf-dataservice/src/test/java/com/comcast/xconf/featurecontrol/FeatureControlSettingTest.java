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
 *  ===========================================================================
 *
 *  Author: ystagit
 *  Created: 02/09/17 00:00 PM
 */
package com.comcast.xconf.featurecontrol;

import com.comcast.apps.hesperius.ruleengine.domain.standard.StandardOperation;
import com.comcast.apps.hesperius.ruleengine.main.impl.Condition;
import com.comcast.apps.hesperius.ruleengine.main.impl.Rule;
import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.util.CoreUtil;
import com.comcast.xconf.estbfirmware.factory.RuleFactory;
import com.comcast.xconf.queries.QueriesHelper;
import com.comcast.xconf.queries.controllers.BaseQueriesControllerTest;
import com.comcast.xconf.rfc.Feature;
import com.comcast.xconf.rfc.FeatureControl;
import com.comcast.xconf.rfc.FeatureResponse;
import com.comcast.xconf.rfc.FeatureRule;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.*;

import static com.comcast.xconf.featurecontrol.FeatureControlSettingsController.URL_MAPPING;
import static com.comcast.xconf.firmware.ApplicationType.STB;
import static com.comcast.xconf.firmware.ApplicationType.XHOME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class FeatureControlSettingTest extends BaseQueriesControllerTest {

    @Test
    public void testFeatureSetting() throws Exception {

        List<String> featureIds = new ArrayList<>();
        Set<FeatureResponse> features = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            Feature feature = createAndSaveFeature();
            featureIds.add(feature.getId());
            FeatureResponse featureResponse = new FeatureResponse(feature);
            features.add(QueriesHelper.nullifyUnwantedFields(featureResponse));
        }

        createAndSaveFeatureRule(featureIds, createRule(createCondition(RuleFactory.MODEL, StandardOperation.IS, "X1-1")), STB);

        performGetSettingsRequestAndVerifyFeatureControl("model=X1-1", features);
    }

    private void performGetSettingsRequestAndVerifyFeatureControl(String url, Set<FeatureResponse> features) throws Exception {
        FeatureControl featureControl = new FeatureControl();
        featureControl.setFeatures(features);

        mockMvc.perform(get("/" + URL_MAPPING  + "/getSettings?" + url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featureControl.features[0].applicationType").doesNotExist())
                .andExpect(content().json(CoreUtil.toJSON(Collections.singletonMap("featureControl", featureControl))));
    }

    @Test
    public void getFeatureSettingByApplicationType() throws Exception {
        Map<String, Feature> features = createAndSaveFeatures();
        createAndSaveFeatureRules(features);
        String url = "/" + FeatureControlSettingsController.URL_MAPPING + "/getSettings";
        FeatureControl stbResponse = new FeatureControl();
        stbResponse.setFeatures(toFeatureResponse(features.get(STB)));
        performGetWithApplication(url + "?model=X1-1", "", Collections.singletonMap("featureControl", stbResponse));

        FeatureControl xhomeResponse = new FeatureControl();
        xhomeResponse.setFeatures(toFeatureResponse(features.get(XHOME)));
        performGetWithApplication(url + "/xhome?model=X1-1", "", Collections.singletonMap("featureControl", xhomeResponse));
    }

    public Feature createAndSaveFeature() throws ValidationException {
        Feature feature = createFeature();
        featureDAO.setOne(feature.getId(), feature);
        return feature;
    }

    public Feature createFeature() {
        String id = UUID.randomUUID().toString();
        Feature feature = new Feature();
        feature.setId(id);
        feature.setName(id + "-name");
        feature.setEffectiveImmediate(false);
        feature.setEnable(false);
        Map<String, String> configData = new LinkedHashMap<>();
        configData.put(id + "-key", "id" + "-value");
        feature.setConfigData(configData);
        return feature;
    }

    public void createAndSaveFeatureRule(List<String> featureIds, Rule rule, String applicationType) throws Exception {
        FeatureRule featureRule = createFeatureRule(featureIds, rule, applicationType);
        featureRuleDAO.setOne(featureRule.getId(), featureRule);
    }

    public FeatureRule createFeatureRule(List<String> featureIds, Rule rule, String applicationType) {
        String id = UUID.randomUUID().toString();
        FeatureRule featureRule = new FeatureRule();
        featureRule.setId(id);
        featureRule.setName(id + "-name");
        featureRule.setApplicationType(applicationType);
        featureRule.setFeatureIds(featureIds);
        featureRule.setRule(rule);
        return featureRule;
    }

    public Rule createRule(Condition condition) {
        return Rule.Builder.of(condition).build();
    }

    private Map<String, FeatureRule> createAndSaveFeatureRules(Map<String, Feature> features) throws Exception {
        Map<String, FeatureRule> featureRules = new HashMap<>();
        FeatureRule stbFeatureRule = createFeatureRule(Collections.singletonList(features.get(STB).getId()), createRule(createCondition(RuleFactory.MODEL, StandardOperation.IS, "X1-1")), STB);
        featureRuleDAO.setOne(stbFeatureRule.getId(), stbFeatureRule);
        featureRules.put(STB, stbFeatureRule);

        FeatureRule xhomeFeatureRule = createFeatureRule(Collections.singletonList(features.get(XHOME).getId()), createRule(createCondition(RuleFactory.MODEL, StandardOperation.IS, "X1-1")), XHOME);
        featureRuleDAO.setOne(xhomeFeatureRule.getId(), xhomeFeatureRule);
        featureRules.put(XHOME, xhomeFeatureRule);
        return featureRules;
    }

    private Map<String, Feature> createAndSaveFeatures() throws ValidationException {
        Map<String, Feature> features = new HashMap<>();
        Feature stbFeature = createFeature();
        stbFeature.setApplicationType(STB);
        featureDAO.setOne(stbFeature.getId(), stbFeature);
        features.put(STB, stbFeature);

        Feature xhomeFeature = createFeature();
        xhomeFeature.setApplicationType(XHOME);
        featureDAO.setOne(xhomeFeature.getId(), xhomeFeature);
        features.put(XHOME, xhomeFeature);
        return features;
    }

    private Set<FeatureResponse> toFeatureResponse(Feature feature) {
        FeatureResponse featureResponse = new FeatureResponse(feature);
        return Sets.newHashSet(QueriesHelper.nullifyUnwantedFields(featureResponse));
    }
}
