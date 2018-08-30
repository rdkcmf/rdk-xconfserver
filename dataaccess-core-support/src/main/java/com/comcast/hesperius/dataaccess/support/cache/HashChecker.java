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
 * Author: obaturynskyi
 * Created: 13.01.2017  20:04
 */
package com.comcast.hesperius.dataaccess.support.cache;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HashChecker {

    private String[] hosts;
    private Map<String, String> badResponses;
    private Map<String, Long> goodResponses;
    public static final String HASH_PATH = "/info/hash";

    public HashChecker(String[] hosts) {
        this.hosts = hosts;
        badResponses = new HashMap<>();
        goodResponses = new HashMap<>();
    }

    public HashCheckerResult checkHash() {
        HashCheckerResult hashCheckerResult = null;
        makeRequestsToHosts();
        Long mostUsedHash = findMostUsedHash(goodResponses);

        if (mostUsedHash != null) {
            Map<String, Long> syncedHosts = new HashMap<>();
            Map<String, Long> desyncedHosts = new HashMap<>();

            //Loop through good responses and sort
            for (Map.Entry<String, Long> entry : goodResponses.entrySet()) {
                if (mostUsedHash.equals(entry.getValue())) {
                    syncedHosts.put(entry.getKey(), entry.getValue());
                } else {
                    desyncedHosts.put(entry.getKey(), entry.getValue());
                }
            }

            hashCheckerResult = new HashCheckerResult(syncedHosts, desyncedHosts, badResponses);
        }

        return hashCheckerResult;
    }

    public static Long findMostUsedHash(Map<String, Long> hostAndHash) {
        Long mostUsedHash = null;
        //Count how many times each hash is used
        Collection<Long> values = hostAndHash.values();

        if (!values.isEmpty()) {
            Map<Long, Integer> frequencies = new HashMap<>();
            for (Long hash : values) {
                frequencies.put(hash, Collections.frequency(values, hash));
            }

            //Find max frequently used hash
            mostUsedHash = (Long) values.toArray()[0];
            for (Map.Entry<Long, Integer> entry : frequencies.entrySet()) {
                if (entry.getValue() > frequencies.get(mostUsedHash)) {
                    mostUsedHash = entry.getKey();
                }
            }
        }

        return mostUsedHash;
    }


    public void makeRequestsToHosts() {
        DefaultHttpClient client = new DefaultHttpClient();

        //make requests to all hosts and store hash
        for (String host : hosts) {
            try {
                HttpResponse response = client.execute(new HttpGet(host + HASH_PATH));
                String responseString = EntityUtils.toString(response.getEntity());
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    Long hash = Long.valueOf(responseString);
                    goodResponses.put(host, hash);
                } else {
                    badResponses.put(host, responseString);
                }
            } catch (Exception e) {
                badResponses.put(host, e.getMessage());
            }
        }
    }

    public String[] getHosts() {
        return hosts;
    }

    public void setHosts(String[] hosts) {
        this.hosts = hosts;
    }

    public Map<String, String> getBadResponses() {
        return badResponses;
    }

    public void setBadResponses(Map<String, String> badResponses) {
        this.badResponses = badResponses;
    }

    public Map<String, Long> getGoodResponses() {
        return goodResponses;
    }

    public void setGoodResponses(Map<String, Long> goodResponses) {
        this.goodResponses = goodResponses;
    }
}
