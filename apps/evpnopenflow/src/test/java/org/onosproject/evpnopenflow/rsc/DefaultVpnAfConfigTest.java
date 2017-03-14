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

package org.onosproject.evpnopenflow.rsc;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.incubator.net.routing.VpnRouteTarget;

import static org.junit.Assert.assertEquals;

/**
 * Provides test implementation of class default vpn af configuration.
 */
public class DefaultVpnAfConfigTest {
    private DefaultVpnAfConfig vpnAfConfig;

    @Before
    public void setUp() throws Exception {
        String exportRoutePolicy = "abc";
        String importRoutePolicy = "def";
        VpnRouteTarget routeTarget = VpnRouteTarget.routeTarget("1000:1000");
        String routeTargetType = "export_extcommunity";

        vpnAfConfig = new DefaultVpnAfConfig(exportRoutePolicy,
                                             importRoutePolicy,
                                             routeTarget,
                                             routeTargetType);
        DefaultVpnAfConfig vpnAfConfig1 = new DefaultVpnAfConfig(exportRoutePolicy,
                                                                 importRoutePolicy,
                                                                 routeTarget,
                                                                 routeTargetType);

        vpnAfConfig.toString();
    }

    @Test
    public void validateVpnInstanceConfig() throws Exception {
        assertEquals("abc", vpnAfConfig.exportRoutePolicy());
        assertEquals("def", vpnAfConfig.importRoutePolicy());
        assertEquals("1000:1000",
                     vpnAfConfig.routeTarget().getRouteTarget());
        assertEquals("export_extcommunity",
                     vpnAfConfig.routeTargetType());
    }
}