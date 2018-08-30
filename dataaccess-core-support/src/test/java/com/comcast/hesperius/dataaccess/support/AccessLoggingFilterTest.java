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
 * Author: obaturynskyi
 * Created: 27.05.2016  16:24
 */
package com.comcast.hesperius.dataaccess.support;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.Appender;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import java.util.*;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AccessLoggingFilterTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private AccessLoggingFilter filter;
    private StringBuffer stringBuffer;
    private String authorization = HttpHeaders.AUTHORIZATION;

    @Mock
    private Appender mockAppender;

    @Captor
    private ArgumentCaptor captorLoggingEvent;


    @Before
    public void setUp() {
        final Logger logger = (Logger) LoggerFactory.getLogger(AccessLoggingFilter.class);
        logger.addAppender(mockAppender);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filter = new AccessLoggingFilter();

        stringBuffer = new StringBuffer("someUrl");
        when(request.getRequestURL()).thenReturn(stringBuffer);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getHeader("User-Agent")).thenReturn("myUserAgent");
    }

    @Test
    public void testNullHeaderValueIsCheckedCorrectly() {
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singleton(authorization)));

        String headerValue = null;

        when(request.getHeader(authorization)).thenReturn(headerValue);

        filter.log(request, response);
    }

    @Test
    public void testNotNullHeaderValueIsCheckedCorrectly() {
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singleton(authorization)));

        String headerValue = "someValue";

        when(request.getHeader(authorization)).thenReturn(headerValue);

        filter.log(request, response);
    }

    @Test
    public void testEmptyHeaderValueIsCheckedCorrectly() {
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singleton(authorization)));

        String headerValue = "";

        when(request.getHeader(authorization)).thenReturn(headerValue);

        filter.log(request, response);
    }

}
