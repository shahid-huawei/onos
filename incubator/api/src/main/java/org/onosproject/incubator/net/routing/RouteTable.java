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

package org.onosproject.incubator.net.routing;

import java.util.Collection;

/**
 * Represents route table for update/remove routes.
 */
public interface RouteTable {
    /**
     * Update the route.
     *
     * @param route route
     */
    void update(Route route);

    /**
     * Remove the route.
     *
     * @param route route
     */
    void remove(Route route);

    /**
     * Get the the routes.
     *
     * @return collection of routes.
     */
    Collection<Route> getRoutes();

    /**
     * Get the routes for next hop.
     *
     * @param nextHop next hop
     * @return collection of routes.
     */
    Collection<Route> getRoutesForNextHop(NextHop nextHop);
}
