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

package org.apache.directory.server.dhcp.options.dhcp;


import org.apache.directory.server.dhcp.options.StringOption;


/**
 * This option is used by a DHCP server to provide an error message to a DHCP
 * client in a DHCPNAK message in the event of a failure. A client may use this
 * option in a DHCPDECLINE message to indicate the why the client declined the
 * offered parameters. The message consists of n octets of NVT ASCII text, which
 * the client may display on an available output device. The code for this
 * option is 56 and its minimum length is 1.
 */
public class Message extends StringOption
{
    /*
     * @see org.apache.directory.server.dhcp.options.DhcpOption#getTag()
     */
    public byte getTag()
    {
        return 56;
    }
}
