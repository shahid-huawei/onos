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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test for BgpEvpnNlriVer4 attributes.
 */
public class BgpEvpnNlriImplTest {

    @Test
    public void testRead() throws Exception {
        BgpEvpnNlriImpl bgpEvpnNlriVer4Obj1 = new BgpEvpnNlriImpl();
        assertThat(bgpEvpnNlriVer4Obj1.getRouteType(),
                   is(BgpEvpnRouteType.MAC_IP_ADVERTISEMENT));
        BgpEvpnNlriImpl bgpEvpnNlriVer4Obj2 = new BgpEvpnNlriImpl((byte) 3,
                                                                  null);
        assertThat(bgpEvpnNlriVer4Obj2.getRouteType(),
                   is(BgpEvpnRouteType.INCLUSIVE_MULTICASE_ETHERNET));
    }
}