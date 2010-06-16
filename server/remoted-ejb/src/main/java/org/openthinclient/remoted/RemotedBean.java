/*******************************************************************************
 * openthinclient.org ThinClient suite
 * 
 * Copyright (C) 2004, 2007 levigo holding GmbH. All Rights Reserved.
 * 
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 ******************************************************************************/
package org.openthinclient.remoted;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.jboss.annotation.ejb.RemoteBinding;

@Stateless
@RemoteBinding(jndiBinding = "RemotedBean/remote")
@Remote(RemotedBean.class)
public class RemotedBean implements Remoted {
	private static final Logger logger = Logger.getLogger(RemotedBean.class);

	public boolean dhcpReloadRealms() throws Exception {
		final ObjectName objectName = new ObjectName("tcat:service=ConfigService");
		final MBeanServer server = MBeanServerFactory.findMBeanServer(null).get(0);

		if (Boolean.FALSE.equals(server.invoke(objectName, "reloadRealms",
				new Object[]{}, new String[]{}))) {
			logger.error("Unable to reloadRealms");
			return false;
		} else
			return true;
	}

	/**
	 * Sends a Ping-like-Packet to the Client to see if it is online
	 */
	public boolean pingClient(String ipAddress, String hostname) {

		try {
			if (InetAddress.getLocalHost().getHostAddress().compareToIgnoreCase(
					"127.0.0.1") != 0) {

				if (!isZeroAddress(ipAddress) && pingIp(ipAddress, hostname))
					return true;
				else
					try {
						if (pingHostname(hostname)) {
							if (InetAddress.getByName(hostname).getHostAddress().equals(
									ipAddress))
								return true; // should not happen...
							else {
								logger
										.info("The Hostname \""
												+ hostname
												+ "\" leads to a different IP-Address than stored in the client settings!");
								return true;
							}
						} else
							return false;
					} catch (final UnknownHostException e) {
						return false;
					}
			} else {
				logger.info("Network not reachable - please check cable connection");
				return false;
			}
		} catch (final UnknownHostException e) {
			logger.error(e);
		} catch (final Exception e) {
			logger.error(e);
		}
		return false;
	}

	private boolean pingIp(String ipAddress, String hostname) throws Exception {
		boolean reachable = false;
		reachable = InetAddress.getByName(ipAddress).isReachable(1000);
		logger.info("Ping was sent to " + hostname + "//" + ipAddress
				+ " - reachable:" + reachable);
		return reachable;
	}

	private boolean pingHostname(String hostname) throws Exception {
		boolean reachable = false;
		reachable = InetAddress.getByName(hostname).isReachable(1000);
		logger.info("Ping was sent to " + InetAddress.getByName(hostname)
				+ " - reachable:" + reachable);
		return reachable;
	}

	private boolean isZeroAddress(String ipAddress) {
		byte addr[] = null;

		try {
			addr = InetAddress.getByName(ipAddress).getAddress();
		} catch (final UnknownHostException e) {
			logger.error(e);
		}

		for (int i = 0; i < addr.length; i++)
			if (addr[i] != 0)
				return false;
		return true;
	}
}
