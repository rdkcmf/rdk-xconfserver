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
    angular
        .module('app.featureset')
        .factory('featureSetService', service);

    service.$inject = ['$http', 'syncHttpService', 'utilsService'];

    function service($http, syncHttpService, utilsService) {
        var URL = "rfc/featureset/";

        return {
            getAll: getAll,
            getFeatureSets: getFeatureSets,
            getFeatureSet: getFeatureSet,
            createFeatureSet: createFeatureSet,
            updateFeatureSet: updateFeatureSet,
            deleteFeatureSet: deleteFeatureSet,
            exportAllFeatureSets: exportAllFeatureSets,
            exportFeatureSet: exportFeatureSet,
            updateSyncEntities: updateSyncEntities,
            createSyncEntities: createSyncEntities
        }

        function getAll() {
            return $http.get(URL);
        }

        function getFeatureSets(pageNumber, pageSize, searchParam) {
            var url = URL + 'filtered?pageNumber=' + pageNumber + '&pageSize=' + pageSize;
            return $http.post(url, searchParam);
        }

        function getFeatureSet(id) {
            return $http.get(URL + id);
        }

        function createFeatureSet(feature) {
            return $http.post(URL, feature);
        }

        function updateFeatureSet(feature) {
            return $http.put(URL, feature);
        }

        function deleteFeatureSet(id) {
            return $http.delete(URL + id);
        }

        function exportAllFeatureSets() {
            window.open(URL + "?export");
        }

        function exportFeatureSet(id) {
            window.open(URL + id + "?export");
        }

        function updateSyncEntities(entities) {
            var requests = utilsService.generateRequestList(entities, {url: URL + 'entities', method: 'PUT'});
            return requests && requests.length ? syncHttpService.http(requests) : null;
        }

        function createSyncEntities(entities) {
            var requests = utilsService.generateRequestList(entities, {url: URL + 'entities', method: 'POST'});
            return requests && requests.length ? syncHttpService.http(requests) : null;
        }
    }
})();