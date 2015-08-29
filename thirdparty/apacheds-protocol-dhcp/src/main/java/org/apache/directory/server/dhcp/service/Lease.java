/*
 * Copyright (c) 1995-2006 levigo holding gmbh. All Rights Reserved.
 * 
 * This software is the proprietary information of levigo holding gmbh  
 * Use is subject to license terms.
 */
package org.apache.directory.server.dhcp.service;


import java.net.InetAddress;

import org.apache.directory.server.dhcp.messages.HardwareAddress;
import org.apache.directory.server.dhcp.options.OptionsField;


/**
 * Leases represent a temporary assignment of an IP address to a DHCP client.
 */
public class Lease
{
    /** Lease state: newly created */
    public static final int STATE_NEW = 1;

    /** Lease state: offered to client */
    public static final int STATE_OFFERED = 2;

    /** Lease state: active - assigned to client */
    public static final int STATE_ACTIVE = 3;

    /** Lease state: released by client */
    public static final int STATE_RELEASED = 4;

    /** Lease state: expired */
    public static final int STATE_EXPIRED = 5;

    /**
     * The lease's state.
     * 
     * @see #STATE_NEW
     * @see #STATE_OFFERED
     * @see #STATE_ACTIVE
     * @see #STATE_RELEASED
     * @see #STATE_EXPIRED
     */
    private int state;

    /**
     * The assigned client address.
     */
    private InetAddress clientAddress;

    /**
     * The client's hardware address.
     */
    private HardwareAddress hardwareAddress;

    /**
     * The next-server (boot-server) address.
     */
    private InetAddress nextServerAddress;

    /**
     * The DhcpOptions to provide to the client along with the lease.
     */
    private OptionsField options = new OptionsField();

    private long acquired = -1;

    private long expires = -1;


    /**
     * @return
     */
    public InetAddress getClientAddress()
    {
        return clientAddress;
    }


    /**
     * @return
     */
    public InetAddress getNextServerAddress()
    {
        return nextServerAddress;
    }


    /**
     * @return
     */
    public OptionsField getOptions()
    {
        return options;
    }


    /**
     * @return
     */
    public int getState()
    {
        return state;
    }


    /**
     * @param state2
     */
    public void setState( int state )
    {
        this.state = state;
    }


    public HardwareAddress getHardwareAddress()
    {
        return hardwareAddress;
    }


    public void setHardwareAddress( HardwareAddress hardwareAddress )
    {
        this.hardwareAddress = hardwareAddress;
    }


    public long getAcquired()
    {
        return acquired;
    }


    public void setAcquired( long acquired )
    {
        this.acquired = acquired;
    }


    public long getExpires()
    {
        return expires;
    }


    public void setExpires( long expires )
    {
        this.expires = expires;
    }


    public void setClientAddress( InetAddress clientAddress )
    {
        this.clientAddress = clientAddress;
    }

}
