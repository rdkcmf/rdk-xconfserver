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

import com.comcast.hydra.astyanax.data.IPersistable;
import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.model.Composite;

/**
 * Implementations of this interface help deal with Composite columns and appropriated object fields
 * @param <T>
 */
public interface ICompositePropertyProvider<T extends IPersistable> {

    /**
     * Returns index of id component of the column name.
     */
    int getIdComponentIndex();

    /**
     * IMPORTANT: type of id is the type it's stored in Cassandra as composite component.
     * May be confusing, because it's saved as String in {@link com.comcast.hydra.astyanax.data.Persistable} bean.
     *
     * Gets id-component of the column name. This id is unique for persistable object
     * @param composite composite column name
     * @return
     */
    Object getIdComponent(Composite composite) ;

    /**
     * Retrieves component of the composite column that stores field name of the persistable object
     * by default it's the last component
     * @param composite composite column name
     * @return a component with a field name of the column name
     */
    String getPropertyComponent(Composite composite);

    /**
     * Get the serializer for Id component
     * @return
     */
    Serializer getIdSerializer();

    /**
     * Builds composite object that represents <code>propertyName</code> of the <code>obj</code>
     * @param obj
     * @param propertyName
     * @return
     */
    Composite createCompositeForObject(T obj, String propertyName);

    /**
     * Gets all field names associated with <code>persistable</code>
     */
    String[] getColumnNames();

    /**
     * Gets the minimum number of columns expected to be saved to DS after mapping to column updater.
     * @return
     */
    int getColumnNumberTobeSaved();
}
