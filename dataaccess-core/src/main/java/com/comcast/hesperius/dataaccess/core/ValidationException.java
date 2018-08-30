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
package com.comcast.hesperius.dataaccess.core;

/**
 * Author: jmccann
 * Date: 2/11/12
 * Time: 3:45 PM
 */
public class ValidationException extends Exception{
    private Type type;

    public ValidationException(String message) {
        super(message);
        type = Type.General;
    }
    public ValidationException(String message, Type type) {
        super(message);
        if (type == null) {
            throw new IllegalArgumentException("error type is null");
        }
        this.type = type;
    }

    public ValidationException() {
        type = Type.General;
    }

    public Type getType() {
        return type;
    }

    public static enum Type {
        General,
        VersionConflict,
        // thrown when the service failed to process object's mutations
        InvalidMutation
    }

}
