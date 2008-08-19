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

import java.util.prefs.BackingStoreException;

import javax.security.auth.callback.CallbackHandler;

import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.util.UsernamePasswordCallbackHandler;
import org.openthinclient.ldap.LDAPConnectionDescriptor;

/**
 * 
 */
public class DeleteAction extends NodeAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/*
	 * @see org.openide.util.actions.CallableSystemAction#asynchronous()
	 */
	@Override
	protected boolean asynchronous() {
		return true;
	}

	/*
	 * @see org.openide.util.actions.NodeAction#performAction(org.openide.nodes.Node[])
	 */
	@Override
	protected void performAction(Node[] nodes) {

		boolean delete = false;
		boolean ask = false;
		if (nodes.length > 1) {
			if (DialogDisplayer.getDefault().notify(
					new NotifyDescriptor.Confirmation(Messages.getString(
							"action.delete.question2", nodes.length),
							NotifyDescriptor.YES_NO_OPTION)) == NotifyDescriptor.YES_OPTION)
				delete = true;
			ask = true;
		}
		for (final Node node : nodes)
			if (node instanceof EditorProvider) {
				final Realm realm = (Realm) node.getLookup().lookup(Realm.class);
				final LDAPConnectionDescriptor lcd = realm.getConnectionDescriptor();
				if (ask == false)
					if (DialogDisplayer.getDefault().notify(
							new NotifyDescriptor.Confirmation(Messages.getString(
									"action.delete.question1", "\"" + node.getName() + "\""),
									NotifyDescriptor.YES_NO_OPTION)) == NotifyDescriptor.YES_OPTION)
						delete = true;

				if (delete == true) {
					String realmName = lcd.getHostname() + lcd.getBaseDN();

					CallbackHandler handler = lcd.getCallbackHandler();
					UsernamePasswordCallbackHandler call = (UsernamePasswordCallbackHandler) handler;

					try {
						RealmManager.deregisterRealm(realmName);
						call.deleteCredentials();
					} catch (BackingStoreException e) {
						ErrorManager.getDefault().notify(e);
					}
				}
			}
	}

	/*
	 * @see org.openide.util.actions.NodeAction#enable(org.openide.nodes.Node[])
	 */
	@Override
	protected boolean enable(Node[] arg0) {
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
