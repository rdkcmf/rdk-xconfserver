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
package com.comcast.hesperius.dataaccess.core.bindery;

import com.comcast.hesperius.dataaccess.core.bindery.annotations.BindingEndpointFor;
import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.dao.IADSSimpleDAO;
import com.comcast.hydra.astyanax.data.IPersistable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core concept of entity interoperability management class that when extended allows tracking for
 * changes that take place in set of entities of particular type
 * in order to get all bonuses from inter-entity bindings one must annotate
 * receiver class (that implementing BoundEntityObserver) with BindingEndpointFor annotation in which
 * tracked types are stated.
 */
public abstract class AbstractBoundEntityObserver<K, T extends IPersistable> {

    private BindingEndpointFor endpointInfo;
    protected IADSSimpleDAO<K, T> hostDao;

    private static final Logger logger = LoggerFactory.getLogger(AbstractBoundEntityObserver.class);

    protected final Logger getLogger() {
        return logger;
    }

    public void setHostDAO(IADSSimpleDAO<K, T> host) {
        this.hostDao = host;
    }

    public void setEndpointInfo(BindingEndpointFor endpointInfo) {
        this.endpointInfo = endpointInfo;
    }

    public abstract void boundEntityCreated(Class<?> entityClass, final Object entity) throws ValidationException;

    public void boundEntityCreated(Class<?> keyClass, final Object key, Class<?> entityClass, final Object entity) throws ValidationException {
        boundEntityCreated(entityClass, entity);
    }

    public abstract void boundEntityDeleted(Class<?> entityClass, final Object entity);

    public void boundEntityDeleted(Class<?> keyClass, final Object key, Class<?> entityClass, final Object entity) {
        boundEntityDeleted(entityClass, entity);
    }

    public final BindingEndpointFor getEndpointInfo() {
        return endpointInfo;
    }
}
