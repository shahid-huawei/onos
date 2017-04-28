/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.provider.bgp.cfg.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.bgp.controller.BgpCfg;
import org.onosproject.bgp.controller.BgpController;
import org.onosproject.bgp.controller.BgpPeerCfg;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.gluon.rsc.GluonConfig;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * BGP config provider to validate and populate the configuration.
 */
@Component(immediate = true)
public class BgpCfgProvider extends AbstractProvider {

    private static final Logger log = getLogger(BgpCfgProvider.class);

    static final String PROVIDER_ID = "org.onosproject.provider.bgp.cfg";
    static final String BGP_PEERING = "BGPPeering";
    static final String DELETE = "delete";
    static final String SET = "set";
    static final String UPDATE = "update";
    static final String SLASH = "/";
    static final String RESPONSE_NOT_NULL = "JsonNode can not be null";
    static final String JSON_NOT_NULL = "JsonNode can not be null";

    private enum BGP_CONFIG {
        BGP_CONFIG_ADD,
        BGP_CONFIG_UPDATE,
        BGP_CONFIG_DELETE
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected BgpController bgpController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry configRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    private final ConfigFactory configFactory =
            new ConfigFactory(SubjectFactories.APP_SUBJECT_FACTORY, BgpAppConfig.class, "bgpapp") {
                @Override
                public BgpAppConfig createConfig() {
                    return new BgpAppConfig();
                }
            };

    private final NetworkConfigListener configListener = new InternalConfigListener();
    Map<String, BgpAppConfig.BgpPeerConfig> gluonPeers = new HashMap<>();

    private ApplicationId appId;

    /**
     * Creates a Bgp config provider.
     */
    public BgpCfgProvider() {
        super(new ProviderId("bgp", PROVIDER_ID));
    }

    @Activate
    public void activate(ComponentContext context) {
        appId = coreService.registerApplication(PROVIDER_ID);
        configService.addListener(configListener);
        configRegistry.registerConfigFactory(configFactory);
        handleNetworkConfiguration(BGP_CONFIG.BGP_CONFIG_ADD);
        log.info("BGP cfg provider started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        configRegistry.unregisterConfigFactory(configFactory);
        configService.removeListener(configListener);
    }

    void setBgpController(BgpController bgpController) {
        this.bgpController = bgpController;
    }

    /**
     * Reads network configuration and set it to the BGP-LS south bound
     * protocol.
     */
    private void handleNetworkConfiguration(BGP_CONFIG operType) {
        BgpAppConfig config = configRegistry.getConfig(appId, BgpAppConfig.class);
        if (config == null) {
            log.warn("Read No configuration found");
            return;
        }

        applyNetworkConfiguration(config, operType);
    }

    private void applyNetworkConfiguration(BgpAppConfig config,
                                           BGP_CONFIG operType) {
        BgpCfg bgpConfig = null;
        List<BgpAppConfig.BgpPeerConfig> nodes;
        bgpConfig = bgpController.getConfig();

        /*Set the configuration */
        bgpConfig.setRouterId(config.routerId());
        bgpConfig.setAsNumber(config.localAs());
        bgpConfig.setLsCapability(config.lsCapability());
        bgpConfig.setHoldTime(config.holdTime());
        bgpConfig.setMaxSession(config.maxSession());
        bgpConfig.setLargeASCapability(config.largeAsCapability());
        bgpConfig.setEvpnCapability(config.evpnCapability());

        if (config.flowSpecCapability() == null) {
            bgpConfig.setFlowSpecCapability(BgpCfg.FlowSpec.NONE);
        } else {
            if (config.flowSpecCapability().equals("IPV4")) {
                bgpConfig.setFlowSpecCapability(BgpCfg.FlowSpec.IPV4);
            } else if (config.flowSpecCapability().equals("VPNV4")) {
                bgpConfig.setFlowSpecCapability(BgpCfg.FlowSpec.VPNV4);
            } else if (config.flowSpecCapability().equals("IPV4_VPNV4")) {
                bgpConfig.setFlowSpecCapability(BgpCfg.FlowSpec.IPV4_VPNV4);
            } else {
                bgpConfig.setFlowSpecCapability(BgpCfg.FlowSpec.NONE);
            }
        }
        bgpConfig.setFlowSpecRpdCapability(config.rpdCapability());

        if (operType.equals(BGP_CONFIG.BGP_CONFIG_UPDATE)) {
            updatePeerConfiguration(bgpConfig, config.bgpPeer());
        }

        nodes = config.bgpPeer();
        for (int i = 0; i < nodes.size(); i++) {
            String connectMode = nodes.get(i).connectMode();
            bgpConfig.addPeer(nodes.get(i).hostname(), nodes.get(i).asNumber(), nodes.get(i).holdTime());
            if (connectMode.equals(BgpAppConfig.PEER_CONNECT_ACTIVE)) {
                bgpConfig.connectPeer(nodes.get(i).hostname());
            }
        }
    }

    /**
     * Read the configuration and update it to the BGP-LS south bound protocol.
     */
    private void updatePeerConfiguration(BgpCfg bgpConfig,
                                         List<BgpAppConfig.BgpPeerConfig> nodes) {
        List<BgpAppConfig.BgpPeerConfig> currNodes;
        TreeMap<String, BgpPeerCfg> bgpPeerTree;
        BgpPeerCfg peer = null;

        /* Update the self configuration */
        if (bgpController.connectedPeerCount() != 0) {
            //TODO: If connections already exist, disconnect
            bgpController.closeConnectedPeers();
        }

        /* update the peer configuration */
        bgpPeerTree = bgpConfig.getPeerTree();
        if (bgpPeerTree.isEmpty()) {
            log.info("There are no BGP peers to iterate");
        } else {
            Set set = bgpPeerTree.entrySet();
            Iterator i = set.iterator();
            List<BgpPeerCfg> absPeerList = new ArrayList<BgpPeerCfg>();

            boolean exists = false;

            while (i.hasNext()) {
                Map.Entry me = (Map.Entry) i.next();
                peer = (BgpPeerCfg) me.getValue();

                currNodes = nodes;
                for (int j = 0; j < currNodes.size(); j++) {
                    String peerIp = currNodes.get(j).hostname();
                    if (peerIp.equals(peer.getPeerRouterId())) {

                        if (bgpConfig.isPeerConnectable(peer.getPeerRouterId())) {
                            peer.setAsNumber(currNodes.get(j).asNumber());
                            peer.setHoldtime(currNodes.get(j).holdTime());
                            log.debug("Peer neighbor IP successfully modified :" + peer.getPeerRouterId());
                        } else {
                            log.debug("Peer neighbor IP cannot be modified :" + peer.getPeerRouterId());
                        }

                        currNodes.remove(j);
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    absPeerList.add(peer);
                    exists = false;
                }

                if (peer.connectPeer() != null) {
                    peer.connectPeer().disconnectPeer();
                    peer.setConnectPeer(null);
                }
            }

            /* Remove the absent nodes. */
            for (int j = 0; j < absPeerList.size(); j++) {
                bgpConfig.removePeer(absPeerList.get(j).getPeerRouterId());
            }
        }
    }

    /**
     * BGP config listener to populate the configuration.
     */
    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            log.info("Received network config {}", event.type());

            if (event.configClass().equals(BgpAppConfig.class)) {
                switch (event.type()) {
                    case CONFIG_ADDED:
                        handleNetworkConfiguration(BGP_CONFIG.BGP_CONFIG_ADD);
                        break;
                    case CONFIG_UPDATED:
                        handleNetworkConfiguration(BGP_CONFIG.BGP_CONFIG_UPDATE);
                        break;
                    case CONFIG_REMOVED:
                    default:
                        break;
                }
            } else if (event.configClass().equals(GluonConfig.class)) {
                String recvConfig = (String) event.subject();
                log.info("Event Type {} and Subject {}", event.type(),
                         recvConfig);
                if (!isGluonBgpConfig(recvConfig)) {
                    /* Need to process only for Gluon BGP configuration */
                    log.info("Received non BGP config {}", recvConfig);
                    return;
                }

                switch (event.type()) {
                    case CONFIG_UPDATED:
                        handleGluonConfiguration(recvConfig,
                                                 BGP_CONFIG.BGP_CONFIG_UPDATE);
                        break;
                    case CONFIG_REMOVED:
                        handleGluonConfiguration(recvConfig,
                                                 BGP_CONFIG.BGP_CONFIG_DELETE);
                        break;
                    default:
                        log.info("Invalid Action has received");
                        break;
                }
            } else {
                /* Not interested to process */
                log.info("Not interested to process", event.configClass());
                return;
            }

        }
    }

    private boolean isGluonBgpConfig(String key) {
        String[] list = key.split(SLASH);
        String target = list[list.length - 2];
        if (target.equals(BGP_PEERING)) {
            log.info("Bgp peering proton key");
            return true;
        }
        return false;
    }

    private String getGluonPeerIdFromKey(String key) {
        String[] list = key.split(SLASH);
        String peerId = list[list.length - 1];
        return peerId;
    }

    /**
     * Reads gluon configuration and set it to the BGP-LS south bound
     * protocol.
     */
    public void handleGluonConfiguration(String recvMsg, BGP_CONFIG operType) {
        try {
            if (operType.equals(BGP_CONFIG.BGP_CONFIG_UPDATE)) {
                log.info("Update operation");
                GluonConfig gluonConfig = configService.getConfig(recvMsg, GluonConfig.class);
                JsonNode jsonNode = gluonConfig.node();
                Map<String, String> gluonBgpConfig;

                gluonBgpConfig = readGluonBgpConfiguration(jsonNode.get(recvMsg));
                applyGluonConfiguration(gluonBgpConfig);
            } else if (operType.equals(BGP_CONFIG.BGP_CONFIG_DELETE)) {
                log.info("Delete Operation");
                removeGluonConfiguration(getGluonPeerIdFromKey(recvMsg));
            } else {
                log.info("Wrongggg Operation");
            }
        } catch (NullPointerException e) {
            log.error("Json node should not be null");
        }
    }

    /**
     * Returns a collection of vpnInstances from subnetNodes.
     *
     * @param jsonNode the BGP json node
     * @return returns the collection of vpn instances
     */
    private Map<String, String> readGluonBgpConfiguration(JsonNode jsonNode) {
        checkNotNull(jsonNode, JSON_NOT_NULL);
        Map<String, String> bgpMap = new HashMap<>();
        String peerIP = jsonNode.get("peer_ip_address").asText();
        String localIP = jsonNode.get("local_ip_address").asText();
        String id = jsonNode.get("id").asText();
        String asNumber = jsonNode.get("as_number").asText();
        String name = jsonNode.get("name").asText();
        bgpMap.put("name", name);
        bgpMap.put("asNumber", asNumber);
        bgpMap.put("id", id);
        bgpMap.put("localIP", localIP);
        bgpMap.put("peerIP", peerIP);

        log.info("bgp peer ip {}, localip {}, as number {} and name {}",
                 peerIP, localIP, asNumber, name);
        return bgpMap;
    }

    private void applyGluonConfiguration(Map<String, String> gluonBgpConfig) {
        BgpCfg bgpConfig = bgpController.getConfig();
        List<BgpAppConfig.BgpPeerConfig> nodes = new ArrayList<>();

        /* Handle BGP configuration */
        bgpConfig.setRouterId(gluonBgpConfig.get("localIP"));
        bgpConfig.setAsNumber(Integer.parseInt(gluonBgpConfig.get("asNumber")));
        bgpConfig.setLsCapability(false);
        bgpConfig.setHoldTime((short) 20);
        bgpConfig.setMaxSession(20);
        bgpConfig.setLargeASCapability(false);
        bgpConfig.setEvpnCapability(true);
        bgpConfig.setFlowSpecCapability(BgpCfg.FlowSpec.NONE);
        bgpConfig.setFlowSpecRpdCapability(false);
        bgpConfig.setMaxConnRetryCout(20);

        /* Handle Peer configuration */
        /* Add or update peers in gluonPeers*/
        String peerId = gluonBgpConfig.get("id");
        if (gluonPeers.containsKey(peerId)) {
            /* Update existing record */
            BgpAppConfig.BgpPeerConfig oldPeerConfig = gluonPeers.get(peerId);
            //TODO:
        } else {
            /* Add new peer */
            BgpAppConfig.BgpPeerConfig peerConfig = new BgpAppConfig
                    .BgpPeerConfig(gluonBgpConfig.get("peerIP"),
                                   Integer.parseInt(gluonBgpConfig.get
                                           ("asNumber")),
                                   20, BgpAppConfig.PEER_CONNECT_ACTIVE);
            gluonPeers.put(peerId, peerConfig);
            log.info("Added BGP peerId={} details={} to gluonPeers", peerId,
                     peerConfig);
            log.info("gluonPeers={}", gluonPeers);
        }

        for (String peerKey : gluonPeers.keySet()) {
            nodes.add(gluonPeers.get(peerKey));
        }
        log.info("nodes1={}", nodes);

        updatePeerConfiguration(bgpConfig, nodes);
        log.info("nodes2={}", nodes);
        for (int i = 0; i < nodes.size(); i++) {
            String connectMode = nodes.get(i).connectMode();
            bgpConfig.addPeer(nodes.get(i).hostname(), nodes.get(i).asNumber(), nodes.get(i).holdTime());
            if (connectMode.equals(BgpAppConfig.PEER_CONNECT_ACTIVE)) {
                bgpConfig.connectPeer(nodes.get(i).hostname());
            }
        }
    }

    private void removeGluonConfiguration(String peerId) {
        BgpCfg bgpConfig = bgpController.getConfig();
        List<BgpAppConfig.BgpPeerConfig> nodes = new ArrayList<>();

        if (gluonPeers.containsKey(peerId)) {
            gluonPeers.remove(peerId);
        }

        for (String peerKey : gluonPeers.keySet()) {
            nodes.add(gluonPeers.get(peerKey));
        }

        updatePeerConfiguration(bgpConfig, nodes);

        for (int i = 0; i < nodes.size(); i++) {
            String connectMode = nodes.get(i).connectMode();
            bgpConfig.addPeer(nodes.get(i).hostname(), nodes.get(i).asNumber(), nodes.get(i).holdTime());
            if (connectMode.equals(BgpAppConfig.PEER_CONNECT_ACTIVE)) {
                bgpConfig.connectPeer(nodes.get(i).hostname());
            }
        }
    }
}
