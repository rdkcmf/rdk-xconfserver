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
 * Author: rdolomansky
 * Created: 4/18/16  6:03 PM
 */
package com.comcast.hesperius.dataaccess.support;

import com.comcast.hesperius.dataaccess.core.cache.CacheManager;
import com.comcast.hesperius.dataaccess.core.util.CoreUtil;
import com.comcast.hesperius.dataaccess.support.astyanax.EmbCassandra;
import com.comcast.hesperius.dataaccess.support.cache.ChangedKeysServiceTest;
import com.comcast.hesperius.dataaccess.support.services.DataServiceInfoTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        ChangedKeysServiceTest.class,
        DataServiceInfoTest.class
})
public class CompleteTestSuite {

    private static EmbCassandra embCassandra;

    @BeforeClass
    public static void startEmbeddedCassandra() throws IOException {
        embCassandra = new EmbCassandra();
        embCassandra.start();
        CacheManager.initCaches(CoreUtil.CF_DEFINITIONS);
    }

    @AfterClass
    public static void stopEmbeddedCassandra() throws IOException {
        embCassandra.stop();
    }

}
