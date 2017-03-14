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

package org.onosproject.incubator.store.routing.impl;
//TODO: Need to adopt latest changes to distributed later.
//TODO: Need to adopt latest changes to distributed later.
//TODO: Need to adopt latest changes to distributed later.

import org.onlab.packet.IpAddress;
import org.onosproject.incubator.net.routing.IpRoute;
import org.onosproject.incubator.net.routing.NextHop;
import org.onosproject.incubator.net.routing.NextHopData;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteEvent;
import org.onosproject.incubator.net.routing.RouteStore;
import org.onosproject.incubator.net.routing.RouteStoreDelegate;
import org.onosproject.incubator.net.routing.RouteTableType;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Route store based on distributed storage.
 */
public class DistributedRouteStore extends AbstractStore<RouteEvent, RouteStoreDelegate>
        implements RouteStore {
    public StorageService storageService;
    private static final Logger log = LoggerFactory.getLogger(DistributedRouteStore.class);

    /**
     * Constructs a distributed route store.
     *
     * @param storageService storage service should be passed from RouteStoreImpl
     */
    public DistributedRouteStore(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Sets up distributed route store.
     */
    public void activate() {
        // Creates and stores maps
        log.info("Started");
    }

    /**
     * Cleans up distributed route store.
     */
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void updateRoute(Route route) {

    }

    @Override
    public void removeRoute(Route route) {

    }

    @Override
    public Set<RouteTableType> getRouteTables() {
        return null;
    }

    @Override
    public Collection<Route> getRoutes(RouteTableType table) {
        return null;
    }

    @Override
    public IpRoute longestPrefixMatch(IpAddress ip) {
        return null;
    }

    @Override
    public Collection<Route> getRoutesForNextHop(IpAddress ip) {
        return null;
    }

    @Override
    public Collection<Route> getRoutesForNextHop(RouteTableType id, NextHop nextHop) {
        return null;
    }

    @Override
    public void updateNextHop(IpAddress ip, NextHopData nextHopData) {

    }

    @Override
    public void removeNextHop(IpAddress ip, NextHopData nextHopData) {

    }

    @Override
    public NextHopData getNextHop(IpAddress ip) {
        return null;
    }

    @Override
    public Map<IpAddress, NextHopData> getNextHops() {
        return null;
    }
}
