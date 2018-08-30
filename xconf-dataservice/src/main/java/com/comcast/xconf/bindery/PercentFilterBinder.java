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
package com.comcast.xconf.bindery;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.bindery.AbstractBoundEntityObserver;
import com.comcast.hesperius.dataaccess.core.bindery.annotations.BindingEndpointFor;
import com.comcast.xconf.GenericNamespacedList;
import com.comcast.xconf.GenericNamespacedListTypes;
import com.comcast.xconf.IpAddressGroupExtended;
import com.comcast.xconf.converter.GenericNamespacedListsConverter;
import com.comcast.xconf.estbfirmware.EnvModelPercentage;
import com.comcast.xconf.estbfirmware.PercentFilterValue;
import com.comcast.xconf.estbfirmware.SingletonFilterValue;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * User: ikostrov
 * Date: 08.09.14
 * Time: 17:45
 */
@BindingEndpointFor(
        from = SingletonFilterValue.class,
        providers = GenericNamespacedList.class
)
public class PercentFilterBinder extends AbstractBoundEntityObserver<String, SingletonFilterValue> {

    @Override
    public void boundEntityDeleted(Class<?> entityClass, Object entity) {
        // do nothing
    }

    @Override
    public void boundEntityCreated(Class<?> entityClass, Object entity) throws ValidationException {
        if (!(entity instanceof GenericNamespacedList)) {
            return;
        }
        GenericNamespacedList genericNamespacedList = (GenericNamespacedList) entity;

        if (!GenericNamespacedListTypes.IP_LIST.equals(genericNamespacedList.getTypeName())) {
            return;
        }

        IpAddressGroupExtended ipGroup = GenericNamespacedListsConverter.convertToIpAddressGroupExtended(genericNamespacedList);

        PercentFilterValue filter = (PercentFilterValue) hostDao.getOne(PercentFilterValue.SINGLETON_ID);
        if (filter == null) {
            return;
        }

        boolean changed = false;
        if (filter.getWhitelist() != null &&
                StringUtils.equals(ipGroup.getName(), filter.getWhitelist().getId())) {

            filter.setWhitelist(ipGroup);
            changed = true;
        }

        Map<String, EnvModelPercentage> percentages = filter.getEnvModelPercentages();
        if (percentages != null) {
            for (EnvModelPercentage percentage : percentages.values()) {
                if (percentage.getWhitelist() != null &&
                        StringUtils.equals(ipGroup.getId(), percentage.getWhitelist().getId())) {

                    percentage.setWhitelist(ipGroup);
                    changed = true;
                }
            }
        }

        if (changed) {
            hostDao.setOne(PercentFilterValue.SINGLETON_ID, filter);
        }
    }
}
