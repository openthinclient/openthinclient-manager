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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

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
	 * Sends a MagicPacket (WakeOnLan) to the Client
	 */
	public boolean wakeOnLan(String broadcast, String macAddress) {

		final int PORT = 9;

		try {
			final byte[] macBytes = getMacBytes(macAddress);
			final byte[] bytes = new byte[6 + 16 * macBytes.length];
			for (int i = 0; i < 6; i++)
				bytes[i] = (byte) 0xff;
			for (int i = 6; i < bytes.length; i += macBytes.length)
				System.arraycopy(macBytes, 0, bytes, i, macBytes.length);

			final InetAddress address = InetAddress.getByName(broadcast);
			final DatagramPacket packet = new DatagramPacket(bytes, bytes.length,
					address, PORT);
			final DatagramSocket socket = new DatagramSocket();
			socket.send(packet);
			socket.close();

			logger.info("Wake-On-LAN packet was sent to " + address + " - "
					+ macAddress);
		} catch (final Exception e) {
			logger.info("Failed to send Wake-on-LAN packet: + e");
		}
		return true;
	}

	/**
	 * Converts String to Byte-Stream if it is a valid MAC-Address
	 * 
	 * @param macStr
	 * @return
	 * @throws IllegalArgumentException
	 */
	private static byte[] getMacBytes(String macStr)
			throws IllegalArgumentException {
		final byte[] bytes = new byte[6];
		final String[] hex = macStr.split("(\\:|\\-)");
		if (hex.length != 6)
			throw new IllegalArgumentException("Invalid MAC address.");
		try {
			for (int i = 0; i < 6; i++)
				bytes[i] = (byte) Integer.parseInt(hex[i], 16);
		} catch (final NumberFormatException e) {
			throw new IllegalArgumentException("Invalid hex digit in MAC address.");
		}
		return bytes;
	}
}
