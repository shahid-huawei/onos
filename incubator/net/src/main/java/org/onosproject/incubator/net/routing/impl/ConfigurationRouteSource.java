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

package org.onosproject.incubator.net.routing.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.routing.IpRoute;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteAdminService;
import org.onosproject.incubator.net.routing.RouteConfig;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.SubjectFactories;

import java.util.HashSet;
import java.util.Set;

/**
 * Route source that installs static routes configured in the network configuration.
 */
@Component(immediate = true)
public class ConfigurationRouteSource {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry netcfgRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouteAdminService routeService;

    private final ConfigFactory<ApplicationId, RouteConfig> routeConfigFactory =
            new ConfigFactory<ApplicationId, RouteConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY,
                    RouteConfig.class, "routes", true) {
                @Override
                public RouteConfig createConfig() {
                    return new RouteConfig();
                }
            };
    private final InternalNetworkConfigListener netcfgListener =
            new InternalNetworkConfigListener();

    @Activate
    protected void activate() {
        netcfgRegistry.addListener(netcfgListener);
        netcfgRegistry.registerConfigFactory(routeConfigFactory);
    }

    @Deactivate
    protected void deactivate() {
        netcfgRegistry.removeListener(netcfgListener);
        netcfgRegistry.unregisterConfigFactory(routeConfigFactory);
    }

    private void processRouteConfigAdded(NetworkConfigEvent event) {
        Set<Route> routes = ((RouteConfig) event.config().get()).getRoutes();
        routeService.update(routes);
    }

    private void processRouteConfigUpdated(NetworkConfigEvent event) {
        Set<Route> routes = ((RouteConfig) event.config().get()).getRoutes();
        Set<Route> prevRoutes = ((RouteConfig) event.prevConfig().get()).getRoutes();
        Set<Route> pendingRemove = new HashSet<>();
        Set<Route> pendingUpdate = new HashSet<>();
        IpRoute ipPrevRoute = null;
        for (Route prevRoute : prevRoutes) {
            if (prevRoute instanceof IpRoute) {
                ipPrevRoute = (IpRoute) prevRoute;
            }
            for (Route route : routes) {
                if (route instanceof IpRoute) {
                    IpRoute ipRoute = (IpRoute) route;
                    if ((ipPrevRoute != null)
                            && !ipRoute.prefix().equals(ipPrevRoute.prefix())) {
                        pendingRemove.add(ipRoute);
                    }
                }
            }
        }
        IpRoute ipRoute = null;
        for (Route route : routes) {
            if (route instanceof IpRoute) {
                ipRoute = (IpRoute) route;
            }

            if (!pendingRemove.contains(route)) {
                pendingUpdate.add(ipRoute);
            }
        }

        routeService.update(pendingUpdate);
        routeService.withdraw(pendingRemove);
    }

    private void processRouteConfigRemoved(NetworkConfigEvent event) {
        Set<Route> prevRoutes = ((RouteConfig) event.prevConfig().get()).getRoutes();
        routeService.withdraw(prevRoutes);
    }

    private class InternalNetworkConfigListener implements
            NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass().equals(RouteConfig.class)) {
                switch (event.type()) {
                    case CONFIG_ADDED:
                        processRouteConfigAdded(event);
                        break;
                    case CONFIG_UPDATED:
                        processRouteConfigUpdated(event);
                        break;
                    case CONFIG_REMOVED:
                        processRouteConfigRemoved(event);
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
