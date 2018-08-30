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
package com.comcast.hesperius.dataaccess.core.bindery.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to provide dao-level logical binding facility between data entities
 * @author PBura
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BindingEndpointFor {
    Class<?> from();
    Class<?>[] providers();

    /**
     * Special class to indicate that this {@link com.comcast.hesperius.dataaccess.core.bindery.AbstractBoundEntityObserver}
     * does not need a host DAO to be set. Please note that in this case {@code AbstractBoundEntityObserver#hostDao}
     * will be null, thus throwing {@link java.lang.NullPointerException} on usage.
     */
    public static final class Nothing{};

    /**
     * Special class to indicate that this {@link com.comcast.hesperius.dataaccess.core.bindery.AbstractBoundEntityObserver}
     * wants to observe all observable types (those marked with {@link com.comcast.hesperius.data.annotation.CF} annotation)
     */
    public static final class Everything{};
}
