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
        .controller('PercentFilterController', controller);

    controller.$inject = ['$scope', '$controller', 'percentFilterService', 'alertsService', '$uibModal', 'firmwareConfigService', 'percentageBeanService', 'dialogs'];

    function controller($scope, $controller, percentFilterService, alertsService, $uibModal, firmwareConfigService, percentageBeanService, dialogs) {
        var vm = this;

        angular.extend(vm, $controller('MainController', {
            $scope: $scope
        }));

        vm.percentFilter = null;
        vm.firmwareConfigMap = {};
        vm.percentageBeans = [];

        vm.exportWholeFilter = percentFilterService.exportWholeFilter;
        vm.exportGlobalPercentage = percentFilterService.exportGlobalPercentage;
        vm.exportPercentageBean = percentageBeanService.exportPercentageBean;
        vm.exportAllPercentageBeans = percentageBeanService.exportAllPercentageBeans;
        vm.exportAllPercentageBeansAsRule = percentageBeanService.exportAllPercentageBeansAsRule;
        vm.exportPercentageBeanAsRule = percentageBeanService.exportPercentageBeanAsRule;
        vm.exportGlobalPercentageAsRule = percentFilterService.exportGlobalPercentageAsRule;
        vm.viewPercentageBean = viewPercentageBean;
        vm.deletePercentageBean = deletePercentageBean;

        init();

        function init() {
            percentFilterService.getFilter().then(function(resp) {
                vm.filter = resp.data;
            }, function(error) {
                alertsService.showError({title: 'Error', message: error.data.message});
            });
            firmwareConfigService.getFirmwareConfigMap().then(function(resp) {
                vm.firmwareConfigMap = resp.data;
            }, function(error) {
                alertsService.showError({title: 'Exception', message: error.data.message});
            });
            percentageBeanService.getAll().then(function(resp) {
                vm.percentageBeans = resp.data;
            }, function(error) {
                alertsService.showError({title: 'Error', message: error.data.message});
            });
        }

        function viewPercentageBean(percentageBean) {
            percentFilterService.verifyIfFirmwareVersionsExistsByList(percentageBean).then(function(firmwareVersions) {
                showViewPercentageBean(percentageBean, firmwareVersions);
            }, function(error) {
                alertsService.showError({title: 'Error', message: error.data.message});
            });
        }

        function showViewPercentageBean(percentageBean, firmwareVersions) {
            $uibModal.open({
                templateUrl: 'app/xconf/firmware/percentfilter/percentfilter.view.html',
                controller: 'PercentFilterViewController as vm',
                size: 'md',
                resolve : {
                    percentageBean: function() {
                        return percentageBean;
                    },
                    firmwareVersions: function() {
                        return firmwareVersions;
                    },
                    firmwareConfigMap: function() {
                        return vm.firmwareConfigMap;
                    }
                }
            });
        }

        function deletePercentageBean(percentageBean) {
            var dialog = dialogs.confirm('Delete confirmation', '<span class="break-word-inline"> Are you sure you want to delete PercentageBean ' + percentageBean.name + ' ? </span>');
            dialog.result.then(function (btn) {
                percentageBeanService.deleteById(percentageBean.id).then(function(resp) {
                    alertsService.successfullyDeleted(percentageBean.name);
                    for (var i=0; i < vm.percentageBeans.length; i++) {
                        if (percentageBean.id === vm.percentageBeans[i].id) {
                            vm.percentageBeans.splice(i, 1);
                        }
                    }
                    shiftItems();
                }, function(error) {
                    alertsService.showError({title: 'Error', message: error.data.message});
                });
            });
        }



    }
})();