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
        .controller('PercentageBeanEditController', controller);

    controller.$inject = ['$rootScope', '$state', '$stateParams', 'percentFilterService', 'namespacedListService', 'alertsService', 'percentFilterValidationService', 'utilsService', 'firmwareConfigService', '$scope', 'modelService', 'environmentService', 'percentageBeanService', 'NAMESPACED_LIST_TYPE'];

    function controller($rootScope, $state, $stateParams, percentFilterService, namespacedListService, alertsService, percentFilterValidationService, utilsService, firmwareConfigService, $scope, modelService, environmentService, percentageBeanService, NAMESPACED_LIST_TYPE) {
        var vm = this;

        vm.percentageBean = {
            applicationType: $rootScope.applicationType,
            distributions: [],
            firmwareVersions: [],
            firmwareCheckRequired: false,
            active: false,
            rebootImmediately: false
        };
        vm.firmwareVersionSelectObjects = [];
        vm.firmwareConfigs = [];
        vm.missingFirmwareVersions = [];
        vm.firmwareConfigsBySupportedModels = [];
        vm.validator = percentFilterValidationService;
        vm.models = [];
        vm.environments = [];
        vm.hasValue = utilsService.hasValue;
        vm.whitelists = [];
        vm.noop = false;

        vm.save = save;
        vm.selectFirmwareConfig = selectFirmwareConfig;
        vm.getSelectedFirmwareVersions = getSelectedFirmwareVersions;
        vm.addDistribution = addDistribution;
        vm.removeDistribution = removeDistribution;
        vm.reloadFirmwareConfigsByModelChanging = reloadFirmwareConfigsByModelChanging;
        vm.getTotalDistributionPercentage = percentageBeanService.getTotalDistributionPercentage;
        vm.setNoop = setNoop;
        vm.isNoop = isNoop;
        init();

        function init() {
            vm.validator.cleanErrors();
            if ($stateParams.id) {
                percentageBeanService.getById($stateParams.id).then(function (resp) {
                    vm.percentageBean = resp.data;
                    reloadFirmwareConfigsByModelChanging(vm.percentageBean.model);
                    percentFilterService.verifyIfFirmwareVersionsExistsByList(vm.percentageBean, $rootScope.applicationType).then(function (missingFirmwareVersions) {
                        vm.missingFirmwareVersions = missingFirmwareVersions;
                    }, alertsService.errorHandler);
                    vm.noop = isNoop();

                }, alertsService.errorHandler);
            }

            namespacedListService.getNamespacedListIdsByType(NAMESPACED_LIST_TYPE.IP_LIST).then(function(resp) {
                vm.whitelists = resp.data;
            }, alertsService.errorHandler);

            modelService.getAll().then(function(resp) {
                vm.models = resp.data;
            }, alertsService.errorHandler);

            environmentService.getAll().then(function(resp) {
                vm.environments = resp.data;
            }, alertsService.errorHandler);
        }

        $rootScope.$on('applicationType:changed', function(event, data) {
            $state.go('percentfilter');
        });

        function save(percentageBean) {
            percentageBean.firmwareVersions = getSelectedFirmwareVersions(vm.firmwareVersionSelectObjects);
            if (!percentageBean.firmwareCheckRequired) {
                percentageBean.rebootImmediately = false;
                percentageBean.firmwareVersions = [];
            }

            if (vm.validator.validatePercentageBean(percentageBean, getSelectedFirmwareVersions(vm.firmwareVersionSelectObjects), vm.firmwareConfigsBySupportedModels)) {
                percentageBean.firmwareVersions = getSelectedFirmwareVersions(vm.firmwareVersionSelectObjects);
                if (!percentageBean.firmwareCheckRequired) {
                    percentageBean.rebootImmediately = false;
                    percentageBean.firmwareVersions = [];
                }
                if ($stateParams.id) {
                    percentageBeanService.update(percentageBean).then(function (resp) {
                        alertsService.successfullySaved(percentageBean.name);
                        $state.go('percentfilter');
                    }, alertsService.errorHandler);
                } else {
                    percentageBeanService.create(percentageBean).then(function (resp) {
                        alertsService.successfullySaved(percentageBean.name);
                        $state.go('percentfilter');
                    }, alertsService.errorHandler);
                }
            }
        }

        function selectFirmwareConfig(firmwareConfigSelectObject) {
            firmwareConfigSelectObject.selected = !firmwareConfigSelectObject.selected;
        }

        function getSelectedFirmwareVersions(firmwareConfigSelectEntities) {
            var selectedVersions = [];
            angular.forEach(firmwareConfigSelectEntities, function (val, key) {
                if (val.selected === true) {
                    selectedVersions.push(val.config.firmwareVersion);
                }
            });
            return selectedVersions;
        }

        $scope.$watch('vm.percentageBean.lastKnownGood', function(newLkgConfigId, oldLkgConfigId) {
            vm.firmwareVersionSelectObjects.forEach(function (firmwareVersionSelectObject) {
                if (vm.percentageBean && vm.percentageBean.firmwareCheckRequired) {
                    var oldLkgConfig = utilsService.getItemFromListById(oldLkgConfigId, vm.firmwareConfigsBySupportedModels);
                    if (firmwareVersionSelectObject.config.id === newLkgConfigId) {
                        firmwareVersionSelectObject.selected = true;
                    } else if (firmwareVersionSelectObject.config.id == oldLkgConfigId
                        && vm.percentageBean.firmwareVersions.indexOf(oldLkgConfig.firmwareVersion) === -1) {
                        firmwareVersionSelectObject.selected = false;
                    }
                }
            });
            vm.noop = isNoop();
        });

        $scope.$watch('vm.percentageBean.intermediateVersion', function() {
            vm.noop = isNoop();
        });

        $scope.$watch('vm.percentageBean.distributions', function() {
            if (Math.floor(vm.getTotalDistributionPercentage(vm.percentageBean)) === 100) {
                vm.percentageBean.lastKnownGood = null;
            }
            vm.noop = isNoop();
        }, true);

        function addDistribution(configs) {
            var newConfigEntry = {
                configId: '',
                percentage: ''
            };
            configs.push(newConfigEntry);
        }

        function removeDistribution(configs, item) {
            utilsService.removeItemFromArray(configs, item);
            vm.validator.validateDistribution(vm.percentageBean);
        }

        function setNoop() {
            if (vm.percentageBean.distributions.length) {
                vm.percentageBean.distributions.length = 0;
            }
            if (vm.percentageBean.lastKnownGood) {
                vm.percentageBean.lastKnownGood = '';
            }
            if (vm.percentageBean.intermediateVersion) {
                vm.percentageBean.intermediateVersion  = '';
            }
        }

        function isNoop() {
            var configPresent = vm.percentageBean.distributions.length || vm.percentageBean.lastKnownGood || vm.percentageBean.intermediateVersion;
            return !configPresent;
        }

        function reloadFirmwareConfigsByModelChanging(modelId) {
            vm.firmwareVersionSelectObjects = [];
            firmwareConfigService.getByModelId(modelId).then(function(firmwareConfigResp) {
                vm.firmwareConfigsBySupportedModels = firmwareConfigResp.data;
                angular.forEach(firmwareConfigResp.data, function (val, key) {
                    var selectObject = {
                        config: val,
                        selected: false
                    };
                    if (vm.percentageBean.firmwareVersions.indexOf(val.firmwareVersion) !== -1) {
                        selectObject.selected = true;
                    }
                    vm.firmwareVersionSelectObjects.push(selectObject);
                });
            }, alertsService.errorHandler);
        }
    }
})();