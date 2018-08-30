package com.comcast.xconf.rfc;

import com.comcast.hesperius.data.annotation.CF;
import com.comcast.hydra.astyanax.data.IPersistable;
import com.comcast.xconf.Applicationable;
import com.comcast.xconf.firmware.ApplicationType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.collections.comparators.NullComparator;

import java.util.Date;
import java.util.List;

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
 * Author: Yury Stagit
 * Created: 04/11/16  12:00 PM
 */

@CF(cfName = "FeatureSet", keyType = String.class)
public class FeatureSet implements IPersistable, Comparable<FeatureSet>, Applicationable {

    private String id;

    private String name;

    private List<String> featureIdList;

    private String applicationType = ApplicationType.STB;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    @Override
    public Date getUpdated() {
        return null;
    }

    @JsonIgnore
    @Override
    public void setUpdated(Date date) {

    }

    @Override
    public int getTTL(String s) {
        return 0;
    }

    @JsonIgnore
    @Override
    public void setTTL(String s, int i) {

    }

    @JsonIgnore
    @Override
    public void clearTTL() {

    }

    @Override
    public int compareTo(FeatureSet o) {
        String id1 = (name != null) ? name.toLowerCase() : null;
        String id2 = (o != null && o.name != null) ? o.name.toLowerCase() : null;
        return new NullComparator().compare(id1, id2);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getFeatureIdList() {
        return featureIdList;
    }

    public void setFeatureIdList(List<String> featureIdList) {
        this.featureIdList = featureIdList;
    }
}
