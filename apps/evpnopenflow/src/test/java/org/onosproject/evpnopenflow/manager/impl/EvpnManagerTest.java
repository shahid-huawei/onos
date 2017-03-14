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

package org.onosproject.evpnopenflow.manager.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.evpnopenflow.rsc.DefaultVpnInstance;
import org.onosproject.evpnopenflow.rsc.DefaultVpnPort;
import org.onosproject.evpnopenflow.rsc.VpnAfConfig;
import org.onosproject.evpnopenflow.rsc.VpnInstance;
import org.onosproject.evpnopenflow.rsc.VpnInstanceId;
import org.onosproject.evpnopenflow.rsc.VpnPort;
import org.onosproject.evpnopenflow.rsc.VpnPortId;
import org.onosproject.evpnopenflow.rsc.vpnafconfig.VpnAfConfigListener;
import org.onosproject.evpnopenflow.rsc.vpnafconfig.VpnAfConfigService;
import org.onosproject.evpnopenflow.rsc.vpninstance.VpnInstanceService;
import org.onosproject.evpnopenflow.rsc.vpnport.VpnPortListener;
import org.onosproject.evpnopenflow.rsc.vpnport.VpnPortService;
import org.onosproject.incubator.net.resource.label.LabelResourceAdminService;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.resource.label.LabelResourcePool;
import org.onosproject.incubator.net.resource.label.LabelResourceService;
import org.onosproject.incubator.net.routing.EvpnInstanceName;
import org.onosproject.incubator.net.routing.EvpnInstanceRoute;
import org.onosproject.incubator.net.routing.EvpnRoute;
import org.onosproject.incubator.net.routing.EvpnRoute.Source;
import org.onosproject.incubator.net.routing.Label;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteAdminService;
import org.onosproject.incubator.net.routing.RouteDistinguisher;
import org.onosproject.incubator.net.routing.RouteEvent;
import org.onosproject.incubator.net.routing.RouteListener;
import org.onosproject.incubator.net.routing.RouteServiceAdapter;
import org.onosproject.incubator.net.routing.RouteTableType;
import org.onosproject.incubator.net.routing.VpnRouteTarget;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Annotations;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverAdapter;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverServiceAdapter;
import org.onosproject.net.flow.instructions.ExtensionPropertyException;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.HostServiceAdapter;
import org.onosproject.net.provider.ProviderId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.onosproject.net.Device.Type.SWITCH;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.Port.Type.COPPER;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Provides test implementation of class evpn manager.
 */
public class EvpnManagerTest {
    private MockCoreService coreService = new MockCoreService();
    private MockDeviceService deviceService = new MockDeviceService();
    private MockRouteService routeService = new MockRouteService();
    private TestDriverService driverService = new TestDriverService();

    private DeviceId id1 = NetTestTools.did("d1");
    private static final ProviderId PID = new ProviderId("of",
                                                         "foo");
    private static final DeviceId DID1 = deviceId("of:foo");
    private static final long SP1 = 1_000_000;

    private LabelResourceService labelRsrcService = new MockLabelResource();
    private final MockMasterShipService masterShipService
            = new MockMasterShipService();
    private MockConfigService configService = new MockConfigService();
    private EvpnManager evpnManager = new EvpnManager();

    private DefaultVpnPort vpnPort;
    private DefaultVpnInstance vpnInstance;
    private static final ConnectPoint CP1 = new ConnectPoint(
            DeviceId.deviceId("of:0000000000000001"),
            PortNumber.portNumber(1));

    private static final IpAddress V4_NEXT_HOP1
            = Ip4Address.valueOf("192.168.10.1");
    private static final IpAddress V4_NEXT_HOP2
            = Ip4Address.valueOf("192.168.20.1");

    private static final MacAddress MAC1
            = MacAddress.valueOf("00:00:00:00:00:01");
    private static final MacAddress MAC2
            = MacAddress.valueOf("00:00:00:00:00:02");

    private HostService hostService;
    private VpnInstanceService vpnInstanceService;
    private VpnAfConfigService vpnAfConfigService;
    private VpnPortService vpnPortService;
    private DeviceService deviceService1;
    private RouteListener routeListener;
    private HostListener hostListener;

    private Set<Host> availableHosts;
    private static final SparseAnnotations SA = DefaultAnnotations.builder()
            .set("ifaceid", "6ad5e893-4629-4876-aef2-f7ab9a44f218").build();
    private static final String DEVICE1 = "of:0000000000000001";
    private static final String UNKNOWN = "unknown";
    private static final String L3 = "192.168.1.1:8181";

    private Host host1;

    @Before
    public void startUp() throws Exception {
        evpnManager.coreService = coreService;
        setUpVpnInstanceService();
        evpnManager.vpnInstanceService = vpnInstanceService;
        setUpVpnPortService();
        evpnManager.vpnPortService = vpnPortService;
        setUpVpnAfConfigService();
        evpnManager.vpnAfConfigService = vpnAfConfigService;
        evpnManager.labelAdminService = new MockLabelResourceService();
        setUpHostService();
        evpnManager.hostService = hostService;
        evpnManager.labelService = labelRsrcService;
        evpnManager.deviceService = deviceService;
        setUpDeviceService();
        evpnManager.deviceService = deviceService1;
        evpnManager.deviceService = deviceService;
        setUpMasterShipService();
        evpnManager.mastershipService = masterShipService;
        evpnManager.configService = configService;
        evpnManager.routeService = routeService;

        RouteAdminService routeAdminService
                = createNiceMock(RouteAdminService.class);
        replay(routeAdminService);
        evpnManager.routeAdminService = routeAdminService;

        FlowObjectiveService flowObjectiveService
                = createNiceMock(FlowObjectiveService.class);
        replay(flowObjectiveService);
        evpnManager.flowObjectiveService = flowObjectiveService;

        evpnManager.driverService = driverService;

        evpnManager.activate();

        VpnInstanceId vpnInstanceId = VpnInstanceId
                .vpnInstanceId("d54d8f0e-81ad-424c-9fee-a1dfd8e47122");
        EvpnInstanceName name = EvpnInstanceName
                .evpnName("TestVPN1");
        String description = "vpninstance1";
        RouteDistinguisher routeDistinguisher = RouteDistinguisher
                .routeDistinguisher("2000:2000");

        Set<VpnRouteTarget> rtImport = new HashSet<>();
        rtImport.add(VpnRouteTarget.routeTarget("2000:2000"));

        Set<VpnRouteTarget> rtExport = new HashSet<>();
        rtExport.add(VpnRouteTarget.routeTarget("2000:2000"));

        Set<VpnRouteTarget> cfgRtSet = new HashSet<>();

        vpnInstance = new DefaultVpnInstance(vpnInstanceId, name,
                                             description,
                                             routeDistinguisher,
                                             rtImport,
                                             rtExport, cfgRtSet);

        VpnPortId vpnPortId = VpnPortId.vpnPortId("6ad5e893-4629-4876-aef2-f7ab9a44f218");
        VpnInstanceId vpnInstanceId1
                = VpnInstanceId
                .vpnInstanceId("46befe74-27a8-4b3b-a726-b35d98b9f476");

        vpnPort = new DefaultVpnPort(vpnPortId, vpnInstanceId1);
    }

    /**
     * Sets up the mastership service with details of master.
     */
    private void setUpMasterShipService() {
        MastershipService mastershipService1;
        mastershipService1 = createMock(MastershipService.class);
        mastershipService1.isLocalMaster(anyObject(DeviceId.class));
        expectLastCall().andDelegateTo(new MockMasterShipService()).anyTimes();
    }

    /**
     * Sets up the device service with details of device.
     */
    private void setUpDeviceService() {
        deviceService1 = createMock(DeviceService.class);
        deviceService1.getDevice(anyObject(DeviceId.class));
        expectLastCall().andDelegateTo(new MockDeviceService()).anyTimes();
    }

    /**
     * Sets up the VPN port service with details of some ports.
     */
    private void setUpVpnPortService() {
        vpnPortService = createMock(VpnPortService.class);
        vpnPortService.addListener(anyObject(VpnPortListener.class));
        expectLastCall().andDelegateTo(new TestVpnPortService()).anyTimes();
        vpnPortService.exists(anyObject(VpnPortId.class));
        expectLastCall().andDelegateTo(new TestVpnPortService()).anyTimes();
        vpnPortService.getPort(anyObject(VpnPortId.class));
        expectLastCall().andDelegateTo(new TestVpnPortService()).anyTimes();
        vpnPortService.removeListener(anyObject(VpnPortListener.class));
        expectLastCall().andDelegateTo(new TestVpnPortService()).anyTimes();
        replay(vpnPortService);
    }

    /**
     * Sets up the VPN af config service.
     */
    private void setUpVpnAfConfigService() {
        vpnAfConfigService = createMock(VpnAfConfigService.class);
        vpnAfConfigService.addListener(anyObject(VpnAfConfigListener.class));
        expectLastCall().andDelegateTo(new TestVpnAfConfigService()).anyTimes();
        vpnAfConfigService.removeListener(anyObject(VpnAfConfigListener.class));
        expectLastCall().andDelegateTo(new TestVpnAfConfigService()).anyTimes();
        replay(vpnAfConfigService);
    }

    /**
     * Sets up the VPN Instance service with details of some hosts.
     */
    private void setUpVpnInstanceService() {
        vpnInstanceService = createMock(VpnInstanceService.class);

        vpnInstanceService.exists(anyObject(VpnInstanceId.class));
        expectLastCall().andDelegateTo(new TestVpnInstanceService()).anyTimes();
        vpnInstanceService.getInstance(anyObject(VpnInstanceId.class));
        expectLastCall().andDelegateTo(new TestVpnInstanceService()).anyTimes();
        vpnInstanceService.getInstances();
        expectLastCall().andDelegateTo(new TestVpnInstanceService()).anyTimes();
        replay(vpnInstanceService);
    }

    /**
     * Sets up the host service with details of some hosts.
     */

    private void setUpHostService() {
        hostService = createMock(HostService.class);

        hostService.addListener(anyObject(HostListener.class));
        expectLastCall().andDelegateTo(new TestHostService()).anyTimes();

        hostService.removeListener(anyObject(HostListener.class));
        expectLastCall().andDelegateTo(new TestHostService()).anyTimes();

        hostService.getHosts();
        expectLastCall().andDelegateTo(new TestHostService()).anyTimes();

        hostService.getHostsByMac(MAC1);
        expectLastCall().andDelegateTo(new TestHostService()).anyTimes();

        hostService.getConnectedHosts(anyObject(DeviceId.class));
        expectLastCall().andDelegateTo(new TestHostService()).anyTimes();

        host1 = createHost(MAC1, V4_NEXT_HOP1);
        expectHost(host1);

        Host host2 = createHost(MAC2, V4_NEXT_HOP2);
        expectHost(host2);

        replay(hostService);

        availableHosts =
                ImmutableSet.of(host1,
                                host2);
    }

    /**
     * Sets expectations on the host service for a given host.
     *
     * @param host host
     */
    private void expectHost(Host host) {
        // Assume the host only has one IP address
        IpAddress ip = host.ipAddresses().iterator().next();

        expect(hostService.getHostsByIp(ip))
                .andReturn(Sets.newHashSet(host)).anyTimes();

        hostService.startMonitoringIp(ip);
        expectLastCall().anyTimes();
    }

    /**
     * Creates a host with the given parameters.
     *
     * @param macAddress MAC address
     * @param ipAddress  IP address
     * @return new host
     */
    private Host createHost(MacAddress macAddress, IpAddress ipAddress) {

        return new DefaultHost(ProviderId.NONE, HostId.NONE, macAddress,
                               VlanId.NONE, new HostLocation(CP1, 1),
                               Sets.newHashSet(ipAddress), SA);
    }

    @Test
    /**
     * Tests the vpn port set event.
     */
    public void onVpnPortSetTest() {
        //Device1
        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.CHANNEL_ID, L3);
        builder.set(AnnotationKeys.MANAGEMENT_ADDRESS, "12.1.1.1");
        addDevice(DEVICE1, builder);
        evpnManager.onVpnPortSet(vpnPort);
    }

    @Test
    /**
     * Tests the vpn port delete event.
     */
    public void onVpnPortSetDeleteTest() {
        host1 = createHost(MAC1, V4_NEXT_HOP1);
        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.CHANNEL_ID, L3);
        builder.set(AnnotationKeys.MANAGEMENT_ADDRESS, "12.1.1.1");
        addDevice(DEVICE1, builder);
        evpnManager.onVpnPortDelete(vpnPort);
    }

    @Test
    /**
     * Tests the host add event.
     */
    public void onHostDetectedTest() {
        //Device1
        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.CHANNEL_ID, L3);
        builder.set(AnnotationKeys.MANAGEMENT_ADDRESS, "12.1.1.1");
        addDevice(DEVICE1, builder);
        //evpnManager.onHostDetected(host1);
        hostListener.event(new HostEvent(HostEvent.Type.HOST_ADDED, host1));
    }

    @Test
    /**
     * Tests the host remove event.
     */
    public void onHostVanishedTest() {
        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.CHANNEL_ID, L3);
        builder.set(AnnotationKeys.MANAGEMENT_ADDRESS, "12.1.1.1");
        addDevice(DEVICE1, builder);
        //evpnManager.onHostVanished(host1);
        hostListener.event(new HostEvent(HostEvent.Type.HOST_REMOVED, host1));
    }

    /**
     * Test about route learned from route subsystem.
     */
    @Test
    public void routeEventUpdateTest1() {
        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.CHANNEL_ID, L3);
        builder.set(AnnotationKeys.MANAGEMENT_ADDRESS, "12.1.1.1");
        addDevice(DEVICE1, builder);
        //prepare the route and send the event.
        List<VpnRouteTarget> rtImport = new LinkedList<>();
        rtImport.add(VpnRouteTarget.routeTarget("2000:2000"));

        List<VpnRouteTarget> rtExport = new LinkedList<>();
        rtExport.add(VpnRouteTarget.routeTarget("2000:2000"));

        EvpnRoute evpnroute
                = new EvpnRoute(Source.REMOTE,
                                MacAddress.valueOf("00:00:00:00:00:01"),
                                IpPrefix.valueOf(Ip4Address.valueOf("192.168" +
                                                                            "" +
                                                                            "" +
                                                                            "" +
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
     * Test about route learned from route subsystem.
     */
    @Test
    public void routeEventUpdateTest2() {
        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.CHANNEL_ID, L3);
        builder.set(AnnotationKeys.MANAGEMENT_ADDRESS, "12.1.1.1");
        addDevice(DEVICE1, builder);
        //prepare the route and send the event.
        List<VpnRouteTarget> rtImport = new LinkedList<>();
        rtImport.add(VpnRouteTarget.routeTarget("2000:2000"));

        List<VpnRouteTarget> rtExport = new LinkedList<>();
        rtExport.add(VpnRouteTarget.routeTarget("2000:2000"));

        EvpnRoute evpnroute
                = new EvpnRoute(Source.REMOTE,
                                MacAddress.valueOf("00:00:00:00:00:01"),
                                IpPrefix.valueOf(Ip4Address.valueOf("192.168" +
                                                                            "" +
                                                                            "" +
                                                                            "" +
                                                                            ".10.1"), 32),
                                Ip4Address.valueOf("192.168.10.2"),
                                RouteDistinguisher
                                        .routeDistinguisher("1:1"),
                                rtImport,
                                rtExport,
                                Label.label(1));

        routeListener.event(new RouteEvent(RouteEvent.Type.ROUTE_REMOVED,
                                           evpnroute));
    }


    @After
    public void tearDown() {
        evpnManager.deactivate();
        evpnManager.coreService = null;
        evpnManager.hostService = null;
        evpnManager.vpnPortService = null;
        evpnManager.configService = null;
        evpnManager.routeAdminService = null;
        evpnManager.routeService = null;
    }

    public class MockRouteService extends RouteServiceAdapter {
        @Override
        public void addListener(RouteListener listener) {
            EvpnManagerTest.this.routeListener = listener;
        }

        @Override
        public Map<RouteTableType, Collection<Route>> getAllRoutes() {
            Map<RouteTableType, Collection<Route>> routeMap
                    = new ConcurrentHashMap<>();

            List<VpnRouteTarget> rtImport = new LinkedList<>();
            rtImport.add(VpnRouteTarget.routeTarget("2000:2000"));

            List<VpnRouteTarget> rtExport = new LinkedList<>();
            rtExport.add(VpnRouteTarget.routeTarget("2000:2000"));

            List<Route> routes = new LinkedList<>();
            EvpnRoute evpnroute
                    = new EvpnRoute(Source.LOCAL,
                                    MacAddress.valueOf("00:00:00:00:00:01"),
                                    IpPrefix.valueOf(Ip4Address.valueOf("192.168" +
                                                                                "" +
                                                                                "" +
                                                                                "" +
                                                                                ".10.1"), 32),
                                    Ip4Address.valueOf("192.168.10.2"),
                                    RouteDistinguisher
                                            .routeDistinguisher("1:1"),
                                    rtImport,
                                    rtExport,
                                    Label.label(1));
            EvpnInstanceRoute evpnInstanceRoute
                    = new EvpnInstanceRoute(EvpnInstanceName.evpnName("TestVPN1"),
                                            RouteDistinguisher
                                                    .routeDistinguisher("1:1"),
                                            rtImport,
                                            rtExport,
                                            null,
                                            null,
                                            IpPrefix.valueOf(Ip4Address.valueOf("192.168" +
                                                                                        "" +
                                                                                        "" +
                                                                                        "" +
                                                                                        ".10.1"), 32),
                                            Ip4Address.valueOf("192.168.10.2"),
                                            Label.label(1));

            routes.add(evpnroute);
            routes.add(evpnInstanceRoute);
            routeMap.put(RouteTableType.EVPN_IPV4, routes);
            return routeMap;
        }
    }

    public class TestDriverService extends DriverServiceAdapter {
        @Override
        public DriverHandler createHandler(DeviceId deviceId,
                                           String... credentials) {
            Driver driver = new TestDriver();

            return new MockDefaultDriverHandler(new DefaultDriverData(driver,
                                                                      id1));
        }
    }

    public final class MockDefaultDriverHandler implements DriverHandler {

        private final DriverData data;

        /**
         * Creates new driver handler with the attached driver data.
         *
         * @param data driver data to attach
         */

        private MockDefaultDriverHandler(DriverData data) {
            this.data = data;
        }

        @Override
        public Driver driver() {
            return data.driver();
        }

        @Override
        public DriverData data() {
            return data;
        }

        @Override
        public <T extends Behaviour> T behaviour(Class<T> behaviourClass) {
            if (behaviourClass == ExtensionTreatmentResolver.class) {
                return (T) new MockExtensionTreatmentResolver();
            }
            return null;
        }

        @Override
        public <T> T get(Class<T> serviceClass) {
            return null;
        }

        @Override
        public String toString() {
            return toStringHelper(this).add("data", data).toString();
        }

    }

    public class TestDriver extends DriverAdapter {

        @Override
        public boolean hasBehaviour(Class<? extends Behaviour> behaviourClass) {
            return true;
        }
    }


    public class MockExtensionTreatmentResolver
            implements ExtensionTreatmentResolver {

        @Override
        public DriverHandler handler() {
            return null;
        }

        @Override
        public void setHandler(DriverHandler handler) {
        }

        @Override
        public DriverData data() {
            return null;
        }

        @Override
        public void setData(DriverData data) {
        }

        @Override
        public ExtensionTreatment getExtensionInstruction(ExtensionTreatmentType type) {
            return new MockExtensionTreatment(type);
        }

    }

    public final class MockExtensionTreatment implements ExtensionTreatment {

        private ExtensionTreatmentType type;

        private MockExtensionTreatment(ExtensionTreatmentType type) {
            this.type = type;
        }

        @Override
        public <T> void setPropertyValue(String key, T value)
                throws ExtensionPropertyException {
        }

        @Override
        public <T> T getPropertyValue(String key)
                throws ExtensionPropertyException {
            return null;
        }

        @Override
        public List<String> getProperties() {
            return null;
        }

        @Override
        public byte[] serialize() {
            return null;
        }

        @Override
        public void deserialize(byte[] data) {
        }

        @Override
        public ExtensionTreatmentType type() {
            return type;
        }

    }

    private static class MockCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return new DefaultApplicationId(1, name);
        }
    }

    private class MockLabelResourceService implements LabelResourceAdminService {

        Map<DeviceId, LabelResourcePool> resourcePool = new HashMap<>();

        @Override
        public boolean createDevicePool(DeviceId deviceId,
                                        LabelResourceId beginLabel,
                                        LabelResourceId endLabel) {
            LabelResourcePool labelResource
                    = new LabelResourcePool(deviceId.toString(),
                                            beginLabel.labelId(),
                                            endLabel.labelId());
            if (resourcePool.containsValue(labelResource)) {
                return false;
            }

            resourcePool.put(deviceId, labelResource);
            return true;
        }

        @Override
        public boolean createGlobalPool(LabelResourceId beginLabel,
                                        LabelResourceId endLabel) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean destroyDevicePool(DeviceId deviceId) {
            LabelResourcePool devicePool = resourcePool.get(deviceId);

            if (devicePool == null) {
                return false;
            }

            resourcePool.remove(deviceId);
            return true;
        }

        @Override
        public boolean destroyGlobalPool() {
            // TODO Auto-generated method stub
            return false;
        }
    }

    /**
     * Test host service that stores a reference to the host listener.
     */
    private class TestHostService extends HostServiceAdapter {
        @Override
        public void addListener(HostListener listener) {
            EvpnManagerTest.this.hostListener = listener;
        }

        @Override
        public Iterable<Host> getHosts() {
            return availableHosts;
        }

        @Override
        public void removeListener(HostListener listener) {
        }

        @Override
        public Set<Host> getHostsByMac(MacAddress mac) {
            if (mac.equals(MacAddress.valueOf("00:00:00:00:00:01"))) {
                return new HashSet<>();
            }
            return availableHosts;
        }

        @Override
        public Set<Host> getConnectedHosts(DeviceId deviceId) {
            return ImmutableSet.of(host1);
        }
    }

    /**
     * Test host service that stores a reference to the host listener.
     */
    private class TestVpnInstanceService implements VpnInstanceService {
        protected Map<VpnInstanceId, VpnInstance> vpnInstanceStore = new HashMap<>();

        @Override
        public boolean exists(VpnInstanceId vpnInstanceId) {
            return true;
        }

        @Override
        public VpnInstance getInstance(VpnInstanceId vpnInstanceId) {
            return vpnInstance;
        }

        @Override
        public Collection<VpnInstance> getInstances() {
            vpnInstanceStore.put(VpnInstanceId
                                         .vpnInstanceId("d54d8f0e-81ad-424c-9fee-a1dfd8e47122"),
                                 vpnInstance);
            return Collections.unmodifiableCollection(vpnInstanceStore.values());
        }

        @Override
        public boolean createInstances(Iterable<VpnInstance> vpnInstances) {
            return false;
        }

        @Override
        public boolean updateInstances(Iterable<VpnInstance> vpnInstances) {
            return false;
        }

        @Override
        public boolean removeInstances(Iterable<VpnInstanceId> vpnInstanceIds) {
            return false;
        }

        @Override
        public void processGluonConfig(String action, String key,
                                       JsonNode value) {

        }

        @Override
        public void updateImpExpRouteTargets(String routeTargetType,
                                             Set<VpnRouteTarget> exportRouteTargets,
                                             Set<VpnRouteTarget> importRouteTargets,
                                             VpnRouteTarget vpnRouteTarget) {

        }
    }

    /**
     * Test vpn af config service that stores a reference to the af config
     * listener.
     */
    private class TestVpnAfConfigService implements VpnAfConfigService {

        @Override
        public boolean exists(VpnRouteTarget routeTarget) {
            return true;
        }

        @Override
        public VpnAfConfig getVpnAfConfig(VpnRouteTarget routeTarget) {
            return null;
        }

        @Override
        public Collection<VpnAfConfig> getVpnAfConfigs() {
            return null;
        }

        @Override
        public boolean createVpnAfConfigs(Iterable<VpnAfConfig> vpnAfConfigs) {
            return false;
        }

        @Override
        public boolean updateVpnAfConfigs(Iterable<VpnAfConfig> vpnAfConfigs) {
            return false;
        }

        @Override
        public boolean removeVpnAfConfigs(Iterable<VpnRouteTarget> routeTarget) {
            return false;
        }

        @Override
        public void processGluonConfig(String action, String key, JsonNode value) {
        }

        @Override
        public void addListener(VpnAfConfigListener listener) {
        }

        @Override
        public void removeListener(VpnAfConfigListener listener) {
        }
    }

    /**
     * Test vpn port service that stores a reference to the vpn port listener.
     */
    private class TestVpnPortService implements VpnPortService {

        @Override
        public boolean exists(VpnPortId vpnPortId) {
            return true;
        }

        @Override
        public VpnPort getPort(VpnPortId vpnPortId) {
            return vpnPort;
        }

        @Override
        public Collection<VpnPort> getPorts() {
            return null;
        }

        @Override
        public boolean createPorts(Iterable<VpnPort> vpnPorts) {
            return false;
        }

        @Override
        public boolean updatePorts(Iterable<VpnPort> vpnPorts) {
            return false;
        }

        @Override
        public boolean removePorts(Iterable<VpnPortId> vpnPortIds) {
            return false;
        }

        @Override
        public void processGluonConfig(String action, String key,
                                       JsonNode value) {
        }

        @Override
        public void addListener(VpnPortListener listener) {
        }

        @Override
        public void removeListener(VpnPortListener listener) {
        }
    }

    /**
     * Test device service that stores a reference to the device.
     */
    private class MockDeviceService extends DeviceServiceAdapter {
        List<Device> devices = new LinkedList<>();
        List<Device> swDevices = new LinkedList<>();

        private void addDevice(Device dev) {
            devices.add(dev);
        }

        @Override
        public Iterable<Device> getAvailableDevices(Device.Type type) {
            Device device
                    = new DefaultDevice(PID, DID1,
                                        SWITCH, "m",
                                        "h", "s",
                                        "n", new ChassisId());
            swDevices.add(device);
            return swDevices;
        }

        @Override
        public Device getDevice(DeviceId deviceId) {
            for (Device dev : devices) {
                if (dev.id().equals(deviceId)) {
                    return dev;
                }
            }
            return null;
        }

        @Override
        public Iterable<Device> getAvailableDevices() {
            return devices;
        }

        @Override
        public List<Port> getPorts(DeviceId deviceId) {
            List<Port> ports = new ArrayList<>();
            Device device
                    = new DefaultDevice(PID, DID1,
                                        SWITCH, "m",
                                        "h", "s",
                                        "n", new ChassisId());

            Annotations annotations
                    = DefaultAnnotations.builder()
                    .set(AnnotationKeys.PORT_NAME, "vxlan-0.0.0.0").build();
            Port p1 = new DefaultPort(device, portNumber(1),
                                      true, COPPER,
                                      SP1, annotations);
            ports.add(p1);
            return ports;
        }
    }

    private void addDevice(String device, DefaultAnnotations.Builder builder) {
        deviceService
                .addDevice(new DefaultDevice(ProviderId.NONE,
                                             deviceId(device),
                                             Device.Type.ROUTER,
                                             UNKNOWN, UNKNOWN, UNKNOWN,
                                             UNKNOWN, new ChassisId(),
                                             builder.build()));
    }

    private class MockMasterShipService extends MastershipServiceAdapter {
        boolean set;

        @Override
        public MastershipRole getLocalRole(DeviceId deviceId) {
            return set ? MastershipRole.MASTER : MastershipRole.STANDBY;
        }

        @Override
        public boolean isLocalMaster(DeviceId deviceId) {
            return true;
        }
    }

    private static class MockConfigService
            extends NetworkConfigRegistryAdapter {
    }
}