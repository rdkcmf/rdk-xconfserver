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
 * Created: 17.08.2015
*/
package com.comcast.xconf.bindery;

import com.comcast.apps.hesperius.ruleengine.domain.additional.data.IpAddress;
import com.comcast.hesperius.dataaccess.core.dao.IADSSimpleDAO;
import com.comcast.xconf.GenericNamespacedList;
import com.comcast.xconf.IpAddressGroupExtended;
import com.comcast.xconf.converter.GenericNamespacedListsConverter;
import com.comcast.xconf.estbfirmware.PercentFilterValue;
import com.comcast.xconf.estbfirmware.SingletonFilterValue;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;

import static org.mockito.Mockito.*;

public class PercentFilterBinderTest {
    @Test
    public void testBoundEntityCreated_Changed() throws Exception {
        PercentFilterBinder binder = new PercentFilterBinder();
        PercentFilterValue value = new PercentFilterValue();
        IpAddressGroupExtended addressGroup = getWhitelist();
        value.setWhitelist(addressGroup);
        IADSSimpleDAO<String, SingletonFilterValue> hostDao = getHostDao(value);
        binder.setHostDAO(hostDao);
        binder.boundEntityCreated(GenericNamespacedList.class, GenericNamespacedListsConverter.convertFromIpAddressGroupExtended(addressGroup));
        verify(hostDao).setOne(anyString(), any(SingletonFilterValue.class));
    }

    @Test
    public void testBoundEntityCreated_NotChanged() throws Exception {
        PercentFilterBinder binder = new PercentFilterBinder();
        PercentFilterValue value = new PercentFilterValue();
        value.setEnvModelPercentages(null);
        IADSSimpleDAO<String, SingletonFilterValue> hostDao = getHostDao(value);
        binder.setHostDAO(hostDao);
        binder.boundEntityCreated(IpAddressGroupExtended.class, new IpAddressGroupExtended());
        verify(hostDao, never()).setOne(anyString(), any(SingletonFilterValue.class));
    }

    private IpAddressGroupExtended getWhitelist() {
        IpAddressGroupExtended group = new IpAddressGroupExtended();
        group.setId("groupID");
        group.setName("groupID");
        group.setIpAddresses(new HashSet<>(Collections.singleton(new IpAddress("1.1.1.1"))));
        return group;
    }

    private IADSSimpleDAO<String, SingletonFilterValue> getHostDao(PercentFilterValue value) {
        IADSSimpleDAO<String, SingletonFilterValue> dao = mock(IADSSimpleDAO.class);
        when(dao.getOne(PercentFilterValue.SINGLETON_ID)).thenReturn(value);
        return dao;
    }
}
