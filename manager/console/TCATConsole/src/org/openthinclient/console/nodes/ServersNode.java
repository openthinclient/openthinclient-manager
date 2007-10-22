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
package org.openthinclient.console.nodes;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.MessageFormat;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.Repository;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.FilterNode;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.AddRealmAction;
import org.openthinclient.console.Messages;
import org.openthinclient.console.wizards.registerrealm.RegisterRealmWizardIterator;

// import org.openthinclient.console.nodes.RealmsNode.AddRealmAction;

/** Getting the root node */
public class ServersNode extends FilterNode {
	public ServersNode() throws DataObjectNotFoundException {
		// super(new RealmsNode());
		super(DataObject.find(
				Repository.getDefault().getDefaultFileSystem().getRoot().getFileObject(
						"Servers")).getNodeDelegate()); //$NON-NLS-1$

		getChildren().add(
				new org.openide.nodes.Node[]{new ErrorNode(Messages
						.getString("ServersNode.notFound"))}); //$NON-NLS-1$

		disableDelegation(DELEGATE_GET_DISPLAY_NAME);

	}

	/** An action for adding a realm */
	private static class AddServerAction extends AbstractAction {

		private final DataFolder folder;

		public AddServerAction(DataFolder df) {
			super(Messages.getString("action." //$NON-NLS-1$
					+ AddServerAction.class.getSimpleName()));
			folder = df;
		}

		public void actionPerformed(ActionEvent ae) {
			WizardDescriptor.Iterator iterator = new RegisterRealmWizardIterator();
			WizardDescriptor wizardDescriptor = new WizardDescriptor(iterator);
			wizardDescriptor.setTitleFormat(new MessageFormat("{0} ({1})")); //$NON-NLS-1$
			wizardDescriptor.setTitle(Messages.getString("action." //$NON-NLS-1$
					+ AddRealmAction.class.getSimpleName()));
			Dialog dialog = DialogDisplayer.getDefault().createDialog(
					wizardDescriptor);
			dialog.setVisible(true);
			dialog.toFront();

			if (wizardDescriptor.getValue() == WizardDescriptor.FINISH_OPTION) {
				Realm realm = (Realm) wizardDescriptor.getProperty("realm"); //$NON-NLS-1$
				assert null != realm;

				FileObject fld = folder.getPrimaryFile();
				String baseName = "realm-" + realm.getDn(); //$NON-NLS-1$
				if (fld.getFileObject(baseName, "ser") != null) { //$NON-NLS-1$
					DialogDisplayer.getDefault().notify(
							new NotifyDescriptor.Message(Messages
									.getString("error.RealmAlreadyExists"), //$NON-NLS-1$
									NotifyDescriptor.WARNING_MESSAGE));
					return;
				}

				try {
					FileObject writeTo = fld.createData(baseName, "ser"); //$NON-NLS-1$
					FileLock lock = writeTo.lock();
					try {
						ObjectOutputStream str = new ObjectOutputStream(writeTo
								.getOutputStream(lock));
						try {
							str.writeObject(realm);
						} finally {
							str.close();
						}
					} finally {
						lock.releaseLock();
					}
				} catch (IOException ioe) {
					ErrorManager.getDefault().notify(ioe);
				}
			}
		}
	}

	/** Declaring the Add Feed action and Add Folder action */
	@Override
	public Action[] getActions(boolean popup) {
		DataFolder df = (DataFolder) getLookup().lookup(DataFolder.class);
		return new Action[]{new AddServerAction(df)};
	}

	@Override
	public String getName() {
		return Messages.getString("node." + getClass().getSimpleName()); //$NON-NLS-1$
	}
}
