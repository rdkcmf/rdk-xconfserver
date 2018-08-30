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
 * Created: 11/26/13
 */
package com.comcast.hesperius.dataaccess.core.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * If data service throws an exception and specific ExceptionMapper for that exception was not provided, this class will be used.
 */
@Provider
public class DataServiceExceptionMapper implements ExceptionMapper<Throwable> {
    private static Logger log = LoggerFactory.getLogger(DataServiceExceptionMapper.class);

    @Override
    public Response toResponse(Throwable throwable) {
        log.error(throwable.getMessage(), throwable); // if ExceptionMapper intercepts an exception, Jersey will not log it
        return Response.serverError()
                .entity("Internal Server Error caused by " + throwable)
                .type(MediaType.TEXT_PLAIN)
                .build();
    }
}
