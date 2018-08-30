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

/**
 * User: ikostrov
 * Date: 28.08.14
 * Time: 18:25
 */

import com.comcast.apps.hesperius.ruleengine.domain.additional.data.IpAddressGroup;
import com.comcast.apps.hesperius.ruleengine.main.api.FixedArg;
import com.comcast.apps.hesperius.ruleengine.main.impl.Condition;
import com.comcast.apps.hesperius.ruleengine.main.impl.Rule;
import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.bindery.AbstractBoundEntityObserver;
import com.comcast.hesperius.dataaccess.core.bindery.annotations.BindingEndpointFor;
import com.comcast.xconf.IpAddressGroupExtended;
import com.comcast.xconf.estbfirmware.FirmwareRule;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


//@BindingEndpointFor(
//        from = FirmwareRule.class,
//        providers = IpAddressGroupExtended.class
//)
public class FirmwareRuleBinder extends AbstractBoundEntityObserver<String, FirmwareRule> {

    @Override
    public void boundEntityDeleted(Class<?> entityClass, Object entity) {

    }

    @Override
    public void boundEntityCreated(Class<?> entityClass, Object entity) throws ValidationException {

        if (!(entity instanceof IpAddressGroupExtended)) {
            return;
        }
        IpAddressGroupExtended ipGroup = (IpAddressGroupExtended) entity;

        Iterator<FirmwareRule> iterator = hostDao.getIteratedAll();
        while (iterator.hasNext()) {
            FirmwareRule next = iterator.next();
            try {
                if (updateRule(ipGroup, next)) {
                    hostDao.setOne(next.getId(), next);
                }
            } catch (Exception e) {
                getLogger().error("Not able to update ipAddressGroup in rule. ID: " + next.getId());
            }
        }

    }

    private boolean updateRule(IpAddressGroupExtended ipGroup, FirmwareRule next) {
        switch (next.getType()) {
            case IP_FILTER:
            case DOWNLOAD_LOCATION_FILTER:
            case IP_RULE:
            case TIME_FILTER:
            case REBOOT_IMMEDIATELY_FILTER:

                return changeIpConditions(ipGroup, next);

            case ENV_MODEL_RULE:
            case MAC_RULE:
            default:
                // do nothing
                return false;
        }
    }

    private boolean changeIpConditions(IpAddressGroupExtended ipGroup, FirmwareRule next) {
        boolean changed = false;
        List<Rule> list = new ArrayList<Rule>();
        list.add(next);
        while (!list.isEmpty()) {
            Rule rule = list.remove(0);
            if (changeCondition(ipGroup, rule)) {
                changed = true;
            }
            if (rule.getCompoundParts() != null) {
                list.addAll(rule.getCompoundParts());
            }
        }
        return changed;
    }

    private boolean changeCondition(IpAddressGroupExtended ipGroup, Rule next) {
        Condition condition = next.getCondition();
        if (condition != null && FirmwareRule.IP.equals(condition.getFreeArg())) {
            FixedArg arg = condition.getFixedArg();
            IpAddressGroup group = (IpAddressGroup) arg.getValue();
            if (StringUtils.equals(group.getId(), ipGroup.getId())) {
                arg.setValue(ipGroup);
                return true;
            }
        }

        return false;
    }
}
