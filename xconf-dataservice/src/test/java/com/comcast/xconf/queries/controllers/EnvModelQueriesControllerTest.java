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
 * Author: ikostrov
 * Created: 31.08.15 17:52
*/
package com.comcast.xconf.queries.controllers;

import com.comcast.hesperius.dataaccess.core.util.CoreUtil;
import com.comcast.xconf.Environment;
import com.comcast.xconf.estbfirmware.Model;
import org.junit.Test;
import org.springframework.http.MediaType;

import java.util.Collections;

import static com.comcast.xconf.queries.QueriesHelper.nullifyUnwantedFields;
import static com.comcast.xconf.queries.QueryConstants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EnvModelQueriesControllerTest extends BaseQueriesControllerTest {

    @Test
    public void testGetModels() throws Exception {
        Model model = createDefaultModel();
        mockMvc.perform(post("/" + UPDATE_MODELS).contentType(MediaType.APPLICATION_JSON)
            .content(CoreUtil.toJSON(model)))
            .andExpect(status().isCreated());

        performRequestAndVerifyResponse(QUERIES_MODELS, Collections.singleton(nullifyUnwantedFields(model)));
        performRequestAndVerifyResponse(QUERIES_MODELS + "/{id}", model.getId(), nullifyUnwantedFields(model));


        mockMvc.perform(delete("/" + DELETE_MODELS + "/{id}", model.getId()))
                .andExpect(status().isNoContent());

        performRequestAndVerifyResponse(QUERIES_MODELS, Collections.emptyList());
    }

    @Test
    public void getModelByWrongId() throws Exception {
        String wrongModelId = "wrongId";

        mockMvc.perform(get(QUERIES_MODELS + "/" + wrongModelId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testEnvironments() throws Exception {
        Environment environment = new Environment();
        environment.setDescription("environmentDescription");
        environment.setId("environmentId");
        mockMvc.perform(post("/" + UPDATE_ENVIRONMENTS).contentType(MediaType.APPLICATION_JSON)
            .content(CoreUtil.toJSON(environment)))
            .andExpect(status().isCreated());

        performRequestAndVerifyResponse(QUERIES_ENVIRONMENTS, Collections.singleton(nullifyUnwantedFields(environment)));
        performRequestAndVerifyResponse(QUERIES_ENVIRONMENTS + "/{id}", environment.getId(), nullifyUnwantedFields(environment));

        mockMvc.perform(delete("/" + DELETE_ENVIRONMENTS + "/{id}", environment.getId()))
                .andExpect(status().isNoContent());

        performRequestAndVerifyResponse(QUERIES_ENVIRONMENTS, Collections.emptyList());
    }

    @Test
    public void getNotExistedEnvironment() throws Exception {
        Environment environment = createEnvironment("id111");
        environmentDAO.setOne(environment.getId(), environment);

        mockMvc.perform(get("/" + QUERIES_ENVIRONMENTS + "/{id}", "wrongId"))
                .andExpect(status().isBadRequest());
    }
}
