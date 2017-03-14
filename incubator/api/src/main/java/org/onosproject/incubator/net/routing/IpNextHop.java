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

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

public final class IpNextHop implements NextHop {
    private final IpAddress ip;
    private final NextHopData nextHopData;

    /**
     * Constructor to initialize the parameters.
     *
     * @param ip          IP address
     * @param nextHopData Next hop data
     */
    public IpNextHop(IpAddress ip, NextHopData nextHopData) {
        this.ip = ip;
        this.nextHopData = nextHopData;
    }

    /**
     * Constructor to initialize the parameters.
     *
     * @param ip IP address
     */
    public IpNextHop(IpAddress ip) {
        this.ip = ip;
        nextHopData = null;
    }

    /**
     * Creates the ip next hop.
     *
     * @param ip          ip address
     * @param nextHopData next hop data
     * @return IpNextHop
     */
    public static IpNextHop ipAddress(IpAddress ip,
                                      NextHopData nextHopData) {
        return new IpNextHop(ip, nextHopData);
    }

    /**
     * Creates the ip next hop.
     *
     * @param ip ip address
     * @return IpNextHop
     */
    public static IpNextHop ipAddress(IpAddress ip) {
        return new IpNextHop(ip);
    }

    /**
     * Returns the IP address of the next hop.
     *
     * @return IP address
     */
    public IpAddress ip() {
        return ip;
    }

    /**
     * Returns the MAC address of the next hop.
     *
     * @return MAC address
     */
    public MacAddress mac() {
        return nextHopData.mac();
    }

    /**
     * Returns the location of the next hop.
     *
     * @return Connect point
     */
    public ConnectPoint location() {
        return nextHopData.location();
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, nextHopData);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof NextHop)) {
            return false;
        }

        IpNextHop that = (IpNextHop) other;

        return Objects.equals(this.ip, that.ip) &&
                Objects.equals(this.nextHopData, that.nextHopData);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("ip", ip)
                .add("nextHopData", nextHopData)
                .toString();
    }
}
