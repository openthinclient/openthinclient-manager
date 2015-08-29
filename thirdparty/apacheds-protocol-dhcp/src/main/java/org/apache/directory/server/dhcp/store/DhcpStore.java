/*
 * Copyright (c) 1995-2006 levigo holding gmbh. All Rights Reserved.
 * 
 * This software is the proprietary information of levigo holding gmbh  
 * Use is subject to license terms.
 */
package org.apache.directory.server.dhcp.store;


import java.net.InetAddress;

import org.apache.directory.server.dhcp.DhcpException;
import org.apache.directory.server.dhcp.messages.HardwareAddress;
import org.apache.directory.server.dhcp.options.OptionsField;
import org.apache.directory.server.dhcp.service.Lease;


/**
 * @author J�rg Henne
 */
public interface DhcpStore
{
    /**
     * Find a lease to offer in response to a DHCPDISCOVER request.
     * <p>
     * The lease to offer should be determined by an algorithme like the
     * following:
     * <ul>
     * <li> Try to find an existing lease for the given hardware address. The
     * lease may be either ACTIVE or EXPIRED.
     * <li>Try to find a lease which has been explicitely dedicated to the
     * given hardware address.
     * <li>Try to get a lease from a pool of leases. If the client requested a
     * specific address, the request should be honored, if possible. Otherwise
     * the selection of an address should be based on the selection base address
     * and may be refined using the supplied options.
     * </ul>
     * <p>
     * If the requestedLeaseTime is >= 0, the validity duration of the returned
     * lease must be updated, so that the lease is valid for at least the
     * specified time. The duration may, however, be constrained by a configured
     * maximum lease time.
     * 
     * @param hardwareAddress
     *            hardwareAddress the hardware address of the client requesting
     *            the lease.
     * @param requestedAddress
     *            the address requested by the client or <code>null</code> if
     *            the client did not request a specific address.
     * @param selectionBase
     *            the address on which to base the selection of a lease from a
     *            pool, i.e. either the address of the interface on which the
     *            request was received or the address of a DHCP relay agent.
     * @param requestedLeaseTime
     *            the lease time in milliseconds as requested by the client, or
     *            -1 if the client did not request a specific lease time.
     * @param options
     *            the supplied DHCP options. Lease selection may be refined by
     *            using those options
     * @return a lease or <code>null</code> if no matching lease was found.
     * @throws DhcpException
     */
    Lease getLeaseOffer( HardwareAddress hardwareAddress, InetAddress requestedAddress, InetAddress selectionBase,
        long requestedLeaseTime, OptionsField options ) throws DhcpException;


    /**
     * Retrieve an existing lease from the dhcp store.
     * 
     * @param hardwareAddress
     * @param requestedAddress
     * @param selectionBase
     * @param requestedLeaseTime
     * @param options
     * @return
     * @throws DhcpException 
     */
    Lease getExistingLease( HardwareAddress hardwareAddress, InetAddress requestedAddress, InetAddress selectionBase,
        long requestedLeaseTime, OptionsField options ) throws DhcpException;


    /**
     * Release the specified lease. 
     * 
     * @param lease
     */
    void releaseLease( Lease lease );
}
