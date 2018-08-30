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
        .controller('FeatureSetImportController', controller);

    controller.$inject = ['$scope', 'featureSetService', 'featureService', 'importService', 'paginationService', 'utilsService', 'alertsService'];

    function controller($scope, featureSetService, featureService, importService, paginationService, utilsService, alertsService) {
        var vm = this;
        vm.isOverwritten = false;
        vm.overwriteAll = overwriteAll;

        vm.wrappedFeatureSets = null;

        vm.importFeatureSet = importFeatureSet;
        vm.importAllFeatureSets = importAllFeatureSets;
        vm.retrieveFile = retrieveFile;

        vm.paginationStorageKey = 'featureSetPageSize';
        vm.pageSize = paginationService.getPageSize(vm.paginationStorageKey);
        vm.pageNumber = paginationService.getPageNumber();

        vm.selectPage = selectPage;
        vm.getGeneralItemsNumber = getGeneralItemsNumber;

        vm.progressBarControl = importService.progressBarControl;

        $scope.$on('$locationChangeSuccess', function () {
            if (paginationService.paginationSettingsInLocationHaveChanged(vm.pageNumber, vm.pageSize)) {
                vm.pageSize = paginationService.getPageSize(vm.paginationStorageKey);
                vm.pageNumber = paginationService.getPageNumber();
                selectPage();
            }
        });

        init();

        function init() {
            getAllFeatures();
        }

        function getAllFeatures() {
            vm.features = [];
            featureService.getAll().then(function(result) {
                vm.features = result.data;
            }, function(reason) {
                alertsService.showError({message: reason.data.message, title: 'Error'});
            });
        }

        function retrieveFile(fileName) {
            var featureSets = [];
            importService.openFile(fileName, null, this).then(function (result) {
                featureSets = getFeaturesFromFile(result);
                utilsService.sortObjectsById(featureSets);
                vm.wrappedFeatureSets = [];
                angular.forEach(featureSets, function (value) {
                    var wrappedFeatureSet = {};
                    wrappedFeatureSet.entity = value;
                    wrappedFeatureSet.overwrite = false;
                    vm.wrappedFeatureSets.push(wrappedFeatureSet);
                });
                vm.isOverwritten = false;
                selectPage();
            }, function (reason) {
                alertsService.showError({message: reason.data.message, title: 'Error'});
            });
        }

        function getFeaturesFromFile(data) {
            try {
                return JSON.parse(data);
            } catch (e) {
                alertsService.showError({
                    title: 'JSONStructureException',
                    message: 'Setting profiles JSON has some errors! Please, check this file!'
                });
                $log.error('error', e);
            }
        }

        function importFeatureSet(wrappedFeatureSet) {
            var promise = wrappedFeatureSet.overwrite
                ? featureSetService.updateFeatureSet(wrappedFeatureSet.entity)
                : featureSetService.createFeatureSet(wrappedFeatureSet.entity);
            promise.then(function () {
                alertsService.successfullySaved(wrappedFeatureSet.entity.name);
                utilsService.removeSelectedItemFromListById(vm.wrappedFeatureSets, wrappedFeatureSet.entity.id);
            }, function (error) {
                alertsService.showError({message: error.data.message, title: 'Exception'});
            });
        }

        function importAllFeatureSets() {
            importService.importAllEntities(featureSetService, vm.wrappedFeatureSets);
        }

        function overwriteAll() {
            angular.forEach(vm.wrappedFeatureSets, function (value) {
                value.overwrite = vm.isOverwritten;
            });
        }

        function selectPage() {
            paginationService.savePaginationSettingsInLocation(vm.pageNumber, vm.pageSize);
            computeStartAndEndIndex();
        }

        function computeStartAndEndIndex() {
            vm.startIndex = (vm.pageNumber - 1) * vm.pageSize;
            vm.endIndex = vm.pageNumber * vm.pageSize;
        }

        function getGeneralItemsNumber() {
            return vm.wrappedFeatureSets ? vm.wrappedFeatureSets.length : 0;
        }
    }
})();