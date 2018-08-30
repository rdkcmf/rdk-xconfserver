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
angular
    .module('app.permanentprofile')
    .factory('PermanentProfileService', service);

    service.$inject=['utilsService', '$http', 'syncHttpService'];

    function service(utilsService, $http, syncHttpService) {

        var API_URL = 'telemetry/profile/';

        return {
            getAll: getAll,
            getProfile: getProfile,
            createProfile: createProfile,
            updateProfile: updateProfile,
            deleteProfile: deleteProfile,
            getProfiles: getProfiles,
            searchProfileByName: searchProfileByName,
            updateSyncEntities: updateSyncEntities,
            createSyncEntities: createSyncEntities
        };

        function getAll() {
            return $http.get(API_URL);
        }

        function getProfile(id) {
            return $http.get(API_URL + id);
        }

        function createProfile(firmwareRule) {
            return $http.post(API_URL, firmwareRule);
        }

        function updateProfile(firmwareRule) {
            return $http.put(API_URL, firmwareRule);
        }

        function deleteProfile(id) {
            return $http.delete(API_URL + id);
        }

        function getProfiles(pageNumber, pageSize, context) {
            var url = API_URL + 'filtered?pageNumber=' + pageNumber + '&pageSize=' + pageSize;
            return $http.post(url, context);
        }

        function searchProfileByName(searchName, pageNumber, pageSize) {
            var url = API_URL + 'filtered?searchName=' + searchName + '&pageNumber=' + pageNumber + '&pageSize=' + pageSize;
            return $http.get(url);
        }

        function updateSyncEntities(permanentProfiles) {
            var requests = utilsService.generateRequestList(permanentProfiles, {url: API_URL + 'entities', method: 'PUT'});
           return requests && requests.length ? syncHttpService.http(requests) : null;
        }

        function createSyncEntities(permanentProfiles) {
            var requests = utilsService.generateRequestList(permanentProfiles, {url: API_URL + 'entities', method: 'POST'});
            return requests && requests.length ? syncHttpService.http(requests) : null;
        }
    }