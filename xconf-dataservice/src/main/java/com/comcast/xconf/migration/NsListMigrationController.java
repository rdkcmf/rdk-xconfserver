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
 * Author: Stanislav Menshykov
 * Created: 03.11.15  16:52
 */
package com.comcast.xconf.migration;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.xconf.GenericNamespacedList;
import com.comcast.xconf.GenericNamespacedListTypes;
import com.comcast.xconf.IpAddressGroupExtended;
import com.comcast.xconf.NamespacedList;
import com.comcast.xconf.converter.GenericNamespacedListsConverter;
import com.comcast.xconf.utils.annotation.Migration;
import com.google.common.base.Joiner;
import org.apache.commons.collections.map.MultiValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(NsListMigrationController.URL_MAPPING)
@Migration
public class NsListMigrationController {

    public static final String URL_MAPPING = "migration";

    private static final Logger log = LoggerFactory.getLogger(NsListMigrationController.class);

    @Autowired
    private ISimpleCachedDAO<String, NamespacedList> namespacedListDAO;
    @Autowired
    private ISimpleCachedDAO<String, IpAddressGroupExtended> ipAddressGroupDAO;
    @Autowired
    private ISimpleCachedDAO<String, GenericNamespacedList> genericNamespacedListDAO;

    @RequestMapping(value = "/namespacedLists", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    /*@Migration(oldKey = String.class, oldEntity = NamespacedList.class, newKey = String.class, newEntity = GenericNamespacedList.class, migrationURL = "/namespacedLists")*/
    public ResponseEntity migrateNamespacedLists() {
        log.warn("Started NamespacedList Migration");
        final List<NamespacedList> allNamespacedLists = namespacedListDAO.getAll();
        final List<GenericNamespacedList> convertedLists = new ArrayList<>();
        for (NamespacedList list : allNamespacedLists) {
            log.info("Converting namespacedList with name: " + list.getId());
            convertedLists.add(GenericNamespacedListsConverter.convertFromNamespacedList(list));
            log.info("Converted successfully");
        }

        log.warn("Saving into GenericNamespacedLists CF");

        String responseMessage = saveConvertedNsLists(convertedLists, GenericNamespacedListTypes.MAC_LIST, GenericNamespacedListTypes.IP_LIST);

        log.warn(responseMessage);

        return new ResponseEntity<>(responseMessage, HttpStatus.OK);
    }

    @RequestMapping(value = "/ipAddressGroups", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    /*@Migration(oldKey = String.class, oldEntity = IpAddressGroupExtended.class, newKey = String.class, newEntity = GenericNamespacedList.class, migrationURL = "/ipAddressGroups")*/
    public ResponseEntity migrateIpAddressGroups() {
        log.warn("Started IpAddressGroups Migration");
        final List<IpAddressGroupExtended> allIpAddressGroups = ipAddressGroupDAO.getAll();
        final List<GenericNamespacedList> convertedLists = new ArrayList<>();
        for (IpAddressGroupExtended group : allIpAddressGroups) {
            log.info("Converting ipAddressGroup with id: " + group.getId());
            convertedLists.add(GenericNamespacedListsConverter.convertFromIpAddressGroupExtended(group));
            log.info("Converted successfully");
        }

        log.warn("Saving into GenericNamespacedLists CF");

        String responseMessage = saveConvertedNsLists(convertedLists, GenericNamespacedListTypes.IP_LIST, GenericNamespacedListTypes.MAC_LIST);

        log.warn(responseMessage);

        return new ResponseEntity<>(responseMessage, HttpStatus.OK);
    }

    private String saveConvertedNsLists(List<GenericNamespacedList> convertedLists, String nsListType, String oppositeNsListType) {
        Integer successfully = 0;
        Integer failed = 0;
        Map<String, String> duplicateListNames = new MultiValueMap();
        for (GenericNamespacedList convertedList : convertedLists) {
            try {
                GenericNamespacedList genericNamespacedList = genericNamespacedListDAO.getOne(convertedList.getId());
                if (genericNamespacedList == null || genericNamespacedList.getTypeName().equals(convertedList.getTypeName())) {
                    genericNamespacedListDAO.setOne(convertedList.getId(), convertedList);
                } else {
                    String oldId = convertedList.getId();
                    convertedList.setId(convertedList.getId() + "_" + nsListType);
                    duplicateListNames.put(oldId, convertedList.getId());

                    oldId = genericNamespacedList.getId();
                    genericNamespacedList.setId(genericNamespacedList.getId() + "_" + oppositeNsListType);
                    genericNamespacedListDAO.setOne(genericNamespacedList.getId(), genericNamespacedList);
                    genericNamespacedListDAO.deleteOne(oldId);
                    duplicateListNames.put(oldId, genericNamespacedList.getId());
                }

                genericNamespacedListDAO.setOne(convertedList.getId(), convertedList);
                successfully++;
            } catch (ValidationException e) {
                log.error("Error occurred while saving genericNamespacedLists: " + convertedList.getId() + ", Exception: " + e);
                failed++;
            }
        }
        String responseMessage = "Successfully saved " + successfully + ", failed " + failed;
        if (duplicateListNames.size() > 0) {
            responseMessage += ", duplicated names " + Joiner.on(", ").join(duplicateListNames.keySet()) + " were renamed to " + Joiner.on(", ").join(duplicateListNames.values());
        }
        return responseMessage;
    }
}
