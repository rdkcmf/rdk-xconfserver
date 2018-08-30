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
 * Created: 23.11.15  11:48
 */
package com.comcast.xconf.admin.service.dcm;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.hesperius.dataaccess.core.exception.EntityConflictException;
import com.comcast.xconf.admin.validator.dcm.UploadRepositoryValidator;
import com.comcast.xconf.logupload.LogUploadSettings;
import com.comcast.xconf.logupload.UploadRepository;
import com.comcast.xconf.permissions.DcmPermissionService;
import com.comcast.xconf.permissions.PermissionService;
import com.comcast.xconf.search.PredicateManager;
import com.comcast.xconf.search.SearchFields;
import com.comcast.xconf.shared.service.AbstractApplicationTypeAwareService;
import com.comcast.xconf.validators.IValidator;
import com.google.common.base.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class UploadRepositoryService extends AbstractApplicationTypeAwareService<UploadRepository> {

    private static final Logger log = LoggerFactory.getLogger(UploadRepositoryService.class);

    @Autowired
    private ISimpleCachedDAO<String, LogUploadSettings> logUploadSettingsDAO;

    @Autowired
    private ISimpleCachedDAO<String, UploadRepository> uploadRepositoryDAO;

    @Autowired
    private UploadRepositoryValidator uploadRepositoryValidator;

    @Autowired
    private PredicateManager predicateManager;

    @Autowired
    private DcmPermissionService permissionService;

    @Override
    public PermissionService getPermissionService() {
        return permissionService;
    }

    @Override
    public UploadRepository delete(String id) throws ValidationException {
        UploadRepository uploadRepository = super.delete(id);

        List<LogUploadSettings> allLogUploadSettings = logUploadSettingsDAO.getAll(Integer.MAX_VALUE / 100);
        for (LogUploadSettings logUploadSettings : allLogUploadSettings) {
            String uplRepoId = logUploadSettings.getUploadRepositoryId();
            if ((uplRepoId != null) && (uplRepoId.equals(id))) {
                logUploadSettings.setUploadRepositoryId("");
                try {
                    logUploadSettingsDAO.setOne(logUploadSettings.getId(), logUploadSettings);
                } catch (com.comcast.hesperius.dataaccess.core.ValidationException e) {
                    log.error("Can't update LogUploadSettings when deleting from UploadRepository: " + e);
                    throw new EntityConflictException("Can't update LogUploadSettings when deleting from UploadRepository");
                }
            }
        }

        return uploadRepository;
    }

    @Override
    protected List<Predicate<UploadRepository>> getPredicatesByContext(Map<String, String> context) {
        List<Predicate<UploadRepository>> predicates = new ArrayList<>();
        if (context == null || context.isEmpty()) {
            return predicates;
        }
        for (final Map.Entry<String, String> contextEntry : context.entrySet()) {
            switch (contextEntry.getKey()) {
                case SearchFields.NAME:
                    predicates.add(predicateManager.new UploadRepositoryNamePredicate(contextEntry.getValue()));
                    break;
            }
        }
        return predicates;
    }

    @Override
    public ISimpleCachedDAO<String, UploadRepository> getEntityDAO() {
        return uploadRepositoryDAO;
    }

    @Override
    public IValidator<UploadRepository> getValidator() {
        return uploadRepositoryValidator;
    }
}
