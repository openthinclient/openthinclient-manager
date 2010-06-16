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
import org.openthinclient.console.nodes.DirObjectListNode;
import org.openthinclient.remoted.Remoted;

/**
 * Provides the logic to send Ping-like-Packets to the Client by means of
 * Threads (one per Client)
 */
public class PingClientAction extends NodeAction {

	private static final long serialVersionUID = 1L;
	private int responseCounter;

	/*
	 * @see
	 * org.openide.util.actions.NodeAction#performAction(org.openide.nodes.Node[])
	 */
	@Override
	protected void performAction(Node[] nodes) {
		responseCounter = 0;
		if (nodes[0] instanceof DirObjectListNode) {
			final int length = nodes[0].getChildren().getNodes().length;
			for (final Node childNode : nodes[0].getChildren().getNodes()) {
				final Thread thread = new Thread() {
					@Override
					public void run() {
						pingClient(childNode);
					}
				};
				thread.start();

				if (childNode.equals(nodes[0].getChildren().getNodes()[length - 1]))
					while (true)
						if (responseCounter == length) {
							refreshNode(nodes[0]);
							break;
						}
			}
		} else {
			Thread thread;
			for (final Node node : nodes) {
				thread = new Thread() {
					@Override
					public void run() {
						pingClient(node);
					}
				};
				thread.start();
				if (node.equals(nodes[nodes.length - 1]))
					while (true)
						if (responseCounter == nodes.length) {
							refreshNode(nodes[0].getParentNode());
							break;
						}
			}
		}
	}

	private void pingClient(Node node) {
		final String ipAddress = ((Client) (DirectoryObject) node.getLookup()
				.lookup(DirectoryObject.class)).getIpHostNumber();
		final String hostname = ((Client) (DirectoryObject) node.getLookup()
				.lookup(DirectoryObject.class)).getName();
		boolean clientStatus = false;
		final boolean isZero = false;

		final Realm realm = (Realm) node.getLookup().lookup(Realm.class);

		try {
			String schemaProviderName;
			if (null != realm.getSchemaProviderName())
				schemaProviderName = realm.getSchemaProviderName();
			else if (null != realm.getConnectionDescriptor().getHostname())
				schemaProviderName = realm.getConnectionDescriptor().getHostname();
			else
				schemaProviderName = "localhost";

			final Properties p = new Properties();
			p.setProperty("java.naming.factory.initial",
					"org.jnp.interfaces.NamingContextFactory");
			p.setProperty("java.naming.provider.url", "jnp://" + schemaProviderName
					+ ":1099");

			final InitialContext initialContext = new InitialContext(p);

			final Remoted remoted = (Remoted) initialContext
					.lookup("RemotedBean/remote");
			clientStatus = remoted.pingClient(ipAddress, hostname);
			initialContext.close();

		} catch (final NamingException e) {
			e.printStackTrace();
			ErrorManager.getDefault().notify(e);
		}
		setStatus(node, clientStatus, isZero);
		responseCounter++;
	}

	private void setStatus(Node node, boolean clientStatus, boolean isZero) {
		if (isZero) {
		//Statische Methode
			ClientStatus.setClientStatus(node.getName(), "Unchecked");
		//Objektorientierter Ansatz - nicht funktionsfähig atm!
		//	((Client) (DirectoryObject) node.getLookup()
		//			.lookup(DirectoryObject.class)).setStatus("Unchecked");
		} else if (clientStatus) {
			ClientStatus.setClientStatus(node.getName(), "Online");
		//	((Client) (DirectoryObject) node.getLookup()
		//			.lookup(DirectoryObject.class)).setStatus("Online");
		} else {
			ClientStatus.setClientStatus(node.getName(), "Offline");
		//	((Client) (DirectoryObject) node.getLookup()
		//			.lookup(DirectoryObject.class)).setStatus("Offline");
		}
	}

	private void refreshNode(Node node) {
		if (node instanceof Refreshable)
			((Refreshable) node).refresh();
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
		// if (!(activatedNodes.length == 0))
		// if ((DirectoryObject) activatedNodes[0].getLookup().lookup(
		// DirectoryObject.class) instanceof Client)
		// for (final Node node : activatedNodes)
		// if (isZeroAddress(((Client) (DirectoryObject) node.getLookup()
		// .lookup(DirectoryObject.class)).getIpHostNumber()))
		// return false;

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