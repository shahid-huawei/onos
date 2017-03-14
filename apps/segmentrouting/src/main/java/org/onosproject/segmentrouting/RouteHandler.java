/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.segmentrouting;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.incubator.net.routing.IpNextHop;
import org.onosproject.incubator.net.routing.IpRoute;
import org.onosproject.incubator.net.routing.ResolvedRoute;
import org.onosproject.incubator.net.routing.RouteEvent;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles RouteEvent and manages routing entries.
 */
public class RouteHandler {
    private static final Logger log = LoggerFactory.getLogger(RouteHandler.class);
    private final SegmentRoutingManager srManager;

    public RouteHandler(SegmentRoutingManager srManager) {
        this.srManager = srManager;
    }

    protected void init(DeviceId deviceId) {
        srManager.routeService.getNextHops().forEach(nextHop -> {
            if (nextHop instanceof IpNextHop) {
                IpNextHop ipNextHop = (IpNextHop) nextHop;
                if (ipNextHop.location().deviceId().equals(deviceId)) {
                    srManager.routeService.getRoutesForNextHop(ipNextHop.ip()).forEach(route -> {
                        if (route instanceof IpRoute) {
                            IpRoute ipRoute = (IpRoute) route;
                            ResolvedRoute resolvedRoute =
                                    new ResolvedRoute(ipRoute, ipNextHop.mac(), ipNextHop.location());
                            processRouteAddedInternal(resolvedRoute);
                        }
                    });
                }
            }
        });
    }

    protected void processRouteAdded(RouteEvent event) {
        log.info("processRouteAdded {}", event);
        processRouteAddedInternal((ResolvedRoute) event.subject());
    }

    private void processRouteAddedInternal(ResolvedRoute route) {
        IpPrefix prefix = route.prefix();
        MacAddress nextHopMac = route.nextHopMac();
        // TODO ResolvedRoute does not contain VLAN information.
        //      Therefore we only support untagged nexthop for now.
        VlanId nextHopVlan = VlanId.NONE;
        ConnectPoint location = route.location();

        srManager.deviceConfiguration.addSubnet(location, prefix);
        srManager.defaultRoutingHandler.populateSubnet(location, ImmutableSet.of(prefix));
        srManager.routingRulePopulator.populateRoute(location.deviceId(), prefix,
                                                     nextHopMac, nextHopVlan, location.port());
    }

    protected void processRouteUpdated(RouteEvent event) {
        log.info("processRouteUpdated {}", event);
        processRouteRemovedInternal(event.prevSubject());
        processRouteAddedInternal((ResolvedRoute) event.subject());
    }

    protected void processRouteRemoved(RouteEvent event) {
        log.info("processRouteRemoved {}", event);
        processRouteRemovedInternal((ResolvedRoute) event.subject());
    }

    private void processRouteRemovedInternal(ResolvedRoute route) {
        IpPrefix prefix = route.prefix();
        MacAddress nextHopMac = route.nextHopMac();
        // TODO ResolvedRoute does not contain VLAN information.
        //      Therefore we only support untagged nexthop for now.
        VlanId nextHopVlan = VlanId.NONE;
        ConnectPoint location = route.location();

        srManager.deviceConfiguration.removeSubnet(location, prefix);
        srManager.defaultRoutingHandler.revokeSubnet(ImmutableSet.of(prefix));
        srManager.routingRulePopulator.revokeRoute(
                location.deviceId(), prefix, nextHopMac, nextHopVlan, location.port());
    }
}
