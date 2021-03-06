/*
 * Copyright 2017-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 ONOS GUI -- Topology Background Module.
 Module that maintains the map and sprite layers
 */

(function () {

    var $log;

    var instance;

    angular.module('ovTopo2')
        .factory('Topo2BackgroundService', [
            '$log', 'Topo2ViewController', 'Topo2SpriteLayerService', 'Topo2MapService',
            'Topo2MapConfigService', 'Topo2RegionService',
            function (_$log_, ViewController, t2sls, t2ms, t2mcs, t2rs) {

                $log = _$log_;

                var BackgroundView = ViewController.extend({

                    id: 'topo2-background',
                    displayName: 'Background',

                    init: function () {
                        instance = this;
                        this.appendElement('#topo2-zoomlayer', 'g');
                        t2sls.init();
                        t2ms.init();
                    },
                    addLayout: function (data) {
                        this.background = data;
                        t2rs.bgRendered = false;

                        if (data.bgType === 'geo') {

                            // Hide Sprite Layer and show Map
                            t2sls.hide();
                            t2ms.show();

                            t2ms.setUpMap(data.bgId, data.bgFilePath, data.bgZoomScale)
                            .then(function (proj) {
                                // var z = ps.getPrefs('topo2_zoom', { tx: 0, ty: 0, sc: 1 });
                                // zoomer.panZoom([z.tx, z.ty], z.sc);

                                t2mcs.projection(proj);
                                // $log.debug('** Zoom restored:', z);
                                $log.debug('** We installed the projection:', proj);
                                t2rs.backgroundRendered();
                            });
                        }

                        if (data.bgType === 'grid') {

                            // Hide Sprite Layer and show Map
                            t2ms.hide();
                            t2sls.show();

                            t2sls.loadLayout(data.bgId).then(function () {
                                t2rs.backgroundRendered();
                            });
                        }
                    }
                });


                return instance || new BackgroundView();;
            }
        ]);
})();