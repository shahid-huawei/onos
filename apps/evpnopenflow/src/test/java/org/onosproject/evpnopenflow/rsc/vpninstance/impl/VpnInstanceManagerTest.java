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


package org.onosproject.evpnopenflow.rsc.vpninstance.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.evpnopenflow.rsc.DefaultVpnAfConfig;
import org.onosproject.evpnopenflow.rsc.VpnAfConfig;
import org.onosproject.evpnopenflow.rsc.VpnInstance;
import org.onosproject.evpnopenflow.rsc.VpnInstanceId;
import org.onosproject.evpnopenflow.rsc.vpnafconfig.VpnAfConfigListener;
import org.onosproject.evpnopenflow.rsc.vpnafconfig.VpnAfConfigService;
import org.onosproject.incubator.net.routing.RouteAdminService;
import org.onosproject.incubator.net.routing.VpnRouteTarget;
import org.onosproject.store.service.TestStorageService;

import java.io.IOException;
import java.util.Collection;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

/**
 * Provides test implementation of class vpn instance manager.
 */
public class VpnInstanceManagerTest {

    private TestStorageService storageService = new TestStorageService();
    private MockCoreService coreService = new MockCoreService();
    private VpnInstanceManager vpnInstanceManager = new VpnInstanceManager();
    private VpnAfConfigService vpnAfConfigService;

    @Before
    public void startUp() throws TestUtils.TestUtilsException {
        vpnInstanceManager.coreService = coreService;
        vpnInstanceManager.storageService = storageService;
        setUpVpnAfcfgService();
        vpnInstanceManager.vpnAfConfigService = vpnAfConfigService;
        RouteAdminService routeAdminService
                = createNiceMock(RouteAdminService.class);
        replay(routeAdminService);
        vpnInstanceManager.routeService = routeAdminService;
        vpnInstanceManager.activate();
    }

    @Test
    public void setVpnInstance() {
        String json = "{\"action\":\"set\",\"node\":{\"key\":\"/proton/" +
                "net-l3vpn/VpnInstance\",\"dir\":true,\"nodes\":[{\"key\"" +
                ":\"/proton/net-l3vpn/VpnInstance/46befe74-27a8-4b3b" +
                "-a726-b35d98b9f476\"," +
                "\"value\":\"{\\\"description\\\": \\\"My Test VPN\\\"," +
                " \\\"route_distinguishers\\\": \\\"1000:1000\\\", \\\"" +
                "created_at\\\": \\\"2017-01-09 15:24:44.417362\\\", \\\"" +
                "updated_at\\\": \\\"2017-01-09 15:24:44.417362\\\", \\\"" +
                "name\\\": \\\"TestVPN1\\\", \\\"ipv6_family\\\"" +
                ": \\\"1000:1000\\\", \\\"id\\\": \\\"46befe74-27a8-4b3b-" +
                "a726-b35d98b9f476\\\", \\\"ipv4_family\\\": \\\"" +
                "1000:1000\\\"}\",\"modifiedIndex\":21,\"createdIndex\"" +
                ":21}],\"modifiedIndex\":5,\"createdIndex\":5}}";
        parseJsonMessage(json);

        boolean isExit = vpnInstanceManager.exists(VpnInstanceId.
                vpnInstanceId("46befe74-27a8-4b3b-a726-b35d98b9f476"));
        assertEquals(true, isExit);

        VpnInstance vpnInstance = vpnInstanceManager.getInstance(VpnInstanceId.
                vpnInstanceId("46befe74-27a8-4b3b-a726-b35d98b9f476"));
        assertEquals("46befe74-27a8-4b3b-a726-b35d98b9f476",
                     vpnInstance.id().vpnInstanceId());

        Collection<VpnInstance> instances = vpnInstanceManager.getInstances();
        for (VpnInstance instance : instances) {
            assertEquals("46befe74-27a8-4b3b-a726-b35d98b9f476",
                         instance.id().vpnInstanceId());
        }
        //update same vpn instance which is already created
        json = "{\"action\":\"update\",\"node\":{\"key\":\"/proton/" +
                "net-l3vpn/VpnInstance\",\"dir\":true,\"nodes\":[{\"key\"" +
                ":\"/proton/net-l3vpn/VpnInstance/46befe74-27a8-4b3b" +
                "-a726-b35d98b9f476\"," +
                "\"value\":\"{\\\"description\\\": \\\"My Test VPN\\\"," +
                " \\\"route_distinguishers\\\": \\\"1000:1000\\\", \\\"" +
                "created_at\\\": \\\"2017-01-09 15:24:44.417362\\\", \\\"" +
                "updated_at\\\": \\\"2017-01-09 15:24:44.417362\\\", \\\"" +
                "name\\\": \\\"TestVPN1\\\", \\\"ipv6_family\\\"" +
                ": \\\"1000:1000\\\", \\\"id\\\": \\\"46befe74-27a8-4b3b-" +
                "a726-b35d98b9f476\\\", \\\"ipv4_family\\\": \\\"" +
                "1000:1000\\\"}\",\"modifiedIndex\":21,\"createdIndex\"" +
                ":21}],\"modifiedIndex\":5,\"createdIndex\":5}}";
        parseJsonMessage(json);

        //update with different vpn instance id
        json = "{\"action\":\"update\",\"node\":{\"key\":\"/proton/" +
                "net-l3vpn/VpnInstance\",\"dir\":true,\"nodes\":[{\"key\"" +
                ":\"/proton/net-l3vpn/VpnInstance/46befe74-27a8-4b3b" +
                "-a726-b35d98b9f478\"," +
                "\"value\":\"{\\\"description\\\": \\\"My Test VPN\\\"," +
                " \\\"route_distinguishers\\\": \\\"1000:1000\\\", \\\"" +
                "created_at\\\": \\\"2017-01-09 15:24:44.417362\\\", \\\"" +
                "updated_at\\\": \\\"2017-01-09 15:24:44.417362\\\", \\\"" +
                "name\\\": \\\"TestVPN1\\\", \\\"ipv6_family\\\"" +
                ": \\\"1000:1000\\\", \\\"id\\\": \\\"46befe74-27a8-4b3b-" +
                "a726-b35d98b9f478\\\", \\\"ipv4_family\\\": \\\"" +
                "1000:1000\\\"}\",\"modifiedIndex\":21,\"createdIndex\"" +
                ":21}],\"modifiedIndex\":5,\"createdIndex\":5}}";
        parseJsonMessage(json);

    }

    private void parseJsonMessage(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonNode = mapper.readTree(json);
            String action = jsonNode.get("action").asText();
            JsonNode nodes = jsonNode.get("node").get("nodes");
            for (JsonNode confNode : nodes) {
                String key = confNode.get("key").asText();
                String value = confNode.get("value").asText();
                JsonNode modifyValue = mapper.readTree(value.replace("\\", ""));
                sendEtcdResponse(action, key, modifyValue);
            }
        } catch (IOException e) {
            System.out.println("Change etcd response into json with error {}");
        }
    }

    private void sendEtcdResponse(String action, String key, JsonNode value) {
        vpnInstanceManager.processGluonConfig(action, key, value);
    }

    @Test
    public void deleteVpnInstance() {
        String json = "{\"action\":\"delete\",\"node\":{\"key\":\"/proton/" +
                "net-l3vpn/VpnInstance\",\"dir\":true,\"nodes\":[{\"key\"" +
                ":\"/proton/net-l3vpn/VpnInstance/46befe74-27a8-4b3b" +
                "-a726-b35d98b9f476\"," +
                "\"value\":\"{\\\"description\\\": \\\"My Test VPN\\\"," +
                " \\\"route_distinguishers\\\": \\\"1000:1000\\\", \\\"" +
                "created_at\\\": \\\"2017-01-09 15:24:44.417362\\\", \\\"" +
                "updated_at\\\": \\\"2017-01-09 15:24:44.417362\\\", \\\"" +
                "name\\\": \\\"TestVPN1\\\", \\\"ipv6_family\\\"" +
                ": \\\"1000:1000\\\", \\\"id\\\": \\\"46befe74-27a8-4b3b-" +
                "a726-b35d98b9f476\\\", \\\"ipv4_family\\\": \\\"" +
                "1000:1000\\\"}\",\"modifiedIndex\":21,\"createdIndex\"" +
                ":21}],\"modifiedIndex\":5,\"createdIndex\":5}}";
        parseJsonMessage(json);
    }

    @After
    public void tearDown() {
        vpnInstanceManager.deactivate();
        vpnInstanceManager.coreService = null;
        vpnInstanceManager.storageService = null;
    }

    /**
     * Test host service that stores a reference to the host listener.
     */
    private class TestVpnAfConfigService implements VpnAfConfigService {

        @Override
        public boolean exists(VpnRouteTarget routeTarget) {
            return false;
        }

        @Override
        public VpnAfConfig getVpnAfConfig(VpnRouteTarget routeTarget) {
            String exportRoutePolicy = "abc";
            String importRoutePolicy = "def";
            routeTarget = VpnRouteTarget.routeTarget("2000:2000");
            String routeTargetType = "export_extcommunity";

            return new DefaultVpnAfConfig(exportRoutePolicy,
                                          importRoutePolicy,
                                          routeTarget,
                                          routeTargetType);
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
        public void processGluonConfig(String action,
                                       String key,
                                       JsonNode value) {
        }

        @Override
        public void addListener(VpnAfConfigListener listener) {
        }

        @Override
        public void removeListener(VpnAfConfigListener listener) {
        }
    }

    /**
     * Sets up the VPN Instance service with details of some hosts.
     */
    private void setUpVpnAfcfgService() {
        vpnAfConfigService = createMock(VpnAfConfigService.class);
        vpnAfConfigService.exists(anyObject(VpnRouteTarget.class));
        expectLastCall().andDelegateTo(new TestVpnAfConfigService()).anyTimes();
        vpnAfConfigService.getVpnAfConfig(anyObject(VpnRouteTarget.class));
        expectLastCall().andDelegateTo(new TestVpnAfConfigService()).anyTimes();
        replay(vpnAfConfigService);
    }

    private static class MockCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return new DefaultApplicationId(1, name);
        }
    }
}