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
 * Created: 11/22/13
 */
package com.comcast.hesperius.dataaccess.core;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

/**
 * Wrapper for exceptions thrown by an Astyanax client.
 *
 * Is recommended to use within {@link com.comcast.hesperius.dataaccess.core.dao.util.ExecuteWithUncheckedException}, thus all code
 * related to such wrapping will be located in one class.
 *
 * @see com.comcast.hesperius.dataaccess.core.dao.util.ExecuteWithUncheckedException
 */
public class AstyanaxException extends RuntimeException {

    public AstyanaxException(ConnectionException cause) {
        super(cause);
    }

    public AstyanaxException(String message, ConnectionException cause) {
        super(message, cause);
    }
}
