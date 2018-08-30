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
 * Created: 23.11.15  11:18
 */
(function() {
    'use strict';

    angular
        .module('app.uploadRepository')
        .controller('UploadRepositoryImportController', controller);

    controller.$inject=['$scope', '$log', '$uibModal', '$location', 'alertsService', 'uploadRepositoryService', 'importService', 'utilsService', 'paginationService'];

    function controller($scope, $log, $modal, $location, alertsService, uploadRepositoryService, importService, utilsService, paginationService) {
        var vm = this;

        vm.retrieveFile = retrieveFile;
        vm.importUploadRepository = importUploadRepository;
        vm.importAllUploadRepositories = importAllUploadRepositories;
        vm.selectPage = selectPage;
        vm.changePageSize = changePageSize;
        vm.paginationStorageKey = 'uploadRepositoriesPageSize';
        vm.availablePageSizes = paginationService.getAvailablePageSizes();
        vm.pageSize = paginationService.getPageSize(vm.paginationStorageKey);
        vm.pageSizeModel = vm.pageSize;
        vm.pageNumber = paginationService.getPageNumber();
        vm.uploadRepositories = null;
        vm.wrappedUploadRepositories = null;
        vm.numPages = 0;
        vm.overwriteAll = overwriteAll;
        vm.isOverwritten = false;
        vm.progressBarControl = importService.progressBarControl;

        $scope.$on('$locationChangeSuccess', function () {
            if (paginationService.paginationSettingsInLocationHaveChanged(vm.pageNumber, vm.pageSize)) {
                vm.pageSize = paginationService.getPageSize(vm.paginationStorageKey);
                vm.pageSizeModel = vm.pageSize;
                paginationService.saveDefaultPageSize(vm.pageSize, vm.paginationStorageKey);
                vm.pageNumber = paginationService.getPageNumber();
                selectPage();
            }
        });


        function retrieveFile(fileName) {
            vm.uploadRepositories = null;
            importService.openFile(fileName, null, this).then(function (result) {
                vm.uploadRepositories = getUploadRepositoriesFromFile(result);
                utilsService.sortObjectsById(vm.uploadRepositories);
                vm.wrappedUploadRepositories = [];
                angular.forEach(vm.uploadRepositories, function(val, key) {
                    var wrappedUploadRepository = {};
                    wrappedUploadRepository.entity = val;
                    wrappedUploadRepository.overwrite = false;
                    vm.wrappedUploadRepositories.push(wrappedUploadRepository);
                });
                vm.isOverwritten = false;
                selectPage();
            }, function (reason) {
                alertsService.showError({message: reason});
            });
        }

        function getUploadRepositoriesFromFile(data) {
            try {
                 return JSON.parse(data);
            } catch(e) {
                alertsService.showError({title: 'JSONStructureException', message: 'UploadRepository JSON has some errors! Please, check this file!'});
                $log.error('UploadRepository JSON file is invalid! Please, check it!');
            }

        }

        function importUploadRepository(wrappedUploadRepository) {
            if (wrappedUploadRepository.overwrite) {
                uploadRepositoryService.update(wrappedUploadRepository.entity).then(function () {
                    handleSaveSuccess(wrappedUploadRepository);
                }, function (reason) {
                    handleSaveFailure(reason);
                });
            } else {
                uploadRepositoryService.create(wrappedUploadRepository.entity).then(function () {
                    handleSaveSuccess(wrappedUploadRepository);
                }, function (reason) {
                    handleSaveFailure(reason);
                });
            }
        }

        function handleSaveSuccess(wrappedUploadRepository) {
            alertsService.successfullySaved(wrappedUploadRepository.entity.name);
            utilsService.removeSelectedItemFromListById(vm.wrappedUploadRepositories, wrappedUploadRepository.entity.id);
        }

        function handleSaveFailure(reason) {
            var data = reason.data;
            alertsService.showError({title: data.type, message: data.message});
        }

        function importAllUploadRepositories() {
            importService.importAllEntities(uploadRepositoryService, vm.wrappedUploadRepositories);
        }

        function selectPage() {
            paginationService.savePaginationSettingsInLocation(vm.pageNumber, vm.pageSize);
            computeStartAndEndIndex();
        }

        function computeStartAndEndIndex() {
            vm.startIndex = (vm.pageNumber - 1) * vm.pageSize;
            vm.endIndex = vm.pageNumber * vm.pageSize;
        }

        function changePageSize(pageSizeModel) {
            vm.pageSize = pageSizeModel;
            paginationService.saveDefaultPageSize(vm.pageSize, vm.paginationStorageKey);
            selectPage();
        }

        function overwriteAll() {
            angular.forEach(vm.wrappedUploadRepositories, function (val, key) {
                val.overwrite = vm.isOverwritten;
            });
        }
    }
})();