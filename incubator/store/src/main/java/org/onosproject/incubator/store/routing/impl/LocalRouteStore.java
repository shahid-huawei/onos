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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.googlecode.concurrenttrees.common.KeyValuePair;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree;
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.incubator.net.routing.EvpnInstanceName;
import org.onosproject.incubator.net.routing.EvpnInstanceNextHop;
import org.onosproject.incubator.net.routing.EvpnInstancePrefix;
import org.onosproject.incubator.net.routing.EvpnInstanceRoute;
import org.onosproject.incubator.net.routing.EvpnNextHop;
import org.onosproject.incubator.net.routing.EvpnPrefix;
import org.onosproject.incubator.net.routing.EvpnRoute;
import org.onosproject.incubator.net.routing.IpNextHop;
import org.onosproject.incubator.net.routing.IpRoute;
import org.onosproject.incubator.net.routing.NextHop;
import org.onosproject.incubator.net.routing.NextHopData;
import org.onosproject.incubator.net.routing.ResolvedRoute;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteEvent;
import org.onosproject.incubator.net.routing.RouteStore;
import org.onosproject.incubator.net.routing.RouteStoreDelegate;
import org.onosproject.incubator.net.routing.RouteTable;
import org.onosproject.incubator.net.routing.RouteTableType;
import org.onosproject.store.AbstractStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Route store based on in-memory storage.
 */
@Service
@Component
public class LocalRouteStore extends AbstractStore<RouteEvent, RouteStoreDelegate>
        implements RouteStore {

    private Logger log = LoggerFactory.getLogger(getClass());

    private Map<RouteTableType, RouteTable> routeTables;
    //private static final RouteTableId IPV4 = new RouteTableId("ipv4");
    //private static final RouteTableId IPV6 = new RouteTableId("ipv6");

    private Map<IpAddress, NextHopData> nextHops = new ConcurrentHashMap<>();

    /**
     * Sets up local route store.
     */
    @Activate
    public void activate() {
        routeTables = new ConcurrentHashMap<>();
        routeTables.put(RouteTableType.IPV4, new IpRouteTable());
        routeTables.put(RouteTableType.IPV6, new IpRouteTable());
        routeTables.put(RouteTableType.EVPN_IPV4, new EvpnRouteTable());
        routeTables.put(RouteTableType.EVPN_IPV6, new EvpnRouteTable());
        log.info("Started");
    }

    /**
     * Cleans up local route store. Currently nothing is done here.
     */
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void updateRoute(Route route) {
        getDefaultRouteTable(route).update(route);
    }

    @Override
    public void removeRoute(Route route) {
        RouteTable table = getDefaultRouteTable(route);
        table.remove(route);

        //currently handling next hops only for IpRoute.
        if (route instanceof IpRoute) {
            Collection<Route> routes = table.getRoutesForNextHop(route.nextHop());
            IpRoute ipRoute = (IpRoute) route;
            if (routes.isEmpty()) {
                nextHops.remove(ipRoute.ipNextHop());
            }
        }
    }

    @Override
    public Set<RouteTableType> getRouteTables() {
        return routeTables.keySet();
    }

    @Override
    public Collection<Route> getRoutes(RouteTableType table) {
        RouteTable routeTable = routeTables.get(table);
        if (routeTable == null) {
            return Collections.emptySet();
        }
        return routeTable.getRoutes();
    }

    @Override
    public IpRoute longestPrefixMatch(IpAddress ip) {
        return getDefaultRouteTable(ip).longestPrefixMatch(ip);
    }

    @Override
    public Collection<Route> getRoutesForNextHop(IpAddress ip) {
        return getDefaultRouteTable(ip).getRoutesForNextHop(IpNextHop.ipAddress(ip));
    }

    @Override
    public Collection<Route> getRoutesForNextHop(RouteTableType table,
                                                 NextHop nextHop) {
        List<Route> routes = new LinkedList<>();
        routes.addAll(routeTables.get(table).getRoutesForNextHop(nextHop));
        return routes;
    }

    @Override
    public void updateNextHop(IpAddress ip, NextHopData nextHopData) {
        checkNotNull(ip);
        checkNotNull(nextHopData);
        Collection<Route> routes = getDefaultRouteTable(ip)
                .getRoutesForNextHop(IpNextHop.ipAddress(ip));

        if (!routes.isEmpty() && !nextHopData.equals(nextHops.get(ip))) {
            NextHopData oldNextHopData = nextHops.put(ip, nextHopData);

            for (Route route : routes) {
                if (route instanceof IpRoute) {
                    IpRoute ipRoute = (IpRoute) route;
                    if (oldNextHopData == null) {
                        notifyDelegate(new RouteEvent(RouteEvent
                                                              .Type.ROUTE_ADDED,
                                                      new ResolvedRoute(ipRoute,
                                                                        nextHopData.mac(),
                                                                        nextHopData.location())));
                    } else {
                        notifyDelegate(new RouteEvent(RouteEvent
                                                              .Type.ROUTE_UPDATED,
                                                      new ResolvedRoute(ipRoute,
                                                                        nextHopData.mac(),
                                                                        nextHopData.location()),
                                                      new ResolvedRoute(ipRoute,
                                                                        oldNextHopData.mac(),
                                                                        oldNextHopData.location())));
                    }
                }
            }
        }
    }

    @Override
    public void removeNextHop(IpAddress ip, NextHopData nextHopData) {
        checkNotNull(ip);
        checkNotNull(nextHopData);
        if (nextHops.remove(ip, nextHopData)) {
            Collection<Route> routes = getDefaultRouteTable(ip)
                    .getRoutesForNextHop(IpNextHop.ipAddress(ip));
            for (Route route : routes) {
                if (route instanceof IpRoute) {
                    IpRoute ipRoute = (IpRoute) route;
                    notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_REMOVED,
                                                  new ResolvedRoute(ipRoute,
                                                                    nextHopData.mac(),
                                                                    nextHopData.location())));
                }
            }
        }
    }

    @Override
    public NextHopData getNextHop(IpAddress ip) {
        return nextHops.get(ip);
    }

    @Override
    public Map<IpAddress, NextHopData> getNextHops() {
        return ImmutableMap.copyOf(nextHops);
    }

    private RouteTable getDefaultRouteTable(Route route) {
        RouteTableType tableType = null;
        if (route instanceof IpRoute) {
            IpAddress ip = ((IpRoute) route).prefix().address();
            tableType = (ip.isIp4()) ? RouteTableType.IPV4
                    : RouteTableType.IPV6;
        } else if (route instanceof EvpnRoute) {
            IpAddress ip = ((EvpnRoute) route).prefixIp().address();
            tableType = (ip.isIp4()) ? RouteTableType.EVPN_IPV4
                    : RouteTableType.EVPN_IPV6;
        } else if (route instanceof EvpnInstanceRoute) {
            IpAddress ip = ((EvpnInstanceRoute) route).prefix().address();
            tableType = (ip.isIp4()) ? RouteTableType.EVPN_IPV4
                    : RouteTableType.EVPN_IPV6;
        }
        return routeTables.get(tableType);
    }

    private IpRouteTable getDefaultRouteTable(IpAddress ip) {
        RouteTableType routeTableId = (ip.isIp4()) ? RouteTableType.IPV4
                : RouteTableType.IPV6;
        return (IpRouteTable) routeTables.get(routeTableId);
    }


    //New code start from here.
    private class IpRouteTable implements RouteTable {

        private final InvertedRadixTree<IpRoute> routeTable;

        private final Map<IpPrefix, IpRoute> routes = new ConcurrentHashMap<>();
        private final Multimap<IpAddress, IpRoute> reverseIndex = Multimaps
                .synchronizedMultimap(HashMultimap.create());

        /**
         * Creates a new route table.
         */
        public IpRouteTable() {
            routeTable = new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());
        }

        @Override
        public void update(Route route) {
            synchronized (this) {
                IpRoute ipRoute = (IpRoute) route;
                IpRoute oldRoute = routes.put(ipRoute.prefix(), ipRoute);
                routeTable.put(createBinaryString(ipRoute.prefix()), ipRoute);
                reverseIndex.put(ipRoute.ipNextHop(), ipRoute);

                if (oldRoute != null) {
                    reverseIndex.remove(oldRoute.ipNextHop(), oldRoute);

                    if (reverseIndex.get(oldRoute.ipNextHop()).isEmpty()) {
                        nextHops.remove(oldRoute.ipNextHop());
                    }
                }

                if (ipRoute.equals(oldRoute)) {
                    // No need to send events if the new route is the same
                    return;
                }

                NextHopData nextHopMac = nextHops.get(ipRoute.ipNextHop());

                if (oldRoute != null
                        && !oldRoute.ipNextHop().equals(ipRoute.ipNextHop())) {
                    if (nextHopMac == null) {
                        // We don't know the new MAC address yet so delete the
                        // route
                        notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_REMOVED,
                                                      new ResolvedRoute(oldRoute,
                                                                        null, null)));
                    } else {
                        // We know the new MAC address so update the route
                        notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_UPDATED,
                                                      new ResolvedRoute(ipRoute,
                                                                        nextHopMac.mac(), nextHopMac.location())));
                    }
                    return;
                }

                if (nextHopMac != null) {
                    notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_ADDED,
                                                  new ResolvedRoute(ipRoute,
                                                                    nextHopMac.mac(), nextHopMac.location())));
                }
            }
        }

        @Override
        public void remove(Route route) {
            synchronized (this) {
                IpRoute ipRoute = (IpRoute) route;
                IpRoute removed = routes.remove(ipRoute.prefix());
                routeTable.remove(createBinaryString(ipRoute.prefix()));

                if (removed != null) {
                    reverseIndex.remove(removed.nextHop(), removed);
                    notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_REMOVED,
                                                  new ResolvedRoute(ipRoute,
                                                                    null, null)));
                }
            }
        }

        @Override
        public Collection<Route> getRoutesForNextHop(NextHop nextHop) {
            List<Route> routes = new LinkedList<Route>();
            IpNextHop ipNextHop = (IpNextHop) nextHop;
            routes.addAll(reverseIndex.get(ipNextHop.ip()));
            return routes;
        }

        @Override
        public Collection<Route> getRoutes() {
            Iterator<KeyValuePair<IpRoute>> it = routeTable
                    .getKeyValuePairsForKeysStartingWith("").iterator();

            List<Route> routes = new LinkedList<Route>();

            while (it.hasNext()) {
                KeyValuePair<IpRoute> entry = it.next();
                routes.add(entry.getValue());
            }

            return routes;
        }

        /**
         * Performs a longest prefix match with the given IP in the route table.
         *
         * @param ip IP address to look up
         * @return most specific prefix containing the given
         */
        public IpRoute longestPrefixMatch(IpAddress ip) {
            Iterable<IpRoute> prefixes = routeTable
                    .getValuesForKeysPrefixing(createBinaryString(ip
                                                                          .toIpPrefix()));

            Iterator<IpRoute> it = prefixes.iterator();

            IpRoute route = null;
            while (it.hasNext()) {
                route = it.next();
            }

            return route;
        }

        private String createBinaryString(IpPrefix ipPrefix) {
            byte[] octets = ipPrefix.address().toOctets();
            StringBuilder result = new StringBuilder(ipPrefix.prefixLength());
            result.append("0");
            for (int i = 0; i < ipPrefix.prefixLength(); i++) {
                int byteOffset = i / Byte.SIZE;
                int bitOffset = i % Byte.SIZE;
                int mask = 1 << (Byte.SIZE - 1 - bitOffset);
                byte value = octets[byteOffset];
                boolean isSet = ((value & mask) != 0);
                result.append(isSet ? "1" : "0");
            }

            return result.toString();
        }

    }

    private class EvpnRouteTable implements RouteTable {
        private Map<EvpnInstanceName, EvpnInstanceRouteTable>
                evpnRouteTables = new HashMap<>();

        @Override
        public void update(Route route) {

            if (route instanceof EvpnRoute) {
                EvpnRoute evpnRoute = (EvpnRoute) route;
                if (evpnRouteTables.get(EvpnInstanceName.evpnName("evpn-public")) == null) {

                    EvpnInstanceRouteTable evpnInstanceRouteTable = new
                            EvpnInstanceRouteTable(EvpnInstanceName
                                                           .evpnName("evpn-public"));
                    evpnRouteTables.put(EvpnInstanceName.evpnName("evpn-public"),
                                        evpnInstanceRouteTable);
                }
                evpnRouteTables.get(EvpnInstanceName.evpnName("evpn-public"))
                        .update(evpnRoute);

            } else if (route instanceof EvpnInstanceRoute) {
                EvpnInstanceRoute evpnInstanceRoute = (EvpnInstanceRoute) route;
                if (evpnRouteTables
                        .get(evpnInstanceRoute.evpnInstanceName()) == null) {
                    EvpnInstanceRouteTable evpnInstanceRouteTable = new
                            EvpnInstanceRouteTable(evpnInstanceRoute
                                                           .evpnInstanceName());
                    evpnRouteTables.put(evpnInstanceRoute.evpnInstanceName(),
                                        evpnInstanceRouteTable);
                }
                evpnRouteTables.get(evpnInstanceRoute.evpnInstanceName())
                        .update(evpnInstanceRoute);
            }
        }

        @Override
        public void remove(Route route) {
            if (route instanceof EvpnRoute) {
                EvpnRoute evpnRoute = (EvpnRoute) route;
                evpnRouteTables.get(EvpnInstanceName.evpnName("evpn-public"))
                        .remove(evpnRoute);

            } else if (route instanceof EvpnInstanceRoute) {
                EvpnInstanceRoute evpnInstanceRoute = (EvpnInstanceRoute) route;
                evpnRouteTables.get(evpnInstanceRoute.evpnInstanceName())
                        .remove(evpnInstanceRoute);
            }
        }

        @Override
        public Collection<Route> getRoutes() {
            if (evpnRouteTables == null) {
                return Collections.emptySet();
            }
            Collection<Route> list = Lists.newLinkedList();
            evpnRouteTables.keySet().forEach(evpnName -> {
                list.addAll(evpnRouteTables.get(evpnName).getRoutes());
            });
            return list;
        }

        @Override
        public Collection<Route> getRoutesForNextHop(NextHop nextHop) {
            if (evpnRouteTables == null) {
                return Collections.emptySet();
            }
            Collection<Route> list = Lists.newLinkedList();
            evpnRouteTables.keySet().forEach(evpnName -> {
                list.addAll(evpnRouteTables.get(evpnName)
                                    .getRoutesForNextHop(nextHop));
            });
            return list;
        }

        private class EvpnInstanceRouteTable {
            private final EvpnInstanceName evpnName;
            private final Map<EvpnInstancePrefix, EvpnInstanceRoute>
                    routesMap = new ConcurrentHashMap<>();
            private final Multimap<EvpnInstanceNextHop, EvpnInstanceRoute>
                    reverseIndex = Multimaps
                    .synchronizedMultimap(HashMultimap.create());
            private final Map<EvpnPrefix, EvpnRoute> routesMapPublic = new
                    ConcurrentHashMap<>();
            private final Multimap<EvpnNextHop, EvpnRoute>
                    reverseIndexPublic = Multimaps
                    .synchronizedMultimap(HashMultimap.create());

            public EvpnInstanceRouteTable(EvpnInstanceName evpnName) {
                this.evpnName = evpnName;
            }

            public void update(Route route) {
                synchronized (this) {
                    if (route instanceof EvpnRoute) {
                        EvpnRoute evpnRoute = (EvpnRoute) route;
                        EvpnPrefix prefix = EvpnPrefix
                                .evpnPrefix(evpnRoute.routeDistinguisher(),
                                            evpnRoute.prefixMac(),
                                            evpnRoute.prefixIp());
                        EvpnNextHop nextHop = EvpnNextHop
                                .evpnNextHop(evpnRoute.ipNextHop(),
                                             evpnRoute.importRouteTarget(),
                                             evpnRoute.exportRouteTarget(),
                                             evpnRoute.label());
                        EvpnRoute oldRoute = routesMapPublic.put(prefix, evpnRoute);
                        reverseIndexPublic.put(nextHop, evpnRoute);

                        if (oldRoute != null) {
                            EvpnNextHop odlNextHop = EvpnNextHop
                                    .evpnNextHop(oldRoute.ipNextHop(),
                                                 oldRoute.importRouteTarget(),
                                                 oldRoute.exportRouteTarget(),
                                                 oldRoute.label());
                            reverseIndex.remove(odlNextHop, oldRoute);
                        }

                        if (evpnRoute.equals(oldRoute)) {
                            // No need to send events if the new route is the same
                            return;
                        }

                        if (oldRoute != null) {
                            notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_REMOVED,
                                                          oldRoute));
                            log.debug("Notify route remove event {}",
                                      evpnRoute);
                            // notifyDelegate(new
                            // EvpnRouteEvent(EvpnRouteEvent.Type.ROUTE_UPDATED,
                            // route));
                        }
                        notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_ADDED,
                                                      evpnRoute));
                        log.debug("Notify route add event {}", evpnRoute);
                    } else if (route instanceof EvpnInstanceRoute) {
                        EvpnInstanceRoute evpnInstanceRoute = (EvpnInstanceRoute) route;
                        if (evpnInstanceRoute.evpnInstanceName()
                                .equals(evpnName)) {
                            EvpnInstanceRoute oldRoute = routesMap
                                    .put(evpnInstanceRoute.getevpnInstancePrefix(),
                                         evpnInstanceRoute);

                            reverseIndex.put(evpnInstanceRoute
                                                     .getEvpnInstanceNextHop(),
                                             evpnInstanceRoute);

                            if (oldRoute != null) {
                                EvpnInstanceNextHop odlNextHop = EvpnInstanceNextHop
                                        .evpnNextHop(oldRoute.getNextHopl(),
                                                     oldRoute.getLabel());
                                reverseIndex.remove(odlNextHop, oldRoute);
                            }

                            if (evpnInstanceRoute.equals(oldRoute)) {
                                // No need to send events if the new route is the same
                                return;
                            }

                            if (oldRoute != null) {
                                notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_REMOVED,
                                                              oldRoute));
                                log.debug("Notify route remove event {}", evpnInstanceRoute);
                            }
                            notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_ADDED,
                                                          evpnInstanceRoute));
                            log.debug("Notify route add event {}", evpnInstanceRoute);
                            notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_ADDED,
                                                          evpnInstanceRoute));
                        }
                    }
                }
            }

            public void remove(Route route) {
                synchronized (this) {
                    if (route instanceof EvpnInstanceRoute) {
                        EvpnInstanceRoute evpnInstanceRoute = (EvpnInstanceRoute) route;
                        if (evpnInstanceRoute.evpnInstanceName()
                                .equals(evpnName)) {
                            EvpnInstanceRoute removedRoute = routesMap
                                    .remove(evpnInstanceRoute.getevpnInstancePrefix());
                            if (removedRoute != null) {
                                reverseIndex.remove(removedRoute.nextHop(),
                                                    removedRoute);
                                notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_REMOVED,
                                                              removedRoute));
                            }
                        }
                    } else if (route instanceof EvpnRoute) {
                        EvpnRoute evpnRoute = (EvpnRoute) route;
                        EvpnPrefix prefix = EvpnPrefix
                                .evpnPrefix(evpnRoute.routeDistinguisher(),
                                            evpnRoute.prefixMac(),
                                            evpnRoute.prefixIp());
                        EvpnRoute removedRoute = routesMapPublic.remove(prefix);

                        if (removedRoute != null) {
                            reverseIndex.remove(removedRoute.nextHop(),
                                                removedRoute);
                            notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_REMOVED,
                                                          removedRoute));
                        }
                    }
                }
            }

            public Collection<Route> getRoutes() {
                List<Route> routes = new LinkedList<>();
                //fetch the private routes and return.
                for (Map.Entry<EvpnInstancePrefix, EvpnInstanceRoute> e :
                        routesMap.entrySet()) {
                    routes.add(e.getValue());
                }
                //fetch the public routes and return.
                for (Map.Entry<EvpnPrefix, EvpnRoute> e : routesMapPublic.entrySet()) {
                    routes.add(e.getValue());
                }
                return routes;
            }

            public Collection<Route> getRoutesForNextHop(NextHop nextHop) {
                List<Route> routes = new LinkedList<Route>();
                if (nextHop instanceof EvpnNextHop) {
                    EvpnNextHop evpnInstancenextHop = (EvpnNextHop) nextHop;
                    routes.addAll(reverseIndexPublic.get(evpnInstancenextHop));
                } else if (nextHop instanceof EvpnInstanceNextHop) {
                    EvpnInstanceNextHop evpnInstancenextHop
                            = (EvpnInstanceNextHop) nextHop;
                    routes.addAll(reverseIndex.get(evpnInstancenextHop));
                }
                return routes;
            }
        }
    }
}