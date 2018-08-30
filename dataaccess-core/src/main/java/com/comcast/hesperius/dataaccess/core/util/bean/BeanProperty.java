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
 * Author: slavrenyuk
 * Created: 5/15/14
 */
package com.comcast.hesperius.dataaccess.core.util.bean;

import com.netflix.astyanax.Serializer;

/**
 * @param <T> bean type
 * @param <V> property type
 */
public interface BeanProperty<T, V> {

    String getName();

    Class<V> getType();

    V invokeGet(T obj);

    void invokeSet(T obj, V value);

    Serializer<V> getSerializer();
}
