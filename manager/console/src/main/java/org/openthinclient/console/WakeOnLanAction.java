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
package org.openthinclient.console;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.openide.ErrorManager;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Realm;
import org.openthinclient.remoted.Remoted;

/**
 * Provides the logic to send a MagicPacket to the Client by means of its
 * MAC-Address
 */
public class WakeOnLanAction extends NodeAction {

	private static final long serialVersionUID = 1L;

	/*
	 * @see
	 * org.openide.util.actions.NodeAction#performAction(org.openide.nodes.Node[])
	 */
	@Override
	protected void performAction(Node[] nodes) {

		String broadcast = "";
		String macAddress = "";
		for (final Node node : nodes) {
			macAddress = ((Client) (DirectoryObject) node.getLookup().lookup(
					DirectoryObject.class)).getMacAddress();

			try {
				final Enumeration<NetworkInterface> nets = NetworkInterface
						.getNetworkInterfaces();
				for (final NetworkInterface netint : Collections.list(nets)) {
					final Enumeration<InetAddress> inetAddresses = netint
							.getInetAddresses();
					for (final InetAddress inetAddress : Collections.list(inetAddresses))
						if (inetAddress instanceof Inet4Address
								&& !inetAddress.isLoopbackAddress()) {
							final List<InterfaceAddress> interfaceAddresses = netint
									.getInterfaceAddresses();
							for (final InterfaceAddress addr : interfaceAddresses)
								if (inetAddress.equals(addr.getAddress())) {
									broadcast = addr.getBroadcast().getHostAddress();

									final Realm realm = (Realm) node.getLookup().lookup(
											Realm.class);

									try {

										String schemaProviderName;
										if (null != realm.getSchemaProviderName())
											schemaProviderName = realm.getSchemaProviderName();
										else if (null != realm.getConnectionDescriptor()
												.getHostname())
											schemaProviderName = realm.getConnectionDescriptor()
													.getHostname();
										else
											schemaProviderName = "localhost";

										final Properties p = new Properties();
										p.setProperty("java.naming.factory.initial",
												"org.jnp.interfaces.NamingContextFactory");
										p.setProperty("java.naming.provider.url", "jnp://"
												+ schemaProviderName + ":1099");

										final InitialContext initialContext = new InitialContext(p);

										Remoted remoted;
										remoted = (Remoted) initialContext
												.lookup("RemotedBean/remote");
										remoted.wakeOnLan(broadcast, macAddress);

										initialContext.close();

									} catch (final NamingException e) {
										e.printStackTrace();
										ErrorManager.getDefault().notify(e);
									}
								}
						}
				}
			} catch (final SocketException e) {
				ErrorManager.getDefault().notify(e);
				e.printStackTrace();
			}
		}
	}

	/*
	 * @see org.openide.util.actions.CallableSystemAction#asynchronous()
	 */
	@Override
	protected boolean asynchronous() {
		return true;
	}

	/*
	 * @see org.openide.util.actions.NodeAction#enable(org.openide.nodes.Node[])
	 */
	@Override
	protected boolean enable(Node[] activatedNodes) {
		for (final Node node : activatedNodes)
			if (ClientStatus.getClientStatus(node.getName()).equals("Online"))
				return false;
		return true;
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getName()
	 */
	@Override
	public String getName() {
		return Messages.getString("action." + this.getClass().getSimpleName()); //$NON-NLS-1$
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getHelpCtx()
	 */
	@Override
	public HelpCtx getHelpCtx() {
		return null;
	}

}
