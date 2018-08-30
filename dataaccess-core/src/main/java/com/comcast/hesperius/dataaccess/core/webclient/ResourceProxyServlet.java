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
package com.comcast.hesperius.dataaccess.core.webclient;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: andrey
 * Date: 4/18/12
 * Time: 6:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResourceProxyServlet extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String name = request.getRequestURI().substring(request.getContextPath().length());

        ClassLoader classLoader =
                Thread.currentThread().getContextClassLoader();

        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }

        InputStream resource = classLoader.getResourceAsStream(name);
        URL resURL = classLoader.getResource(name);
        if (resURL != null) {
            try {
                File f = new File(resURL.toURI());
                response.setDateHeader("Expires", f.lastModified());
            } catch (Exception e) {
                // Do not process
            }
        }
        OutputStream output = new BufferedOutputStream(response.getOutputStream());

        try {
            int length;
            byte[] bbuf = new byte[8192];
            while ((resource != null) && ((length = resource.read(bbuf)) != -1)) {
                output.write(bbuf, 0, length);
            }
            output.flush();
        } finally {
            output.close();
            if (resource != null) {
                resource.close();
            }
        }

    }
}
