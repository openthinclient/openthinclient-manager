/*
 * Copyright (c) 1995-2006 levigo holding gmbh. All Rights Reserved.
 * 
 * This software is the proprietary information of levigo holding gmbh
 * Use is subject to license terms.
 */
package org.apache.directory.server.dhcp.store;


import java.net.InetAddress;

import org.apache.directory.server.dhcp.messages.HardwareAddress;


/**
 * The definition of a host.
 */
public class Host extends DhcpConfigElement
{
    private final String name;

    private HardwareAddress hardwareAddress;

    /**
     * The host's fixed address. May be <code>null</code>.
     */
    private InetAddress address;


    public Host(String name, InetAddress address, HardwareAddress hardwareAddress)
    {
        this.name = name;
        this.address = address;
        this.hardwareAddress = hardwareAddress;
    }


    public HardwareAddress getHardwareAddress()
    {
        return hardwareAddress;
    }


    public String getName()
    {
        return name;
    }


    public InetAddress getAddress()
    {
        return address;
    }
}
