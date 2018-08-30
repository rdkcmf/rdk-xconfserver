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
        .module('app.permanentprofile')
        .controller('PermanentProfilesController', controller);

    controller.$inject = ['$scope', '$controller', 'PermanentProfileService', 'alertsService', 'utilsService', 'dialogs', '$log', 'paginationService'];

    function controller($scope, $controller, PermanentProfileService, alertsService, utilsService, dialogs, $log, paginationService) {
        var vm = this;

        angular.extend(vm, $controller('MainController', {
            $scope: $scope
        }));

        vm.profiles = [];
        vm.paginationStorageKey = 'permanentProfilePageSize';
        vm.pageSize = paginationService.getPageSize(vm.paginationStorageKey);
        vm.pageNumber = paginationService.getPageNumber();
        vm.generalItemsNumber = 0;
        vm.searchParam = {};
        vm.searchOptions = {
            data: [
                {
                    "name": {
                        friendlyName: "Name",
                        apiArgs: ['NAME']
                    }
                }
            ]
        };

        vm.startParse = startParse;
        vm.getGeneralItemsNumber = getGeneralItemsNumber;
        vm.shiftItems = shiftItems;
        vm.deleteProfile = deleteProfile;
        vm.exportOne = exportOne;
        vm.exportAll = exportAll;
        vm.getProfiles = getProfiles;

        init();

        function init() {
            getProfiles();
        }

        $scope.$on('$locationChangeSuccess', function () {
            if (paginationService.paginationSettingsInLocationHaveChanged(vm.pageNumber, vm.pageSize)) {
                vm.pageSize = paginationService.getPageSize(vm.paginationStorageKey);
                vm.pageNumber = paginationService.getPageNumber();
                init();
            }
        });

        $scope.$on('search-entities', function(event, data) {
            vm.searchParam = data.searchParam;
            getProfiles();
        });

        function getProfiles() {
            PermanentProfileService.getProfiles(vm.pageNumber, vm.pageSize, vm.searchParam).then(function(result) {
                    vm.profiles = result.data;
                    vm.generalItemsNumber = result.headers('numberOfItems');
                    paginationService.savePaginationSettingsInLocation(vm.pageNumber, vm.pageSize);
                },
                function(error) {
                    alertsService.showError({title: 'Error', message: 'Error by loading permanent profiles'});
                });
        }

        function deleteProfile(profile) {
            if (profile.id) {
                var dialog = dialogs.confirm('Delete confirmation', '<span class="break-word-inline"> Are you sure you want to delete Permanent Profile ' + profile['telemetryProfile:name'] + ' ? </span>');
                dialog.result.then(function (btn) {
                    PermanentProfileService.deleteProfile(profile.id)
                        .then(function() {
                            utilsService.removeItemFromArray(vm.profiles, profile);
                            alertsService.successfullyDeleted(profile['telemetryProfile:name']);
                            shiftItems();
                        }, function(error) {
                            alertsService.showError({title: 'Error', message: error.data.message});
                        });
                });
            }
        }

        function exportOne(id) {
            if (id) {
                window.open('telemetry/profile/' + id + '?export');
            }
        }

        function exportAll() {
            window.open('telemetry/profile?export');
        }

        function shiftItems() {
            var numberOfPagesAfterDeletion = Math.ceil((getGeneralItemsNumber() - 1) / vm.pageSize);
            vm.pageNumber = (vm.pageNumber > numberOfPagesAfterDeletion && numberOfPagesAfterDeletion > 0) ? numberOfPagesAfterDeletion : vm.pageNumber;
            getProfiles();
        }

        function startParse() {
            return getGeneralItemsNumber() > 0;
        }

        function getGeneralItemsNumber() {
            return vm.generalItemsNumber;
        }
    }
})();