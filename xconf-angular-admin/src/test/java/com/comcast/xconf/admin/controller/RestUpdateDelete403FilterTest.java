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
 * Created: 3/1/16  12:50 PM
 */
package com.comcast.xconf.admin.controller;

import com.comcast.xconf.BaseIntegrationTest;
import com.comcast.xconf.admin.filter.RestUpdateDelete403Filter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.Filter;
import java.lang.reflect.Field;
import java.util.Collections;

import static com.comcast.xconf.admin.filter.RestUpdateDelete403Filter.DEV_PROFILE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RestUpdateDelete403FilterTest extends BaseIntegrationTest {
    public static final String PROD_PROFILE = "prod";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;
    private Filter restUpdateDelete403Filter = new RestUpdateDelete403Filter();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .addFilter(restUpdateDelete403Filter, "/dataService/*").build();
    }

    @Test
    public void testUpdatesPathUnderDev() throws Exception {
        setProfile(DEV_PROFILE);
        mockMvc.perform(
                get("/dataService/updates/123/")
        )
                .andExpect(status().is(200));
    }

    @Test
    public void testUpdatesPathUnderProd() throws Exception {
        setProfile(PROD_PROFILE);
        mockMvc.perform(
                get("/dataService/updates/123/")
        )
                .andExpect(status().is(403));
    }

    @Test
    public void testDeletePathUnderDev() throws Exception {
        setProfile(DEV_PROFILE);
        mockMvc.perform(
                get("/dataService/delete/123/")
        )
                .andExpect(status().is(200));
    }


    @Test
    public void testDeletePathUnderProd() throws Exception {
        setProfile(PROD_PROFILE);
        mockMvc.perform(
                get("/dataService/delete/123/")
        )
                .andExpect(status().is(403));
    }

    @Test
    public void testQueriesPathUnderProd() throws Exception {
        setProfile(PROD_PROFILE);
        mockMvc.perform(
                get("/dataService/queries/123/")
        )
                .andExpect(status().is(200));
    }

    private void setProfile(final String value) throws NoSuchFieldException, IllegalAccessException {
        Field field = ReflectionUtils.findField(RestUpdateDelete403Filter.class, "profiles");
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, restUpdateDelete403Filter, Collections.singleton(value));
    }
}
