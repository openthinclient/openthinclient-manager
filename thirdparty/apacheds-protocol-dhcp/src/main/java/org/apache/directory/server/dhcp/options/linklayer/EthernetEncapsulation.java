/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.directory.server.dhcp.options.linklayer;


import org.apache.directory.server.dhcp.options.ByteOption;


/**
 * This option specifies whether or not the client should use Ethernet Version 2
 * (RFC 894) or IEEE 802.3 (RFC 1042) encapsulation if the interface is an
 * Ethernet. A value of 0 indicates that the client should use RFC 894
 * encapsulation. A value of 1 means that the client should use RFC 1042
 * encapsulation. The code for this option is 36, and its length is 1.
 */
public class EthernetEncapsulation extends ByteOption
{
    /*
     * @see org.apache.directory.server.dhcp.options.DhcpOption#getTag()
     */
    public byte getTag()
    {
        return 36;
    }
}
