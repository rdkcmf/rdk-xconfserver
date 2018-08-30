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
 *  Created: 7:59 PM
 */
package com.comcast.xconf.admin.service.common;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.xconf.GenericNamespacedList;
import com.comcast.xconf.GenericNamespacedListTypes;
import com.comcast.xconf.IpAddressGroupExtended;
import com.comcast.xconf.admin.validator.common.GenericNamespacedListValidator;
import com.comcast.xconf.converter.GenericNamespacedListsConverter;
import com.comcast.xconf.search.PredicateManager;
import com.comcast.xconf.search.SearchFields;
import com.comcast.xconf.service.GenericNamespacedListQueriesService;
import com.comcast.xconf.shared.service.AbstractService;
import com.comcast.xconf.util.GenericNamespacedListUtils;
import com.comcast.xconf.validators.IValidator;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GenericNamespacedListService extends AbstractService<GenericNamespacedList> {

    @Autowired
    private ISimpleCachedDAO<String, GenericNamespacedList> genericNamespacedListDAO;

    @Autowired
    private GenericNamespacedListValidator genericNamespacedListValidator;

    @Autowired
    private GenericNamespacedListQueriesService genericNamespacedListQueriesService;

    @Autowired
    private PredicateManager predicateManager;

    @Override
    public ISimpleCachedDAO<String, GenericNamespacedList> getEntityDAO() {
        return genericNamespacedListDAO;
    }

    @Override
    public IValidator<GenericNamespacedList> getValidator() {
        return genericNamespacedListValidator;
    }

    @Override
    public void normalizeOnSave(GenericNamespacedList genericNamespacedList) {
        if (GenericNamespacedListTypes.MAC_LIST.equals(genericNamespacedList.getTypeName())) {
            GenericNamespacedListUtils.normalizeMacAddress(genericNamespacedList.getData());
        }
    }

    @Override
    public void validateUsage(String namespacedListId) {
        genericNamespacedListQueriesService.checkUsage(namespacedListId);
    }

    public List<String> getNamespacedListsIds() {
        return genericNamespacedListQueriesService.getNamespacedListsIds();
    }

    public List<String> getNamespacedListsIdsByType(String typeName) {
        return genericNamespacedListQueriesService.getNamespacedListsIdsByType(typeName);
    }

    public List<GenericNamespacedList> getAllByType(String typeName) {
        return genericNamespacedListQueriesService.getAllByType(typeName);
    }

    public GenericNamespacedList update(GenericNamespacedList list, String newId) throws ValidationException {
        return genericNamespacedListQueriesService.updateNamespacedList(list, list.getTypeName(), newId);
    }

    public List<GenericNamespacedList> findByContext(Map<String, String> context, String typeName) {
        List<Predicate<GenericNamespacedList>> predicates = getPredicatesByContext(context);
        if (StringUtils.isNotBlank(typeName)) {
            predicates.add(predicateManager.new GenericNamespacedListTypePredicate(typeName));
        }
        return Lists.newArrayList(Iterables.filter(getAll(), Predicates.and(predicates)));
    }

    public List<IpAddressGroupExtended> getAllIpAddressGroups() {
        List<GenericNamespacedList> ipLists = getAllByType(GenericNamespacedListTypes.IP_LIST);
        return GenericNamespacedListsConverter.convertToListOfIpAddressGroups(ipLists);
    }

    protected List<Predicate<GenericNamespacedList>> getPredicatesByContext(final Map<String, String> context) {
        List<Predicate<GenericNamespacedList>> predicates = new ArrayList<>();
        if (context == null || context.isEmpty()) {
            return predicates;
        }
        if(context.containsKey(SearchFields.DATA)) {
            predicates.add(predicateManager.new GenericNamespacedListDataPredicate(context.get(SearchFields.DATA)));
        }
        if (context.containsKey(SearchFields.NAME)) {
            predicates.add(predicateManager.new GenericNamespacedListIdPredicate(context.get(SearchFields.NAME)));
        }
        if (context.containsKey(SearchFields.TYPE)) {
            predicates.add(predicateManager.new GenericNamespacedListTypePredicate(context.get(SearchFields.TYPE)));
        }
        return predicates;
    }

}
