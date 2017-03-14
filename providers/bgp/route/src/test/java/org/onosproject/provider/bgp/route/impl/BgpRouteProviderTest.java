/*
 * Copyright 2017-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.onosproject.provider.bgp.route.impl;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils.TestUtilsException;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.bgp.controller.BgpId;
import org.onosproject.bgp.controller.BgpPeer;
import org.onosproject.bgp.controller.BgpRouteListener;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.BgpFactories;
import org.onosproject.bgpio.protocol.BgpMessage;
import org.onosproject.bgpio.protocol.BgpMessageReader;
import org.onosproject.bgpio.protocol.BgpUpdateMsg;
import org.onosproject.bgpio.types.BgpHeader;
import org.onosproject.incubator.net.routing.EvpnRoute;
import org.onosproject.incubator.net.routing.EvpnRoute.Source;
import org.onosproject.incubator.net.routing.Label;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteDistinguisher;
import org.onosproject.incubator.net.routing.RouteEvent;
import org.onosproject.incubator.net.routing.RouteListener;
import org.onosproject.incubator.net.routing.RouteServiceAdapter;
import org.onosproject.incubator.net.routing.RouteTableType;
import org.onosproject.incubator.net.routing.VpnRouteTarget;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;

/**
 * Test for BGP route provider.
 */
public class BgpRouteProviderTest {
    private final MockBgpController controller = new MockBgpController();
    private final TestRouteService routeService = new TestRouteService();
    private final TestRouteAdminService routeAdminService = new
            TestRouteAdminService();

    private RouteListener routeListener;
    private BgpRouteProvider bgpRouteProvider;
    List<Route> route = new LinkedList<>();

    @Before
    public void startUp() throws TestUtilsException {
        bgpRouteProvider = new BgpRouteProvider();
        bgpRouteProvider.routeService = routeService;
        bgpRouteProvider.controller = controller;
        bgpRouteProvider.routeAdminService = routeAdminService;
        bgpRouteProvider.activate();
    }

    @After
    public void tearDown() {
        bgpRouteProvider.routeService = null;
    }

    private class TestRouteService extends RouteServiceAdapter {
        @Override
        public void addListener(RouteListener routeListener) {
            BgpRouteProviderTest.this.routeListener = routeListener;
        }

        @Override
        public void update(Collection<Route> routes) {
            route.add(routes.iterator().next());
        }

        @Override
        public void withdraw(Collection<Route> routes) {
            route.remove(routes.iterator().next());
        }

        @Override
        public Map<RouteTableType, Collection<Route>> getAllRoutes() {
            Map<RouteTableType, Collection<Route>> routes = new HashMap<>();
            if (route.isEmpty()) {
                return routes;
            }
            routes.put(RouteTableType.EVPN_IPV4, route);
            return routes;
        }
    }

    private class TestRouteAdminService extends RouteServiceAdapter {

        @Override
        public void update(Collection<Route> routes) {
            route.add(routes.iterator().next());
        }

        @Override
        public void withdraw(Collection<Route> routes) {
            route.remove(routes.iterator().next());
        }
    }

    /* Test class for BGP controller */
    private class MockBgpController extends BgpRouteControllerAdapter {

        protected Set<BgpRouteListener> bgpRouteListener = new
                CopyOnWriteArraySet<>();

        @Override
        public void addRouteListener(BgpRouteListener listener) {
            this.bgpRouteListener.add(listener);
        }

        @Override
        public Iterable<BgpPeer> getPeers() {
            ConcurrentHashMap<BgpId, BgpPeer> connectedPeers
                    = new ConcurrentHashMap<>();
            return connectedPeers.values();
        }

        @Override
        public void removeRouteListener(BgpRouteListener listener) {
            this.bgpRouteListener.remove(listener);
        }

        @Override
        public Set<BgpRouteListener> routeListener() {
            return bgpRouteListener;
        }
    }

    /**
     * Validate route is added to the route subsystem and received a event
     * from route subsystem.
     */
    @Test
    public void bgpRouteProviderTest1() {
        //prepare the route and send the event.
        List<VpnRouteTarget> rtImport = new LinkedList<>();
        rtImport.add(VpnRouteTarget.routeTarget("2000:2000"));

        List<VpnRouteTarget> rtExport = new LinkedList<>();
        rtExport.add(VpnRouteTarget.routeTarget("2000:2000"));

        EvpnRoute evpnroute
                = new EvpnRoute(Source.LOCAL,
                                MacAddress.valueOf("00:00:00:00:00:01"),
                                IpPrefix
                                        .valueOf(Ip4Address.valueOf("192.168" +
                                                                            ".10.1"), 32),
                                Ip4Address.valueOf("192.168.10.2"),
                                RouteDistinguisher
                                        .routeDistinguisher("1:1"),
                                rtImport,
                                rtExport,
                                Label.label(1));

        routeListener.event(new RouteEvent(RouteEvent.Type.ROUTE_ADDED,
                                           evpnroute));
    }

    /**
     * MP reach bgp update packet received by route provider and check the
     * route subsystem validation.
     *
     * @throws BgpParseException
     */
    @Test
    public void bgpRouteProviderTest2() throws BgpParseException {
        byte[] updateMsg = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x60, //length
                0x02, //packet type(update)
                0x00, 0x00, //withdrawn len
                0x00, 0x49, //path attribute len
                (byte) 0x40, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med -> 7
                (byte) 0x90, 0x0e, 0x00, 0x33, 0x00, 0x19, 0x46, //mpreach with
                // safi = 70
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                //EVPN NLRI START
                0x02, 0x28, //route type and length
                0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, //RD
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, //ESI
                0x00, 0x00, 0x00, 0x03, //Ethernet tag ID
                0x30, //mac address length
                0x08, 0x00, 0x00, 0x00, 0x00, 0x04, //mac address
                0x20, //Ip address length
                0x01, 0x02, 0x03, 0x04, //IP address
                0x01, 0x02, 0x06, //MPLS label 1
                0x01, 0x01, 0x07};  //MPLS Lable2//EVPN NLRI END

        BgpId bgpId = new BgpId(IpAddress.valueOf("127.0.0.9"));
        byte[] testUpdateMsg;
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);
        assertThat(message, instanceOf(BgpUpdateMsg.class));

        BgpUpdateMsg bgpUpdateMsg = (BgpUpdateMsg) message;


        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testUpdateMsg = new byte[readLen];
        buf.readBytes(testUpdateMsg, 0, readLen);


        for (BgpRouteListener l : controller.routeListener()) {
            l.processRoute(bgpId, bgpUpdateMsg);
            assertThat(testUpdateMsg, is(updateMsg));
        }
    }

    /**
     * MP un reach bgp update packet received by route provider and check the
     * route subsystem validation.
     *
     * @throws BgpParseException
     */
    @Test
    public void bgpRouteProviderTest3() throws BgpParseException {
        byte[] updateMsg = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x5A, //length
                0x02, //packet type(update)
                0x00, 0x00, //withdrawn len
                0x00, 0x43, //path attribute len
                (byte) 0x40, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med

                (byte) 0x90, 0x0f, 0x00, 0x2D, 0x00, 0x19, 0x46, //mp un reach
                // with
                // safi = 70
                //EVPN NLRI START
                0x02, 0x28, //route type and length
                0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, //RD
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, //ESI
                0x00, 0x00, 0x00, 0x03, //Ethernet tag ID
                0x30, //mac address length
                0x08, 0x00, 0x00, 0x00, 0x00, 0x04, //mac address
                0x20, //Ip address length
                0x01, 0x02, 0x03, 0x04, //IP address
                0x01, 0x02, 0x06, //MPLS label 1
                0x01, 0x01, 0x07};  //MPLS Lable2//EVPN NLRI END

        BgpId bgpId = new BgpId(IpAddress.valueOf("127.0.0.9"));
        byte[] testUpdateMsg;
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);
        assertThat(message, instanceOf(BgpUpdateMsg.class));

        BgpUpdateMsg bgpUpdateMsg = (BgpUpdateMsg) message;

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testUpdateMsg = new byte[readLen];
        buf.readBytes(testUpdateMsg, 0, readLen);

        for (BgpRouteListener l : controller.routeListener()) {
            l.processRoute(bgpId, bgpUpdateMsg);
            assertThat(testUpdateMsg, is(updateMsg));
        }
    }
}
