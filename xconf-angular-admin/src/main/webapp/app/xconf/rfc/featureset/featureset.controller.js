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
        .controller('FeatureSetController', controller);

    controller.$inject = ['$scope', 'dialogs', 'featureService', 'featureSetService', 'paginationService', 'alertsService', 'utilsService'];

    function controller($scope, dialogs, featureService, featureSetService, paginationService, alertsService, utilsService) {
        var vm = this;

        vm.wrappedFeatureSets = null;
        vm.features = [];

        vm.paginationStorageKey = 'featureSetPageSize';
        vm.pageSize = paginationService.getPageSize(vm.paginationStorageKey);
        vm.pageNumber = paginationService.getPageNumber();
        vm.generalItemsNumber = 0;
        vm.searchParam = {};
        vm.searchOptions = {
            data: [
                {
                    "name": {
                        friendlyName: "Name",
                        apiArgs: ["NAME"]
                    }
                },
                {
                    "name": {
                        friendlyName: 'Feature',
                        apiArgs: ['FEATURE']
                    }
                }
            ]
        };
        vm.startParse = startParse;
        vm.getFeatureSets = getFeatureSets;
        vm.exportFeatureSet = featureSetService.exportFeatureSet;
        vm.exportAllFeatureSets = featureSetService.exportAllFeatureSets;
        vm.deleteFeatureSet = deleteFeatureSet;

        $scope.$on('$locationChangeSuccess', function () {
            if (paginationService.paginationSettingsInLocationHaveChanged(vm.pageNumber, vm.pageSize)) {
                vm.pageSize = paginationService.getPageSize(vm.paginationStorageKey);
                vm.pageNumber = paginationService.getPageNumber();
                init();
            }
        });

        $scope.$on('search-entities', function(event, data) {
            vm.searchParam = data.searchParam;
            getFeatureSets();
        });

        init();

        function init() {
            getFeatures();
            getFeatureSets();
        }

        function getFeatures() {
            featureService.getAll().then(function (result) {
                vm.features = result.data;
            }, function (error) {
                alertsService.showError({title: 'Error', message: 'Error by loading feature'});
            });
        }

        function deleteFeatureSet(featureSet) {
            if (featureSet.id) {
                var dialog = dialogs.confirm('Delete confirmation'
                    , '<span class="break-word-inline"> Are you sure you want to delete Feature Set ' + featureSet.name + ' ? </span>');
                dialog.result.then(function () {
                    featureSetService.deleteFeatureSet(featureSet.id).then(function(result) {
                        utilsService.removeItemFromArray(vm.wrappedFeatureSets, featureSet);
                        alertsService.successfullyDeleted(featureSet.name);
                        shiftItems();
                    }, function(reason) {
                        alertsService.showError({title: 'Error', message: reason.data.message});
                    });
                });
            }
        }

        function getFeatureSets() {
            featureSetService.getFeatureSets(vm.pageNumber, vm.pageSize, vm.searchParam).then(function (result) {
                vm.wrappedFeatureSets = result.data;
                vm.generalItemsNumber = result.headers('numberOfItems');
                paginationService.savePaginationSettingsInLocation(vm.pageNumber, vm.pageSize);
            }, function (error) {
                alertsService.showError({title: 'Error', message: 'Error by loading feature'});
            });
        }

        function shiftItems() {
            var numberOfPagesAfterDeletion = Math.ceil((vm.generalItemsNumber - 1) / vm.pageSize);
            vm.pageNumber = (vm.pageNumber > numberOfPagesAfterDeletion && numberOfPagesAfterDeletion > 0) ? numberOfPagesAfterDeletion : vm.pageNumber;
            getFeatureSets();
        }

        function startParse() {
            return vm.generalItemsNumber > 0;
        }
    }
})();