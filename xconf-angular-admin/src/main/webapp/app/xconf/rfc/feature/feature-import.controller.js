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
        .module('app.feature')
        .controller('FeatureImportController', controller);

    controller.$inject = ['$scope', 'featureService', 'importService', 'paginationService', 'utilsService', 'alertsService'];

    function controller($scope, featureService, importService, paginationService, utilsService, alertsService) {
        var vm = this;
        vm.isOverwritten = false;
        vm.overwriteAll = overwriteAll;

        vm.features = null;
        vm.wrappedFeatures = null;

        vm.importFeature = importFeature;
        vm.importAllFeatures = importAllFeatures;
        vm.retrieveFile = retrieveFile;

        vm.paginationStorageKey = 'featurePageSize';
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

        function retrieveFile(fileName) {
            vm.features = null;
            importService.openFile(fileName, null, this).then(function (result) {
                vm.features = getFeaturesFromFile(result);
                utilsService.sortObjectsById(vm.features);
                vm.wrappedFeatures = [];
                angular.forEach(vm.features, function (value) {
                    var wrappedFeature = {};
                    wrappedFeature.entity = value;
                    wrappedFeature.overwrite = false;
                    vm.wrappedFeatures.push(wrappedFeature);
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

        function importFeature(wrappedFeature) {
            var promise = wrappedFeature.overwrite
                ? featureService.updateFeature(wrappedFeature.entity)
                : featureService.createFeature(wrappedFeature.entity);
            promise.then(function () {
                alertsService.successfullySaved(wrappedFeature.entity.name);
                utilsService.removeSelectedItemFromListById(vm.wrappedFeatures, wrappedFeature.entity.id);
            }, function (error) {
                alertsService.showError({message: error.data.message, title: 'Exception'});
            });
        }

        function importAllFeatures() {
            importService.importAllEntities(featureService, vm.wrappedFeatures);
        }

        function overwriteAll() {
            angular.forEach(vm.wrappedFeatures, function (value) {
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
            return vm.wrappedFeatures ? vm.wrappedFeatures.length : 0;
        }
    }
})();

