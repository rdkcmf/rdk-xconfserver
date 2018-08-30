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
 * Created: 25.11.15  14:59
 */
package com.comcast.xconf.admin.service.dcm;

import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.xconf.admin.validator.dcm.VodSettingsValidator;
import com.comcast.xconf.dcm.core.Utils;
import com.comcast.xconf.logupload.VodSettings;
import com.comcast.xconf.permissions.DcmPermissionService;
import com.comcast.xconf.permissions.PermissionService;
import com.comcast.xconf.search.PredicateManager;
import com.comcast.xconf.search.SearchFields;
import com.comcast.xconf.shared.service.AbstractApplicationTypeAwareService;
import com.comcast.xconf.validators.IValidator;
import com.google.common.base.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class VodSettingsService extends AbstractApplicationTypeAwareService<VodSettings> {

    @Autowired
    private ISimpleCachedDAO<String, VodSettings> vodSettingsDAO;

    @Autowired
    private VodSettingsValidator vodSettingsValidator;

    @Autowired
    private PredicateManager predicateManager;

    @Autowired
    private DcmPermissionService permissionService;

    @Override
    public PermissionService getPermissionService() {
        return permissionService;
    }

    @Override
    public VodSettings getOne(String id) {
        VodSettings vodSettings = super.getOne(id);

        Map<String, String> map = vodSettings.getSrmIPList();
        vodSettings.setIpNames(new ArrayList<String>());
        vodSettings.setIpList(new ArrayList<String>());
        Utils.mapToLists(map, vodSettings.getIpNames(), vodSettings.getIpList());

        return vodSettings;
    }

    @Override
    protected List<Predicate<VodSettings>> getPredicatesByContext(Map<String, String> context) {
        List<Predicate<VodSettings>> predicates = new ArrayList<>();
        if (context == null || context.isEmpty()) {
            return predicates;
        }
        if (context.containsKey(SearchFields.NAME)) {
            predicates.add(predicateManager.new VodSettingsNamePredicate(context.get(SearchFields.NAME)));
        }
        if (StringUtils.isNotBlank(context.get(SearchFields.APPLICATION_TYPE))) {
            predicates.add(predicateManager.new ApplicationablePredicate<VodSettings>(context.get(SearchFields.APPLICATION_TYPE)));
        } else {
            predicates.add(predicateManager.new ApplicationablePredicate<VodSettings>(permissionService.getReadApplication()));
        }
        return predicates;
    }

    @Override
    public void normalizeOnSave(VodSettings vodSettings) {
        vodSettings.setSrmIPList(Utils.combineListsIntoMap(vodSettings.getIpNames(), vodSettings.getIpList()));
    }

    @Override
    public ISimpleCachedDAO<String, VodSettings> getEntityDAO() {
        return vodSettingsDAO;
    }

    @Override
    public IValidator<VodSettings> getValidator() {
        return vodSettingsValidator;
    }
}
