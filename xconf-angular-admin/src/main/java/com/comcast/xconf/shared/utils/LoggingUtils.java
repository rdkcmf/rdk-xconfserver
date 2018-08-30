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
package com.comcast.xconf.shared.utils;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;

public class LoggingUtils {

    public static void log(Logger logger, final Level level, String s, final Object... objects) {
        final StringBuilder sb = new StringBuilder();
        sb.append("[").append(getUsername()).append("] ").append(s);

        final String levelName = level.getName();
        if (levelName.equals(Level.INFO.getName())) {
            logger.info(sb.toString(), objects);
        } else if (levelName.equals(Level.WARNING.getName())) {
            logger.warn(sb.toString(), objects);
        } else if (levelName.equals(Level.SEVERE.getName())) {
            logger.error(sb.toString(), objects);
        }
    }

    private static String getUsername() {
    	/* user's permission configured to retrieve from auth.properties instead of Authentication service
    	 */
    	String resourceName = "auth.properties";
    	ClassLoader loader = Thread.currentThread().getContextClassLoader();
    	Properties props = new Properties();
    	try(InputStream resourceStream = loader.getResourceAsStream(resourceName)){
    		props.load(resourceStream);
    	} catch(IOException e) {
    		e.printStackTrace();
    	}
    	String userName = props.getProperty("userName");
    	return userName;
    }

}
