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
package com.comcast.xconf.firmware;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class DefinePropertiesAction extends ApplicableAction {

    @JsonProperty
    private Map<String, String> properties;

    private List<String> byPassFilters;

    public DefinePropertiesAction() {
        super(Type.DEFINE_PROPERTIES);
    }

    public DefinePropertiesAction(Map<String, String> properties) {
        super(Type.DEFINE_PROPERTIES);
        this.properties = properties;
    }

    public DefinePropertiesAction(Map<String, String> properties, List<String> byPassFilters) {
        super(Type.DEFINE_PROPERTIES);
        this.properties = properties;
        this.byPassFilters = byPassFilters;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public List<String> getByPassFilters() {
        return byPassFilters;
    }

    public void setByPassFilters(List<String> byPassFilters) {
        this.byPassFilters = byPassFilters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        DefinePropertiesAction that = (DefinePropertiesAction) o;

        if (properties != null ? !properties.equals(that.properties) : that.properties != null) return false;
        return !(byPassFilters != null ? !byPassFilters.equals(that.byPassFilters) : that.byPassFilters != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        result = 31 * result + (byPassFilters != null ? byPassFilters.hashCode() : 0);
        return result;
    }
}
