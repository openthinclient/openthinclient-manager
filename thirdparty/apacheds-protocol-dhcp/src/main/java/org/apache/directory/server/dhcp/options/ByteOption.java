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


/**
 * The Dynamic Host Configuration Protocol (DHCP) provides a framework for
 * passing configuration information to hosts on a TCP/IP network. Configuration
 * parameters and other control information are carried in tagged data items
 * that are stored in the 'options' field of the DHCP message. The data items
 * themselves are also called "options." This abstract base class is for options
 * that carry an unsigned short value (16 bit).
 */
public abstract class ByteOption extends DhcpOption
{
    /**
     * The byte value (represented as a short because of the unsignedness).
     */
    private short byteValue;


    /*
     * @see org.apache.directory.server.dhcp.options.DhcpOption#setData(byte[])
     */
    public void setData( byte[] data )
    {
        byteValue = ( short ) ( data[0] & 0xff );
    }


    /*
     * @see org.apache.directory.server.dhcp.options.DhcpOption#getData()
     */
    public byte[] getData()
    {
        return new byte[]
            { ( byte ) ( byteValue & 0xff ) };
    }


    public short getByteValue()
    {
        return byteValue;
    }


    public void setShortValue( short shortValue )
    {
        this.byteValue = shortValue;
    }
}
