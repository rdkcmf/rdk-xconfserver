/*******************************************************************************
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
 *******************************************************************************/
(function() {
    'use strict';

    angular
        .module('app.percentfilter')
        .factory('percentFilterService', service);

    service.$inject=['$http', 'firmwareConfigService', '$q'];

    function service($http, firmwareConfigService, $q) {
        var URL = 'percentfilter/';

        return {
            getFilter: getFilter,
            saveFilter: saveFilter,
            exportWholeFilter: exportWholeFilter,
            exportGlobalPercentage: exportGlobalPercentage,
            exportGlobalPercentageAsRule: exportGlobalPercentageAsRule,
            verifyIfFirmwareVersionsExistsByList: verifyIfFirmwareVersionsExistsByList
        };

        function getFilter() {
            return $http.get(URL);
        }

        function saveFilter(filter) {
            return $http.post(URL, filter);
        }

        function exportWholeFilter() {
            window.open(URL + '?export');
        }

        function exportGlobalPercentage() {
            window.open(URL + 'globalPercentage?export')
        }

        function verifyIfFirmwareVersionsExistsByList(distributedEnvModelPercentage) {
            var deferred = $q.defer();
            if (distributedEnvModelPercentage && distributedEnvModelPercentage.model && distributedEnvModelPercentage.firmwareVersions && distributedEnvModelPercentage.firmwareVersions.length > 0) {
                firmwareConfigService.verifyIfVersionsExistByListAndEnvModelRuleName(distributedEnvModelPercentage.model, distributedEnvModelPercentage.firmwareVersions).then(function (resp) {
                    deferred.resolve(resp.data);
                }, function (error) {
                    deferred.reject(error);
                });
            } else {
                deferred.resolve({"existedVersions":[],"notExistedVersions":[]});
            }
            return deferred.promise;
        }
        function exportGlobalPercentageAsRule() {
            window.open(URL + 'globalPercentage/asRule?export');
        }
    }
})();