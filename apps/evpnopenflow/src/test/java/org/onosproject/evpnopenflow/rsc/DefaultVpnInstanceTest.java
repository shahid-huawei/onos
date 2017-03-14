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
import org.onosproject.incubator.net.routing.EvpnInstanceName;
import org.onosproject.incubator.net.routing.RouteDistinguisher;
import org.onosproject.incubator.net.routing.VpnRouteTarget;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;


/**
 * Provides test implementation of class default vpn instance.
 */
public class DefaultVpnInstanceTest {
    private DefaultVpnInstance vpnInstance;

    @Before
    public void setUp() throws Exception {
        VpnInstanceId id = VpnInstanceId
                .vpnInstanceId("d54d8f0e-81ad-424c-9fee-a1dfd8e47122");
        EvpnInstanceName name = EvpnInstanceName
                .evpnName("TestVPN1");
        String description = "vpninstance1";
        RouteDistinguisher routeDistinguisher = RouteDistinguisher
                .routeDistinguisher("2000:2000");
        //RouteTarget routeTarget = RouteTarget
        //.routeTarget("2000:2000");

        Set<VpnRouteTarget> rtImport = new HashSet<>();
        rtImport.add(VpnRouteTarget.routeTarget("2000:2000"));

        Set<VpnRouteTarget> rtExport = new HashSet<>();
        rtExport.add(VpnRouteTarget.routeTarget("2000:2000"));

        Set<VpnRouteTarget> cfgRtSet = new HashSet<>();
        vpnInstance = new DefaultVpnInstance(id, name,
                                             description,
                                             routeDistinguisher,
                                             rtImport, rtExport, cfgRtSet);

        DefaultVpnInstance vpnInstance1
                = new DefaultVpnInstance(id, name, description,
                                         routeDistinguisher, rtImport,
                                         rtExport, cfgRtSet);
        vpnInstance.equals(vpnInstance1);
        vpnInstance.toString();
        vpnInstance.hashCode();
    }

    @Test
    public void validateVpnInstanceConfig() throws Exception {
        assertEquals("d54d8f0e-81ad-424c-9fee-a1dfd8e47122",
                     vpnInstance.id().toString());
        assertEquals("vpninstance1", vpnInstance.description());
        //TODO: should put getter in vpninstance name class.
        //assertEquals("TestVPN1", vpnInstance.vpnInstanceName().toString());
        assertEquals("2000:2000",
                     vpnInstance.routeDistinguisher().getRouteDistinguisher());
        assertEquals("2000:2000",
                     vpnInstance.getExportRouteTargets()
                             .iterator().next().getRouteTarget());
    }
}