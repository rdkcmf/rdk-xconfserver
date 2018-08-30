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
 *  Author: mdolina
 *  Created: 4:15 PM
 */
package com.comcast.xconf.admin.controller.migration;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.dao.DaoFactory;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.hesperius.dataaccess.core.util.CoreUtil;
import com.comcast.xconf.RuleHelper;
import com.comcast.xconf.admin.service.rfc.FeatureRuleService;
import com.comcast.xconf.admin.service.rfc.FeatureService;
import com.comcast.xconf.admin.service.rfc.FeatureSetService;
import com.comcast.xconf.rfc.*;
import com.comcast.xconf.utils.annotation.Migration;
import com.google.common.base.Optional;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("migration")
@Migration
public class MigrationUIController {

    private static final Logger log = LoggerFactory.getLogger(MigrationUIController.class);

    @Autowired
    private FeatureService featureService;

    @Autowired
    private FeatureSetService featureSetService;

    @Autowired
    private FeatureRuleService featureRuleService;

    @RequestMapping(method = RequestMethod.GET, value = "/feature")
    @Migration(oldKey = String.class, newKey = String.class, oldEntity = FeatureLegacy.class, newEntity = Feature.class, migrationURL = "/feature")
    public ResponseEntity migrate() throws ValidationException {
        ISimpleCachedDAO<String, FeatureLegacy> featureLegacyDao = DaoFactory.Simple.createCachedDAO(String.class, FeatureLegacy.class);
        int successfullySaved = 0;
        int notSaved = 0;
        for (FeatureLegacy featureLegacy : Optional.presentInstances(featureLegacyDao.asLoadingCache().asMap().values())) {
            Feature feature = new Feature();
            feature.setId(featureLegacy.getId());
            feature.setName(featureLegacy.getName());
            feature.setFeatureName(featureLegacy.getName());
            feature.setEnable(featureLegacy.isEnable());
            feature.setEffectiveImmediate(featureLegacy.isEffectiveImmediate());
            feature.setConfigData(featureLegacy.getConfigData());
            try {
                featureService.create(feature);
                successfullySaved++;
            } catch (Exception e) {
                log.error("Exception: ", e);
                log.info(CoreUtil.toJSON(feature));
                notSaved++;
            }
        }

        return new ResponseEntity<>("Successfully migrated features: " + successfullySaved + ",\n not saved: " + notSaved, HttpStatus.OK);
    }

    @RequestMapping(value = "/fromFeatureSet", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    @Migration(oldKey = String.class, newKey = String.class, oldEntity = FeatureRuleLegacy.class, newEntity = FeatureRule.class, migrationURL = "/fromFeatureSet")
    public ResponseEntity migrateFromFeatureSet() {
        ISimpleCachedDAO<String, FeatureRuleLegacy> featureRuleLegacyDao = DaoFactory.Simple.createCachedDAO(String.class, FeatureRuleLegacy.class);
        int migrated = 0;
        int notMigrated = 0;
        List<FeatureRule> featureRulesToSave = new ArrayList<>();
        for (FeatureRuleLegacy featureRuleLegacy : Optional.presentInstances(featureRuleLegacyDao.asLoadingCache().asMap().values())) {
            FeatureRule featureRule = new FeatureRule();
            featureRule.setName(featureRuleLegacy.getName());
            featureRule.setRule(featureRuleLegacy.getRule());
            if (StringUtils.isNotBlank(featureRuleLegacy.getBoundFeatureSetId())) {
                FeatureSet featureSet = featureSetService.getOne(featureRuleLegacy.getBoundFeatureSetId());
                if (featureSet != null && CollectionUtils.isNotEmpty(featureSet.getFeatureIdList())) {
                    featureRule.setFeatureIds(featureSet.getFeatureIdList());
                }
            }
            featureRulesToSave.add(featureRule);
        }
        reorganizeFeatureRulesByOperationPriority(featureRulesToSave);
        for (FeatureRule featureRule : featureRulesToSave) {
            try {
                featureRuleService.create(featureRule);
                migrated++;
            } catch (ValidationException e) {
                log.error("Exception ", e);
                log.error("Not migrated rule: " + featureRule.toString());
                notMigrated++;
            }
        }

        return new ResponseEntity<>("Successfully migrated " + migrated + " rules , not migrated: " + notMigrated + " rules", HttpStatus.OK);
    }

    private void reorganizeFeatureRulesByOperationPriority(List<FeatureRule> featureRules) {
        Collections.sort(featureRules, Collections.reverseOrder(new Comparator<FeatureRule>() {
            @Override
            public int compare(FeatureRule o1, FeatureRule o2) {
                return RuleHelper.compareRules(o1.getRule(), o2.getRule());
            }
        }));

        for (int i = 0; i < featureRules.size(); i++) {
            FeatureRule featureRule = featureRules.get(i);
            featureRule.setPriority(i + 1);
        }
    }
}
