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
 * Created: 6/10/15  10:46 AM
 */
package com.comcast.hesperius.dataaccess.core.config;

import com.comcast.hesperius.dataaccess.core.acl.AccessControlManager;
import com.comcast.hesperius.dataaccess.core.bindery.BindingFacility;
import com.comcast.hesperius.dataaccess.core.bindery.annotations.BindingEndpointFor;
import com.comcast.hesperius.dataaccess.core.cache.CacheManager;
import com.comcast.hesperius.dataaccess.core.util.AnnotationScanner;
import com.comcast.hesperius.dataaccess.core.util.CoreUtil;
import com.comcast.hesperius.dataaccess.core.util.EntityValidationUtils;

public class CoreInitializer {

    public static void init() {}

    static {
        CoreUtil.init();
        final Iterable<Class<?>> binders = AnnotationScanner.getAnnotatedClasses(new Class[]{BindingEndpointFor.class}, CoreUtil.dsconfig.getBindersBasePackage());
        CacheManager.initCaches(CoreUtil.CF_DEFINITIONS);
        AccessControlManager.init();
        BindingFacility.initBindery(binders);
        EntityValidationUtils.init();
    }
}
