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
package com.comcast.hesperius.dataaccess.core.dao.provider;

import com.comcast.hydra.astyanax.data.IKeyGenerator;
import com.comcast.hydra.astyanax.data.IPersistable;
import com.comcast.hydra.astyanax.util.ReflectionUtils;
import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.model.Composite;
import com.netflix.astyanax.serializers.StringSerializer;

/**
 * @author: Alexander Pletnev
 */
public class DefaultCompositePropertyProvider<K, T extends IPersistable> implements ICompositePropertyProvider<T> {
    public static final Serializer<String> DEFAULT_ID_SERIALIZER = StringSerializer.get();
    public static final int DEFAULT_ID_COMPONENT_INDEX = 0;

    protected final Class<T> persistableClass;
    protected final IKeyGenerator keyGenerator;
    protected final Serializer idSerializer;
    protected final int idComponentIndex;

    public DefaultCompositePropertyProvider(Class<T> persistableClass, IKeyGenerator keyGenerator) {
        this(persistableClass, keyGenerator, DEFAULT_ID_SERIALIZER, DEFAULT_ID_COMPONENT_INDEX);
    }

    public DefaultCompositePropertyProvider(Class<T> persistableClass, IKeyGenerator keyGenerator, Serializer idSerializer) {
        this(persistableClass, keyGenerator, idSerializer, DEFAULT_ID_COMPONENT_INDEX);
    }

    public DefaultCompositePropertyProvider(Class<T> persistableClass, IKeyGenerator keyGenerator, int idComponentIndex) {
        this(persistableClass, keyGenerator, DEFAULT_ID_SERIALIZER, idComponentIndex);
    }

    public DefaultCompositePropertyProvider(Class<T> persistableClass, IKeyGenerator keyGenerator, Serializer idSerializer,
                                            int idComponentIndex) {
        this.persistableClass = persistableClass;
        this.keyGenerator = keyGenerator;
        this.idSerializer = idSerializer;
        this.idComponentIndex = idComponentIndex;
    }

    @Override
    public int getIdComponentIndex() {
        return idComponentIndex;
    }

    /**
     * IMPORTANT: type of id is the type it's stored in Cassandra as composite component.
     * May be confusing, because it's saved as String in {@link com.comcast.hydra.astyanax.data.Persistable} bean.
     *
     * @param composite The composite column name from which the ID should be extracted.
     * @return The extracted ID component.
     */
    @Override
    public Object getIdComponent(Composite composite) {
        return getComponent(composite, idComponentIndex, idSerializer);
    }

    /**
     * Extract the "property name" component from a Composite column.  In this data modeling appraoch, the property
     * name component will always be the last component of the composite.
     *
     * @param composite The composite column name from which the ID should be extracted.
     * @return The extracted property name component.
     */
    @Override
    public String getPropertyComponent(Composite composite) {
        return getComponent(composite, composite.size() - 1, StringSerializer.get());
    }

    @Override
    public Serializer getIdSerializer() {
        return idSerializer;
    }

    /**
     * Create a Composite column with the specified property name for the specified object.  Subclasses are encouraged
     * to override this function to customize which components to use in the Composite.  For example, a subclass
     * may choose to construct the composite like so: entity_id/type/property_name
     *
     * @param obj          The object being stored using the Composite column name.
     * @param propertyName The property name to use in the Composite column name.
     * @return The newly created Composite column name.
     */
    @Override
    public Composite createCompositeForObject(T obj, String propertyName) {
        //TODO: this code is used often during column list creation
        // move idToObject  for columns to upper level to make sure we are doing it only one time for an object
        return new Composite(getSafeObjectId(obj.getId()), propertyName);
    }

    @Override
    public String[] getColumnNames() {
        return ReflectionUtils.getColumnNames(persistableClass);
    }

    @Override
    public int getColumnNumberTobeSaved() {
        return 1;
    }

    /**
     * Why composite.get(index, serializer) is not used instead?
     * Because saved empty String (or empty array) + CompositeSerializer as column name serializer will lead to incorrect behaviour.
     *
     * See (better debug) {@link com.netflix.astyanax.model.Composite#get(int, com.netflix.astyanax.Serializer)}, {@link com.netflix.astyanax.model.Composite.Component#getValue(com.netflix.astyanax.Serializer)}.
     * Looks like an core bug, but it's not obvious.
     */
    protected <C> C getComponent(Composite composite, int index, Serializer<C> serializer) {
        return serializer.fromByteBuffer(composite.getComponent(index).getBytes());
    }

    protected Object getSafeObjectId(String id) {
        return (keyGenerator != null) ? keyGenerator.idToObject(id, persistableClass) : id;
    }
}
