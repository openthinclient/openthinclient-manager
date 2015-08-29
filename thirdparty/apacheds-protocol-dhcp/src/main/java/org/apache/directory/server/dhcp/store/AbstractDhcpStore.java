/*
 * Copyright (c) 1995-2006 levigo holding gmbh. All Rights Reserved.
 * 
 * This software is the proprietary information of levigo holding gmbh  
 * Use is subject to license terms.
 */
package org.apache.directory.server.dhcp.store;


import java.net.InetAddress;
import java.util.Map;

import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.messages.HardwareAddress;
import org.apache.directory.server.dhcp.options.OptionsField;
import org.apache.directory.server.dhcp.options.vendor.HostName;
import org.apache.directory.server.dhcp.options.vendor.SubnetMask;
import org.apache.directory.server.dhcp.service.Lease;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author J�rg Henne
 */
public abstract class AbstractDhcpStore implements DhcpStore
{
    private static final Logger logger = LoggerFactory.getLogger( AbstractDhcpStore.class );


    /*
     * @see org.apache.directory.server.dhcp.service.DhcpStore#getLeaseOffer(org.apache.directory.server.dhcp.messages.HardwareAddress,
     *      java.net.InetAddress, java.net.InetAddress, long,
     *      org.apache.directory.server.dhcp.options.OptionsField)
     */
    public Lease getLeaseOffer( HardwareAddress hardwareAddress, InetAddress requestedAddress,
        InetAddress selectionBase, long requestedLeaseTime, OptionsField options ) throws DhcpException
    {
        Subnet subnet = findSubnet( selectionBase );
        if ( null == subnet )
        {
            logger.warn( "Don't know anything about the sbnet containing " + selectionBase );
            return null;
        }

        // try to find existing lease
        Lease lease = null;
        lease = findExistingLease( hardwareAddress, lease );
        if ( null != lease )
            return lease;

        Host host = null;
        host = findDesignatedHost( hardwareAddress );
        if ( null != host )
        {
            // make sure that the host is actually within the subnet. Depending
            // on the way the DhcpStore configuration is implemented, it is not
            // possible to violate this condition, but we can't be sure.
            if ( !subnet.contains( host.getAddress() ) )
            {
                logger.warn( "Host " + host + " is not within the subnet for which an address is requested" );
            }
            else
            {
                // build properties map
                Map properties = getProperties( subnet );
                properties.putAll( getProperties( host ) );

                // build lease
                lease = new Lease();
                lease.setAcquired( System.currentTimeMillis() );

                long leaseTime = determineLeaseTime( requestedLeaseTime, properties );

                lease.setExpires( System.currentTimeMillis() + leaseTime );

                lease.setHardwareAddress( hardwareAddress );
                lease.setState( Lease.STATE_NEW );
                lease.setClientAddress( host.getAddress() );

                // set lease options
                OptionsField o = lease.getOptions();

                // set (client) host name
                o.add( new HostName( host.getName() ) );

                // add subnet settings
                o.add( new SubnetMask( subnet.getNetmask() ) );
                o.merge( subnet.getOptions() );

                // add the host's options. they override existing
                // subnet options as they take the precedence.
                o.merge( host.getOptions() );
            }
        }

        if ( null == lease )
        {
            // FIXME: use selection base to find a lease in a pool.
        }

        // update the lease state
        if ( null != lease && lease.getState() != Lease.STATE_ACTIVE )
        {
            lease.setState( Lease.STATE_OFFERED );
            updateLease( lease );
        }

        return lease;
    }


    /*
     * @see org.apache.directory.server.dhcp.store.DhcpStore#getExistingLease(org.apache.directory.server.dhcp.messages.HardwareAddress,
     *      java.net.InetAddress, java.net.InetAddress, long,
     *      org.apache.directory.server.dhcp.options.OptionsField)
     */
    public Lease getExistingLease( HardwareAddress hardwareAddress, InetAddress requestedAddress,
        InetAddress selectionBase, long requestedLeaseTime, OptionsField options ) throws DhcpException
    {
        // try to find existing lease. if we don't find a lease based on the
        // client's
        // hardware address, we send a NAK.
        Lease lease = null;
        lease = findExistingLease( hardwareAddress, lease );
        if ( null == lease )
            return null;

        // check whether the notions of the client address match
        if ( !lease.getClientAddress().equals( requestedAddress ) )
        {
            logger.warn( "Requested address " + requestedAddress + " for " + hardwareAddress
                + " doesn't match existing lease " + lease );
            return null;
        }

        // check whether addresses and subnet match
        Subnet subnet = findSubnet( selectionBase );
        if ( null == subnet )
        {
            logger.warn( "No subnet found for existing lease " + lease );
            return null;
        }
        if ( !subnet.contains( lease.getClientAddress() ) )
        {
            logger.warn( "Client with existing lease " + lease + " is on wrong subnet " + subnet );
            return null;
        }
        if ( !subnet.isInRange( lease.getClientAddress() ) )
        {
            logger.warn( "Client with existing lease " + lease + " is out of valid range for subnet " + subnet );
            return null;
        }

        // build properties map
        Map properties = getProperties( subnet );

        // update lease options
        OptionsField o = lease.getOptions();
        o.clear();

        // add subnet settings
        o.add( new SubnetMask( subnet.getNetmask() ) );
        o.merge( subnet.getOptions() );

        // check whether there is a designated host.
        Host host = findDesignatedHost( hardwareAddress );
        if ( null != host )
        {
            // check whether the host matches the address (using a fixed
            // host address is mandatory).
            if ( host.getAddress() != null && !host.getAddress().equals( lease.getClientAddress() ) )
            {
                logger.warn( "Existing fixed address for " + hardwareAddress + " conflicts with existing lease "
                    + lease );
                return null;
            }

            properties.putAll( getProperties( host ) );

            // set (client) host name
            o.add( new HostName( host.getName() ) );

            // add the host's options
            o.merge( host.getOptions() );
        }

        // update other lease fields
        long leaseTime = determineLeaseTime( requestedLeaseTime, properties );
        lease.setExpires( System.currentTimeMillis() + leaseTime );
        lease.setHardwareAddress( hardwareAddress );

        // update the lease state
        if ( lease.getState() != Lease.STATE_ACTIVE )
        {
            lease.setState( Lease.STATE_ACTIVE );
            updateLease( lease );
        }

        // store information about the lease
        updateLease( lease );

        return lease;
    }


    /**
     * Determine the lease time based on the time requested by the client, the
     * properties and a global default.
     * 
     * @param requestedLeaseTime
     * @param properties
     * @return
     */
    private long determineLeaseTime( long requestedLeaseTime, Map properties )
    {
        // built-in default
        long leaseTime = 1000L * 3600;
        Integer propMaxLeaseTime = ( Integer ) properties.get( DhcpConfigElement.PROPERTY_MAX_LEASE_TIME );
        if ( null != propMaxLeaseTime )
            if ( requestedLeaseTime > 0 )
                leaseTime = Math.min( propMaxLeaseTime.intValue() * 1000L, requestedLeaseTime );
            else
                leaseTime = propMaxLeaseTime.intValue() * 1000L;
        return leaseTime;
    }


    /*
     * @see org.apache.directory.server.dhcp.store.DhcpStore#releaseLease(org.apache.directory.server.dhcp.service.Lease)
     */
    public void releaseLease( Lease lease )
    {
        lease.setState( Lease.STATE_RELEASED );
        updateLease( lease );
    }


    /**
     * Update the (possibly changed) lease in the store.
     * 
     * @param lease
     */
    protected abstract void updateLease( Lease lease );


    /**
     * Return a list of all options applicable to the given config element. List
     * list must contain the options specified for the element and all parent
     * elements in an aggregated fashion. For instance, the options for a host
     * must include the global default options, the options of classes the host
     * is a member of, the host's group options and the host's options.
     * 
     * @param element
     * @return
     */
    protected abstract OptionsField getOptions( DhcpConfigElement element );


    /**
     * Return a list of all options applicable to the given config element. List
     * list must contain the options specified for the element and all parent
     * elements in an aggregated fashion. For instance, the options for a host
     * must include the global default options, the options of classes the host
     * is a member of, the host's group options and the host's options.
     * 
     * @param element
     * @return
     */
    protected abstract Map getProperties( DhcpConfigElement element );


    /**
     * Find an existing lease in the store.
     * 
     * @param hardwareAddress
     * @param existingLease
     * @return
     */
    protected abstract Lease findExistingLease( HardwareAddress hardwareAddress, Lease existingLease );


    /**
     * Find a host to with the explicitely designated hardware address.
     * 
     * @param hardwareAddress
     * @return
     * @throws DhcpException
     */
    protected abstract Host findDesignatedHost( HardwareAddress hardwareAddress ) throws DhcpException;


    /**
     * Find the subnet definition matching the given address.
     * 
     * @param clientAddress
     * @return
     */
    protected abstract Subnet findSubnet( InetAddress clientAddress );
}
