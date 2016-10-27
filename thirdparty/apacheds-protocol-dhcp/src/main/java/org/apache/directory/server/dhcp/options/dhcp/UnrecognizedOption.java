/*
 * Copyright (c) 1995-2006 levigo holding gmbh. All Rights Reserved.
 * 
 * This software is the proprietary information of levigo holding gmbh Use is
 * subject to license terms.
 */
package org.apache.directory.server.dhcp.options.dhcp;


import org.apache.directory.server.dhcp.options.DhcpOption;


/**
 * @author Jörg Henne
 */
public class UnrecognizedOption extends DhcpOption
{
    private final byte tag;


    public UnrecognizedOption()
    {
        tag = -1;
    }


    /*
     * @see org.apache.directory.server.dhcp.options.DhcpOption#getTag()
     */
    public byte getTag()
    {
        return tag;
    }


    public UnrecognizedOption(byte tag)
    {
        this.tag = tag;
    }
}
