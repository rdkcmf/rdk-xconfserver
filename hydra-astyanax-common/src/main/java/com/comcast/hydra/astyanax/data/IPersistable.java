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
package com.comcast.hydra.astyanax.data;

import java.util.Date;

/**
 * Author: jmccann
 * Date: 10/10/11
 * Time: 11:58 AM
 */
public interface IPersistable {
	public String getId();
	public void setId(String id);

    /**
     * Gets timesatamp of the persistable object
     * @return
     */
    public Date getUpdated();

    public void setUpdated(Date timestamp);
	
	public int getTTL(String column);

    public void setTTL(String column, int value);

    public static interface Factory<T extends IPersistable> {
		T newObject();
        Class<T> getClassObject();
	}

    /**
     * Method should reset Time To Live data
     */
    public void clearTTL();
}
