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

import static org.junit.Assert.assertEquals;

/**
 * Provides test implementation of class default vpn port.
 */
public class DefaultVpnPortTest {
    private DefaultVpnPort vpnPort;

    @Before
    public void setUp() throws Exception {
        VpnPortId id
                = VpnPortId.vpnPortId("6ad5e893-4629-4876-aef2-f7ab9a44f218");
        VpnInstanceId vpnInstanceId
                = VpnInstanceId
                .vpnInstanceId("46befe74-27a8-4b3b-a726-b35d98b9f476");

        vpnPort = new DefaultVpnPort(id, vpnInstanceId);
        DefaultVpnPort vpnPort1 = new DefaultVpnPort(id, vpnInstanceId);
        vpnPort.equals(vpnPort1);
        vpnPort.toString();
        vpnPort.hashCode();
    }

    @Test
    public void validateVpnPortConfig() throws Exception {
        assertEquals("6ad5e893-4629-4876-aef2-f7ab9a44f218",
                     vpnPort.id().toString());
        assertEquals("46befe74-27a8-4b3b-a726-b35d98b9f476", vpnPort
                .vpnInstanceId().toString());
    }
}