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
 * Created: 03.11.15  16:40
 */
package com.comcast.xconf.bindery;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.bindery.AbstractBoundEntityObserver;
import com.comcast.hesperius.dataaccess.core.bindery.annotations.BindingEndpointFor;
import com.comcast.xconf.GenericNamespacedList;
import com.comcast.xconf.GenericNamespacedListTypes;
import com.comcast.xconf.NamespacedList;
import com.comcast.xconf.converter.GenericNamespacedListsConverter;

//@BindingEndpointFor(
//        from = NamespacedList.class,
//        providers = GenericNamespacedList.class
//)
public class NamespacedListBinder extends AbstractBoundEntityObserver<String, NamespacedList> {

    @Override
    public void boundEntityDeleted(Class<?> entityClass, Object entity) {
        if (!(entity instanceof  GenericNamespacedList)) {
            return;
        }

        final GenericNamespacedList deletedList = (GenericNamespacedList) entity;
        if (GenericNamespacedListTypes.MAC_LIST.equals(deletedList.getTypeName())) {
            try {
                hostDao.deleteOne(deletedList.getId());
            } catch (Exception e) {
                getLogger().error("Not able to delete namespacedList. ID: " + deletedList.getId());
            }
        }
    }

    @Override
    public void boundEntityCreated(Class<?> entityClass, Object entity) throws ValidationException {
        if (!(entity instanceof GenericNamespacedList)) {
            return;
        }

        final GenericNamespacedList createdList = (GenericNamespacedList) entity;
        if (GenericNamespacedListTypes.MAC_LIST.equals(createdList.getTypeName())) {
            try {
                final NamespacedList listToCreate = GenericNamespacedListsConverter.convertToNamespacedList(createdList);
                hostDao.setOne(listToCreate.getId(), listToCreate);
            } catch (Exception e) {
                getLogger().error("Not able to create namespacedList. ID: " + createdList.getId());
            }
        }
    }
}
