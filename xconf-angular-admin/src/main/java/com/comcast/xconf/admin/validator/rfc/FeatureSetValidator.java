package com.comcast.xconf.admin.validator.rfc;

import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.hesperius.dataaccess.core.exception.EntityConflictException;
import com.comcast.hesperius.dataaccess.core.exception.ValidationRuntimeException;
import com.comcast.xconf.rfc.Feature;
import com.comcast.xconf.rfc.FeatureSet;
import com.comcast.xconf.validators.IValidator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

@Component
public class FeatureSetValidator implements IValidator<FeatureSet> {

    @Autowired
    private ISimpleCachedDAO<String, Feature> featureDAO;

    @Override
    public void validate(FeatureSet entity) {
        String msg = validateFeature(entity);
        if (msg != null) {
            throw new ValidationRuntimeException(msg);
        }
    }

    private String validateFeature(FeatureSet entity) {
        if (StringUtils.isBlank(entity.getName())) {
            return "Feature Set name is blank";
        }

        if (CollectionUtils.isEmpty(entity.getFeatureIdList())) {
            return "Feature List is blank";
        }

        for (String featureId : entity.getFeatureIdList()) {
            Feature feature = featureDAO.getOne(featureId);
            if (feature == null) {
                return "Feature with id: " + featureId + " does not exist";
            }
        }

        return null;
    }

    @Override
    public void validateAll(FeatureSet entity, Iterable<FeatureSet> existingEntities) {
        for (FeatureSet featureSet : existingEntities) {
            if (!featureSet.getId().equals(entity.getId()) && StringUtils.equals(featureSet.getName(), entity.getName())) {
                throw new EntityConflictException("Feature Set with such name exists: " + entity.getName());
            }
        }
    }
}
