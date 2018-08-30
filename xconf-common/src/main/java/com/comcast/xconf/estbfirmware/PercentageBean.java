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
 *  Author: mdolina
 *  Created: 3:47 PM
 */
package com.comcast.xconf.estbfirmware;

import com.comcast.hydra.astyanax.data.Persistable;
import com.comcast.xconf.Applicationable;
import com.comcast.xconf.firmware.ApplicationType;
import com.comcast.xconf.firmware.RuleAction;
import org.apache.commons.collections.comparators.NullComparator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PercentageBean extends Persistable implements Comparable<PercentageBean>, Applicationable {

    private String name;

    private String whitelist;

    private boolean active = true;

    private boolean firmwareCheckRequired;

    private boolean rebootImmediately = false;

    private String lastKnownGood;

    private String intermediateVersion;

    private Set<String> firmwareVersions = new HashSet<>();

    private String environment;

    private String model;

    private List<RuleAction.ConfigEntry> distributions = new ArrayList<>();

    private String applicationType = ApplicationType.STB;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(String whitelist) {
        this.whitelist = whitelist;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isFirmwareCheckRequired() {
        return firmwareCheckRequired;
    }

    public void setFirmwareCheckRequired(boolean firmwareCheckRequired) {
        this.firmwareCheckRequired = firmwareCheckRequired;
    }

    public String getLastKnownGood() {
        return lastKnownGood;
    }

    public void setLastKnownGood(String lastKnownGood) {
        this.lastKnownGood = lastKnownGood;
    }

    public String getIntermediateVersion() {
        return intermediateVersion;
    }

    public void setIntermediateVersion(String intermediateVersion) {
        this.intermediateVersion = intermediateVersion;
    }

    public boolean isRebootImmediately() {
        return rebootImmediately;
    }

    public void setRebootImmediately(boolean rebootImmediately) {
        this.rebootImmediately = rebootImmediately;
    }

    public Set<String> getFirmwareVersions() {
        return firmwareVersions;
    }

    public void setFirmwareVersions(Set<String> firmwareVersions) {
        this.firmwareVersions = firmwareVersions;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<RuleAction.ConfigEntry> getDistributions() {
        return distributions;
    }

    public void setDistributions(List<RuleAction.ConfigEntry> distributions) {
        this.distributions = distributions;
    }

    @Override
    public String getApplicationType() {
        return applicationType;
    }

    @Override
    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PercentageBean that = (PercentageBean) o;

        if (active != that.active) return false;
        if (firmwareCheckRequired != that.firmwareCheckRequired) return false;
        if (rebootImmediately != that.rebootImmediately) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (whitelist != null ? !whitelist.equals(that.whitelist) : that.whitelist != null) return false;
        if (lastKnownGood != null ? !lastKnownGood.equals(that.lastKnownGood) : that.lastKnownGood != null)
            return false;
        if (intermediateVersion != null ? !intermediateVersion.equals(that.intermediateVersion) : that.intermediateVersion != null)
            return false;
        if (firmwareVersions != null ? !firmwareVersions.equals(that.firmwareVersions) : that.firmwareVersions != null)
            return false;
        if (environment != null ? !environment.equals(that.environment) : that.environment != null) return false;
        if (model != null ? !model.equals(that.model) : that.model != null) return false;
        if (distributions != null ? !distributions.equals(that.distributions) : that.distributions != null)
            return false;
        return applicationType != null ? applicationType.equals(that.applicationType) : that.applicationType == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (whitelist != null ? whitelist.hashCode() : 0);
        result = 31 * result + (active ? 1 : 0);
        result = 31 * result + (firmwareCheckRequired ? 1 : 0);
        result = 31 * result + (rebootImmediately ? 1 : 0);
        result = 31 * result + (lastKnownGood != null ? lastKnownGood.hashCode() : 0);
        result = 31 * result + (intermediateVersion != null ? intermediateVersion.hashCode() : 0);
        result = 31 * result + (firmwareVersions != null ? firmwareVersions.hashCode() : 0);
        result = 31 * result + (environment != null ? environment.hashCode() : 0);
        result = 31 * result + (model != null ? model.hashCode() : 0);
        result = 31 * result + (distributions != null ? distributions.hashCode() : 0);
        result = 31 * result + (applicationType != null ? applicationType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("DistributedEnvModelPercentage{id=%s, name=%s, environment=%s, model=%s, active=%s,firmwareCheckRequired=%s, lastKnownGood=%s, intermediateVersion=%s, firmwareVersions=%s, distributions=%s, applicationType=%s}",
                id, name, environment, model, active, firmwareCheckRequired, lastKnownGood, intermediateVersion, firmwareVersions, distributions, applicationType);
    }

    @Override
    public int compareTo(PercentageBean o) {
        String name1 = (name != null) ? name.toLowerCase() : null;
        String name2 = (o != null && o.name != null) ? o.name.toLowerCase() : null;
        return new NullComparator().compare(name1, name2);
    }
}
