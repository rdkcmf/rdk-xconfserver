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
package com.comcast.hesperius.data.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ListingCF {
    /**
     * column family name
     */
    public String cfName();

    /**
     * row key type
     */
    public Class<?> keyType() default String.class;

    /**
     * field of persistable bean. value of that field will be used as column name, preserving type
     */
    public String columnNameField() default "id";

    public String comparatorTypeAlias() default "";

    /**
     * ttl for columns this definition produces in seconds, 0 - columns do not expire
     */
    public int ttl() default 0;

    public byte bounds() default 5;

    public boolean compress() default false;
}
