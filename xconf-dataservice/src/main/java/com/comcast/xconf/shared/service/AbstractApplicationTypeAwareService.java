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
 * Created: 10/3/2017
*/
package com.comcast.xconf.shared.service;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.exception.EntityExistsException;
import com.comcast.hesperius.dataaccess.core.exception.EntityNotFoundException;
import com.comcast.hydra.astyanax.data.IPersistable;
import com.comcast.xconf.Applicationable;
import com.comcast.xconf.firmware.ApplicationType;
import com.comcast.xconf.permissions.PermissionService;
import com.comcast.xconf.search.PredicateManager;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public abstract class AbstractApplicationTypeAwareService<T extends IPersistable & Comparable & Applicationable> extends AbstractService<T> {

    @Autowired
    protected PredicateManager predicateManager;

    @Override
    public List<T> getAll() {
        List<T> all = getEntityDAO().getAll();
        String readApplication = getPermissionService().getReadApplication();
        all = Lists.newArrayList(Iterables.filter(all, predicateManager.new ApplicationablePredicate<T>(readApplication)));
        Collections.sort(all);
        return all;
    }

    @Override
    public T delete(String id) throws ValidationException {
        T entity = getEntityDAO().getOne(id);
        String writeApplication = getPermissionService().getWriteApplication();
        if (entity == null || !ApplicationType.equals(writeApplication, entity.getApplicationType())) {
            throw new EntityNotFoundException("Entity with id: " + id + " does not exist");
        }
        validateUsage(id);
        getEntityDAO().deleteOne(id);

        return entity;
    }

    @Override
    protected void beforeCreating(T entity) {
        String id = entity.getId();
        String writeApplication = getPermissionService().getWriteApplication();
        if (StringUtils.isBlank(id)) {
            entity.setId(UUID.randomUUID().toString());
        } else if (getEntityDAO().getOne(id, false) != null && ApplicationType.equals(getEntityDAO().getOne(id, false).getApplicationType(), writeApplication)) {
            throw new EntityExistsException("Entity with id: " + id + " already exists");
        }
    }

    @Override
    protected void beforeUpdating(T entity) throws ValidationException {
        String id = entity.getId();
        if (StringUtils.isBlank(id)) {
            throw new ValidationException("Entity id is empty");
        }
        T existingEntity = getEntityDAO().getOne(id, false);
        String writeApplication = getPermissionService().getWriteApplication();
        if (existingEntity == null || !ApplicationType.equals(existingEntity.getApplicationType(), writeApplication)) {
            throw new EntityNotFoundException("Entity with id: " + id + " does not exist");
        }
    }

    @Override
    protected void beforeSaving(T entity) {
        if (entity != null && StringUtils.isBlank(entity.getApplicationType())) {
            entity.setApplicationType(getPermissionService().getWriteApplication());
        }
        super.beforeSaving(entity);
    }

    public String getApplicationTypeSuffix() {
        return "_" + getPermissionService().getReadApplication();
    }

    protected abstract PermissionService getPermissionService();

}
