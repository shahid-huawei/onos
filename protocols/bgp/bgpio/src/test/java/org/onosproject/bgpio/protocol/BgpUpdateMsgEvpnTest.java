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
package org.onosproject.bgpio.protocol;


import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.types.BgpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;

/**
 * Test cases for BGP update Message.
 */
public class BgpUpdateMsgEvpnTest {
    protected static final Logger log = LoggerFactory.getLogger(BgpUpdateMsgTest.class);
    public static final byte[] MARKER = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
            (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
    public static final byte UPDATE_MSG_TYPE = 0x2;

    /**
     * This test case checks update message with MP reach EVPN NLRI.
     */
    @Test
    public void bgpUpdateMessageTest43() throws BgpParseException {
        byte[] updateMsg = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x5F, //length
                0x02, //packet type(update)
                0x00, 0x00, //withdrawn len
                0x00, 0x48, //path attribute len
                0x04, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med -> 7

                (byte) 0x80, 0x0e, 0x33, 0x00, 0x19, 0x46, //mpreach with safi = 70
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                //EVPN NLRI START
                0x02, 0x28, //route type and length
                0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, //RD
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, //ESI
                0x00, 0x00, 0x00, 0x03, //Ethernet tag ID
                0x30, //mac address length
                0x08, 0x00, 0x00, 0x00, 0x00, 0x04, //mac address
                0x20, //Ip address length
                0x01, 0x02, 0x03, 0x04, //IP address
                0x01, 0x02, 0x06, //MPLS label 1
                0x01, 0x01, 0x07};  //MPLS Lable2//EVPN NLRI END

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);
        assertThat(message, instanceOf(BgpUpdateMsg.class));
        BgpUpdateMsg other = (BgpUpdateMsg) message;

        assertThat(other.getHeader().getMarker(), is(MARKER));
        assertThat(other.getHeader().getType(), is(UPDATE_MSG_TYPE));
        assertThat(other.getHeader().getLength(), is((short) 95));

    }

    /**
     * This test case checks update message with MP un reach EVPN NLRI.
     */
    @Test
    public void bgpUpdateMessageTest44() throws BgpParseException {
        byte[] updateMsg = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x59, //length
                0x02, //packet type(update)
                0x00, 0x00, //withdrawn len
                0x00, 0x42, //path attribute len
                0x04, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med

                (byte) 0x80, 0x0f, 0x2D, 0x00, 0x19, 0x46, //mp un reach with
                // safi = 70
                //EVPN NLRI START
                0x02, 0x28, //route type and length
                0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, //RD
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, //ESI
                0x00, 0x00, 0x00, 0x03, //Ethernet tag ID
                0x30, //mac address length
                0x08, 0x00, 0x00, 0x00, 0x00, 0x04, //mac address
                0x20, //Ip address length
                0x01, 0x02, 0x03, 0x04, //IP address
                0x01, 0x02, 0x06, //MPLS label 1
                0x01, 0x01, 0x07};  //MPLS Lable2//EVPN NLRI END

        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);
        assertThat(message, instanceOf(BgpUpdateMsg.class));
        BgpUpdateMsg other = (BgpUpdateMsg) message;

        assertThat(other.getHeader().getMarker(), is(MARKER));
        assertThat(other.getHeader().getType(), is(UPDATE_MSG_TYPE));
        assertThat(other.getHeader().getLength(), is((short) 89));

    }

    /**
     * This test case checks update message with MP unreach EVPN NLRI for
     * write the data.
     */
    @Test
    public void bgpUpdateMessageTest45() throws BgpParseException {
        byte[] updateMsg = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x5A, //length
                0x02, //packet type(update)
                0x00, 0x00, //withdrawn len
                0x00, 0x43, //path attribute len
                (byte) 0x40, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med

                (byte) 0x90, 0x0f, 0x00, 0x2D, 0x00, 0x19, 0x46, //mp un reach
                // with
                // safi = 70
                //EVPN NLRI START
                0x02, 0x28, //route type and length
                0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, //RD
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, //ESI
                0x00, 0x00, 0x00, 0x03, //Ethernet tag ID
                0x30, //mac address length
                0x08, 0x00, 0x00, 0x00, 0x00, 0x04, //mac address
                0x20, //Ip address length
                0x01, 0x02, 0x03, 0x04, //IP address
                0x01, 0x02, 0x06, //MPLS label 1
                0x01, 0x01, 0x07};  //MPLS Lable2//EVPN NLRI END

        byte[] testUpdateMsg;
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);
        assertThat(message, instanceOf(BgpUpdateMsg.class));

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testUpdateMsg = new byte[readLen];
        buf.readBytes(testUpdateMsg, 0, readLen);

        assertThat(testUpdateMsg, is(updateMsg));
    }

    /**
     * This test case checks update message with MP reach EVPN NLRI.
     */
    @Test
    public void bgpUpdateMessageTest46() throws BgpParseException {
        byte[] updateMsg = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, 0x00, 0x60, //length
                0x02, //packet type(update)
                0x00, 0x00, //withdrawn len
                0x00, 0x49, //path attribute len
                (byte) 0x40, 0x01, 0x01, 0x00, //origin
                0x40, 0x02, 0x04, 0x02, 0x01, (byte) 0xfd, (byte) 0xe9, //as_path
                (byte) 0x80, 0x04, 0x04, 0x00, 0x00, 0x00, 0x00, //med -> 7

                (byte) 0x90, 0x0e, 0x00, 0x33, 0x00, 0x19, 0x46, //mpreach with
                // safi = 70
                0x04, 0x04, 0x00, 0x00, 0x01, //nexthop
                0x00, //reserved
                //EVPN NLRI START
                0x02, 0x28, //route type and length
                0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, //RD
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, //ESI
                0x00, 0x00, 0x00, 0x03, //Ethernet tag ID
                0x30, //mac address length
                0x08, 0x00, 0x00, 0x00, 0x00, 0x04, //mac address
                0x20, //Ip address length
                0x01, 0x02, 0x03, 0x04, //IP address
                0x01, 0x02, 0x06, //MPLS label 1
                0x01, 0x01, 0x07};  //MPLS Lable2//EVPN NLRI END

        byte[] testUpdateMsg;
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeBytes(updateMsg);

        BgpMessageReader<BgpMessage> reader = BgpFactories.getGenericReader();
        BgpMessage message;
        BgpHeader bgpHeader = new BgpHeader();

        message = reader.readFrom(buffer, bgpHeader);
        assertThat(message, instanceOf(BgpUpdateMsg.class));

        ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
        message.writeTo(buf);

        int readLen = buf.writerIndex();
        testUpdateMsg = new byte[readLen];
        buf.readBytes(testUpdateMsg, 0, readLen);

        assertThat(testUpdateMsg, is(updateMsg));

    }

}
