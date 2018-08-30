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
 * Created: 3/1/16  12:28 PM
 */
package com.comcast.xconf.admin.filter;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
public class RestUpdateDelete403Filter implements Filter {
    public static final String DEV_PROFILE = "dev";

    private static final String REST_DELETE_URL = "/delete/";
    private static final String REST_UPDATES_URL = "/updates/";

    private Set<String> profiles;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        profiles = getSpringProfiles(filterConfig.getServletContext());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!profiles.contains(DEV_PROFILE)) {
            String uri = httpRequest.getRequestURI();
            if (uri.contains(REST_DELETE_URL) || uri.contains(REST_UPDATES_URL)) {
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }

    private Set<String> getSpringProfiles(ServletContext servletContext) {
        String springProfiles = System.getProperty("spring.profiles.active");
        if(springProfiles == null) {
            springProfiles = servletContext.getInitParameter("spring.profiles.default");
        }
        final Set<String> result = new HashSet<>();
        if (springProfiles != null) {
            for (final String profile : StringUtils.split(springProfiles, ",")) {
                result.add(profile.trim());
            }
        }
        return result;
    }
}
