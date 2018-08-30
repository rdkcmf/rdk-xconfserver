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
 * Created: 20.10.15  17:43
 */
package com.comcast.xconf.admin.service.dcm;

import com.comcast.hesperius.dataaccess.core.ValidationException;
import com.comcast.hesperius.dataaccess.core.dao.ICompositeDAO;
import com.comcast.hesperius.dataaccess.core.dao.ISimpleCachedDAO;
import com.comcast.hesperius.dataaccess.core.exception.ValidationRuntimeException;
import com.comcast.xconf.admin.validator.dcm.LogUploadSettingsValidator;
import com.comcast.xconf.logupload.LogFile;
import com.comcast.xconf.logupload.LogUploadSettings;
import com.comcast.xconf.logupload.UploadRepository;
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
public class LogUploadSettingsService extends AbstractApplicationTypeAwareService<LogUploadSettings> {

    @Autowired
    private ISimpleCachedDAO<String, LogUploadSettings> logUploadSettingsDAO;

    @Autowired
    private ICompositeDAO<String, LogFile> indexesLogFilesDAO;

    @Autowired
    private ISimpleCachedDAO<String, UploadRepository> uploadRepositoryDAO;

    @Autowired
    private LogUploadSettingsValidator logUploadSettingsValidator;

    @Autowired
    private PredicateManager predicateManager;

    @Autowired
    private DcmPermissionService permissionService;

    @Override
    public PermissionService getPermissionService() {
        return permissionService;
    }

    public List<String> getLogUploadSettingsNames() {
        List<String> result = new ArrayList<>();
        for (LogUploadSettings entity : getAll()) {
            result.add(entity.getName());
        }

        return result;
    }

    @Override
    public LogUploadSettings delete(final String id) throws ValidationException {
        LogUploadSettings logUploadSettings = super.delete(id);
        indexesLogFilesDAO.deleteAll(id);
        return logUploadSettings;
    }

    @Override
    public void validateOnSave(LogUploadSettings entity) {
        super.validateOnSave(entity);

        String uploadRepositoryId = entity.getUploadRepositoryId();
        if (uploadRepositoryDAO.getOne(uploadRepositoryId) == null) {
            throw new ValidationRuntimeException("Upload repository with id " + uploadRepositoryId + " does not exist");
        }
    }

    @Override
    protected List<Predicate<LogUploadSettings>> getPredicatesByContext(Map<String, String> context) {
        List<Predicate<LogUploadSettings>> predicates = new ArrayList<>();
        if (context == null || context.isEmpty()) {
            return predicates;
        }
        if (context.containsKey(SearchFields.NAME)) {
            predicates.add(predicateManager.new LogUploadSettingsNamePredicate(context.get(SearchFields.NAME)));
        }
        if (StringUtils.isNotBlank(context.get(SearchFields.APPLICATION_TYPE))) {
            predicates.add(predicateManager.new ApplicationablePredicate<LogUploadSettings>(context.get(SearchFields.APPLICATION_TYPE)));
        } else {
            predicates.add(predicateManager.new ApplicationablePredicate<LogUploadSettings>(permissionService.getReadApplication()));
        }
        return predicates;
    }

    @Override
    public ISimpleCachedDAO<String, LogUploadSettings> getEntityDAO() {
        return logUploadSettingsDAO;
    }

    @Override
    public IValidator<LogUploadSettings> getValidator() {
        return logUploadSettingsValidator;
    }
}
