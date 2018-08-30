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
 * Created: 13.01.2017  14:28
 */
package com.comcast.hesperius.dataaccess.support;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

public class HashCheckerTest {
    @Test
    @Ignore
    public void test() {
        String path = "/info/hash";
        List<String> allHosts = Arrays.asList(new String[]{
                "http://localhost:9090/appdiscoveryDataService",
                "http://localhost:9095/angularadmin",
        });

        Map<String, String> errorHosts = new HashMap<>();
        Map<String, Long> goodResponses = new HashMap<>();

        DefaultHttpClient client = new DefaultHttpClient();

        //make requests to all hosts and store hash
        for (String host : allHosts) {
            try {
                HttpResponse response = client.execute(new HttpGet(host + path));
                String responseString = EntityUtils.toString(response.getEntity());
                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    Long hash = Long.valueOf(responseString);
                    goodResponses.put(host, hash);
                } else {
                    errorHosts.put(host, responseString);
                }
            } catch (Exception e) {
                errorHosts.put(host, e.getMessage());
            }
        }

        //For testing

        /*goodResponses.put("a", 130L);
        goodResponses.put("b", 130L);
        goodResponses.put("c", 130L);
        goodResponses.put("d", 5L);
        goodResponses.put("e", -8L);
        goodResponses.put("f", -8L);
        goodResponses.put("g", 653L);
        goodResponses.put("h", 91L);
        errorHosts.put("h", "TimeoutError");
        errorHosts.put("h", "Can't convert to long");*/

        //Count how many times each hash is used
        Collection<Long> values = goodResponses.values();

        if (!values.isEmpty()) {
            Map<Long, Integer> frequencies = new HashMap<>();
            for (Long hash : values) {
                frequencies.put(hash, Collections.frequency(values, hash));
            }

            //Find max frequently used hash
            Long mostUsedHash = (Long) values.toArray()[0];
            for (Map.Entry<Long, Integer> entry : frequencies.entrySet()) {
                if (entry.getValue() > frequencies.get(mostUsedHash)) {
                    mostUsedHash = entry.getKey();
                }
            }

            HashMap<String, Long> syncedHosts = new HashMap<>();
            HashMap<String, Long> desyncedHosts = new HashMap<>();

            //Loop through good responses and sort
            for (Map.Entry<String, Long> entry : goodResponses.entrySet()) {
                if (mostUsedHash.equals(entry.getValue())) {
                    syncedHosts.put(entry.getKey(), entry.getValue());
                } else {
                    desyncedHosts.put(entry.getKey(), entry.getValue());
                }
            }

            System.out.println("Synced hosts=" + syncedHosts.values().size());
            for (Map.Entry<String, Long> entry : syncedHosts.entrySet()) {
                System.out.println(entry.getKey() + " " + entry.getValue());
            }

            System.out.println("\nDesynced hosts=" + desyncedHosts.values().size());
            for (Map.Entry<String, Long> entry : desyncedHosts.entrySet()) {
                System.out.println(entry.getKey() + " " + entry.getValue());
            }
        }

        System.out.println("\nFailed requests=" + errorHosts.values().size());
        for (Map.Entry<String, String> entry : errorHosts.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue());
        }
    }
}
