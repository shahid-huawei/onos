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

package org.onosproject.evpnopenflow.rsc.vpnport.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.evpnopenflow.rsc.VpnPort;
import org.onosproject.evpnopenflow.rsc.VpnPortId;
import org.onosproject.evpnopenflow.rsc.vpnport.VpnPortEvent;
import org.onosproject.evpnopenflow.rsc.vpnport.VpnPortListener;
import org.onosproject.store.service.TestStorageService;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;

import java.io.IOException;
import java.util.Collection;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

/**
 * Provides test implementation of class vpn port manager.
 */
public class VpnPortManagerTest {
    private TestStorageService storageService = new TestStorageService();
    private MockCoreService coreService = new MockCoreService();
    private VpnPortManager vpnPortManager = new VpnPortManager();
    private MockInnerVpnPortListener vpnPortListener = new
            MockInnerVpnPortListener();

    @Before
    public void startUp() throws TestUtils.TestUtilsException {
        vpnPortManager.coreService = coreService;
        vpnPortManager.storageService = storageService;
        VirtualPortService virtualPortService
                = createNiceMock(VirtualPortService.class);
        replay(virtualPortService);
        vpnPortManager.virtualPortService = virtualPortService;

        vpnPortManager.activate();
        vpnPortManager.addListener(vpnPortListener);
        vpnPortManager.removeListener(vpnPortListener);
    }

    @Test
    public void setVpnPort() {
        String json = "{\"action\":\"set\"," +
                "\"node\":{\"key\":\"/proton/net-l3vpn/VPNPort\",\"dir\"" +
                ":true,\"nodes\":[{\"key\":\"/proton/net-l3vpn/VPNPort/" +
                "6ad5e893-4629-4876-aef2-f7ab9a44f218\",\"value\":\"" +
                "{\\\"interface_id\\\": \\\"6ad5e893-4629-4876-aef2-f7ab9a44f218\\\"" +
                ", \\\"service_id\\\": \\\"46befe74-27a8-4b3b-a726-b35d9" +
                "8b9f476\\\", \\\"created_at\\\": \\\"2017-01-09 15:25:43" +
                ".441882\\\", \\\"updated_at\\\": \\\"2017-01-09 15:25:43" +
                ".441882\\\"}\",\"modifiedIndex\":24,\"createdIndex\"" +
                ":24}],\"modifiedIndex\":8,\"createdIndex\":8}}";
        parseJsonMessage(json);

        boolean isExit = vpnPortManager.exists(VpnPortId.
                vpnPortId("6ad5e893-4629-4876-aef2-f7ab9a44f218"));
        assertEquals(true, isExit);

        VpnPort vpnPort = vpnPortManager.getPort(VpnPortId.
                vpnPortId("6ad5e893-4629-4876-aef2-f7ab9a44f218"));
        assertEquals("6ad5e893-4629-4876-aef2-f7ab9a44f218",
                     vpnPort.id().vpnPortId());

        Collection<VpnPort> ports = vpnPortManager.getPorts();
        for (VpnPort port : ports) {
            assertEquals("6ad5e893-4629-4876-aef2-f7ab9a44f218",
                         port.id().vpnPortId());
        }

        //update with same vpn port which is already created
        json = "{\"action\":\"update\"," +
                "\"node\":{\"key\":\"/proton/net-l3vpn/VPNPort\",\"dir\"" +
                ":true,\"nodes\":[{\"key\":\"/proton/net-l3vpn/VPNPort/" +
                "6ad5e893-4629-4876-aef2-f7ab9a44f218\",\"value\":\"" +
                "{\\\"interface_id\\\": \\\"6ad5e893-4629-4876-aef2-f7ab9a44f218\\\"" +
                ", \\\"service_id\\\": \\\"46befe74-27a8-4b3b-a726-b35d9" +
                "8b9f476\\\", \\\"created_at\\\": \\\"2017-01-09 15:25:43" +
                ".441882\\\", \\\"updated_at\\\": \\\"2017-01-09 15:25:43" +
                ".441882\\\"}\",\"modifiedIndex\":24,\"createdIndex\"" +
                ":24}],\"modifiedIndex\":8,\"createdIndex\":8}}";
        parseJsonMessage(json);

        // update with different vpn port id

        json = "{\"action\":\"update\"," +
                "\"node\":{\"key\":\"/proton/net-l3vpn/VPNPort\",\"dir\"" +
                ":true,\"nodes\":[{\"key\":\"/proton/net-l3vpn/VPNPort/" +
                "6ad5e893-4629-4876-aef2-f7ab9a44f219\",\"value\":\"" +
                "{\\\"interface_id\\\": \\\"6ad5e893-4629-4876-aef2-f7ab9a44f219\\\"" +
                ", \\\"service_id\\\": \\\"46befe74-27a8-4b3b-a726-b35d9" +
                "8b9f476\\\", \\\"created_at\\\": \\\"2017-01-09 15:25:43" +
                ".441882\\\", \\\"updated_at\\\": \\\"2017-01-09 15:25:43" +
                ".441882\\\"}\",\"modifiedIndex\":24,\"createdIndex\"" +
                ":24}],\"modifiedIndex\":8,\"createdIndex\":8}}";
        parseJsonMessage(json);

        //process with invalid action type
        json = "{\"action\":\"invalid\"," +
                "\"node\":{\"key\":\"/proton/net-l3vpn/VPNPort\",\"dir\"" +
                ":true,\"nodes\":[{\"key\":\"/proton/net-l3vpn/VPNPort/" +
                "6ad5e893-4629-4876-aef2-f7ab9a44f218\",\"value\":\"" +
                "{\\\"interface_id\\\": \\\"6ad5e893-4629-4876-aef2-f7ab9a44f218\\\"" +
                ", \\\"service_id\\\": \\\"46befe74-27a8-4b3b-a726-b35d9" +
                "8b9f476\\\", \\\"created_at\\\": \\\"2017-01-09 15:25:43" +
                ".441882\\\", \\\"updated_at\\\": \\\"2017-01-09 15:25:43" +
                ".441882\\\"}\",\"modifiedIndex\":24,\"createdIndex\"" +
                ":24}],\"modifiedIndex\":8,\"createdIndex\":8}}";
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
        vpnPortManager.processGluonConfig(action, key, value);
    }

    @Test
    public void deleteVpnPort() {
        String json = "{\"action\":\"delete\"," +
                "\"node\":{\"key\":\"/proton/net-l3vpn/VPNPort\",\"dir\"" +
                ":true,\"nodes\":[{\"key\":\"/proton/net-l3vpn/VPNPort/" +
                "6ad5e893-4629-4876-aef2-f7ab9a44f218\",\"value\":\"" +
                "{\\\"interface_id\\\": \\\"6ad5e893-4629-4876-aef2-f7ab9a44f218\\\"" +
                ", \\\"service_id\\\": \\\"46befe74-27a8-4b3b-a726-b35d9" +
                "8b9f476\\\", \\\"created_at\\\": \\\"2017-01-09 15:25:43" +
                ".441882\\\", \\\"updated_at\\\": \\\"2017-01-09 15:25:43" +
                ".441882\\\"}\",\"modifiedIndex\":24,\"createdIndex\"" +
                ":24}],\"modifiedIndex\":8,\"createdIndex\":8}}";
        parseJsonMessage(json);
    }


    @After
    public void tearDown() {
        vpnPortManager.deactivate();
        vpnPortManager.coreService = null;
        vpnPortManager.storageService = null;
    }

    private static class MockCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return new DefaultApplicationId(1, name);
        }
    }

    private class MockInnerVpnPortListener implements VpnPortListener {

        @Override
        public void event(VpnPortEvent event) {
        }
    }

}