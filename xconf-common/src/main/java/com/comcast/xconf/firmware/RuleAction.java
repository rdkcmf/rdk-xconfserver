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
import java.util.Set;

public class RuleAction extends ApplicableAction {

    public RuleAction() {
        super(ApplicableAction.Type.RULE);
    }

    public RuleAction(String config) {
        this();
        configId = config;
    }

    @JsonProperty
    private String configId;

    @JsonProperty
    private List<ConfigEntry> configEntries;

    @JsonProperty
    private boolean active = true;

    @JsonProperty
    private boolean firmwareCheckRequired = false;

    @JsonProperty
    private boolean rebootImmediately = false;

    @JsonProperty
    private String whitelist;

    @JsonProperty
    private String intermediateVersion;

    @JsonProperty
    private Set<String> firmwareVersions;

    public List<ConfigEntry> getConfigEntries() {
        return configEntries;
    }

    public void setConfigEntries(List<ConfigEntry> configEntries) {
        this.configEntries = configEntries;
    }

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(String whitelist) {
        this.whitelist = whitelist;
    }

    public boolean isFirmwareCheckRequired() {
        return firmwareCheckRequired;
    }

    public void setFirmwareCheckRequired(boolean firmwareCheckRequired) {
        this.firmwareCheckRequired = firmwareCheckRequired;
    }

    public boolean isRebootImmediately() {
        return rebootImmediately;
    }

    public void setRebootImmediately(boolean rebootImmediately) {
        this.rebootImmediately = rebootImmediately;
    }

    public String getIntermediateVersion() {
        return intermediateVersion;
    }

    public void setIntermediateVersion(String intermediateVersion) {
        this.intermediateVersion = intermediateVersion;
    }

    public Set<String> getFirmwareVersions() {
        return firmwareVersions;
    }

    public void setFirmwareVersions(Set<String> firmwareVersions) {
        this.firmwareVersions = firmwareVersions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        RuleAction that = (RuleAction) o;

        if (active != that.active) return false;
        if (firmwareCheckRequired != that.firmwareCheckRequired) return false;
        if (rebootImmediately != that.rebootImmediately) return false;
        if (configId != null ? !configId.equals(that.configId) : that.configId != null) return false;
        if (configEntries != null ? !configEntries.equals(that.configEntries) : that.configEntries != null)
            return false;
        if (whitelist != null ? !whitelist.equals(that.whitelist) : that.whitelist != null) return false;
        if (intermediateVersion != null ? !intermediateVersion.equals(that.intermediateVersion) : that.intermediateVersion != null)
            return false;
        return !(firmwareVersions != null ? !firmwareVersions.equals(that.firmwareVersions) : that.firmwareVersions != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (configId != null ? configId.hashCode() : 0);
        result = 31 * result + (configEntries != null ? configEntries.hashCode() : 0);
        result = 31 * result + (active ? 1 : 0);
        result = 31 * result + (firmwareCheckRequired ? 1 : 0);
        result = 31 * result + (rebootImmediately ? 1 : 0);
        result = 31 * result + (whitelist != null ? whitelist.hashCode() : 0);
        result = 31 * result + (intermediateVersion != null ? intermediateVersion.hashCode() : 0);
        result = 31 * result + (firmwareVersions != null ? firmwareVersions.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RuleAction{" +
                "defaultConfigId='" + configId + '\'' +
                ", configEntries=" + configEntries +
                ", active=" + active +
                ", whitelist='" + whitelist + '\'' +
                ", intermediateVersion='" + intermediateVersion + '\'' +
                ", firmwareCheckRequired='" + firmwareCheckRequired + '\'' +
                ", firmwareVersion='" + firmwareVersions + '\'' +
                '}';
    }

    public static class ConfigEntry {
        private String configId;
        private Double percentage;

        public ConfigEntry() { }

        public ConfigEntry(String configId, Double percentage) {
            this.configId = configId;
            this.percentage = percentage;
        }

        public String getConfigId() {
            return configId;
        }

        public void setConfigId(String configId) {
            this.configId = configId;
        }

        public Double getPercentage() {
            return percentage;
        }

        public void setPercentage(Double percentage) {
            this.percentage = percentage;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ConfigEntry that = (ConfigEntry) o;

            if (configId != null ? !configId.equals(that.configId) : that.configId != null) return false;
            return !(percentage != null ? !percentage.equals(that.percentage) : that.percentage != null);

        }

        @Override
        public int hashCode() {
            int result = configId != null ? configId.hashCode() : 0;
            result = 31 * result + (percentage != null ? percentage.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "ConfigEntry{" +
                    "configId='" + configId + '\'' +
                    ", percentage=" + percentage +
                    '}';
        }
    }
}
