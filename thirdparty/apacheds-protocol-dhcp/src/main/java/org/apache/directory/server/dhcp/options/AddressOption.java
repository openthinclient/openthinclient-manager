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

package org.apache.directory.server.dhcp.options;


import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * The Dynamic Host Configuration Protocol (DHCP) provides a framework for
 * passing configuration information to hosts on a TCP/IP network. Configuration
 * parameters and other control information are carried in tagged data items
 * that are stored in the 'options' field of the DHCP message. The data items
 * themselves are also called "options." This abstract base class is for options
 * that carry a single IP address.
 */
public abstract class AddressOption extends DhcpOption
{
    private InetAddress address;


    /*
     * @see org.apache.directory.server.dhcp.options.DhcpOption#getData()
     */
    public byte[] getData()
    {
        return address.getAddress();
    }


    /*
     * @see org.apache.directory.server.dhcp.options.DhcpOption#setData(byte[])
     */
    public void setData( byte[] data )
    {
        try
        {
            address = InetAddress.getByAddress( data );
        }
        catch ( UnknownHostException e )
        {
            throw new IllegalArgumentException( "illegal address", e );
        }
    }


    public InetAddress getAddress()
    {
        return address;
    }


    public void setAddress( InetAddress address )
    {
        this.address = address;
    }
}
