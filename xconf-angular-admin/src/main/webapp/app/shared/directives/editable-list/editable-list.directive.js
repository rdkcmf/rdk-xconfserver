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
        .module('app.directives')
        .directive('editableList', directive);

    directive.$inject = [];

    function directive() {

        var scope = {
            control: '=',
            title: '=',
            entities: '=',
            allItems: '='
        }

        return {
            restrict: 'E',
            scope: scope,
            link: linkFunction,
            templateUrl: 'app/shared/directives/editable-list/editable-list.directive.html'
        }

        function linkFunction(scope) {
            scope.addEntity = addEntity;
            scope.removeEntity = removeEntity;
            scope.filterById = filterById;

            function addEntity() {
                if (scope.selectedEntityId && scope.entities.indexOf(scope.selectedEntityId) == -1) {
                    scope.entities.push(scope.selectedEntityId);
                    scope.selectedEntityId = null;
                }
            }

            function removeEntity(id) {
                var index = scope.entities.indexOf(id);
                if (scope.entities.indexOf(id) != -1) {
                    scope.entities.splice(index, 1);
                }
            }

            function filterById(item) {
                return scope.entities.indexOf(item.id) != -1
            }
        }
    }
})();