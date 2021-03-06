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

package org.onosproject.netconf.client.api;

import org.onosproject.net.DeviceId;
import com.google.common.annotations.Beta;
import org.onosproject.yang.runtime.CompositeData;

import java.io.IOException;

/**
 * Interface for making some standard calls to NETCONF devices with
 * serialization performed according the appropriate device's YANG model.
 */
@Beta
public interface NetconfTranslator {
    /**
     * Retrieves and returns the configuration of the specified device in the scope
     * specified by the filter provided by the supplied {@link CompositeData}.
     *
     * @param deviceId the deviceID of the device to be contacted
     * @return a {@link CompositeData} containing the requested configuration
     * @throws IOException if serialization fails or the netconf subsystem is
     * unable to handle the request
     */
    /*FIXME note in the comments I believe that the composite data should
    provide the filter but I am unsure what data the yang runtime will provide */
    /*TODO a future version of this API will support an optional filter type.*/
    CompositeData getDeviceConfig(DeviceId deviceId) throws IOException;


    /**
     * Adds to, overwrites, or deletes the selected device's configuration in the scope
     * and manner specified by the CompositeData.
     *
     * @param deviceId the deviceID fo the device to be contacted
     * @param compositeData the representation of the configuration to be pushed
     *                      to the device via NETCONF as well as the filter to
     *                      be used.
     * @return a boolean, true if the operation succeeded, false otherwise
     * @throws IOException if serialization fails or the netconf subsystem is
     * unable to handle the request
     */
    boolean editDeviceConfig(DeviceId deviceId, CompositeData compositeData) throws IOException;

    /* FIXME eventually expose the copy, delete, lock and unlock netconf methods */

    /**
     * Returns the configuration and running statistics from the specified device
     * in the scope specified by the filter provided by the {@link CompositeData}.
     *
     * @param deviceId the deviceID of the device to be contacted.
     * @return a {@link CompositeData} containing the requested configuration
     * and statistics
     * @throws IOException if serialization fails or the netconf subsystem is
     * unable to handle the request
     */
    /*TODO a future version of this API will support an optional filter type.*/

    CompositeData getDeviceState(DeviceId deviceId) throws IOException;
}
