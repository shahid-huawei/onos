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

package org.onosproject.bgpio.protocol.evpn;

import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.bgpio.types.BgpEvpnEsi;
import org.onosproject.bgpio.types.BgpEvpnLabel;
import org.onosproject.bgpio.types.RouteDistinguisher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test for BgpMacIpAdvNlriVer4Test attributes.
 */
public class BgpRouteType2NlriTest {

    @Test
    public void testRead() throws Exception {

        RouteDistinguisher rd = new RouteDistinguisher(2);
        byte[] esi = {2};
        BgpEvpnEsi esiObj = new BgpEvpnEsi(esi);
        byte[] mpls = {2};
        BgpEvpnLabel mplsObj = new BgpEvpnLabel(mpls);
        BgpEvpnRouteType2Nlri bgpMacIpAdvNlriVer4Obj2
                = new BgpEvpnRouteType2Nlri(rd, esiObj, 2,
                                            MacAddress.valueOf(1),
                                            IpAddress.valueOf("1.2.3.0")
                                                    .toInetAddress(),
                                            mplsObj, mplsObj);
        assertThat(bgpMacIpAdvNlriVer4Obj2.getRouteDistinguisher()
                           .getRouteDistinguisher(), is((long) 2));
        assertThat(bgpMacIpAdvNlriVer4Obj2.getEthernetSegmentidentifier()
                           .getEthernetSegmentidentifier(), is(esi));
        assertThat(bgpMacIpAdvNlriVer4Obj2.getMacAddress(),
                   is(MacAddress.valueOf(1)));
        assertThat(bgpMacIpAdvNlriVer4Obj2.getIpAddress(),
                   is(IpAddress.valueOf("1.2.3.0").toInetAddress()));
        assertThat(bgpMacIpAdvNlriVer4Obj2.getMplsLable1().getMplsLabel(),
                   is(mpls));
        assertThat(bgpMacIpAdvNlriVer4Obj2.getMplsLable2().getMplsLabel(),
                   is(mpls));
    }
}