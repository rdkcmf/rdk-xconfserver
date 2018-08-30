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
 * Author: rdolomansky
 * Created: 3/26/15
 */

(function () {
    'use strict';
    angular
        .module('app.services')
        .factory('importService', service);

    service.$inject = ['$rootScope', '$q', 'fileReader', 'dialogs', '$timeout', 'utilsService', 'alertsService', 'ENTITY_TYPE'];

    function service($rootScope, $q, fileReader, $dialogs, $timeout, utilsService, alertsService, ENTITY_TYPE) {

        var progressBarControl = {};
        var MAX_VALUES_FILESIZE = 6 * 1024 * 1024;//6 MiB

        $rootScope.$on('fileReaderProgress', function (event, data) {
            if (data.progress < data.total) {
                var percentage = Math.round((data.progress * 100) / data.total);
                $rootScope.$broadcast('dialogs.wait.progress', {'progress': percentage});
            } else {
                $rootScope.$broadcast('dialogs.wait.progress', {'progress': 100});
                $timeout(function () {
                    $rootScope.$broadcast('dialogs.wait.complete');
                }, 1000);
            }
        });

        return {
            openFile: openFile,
            progressBarControl: progressBarControl,
            importAllEntities: importAllEntities
        }

        function openFile(fileName, limit, scope) {
            var deferred = $q.defer();
            if (angular.isUndefined(fileName)) {
                deferred.reject('File name is undefined');
                return deferred.promise;
            }

            if (limit == null || angular.isUndefined(limit)) {
                limit = MAX_VALUES_FILESIZE;
            }

            if (fileName.size > MAX_VALUES_FILESIZE) {
                deferred.reject('File is too big [' + fileName.name + '], try to use a smaller one, limit is ' + limit + ' bytes');
                return deferred.promise;
            }

            //show wait dialog
            $dialogs.wait(undefined, undefined, 0);

            return fileReader.readAsTextContent(fileName, scope);
        }

        function importAllEntities(service, wrappedEntities, success, error, entityType) {
            progressBarControl.total = wrappedEntities.length;
            angular.forEach(generateCreateUpdateEntities(wrappedEntities), function(list, key) {
                var partEntities = utilsService.splitListByPercentage(list, 10);
                if (!partEntities || !partEntities.length) {
                    return null;
                }

                // service must contain the following functions: updateSyncEntities, createSyncEntities
                var http = service[key](partEntities).then(function(result) {
                    angular.forEach(result.data, function(value, id) {
                        if (value.status === "SUCCESS") {
                            alertsService.successfullySaved(value.message);
                            if (success) {
                                success(id, value.message);
                            } else {
                                 utilsService.removeSelectedItemFromListById(wrappedEntities, id);
                            }
                        } else if (value.status === "FAILURE") {
                            if (entityType === ENTITY_TYPE.NS_LIST || entityType === ENTITY_TYPE.PERCENT_FILTER) {
                                $rootScope.$broadcast('import::error', {
                                    id: id,
                                    message: value.message
                                });
                            } else {
                                alertsService.showError({title: 'Error', message: value.message});
                            }
                            if (error) {
                                error(id, value.message);
                            }
                        }
                    });

                    if (progressBarControl) {
                        progressBarControl.progress(Object.keys(result.data).length);
                    }
                });

                // changes a progress bar
                progressBarControl.next = function() {
                    http.next();
                }
            });
        }

        function generateCreateUpdateEntities(wrappedEntities) {
            var entities = {
                updateSyncEntities: [],
                createSyncEntities: []
            };

            angular.forEach(wrappedEntities, function(wrappedEntity) {
                if (wrappedEntity.overwrite) {
                    entities.updateSyncEntities.push(wrappedEntity.entity);
                } else {
                    entities.createSyncEntities.push(wrappedEntity.entity);
                }
            });

            return entities;
        }
    }
})();