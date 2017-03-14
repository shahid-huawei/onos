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

package org.onosproject.evpnopenflow.rsc.vpnafconfig.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.evpnopenflow.rsc.VpnAfConfig;
import org.onosproject.evpnopenflow.rsc.vpnafconfig.VpnAfConfigEvent;
import org.onosproject.evpnopenflow.rsc.vpnafconfig.VpnAfConfigListener;
import org.onosproject.incubator.net.routing.VpnRouteTarget;
import org.onosproject.store.service.TestStorageService;

import java.io.IOException;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * Provides test implementation of class vpn af config manager.
 */
public class VpnAfConfigManagerTest {
    private TestStorageService storageService = new TestStorageService();
    private MockCoreService coreService = new MockCoreService();
    private VpnAfConfigManager vpnAfConfigManager = new VpnAfConfigManager();

    @Before
    public void startUp() throws TestUtils.TestUtilsException {
        vpnAfConfigManager.coreService = coreService;
        vpnAfConfigManager.storageService = storageService;
        vpnAfConfigManager.activate();
        vpnAfConfigManager.addListener(new MockInnerVpnAfConfigListener());
        vpnAfConfigManager.removeListener(new MockInnerVpnAfConfigListener());
    }

    @Test
    public void setVpnAfConfig() {

        String vpnAfConfigGet = "{\"action\":\"set\"," +
                "\"node\":{\"key\":\"/proton/net-l3vpn/VpnAfConfig\"," +
                "\"dir\":true,\"nodes\":[{\"key\":" +
                "\"/proton/net-l3vpn/VpnAfConfig/1000:1000\"," +
                "\"value\":\"{\\\"vrf_rt_type\\\": \\\"both\\\", " +
                "\\\"export_route_policy\\\": null, " +
                "\\\"updated_at\\\": \\\"2017-01-09 15:24:36.324669\\\", " +
                "\\\"created_at\\\": \\\"2017-01-09 15:24:36.324669\\\", " +
                "\\\"import_route_policy\\\": null, " +
                "\\\"vrf_rt_value\\\": \\\"1000:1000\\\"}\"," +
                "\"modifiedIndex\":20,\"createdIndex\":20}," +
                "{\"key\":\"/proton/net-l3vpn/VpnAfConfig/4000:4000\"," +
                "\"value\":\"{\\\"vrf_rt_type\\\": " +
                "\\\"export_extcommunity\\\", " +
                "\\\"export_route_policy\\\": \\\"abc\\\", " +
                "\\\"updated_at\\\": \\\"2017-01-20 15:13:38.235924\\\", " +
                "\\\"created_at\\\": \\\"2017-01-20 15:13:38.235924\\\", " +
                "\\\"import_route_policy\\\": \\\"def\\\", " +
                "\\\"vrf_rt_value\\\": \\\"1000:1000\\\"}\"," +
                "\"modifiedIndex\":67,\"createdIndex\":67}]," +
                "\"modifiedIndex\":4,\"createdIndex\":4}}";
        parseJsonMessage(vpnAfConfigGet);

        boolean isExit = vpnAfConfigManager
                .exists(VpnRouteTarget.routeTarget("1000:1000"));
        assertEquals(true, isExit);
        isExit = vpnAfConfigManager
                .exists(VpnRouteTarget.routeTarget("1000:1000"));
        assertEquals(true, isExit);
        VpnAfConfig vpnAfConfig = vpnAfConfigManager
                .getVpnAfConfig(VpnRouteTarget.routeTarget("1000:1000"));
        assertEquals("1000:1000",
                     vpnAfConfig.routeTarget().getRouteTarget());
        Collection<VpnAfConfig> vpnAfConfigs
                = vpnAfConfigManager.getVpnAfConfigs();
        for (VpnAfConfig vpnAfConfig1 : vpnAfConfigs) {
            assertEquals("1000:1000",
                         vpnAfConfig1.routeTarget().getRouteTarget());
        }

        vpnAfConfigGet = "{\"action\":\"update\"," +
                "\"node\":{\"key\":\"/proton/net-l3vpn/VpnAfConfig\"," +
                "\"dir\":true,\"nodes\":[{\"key\":" +
                "\"/proton/net-l3vpn/VpnAfConfig/1000:1000\"," +
                "\"value\":\"{\\\"vrf_rt_type\\\": \\\"both\\\", " +
                "\\\"export_route_policy\\\": null, " +
                "\\\"updated_at\\\": \\\"2017-01-09 15:24:36.324669\\\", " +
                "\\\"created_at\\\": \\\"2017-01-09 15:24:36.324669\\\", " +
                "\\\"import_route_policy\\\": null, " +
                "\\\"vrf_rt_value\\\": \\\"1000:1000\\\"}\"," +
                "\"modifiedIndex\":20,\"createdIndex\":20}," +
                "{\"key\":\"/proton/net-l3vpn/VpnAfConfig/4000:4000\"," +
                "\"value\":\"{\\\"vrf_rt_type\\\": " +
                "\\\"export_extcommunity\\\", " +
                "\\\"export_route_policy\\\": \\\"abc\\\", " +
                "\\\"updated_at\\\": \\\"2017-01-20 15:13:38.235924\\\", " +
                "\\\"created_at\\\": \\\"2017-01-20 15:13:38.235924\\\", " +
                "\\\"import_route_policy\\\": \\\"def\\\", " +
                "\\\"vrf_rt_value\\\": \\\"4000:4000\\\"}\"," +
                "\"modifiedIndex\":67,\"createdIndex\":67}]," +
                "\"modifiedIndex\":4,\"createdIndex\":4}}";
        parseJsonMessage(vpnAfConfigGet);

        vpnAfConfigGet = "{\"action\":\"update\"," +
                "\"node\":{\"key\":\"/proton/net-l3vpn/VpnAfConfig\"," +
                "\"dir\":true,\"nodes\":[{\"key\":" +
                "\"/proton/net-l3vpn/VpnAfConfig/2000:2000\"," +
                "\"value\":\"{\\\"vrf_rt_type\\\": \\\"both\\\", " +
                "\\\"export_route_policy\\\": null, " +
                "\\\"updated_at\\\": \\\"2017-01-09 15:24:36.324669\\\", " +
                "\\\"created_at\\\": \\\"2017-01-09 15:24:36.324669\\\", " +
                "\\\"import_route_policy\\\": null, " +
                "\\\"vrf_rt_value\\\": \\\"2000:2000\\\"}\"," +
                "\"modifiedIndex\":20,\"createdIndex\":20}}";
        parseJsonMessage(vpnAfConfigGet);
    }

    @Test
    public void deleteVpnPort() {
        String vpnAfConfigGet = "{\"action\":\"delete\"," +
                "\"node\":{\"key\":\"/proton/net-l3vpn/VpnAfConfig\"," +
                "\"dir\":true,\"nodes\":[{\"key\":" +
                "\"/proton/net-l3vpn/VpnAfConfig/1000:1000\"," +
                "\"value\":\"{\\\"vrf_rt_type\\\": \\\"both\\\", " +
                "\\\"export_route_policy\\\": null, " +
                "\\\"updated_at\\\": \\\"2017-01-09 15:24:36.324669\\\", " +
                "\\\"created_at\\\": \\\"2017-01-09 15:24:36.324669\\\", " +
                "\\\"import_route_policy\\\": null, " +
                "\\\"vrf_rt_value\\\": \\\"1000:1000\\\"}\"," +
                "\"modifiedIndex\":20,\"createdIndex\":20}," +
                "{\"key\":\"/proton/net-l3vpn/VpnAfConfig/3000:3000\"," +
                "\"value\":\"{\\\"vrf_rt_type\\\": " +
                "\\\"export_extcommunity\\\", " +
                "\\\"export_route_policy\\\": null, " +
                "\\\"updated_at\\\": \\\"2017-01-20 15:13:38.235924\\\", " +
                "\\\"created_at\\\": \\\"2017-01-20 15:13:38.235924\\\", " +
                "\\\"import_route_policy\\\": null, " +
                "\\\"vrf_rt_value\\\": \\\"3000:3000\\\"}\"," +
                "\"modifiedIndex\":67,\"createdIndex\":67}]," +
                "\"modifiedIndex\":4,\"createdIndex\":4}}";
        parseJsonMessage(vpnAfConfigGet);
    }

    @Test
    public void invalidVpnPort() {
        String vpnAfConfigGet = "{\"action\":\"invalid\"," +
                "\"node\":{\"key\":\"/proton/net-l3vpn/VpnAfConfig\"," +
                "\"dir\":true,\"nodes\":[{\"key\":" +
                "\"/proton/net-l3vpn/VpnAfConfig/1000:1000\"," +
                "\"value\":\"{\\\"vrf_rt_type\\\": \\\"both\\\", " +
                "\\\"export_route_policy\\\": null, " +
                "\\\"updated_at\\\": \\\"2017-01-09 15:24:36.324669\\\", " +
                "\\\"created_at\\\": \\\"2017-01-09 15:24:36.324669\\\", " +
                "\\\"import_route_policy\\\": null, " +
                "\\\"vrf_rt_value\\\": \\\"1000:1000\\\"}\"," +
                "\"modifiedIndex\":20,\"createdIndex\":20}," +
                "{\"key\":\"/proton/net-l3vpn/VpnAfConfig/3000:3000\"," +
                "\"value\":\"{\\\"vrf_rt_type\\\": " +
                "\\\"export_extcommunity\\\", " +
                "\\\"export_route_policy\\\": null, " +
                "\\\"updated_at\\\": \\\"2017-01-20 15:13:38.235924\\\", " +
                "\\\"created_at\\\": \\\"2017-01-20 15:13:38.235924\\\", " +
                "\\\"import_route_policy\\\": null, " +
                "\\\"vrf_rt_value\\\": \\\"3000:3000\\\"}\"," +
                "\"modifiedIndex\":67,\"createdIndex\":67}]," +
                "\"modifiedIndex\":4,\"createdIndex\":4}}";
        parseJsonMessage(vpnAfConfigGet);
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
        vpnAfConfigManager.processGluonConfig(action, key, value);
    }

    @After
    public void tearDown() {
        vpnAfConfigManager.deactivate();
        vpnAfConfigManager.coreService = null;
        vpnAfConfigManager.storageService = null;
    }

    private static class MockCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return new DefaultApplicationId(1, name);
        }
    }

    private class MockInnerVpnAfConfigListener implements VpnAfConfigListener {

        @Override
        public void event(VpnAfConfigEvent event) {
        }
    }
}