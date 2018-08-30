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
 * Author: pbura
 * Created: 08/01/2014  12:56
 */
package com.comcast.hesperius.dataaccess.core.bindery;

import com.comcast.hesperius.data.annotation.CF;
import com.comcast.hesperius.data.annotation.NonCached;
import com.comcast.hesperius.dataaccess.core.bindery.annotations.BindingEndpointFor;
import com.comcast.hesperius.dataaccess.core.bindery.annotations.InitBinder;
import com.comcast.hesperius.dataaccess.core.dao.DaoFactory;
import com.comcast.hesperius.dataaccess.core.dao.IADSSimpleDAO;
import com.comcast.hydra.astyanax.data.IPersistable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Facility to provide notion of bound entities to data service. It is useful when one needs
 * some operations performed on dependent entities when thing they depend on gets created or deleted
 * To achieve this {@link AbstractBoundEntityObserver}
 * needs to be extended and annotated with {@link com.comcast.hesperius.dataaccess.core.bindery.annotations.BindingEndpointFor}
 * for it to be discovered and be able to listen for events.
 */
public final class BindingFacility {
    private static final Multimap<Class<?>, AbstractBoundEntityObserver> observers = HashMultimap.create();
    private static final Logger log = LoggerFactory.getLogger(BindingFacility.class);

    private BindingFacility() {
    }

    /**
     * Initializes binding facility by instantiating binders and providing them with host DAO'a and bound endpointInfo
     * objects wich are just {@link com.comcast.hesperius.dataaccess.core.bindery.annotations.BindingEndpointFor} instances attached to those binders.
     *
     * @param binders list of classes extending {@link AbstractBoundEntityObserver} to be instantiated
     */
    public static void initBindery(final Iterable<Class<?>> binders) {
        log.info("Initializing binding facility");
        final List<AbstractBoundEntityObserver> everythingObservers = Lists.newArrayList();
        for (Class<?> clazz : binders) {
            try {
                final BindingEndpointFor bindingInfo = clazz.getAnnotation(BindingEndpointFor.class);
                final Class<?>[] bindingTargets = bindingInfo.providers();
                final AbstractBoundEntityObserver observer = (AbstractBoundEntityObserver) clazz.newInstance();

                if (!bindingInfo.from().equals(BindingEndpointFor.Nothing.class)) {
                    final CF cfDefinition = bindingInfo.from().getAnnotation(CF.class);
                    if (cfDefinition == null) {
                        throw new RuntimeException(clazz.getCanonicalName() + " has no CF annotation");
                    }


                    /**
                     * Avoiding stack overflow in case of self-binding thus notifying ourselves at most once
                     */
                    IADSSimpleDAO hostDAO = DaoFactory.Simple.createDAOBuilder()
                            .setKeyType(cfDefinition.keyType())
                            .setEntityType(bindingInfo.from())
                            .setAvoidSelfBinding(true)
                            .build();

                    if (!bindingInfo.from().isAnnotationPresent(NonCached.class)) {
                        hostDAO = DaoFactory.Simple.createCachedDAO(hostDAO);
                    }

                    observer.setHostDAO(hostDAO);
                }
                /**
                 * running {@link com.comcast.hesperius.dataaccess.core.bindery.annotations.InitBinder}  annotated methods in binder if any
                 */
                observer.setEndpointInfo(bindingInfo);
                for (final Method m : observer.getClass().getDeclaredMethods()) {
                    if (m.isAnnotationPresent(InitBinder.class)) {

                        log.info("Initializing " + observer.getClass().getSimpleName() + ", invoking " + m.getName());

                        m.setAccessible(true);
                        m.invoke(observer, (Object[]) null);
                        break;  // only first annotated method will be called
                    }
                }

                for (Class<?> bindingTarget : bindingTargets) {
                    if (bindingTarget.equals(BindingEndpointFor.Everything.class)) {
                        everythingObservers.add(observer);
                        continue;
                    }
                    registerObserverOnEntityTypeout(bindingTarget, observer);
                }
            } catch (InstantiationException ex) {
                log.error("Instantiation failure", ex);
            } catch (IllegalAccessException ex) {
                log.error("General protection failure", ex);
            } catch (InvocationTargetException e) {
                log.error("Failed to initialize binder");
            }
        }

        /**
         * registering everything observers (those having {@link com.comcast.hesperius.dataaccess.core.bindery.annotations.BindingEndpointFor.Everything} as binding provider)
         */
        for (final AbstractBoundEntityObserver observer : everythingObservers) {
            registerObserverOnEverything(observer);
        }
    }

    private static void registerObserverOnEverything(final AbstractBoundEntityObserver observer) {
        log.info("registering " + observer.getClass().getSimpleName() + " as binder for all observable types");
        for (final Class<?> type : observers.keySet()) {
            registerObserverOnEntityTypeout(type, observer);
        }
    }

    private static void registerObserverOnEntityTypeout(Class<?> type, AbstractBoundEntityObserver observer) {
        log.info("registering " + observer.getClass().getSimpleName() + " as binder for " + type.getSimpleName());
        observers.put(type, observer);
    }

    /**
     * External interface for clients to trigger entityCreated notification on all observers listening for it
     *
     * @param key
     * @param entity
     * @param excludeSelf trick to avoid stack overflow when self binding
     * @param <K>
     * @param <T>
     */
    public static <K, T extends IPersistable> void entityCreated(final K key, final T entity, final boolean excludeSelf) {
        for (AbstractBoundEntityObserver observer : observers.get(entity.getClass())) {
            try {
                if (!(observer.getEndpointInfo().from().equals(entity.getClass()) && excludeSelf)) {
                    observer.boundEntityCreated(key.getClass(), key, entity.getClass(), entity);
                }
            } catch (Exception ex) {
                //suppress exception and log it
                log.error(ex.getMessage(), ex);
            }
        }
    }

    /**
     * External interface for clients to trigger entityDeleted notification on all observers listening for it
     *
     * @param key
     * @param entity
     * @param excludeSelf trick to avoid stack overflow when self binding
     * @param <K>
     * @param <T>
     */
    public static <K, T extends IPersistable> void entityDeleted(final K key, final T entity, final boolean excludeSelf) {
        for (AbstractBoundEntityObserver observer : observers.get(entity.getClass())) {
            try {
                if (!(observer.getEndpointInfo().from().equals(entity.getClass()) && excludeSelf)) {
                    observer.boundEntityDeleted(key.getClass(), key, entity.getClass(), entity);
                }
            } catch (Exception ex) {
                //suppress exception and log it
                log.error(ex.getMessage(), ex);
            }
        }
    }
}
