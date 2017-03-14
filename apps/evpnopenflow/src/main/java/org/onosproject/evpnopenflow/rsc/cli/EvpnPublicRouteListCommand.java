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

package org.onosproject.evpnopenflow.rsc.cli;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.routing.EvpnRoute;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteService;
import org.onosproject.incubator.net.routing.RouteTableType;

import java.util.Collection;

import static org.onosproject.evpnopenflow.rsc.EvpnConstants.FORMAT_PUBLIC_ROUTE;

/**
 * Support for displaying EVPN public routes.
 */
@Command(scope = "onos", name = "evpn-public-routes", description = "Lists" +
        " all EVPN public routes")
public class EvpnPublicRouteListCommand extends AbstractShellCommand {
    private static final String FORMAT_HEADER =
            "   MAC                  Prefix          Next Hop";

    @Override
    protected void execute() {
        RouteService service = AbstractShellCommand.get(RouteService.class);
        Collection<Route> evpnRoutes = service.getAllRoutes()
                .get(RouteTableType.EVPN_IPV4);
        print(FORMAT_HEADER);
        evpnRoutes.forEach(r -> {
            if (r instanceof EvpnRoute) {
                EvpnRoute evpnRoute = (EvpnRoute) r;
                print(FORMAT_PUBLIC_ROUTE, evpnRoute.prefixMac(),
                      evpnRoute.prefixIp().address().getIp4Address(),
                      evpnRoute.ipNextHop());
            }
        });
    }
}
