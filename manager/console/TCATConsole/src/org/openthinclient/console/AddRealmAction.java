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

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.MessageFormat;

import javax.security.auth.callback.CallbackHandler;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;

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
import org.openide.util.HelpCtx;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.util.UsernamePasswordCallbackHandler;
import org.openthinclient.console.wizards.registerrealm.RegisterRealmWizardIterator;

import com.levigo.util.swing.IconManager;

/** An action for adding a realm */
// @SuppressWarnings("serial")
public class AddRealmAction extends AbstractAction implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final DataFolder folder;

	private static Realm newRealm;

	private static boolean automaticRegistration = false;

	public AddRealmAction() throws DataObjectNotFoundException {
		super(Messages.getString("action." + AddRealmAction.class.getSimpleName()),
				new ImageIcon(AddRealmAction.class.getResource("otc-add.png")));

		folder = (DataFolder) DataObject.find(Repository.getDefault()
				.getDefaultFileSystem().getRoot().getFileObject("Realms")); //$NON-NLS-1$

	}

	public AddRealmAction(DataFolder df) {
		super(
				Messages.getString("action." + AddRealmAction.class.getSimpleName()), new ImageIcon(IconManager.getInstance(DetailViewProvider.class, "icons").getImage( //$NON-NLS-1$
										"Computer"))); //$NON-NLS-1$
		folder = df;
	}

	public void actionPerformed(ActionEvent ae) {
		WizardDescriptor.Iterator iterator = new RegisterRealmWizardIterator();
		WizardDescriptor wizardDescriptor = new WizardDescriptor(iterator);
		wizardDescriptor.setTitleFormat(new MessageFormat("{0} ({1})")); //$NON-NLS-1$
		wizardDescriptor.setTitle(Messages.getString("action." //$NON-NLS-1$
				+ AddRealmAction.class.getSimpleName()));
		Dialog dialog = DialogDisplayer.getDefault().createDialog(wizardDescriptor);
		wizardDescriptor.putProperty("enableForward", false);
		// Dialog dialog = DialogDisplayer.getDefault().createDialog(
		// wizardDescriptor);
		if (automaticRegistration == false) {
			dialog.setSize(830, 600);
			dialog.setVisible(true);
			dialog.toFront();
		}
		wizardDescriptor.putProperty("enableForward", true);

		if (wizardDescriptor.getValue() == WizardDescriptor.FINISH_OPTION
				|| automaticRegistration == true) {

			Realm realm;
			System.out.println("automaticRegistration" + automaticRegistration);
			if (automaticRegistration == true) {
				realm = getRealm();
			} else {
				realm = (Realm) wizardDescriptor.getProperty("realm"); //$NON-NLS-1$
			}
			assert null != realm;
			automaticRegistration = false;

			// fix callback handler to use the correct protection domain
			CallbackHandler callbackHandler = realm.getConnectionDescriptor()
					.getCallbackHandler();
			if (callbackHandler instanceof UsernamePasswordCallbackHandler)
				try {
					((UsernamePasswordCallbackHandler) callbackHandler)
							.setProtectionDomain(realm.getConnectionDescriptor().getLDAPUrl());
				} catch (IOException e) {
					ErrorManager.getDefault().annotate(e,
							"Could not update protection domain.");
					ErrorManager.getDefault().notify(e);
				}
			else
				ErrorManager.getDefault().notify(
						new IOException(
								"CallbackHandler was not of the expected type, but "
										+ callbackHandler.getClass()));

			FileObject fld = folder.getPrimaryFile();
			String baseName = "realm-" //$NON-NLS-1$
					+ realm.getConnectionDescriptor().getHostname()
					+ realm.getConnectionDescriptor().getBaseDN(); //$NON-NLS-1$
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

	public static boolean isAutomaticRegistration() {
		return automaticRegistration;
	}

	public static void setAutomaticRegistration(boolean automaticRegistration) {
		AddRealmAction.automaticRegistration = automaticRegistration;
	}

	public static Realm getRealm() {
		return newRealm;
	}

	public static void setRealm(Realm realm) {
		AddRealmAction.newRealm = realm;
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getName()
	 */
	// @Override
	public String getName() {
		return Messages.getString("action." //$NON-NLS-1$
				+ AddRealmAction.class.getSimpleName());
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getHelpCtx()
	 */
	// @Override
	public HelpCtx getHelpCtx() {
		return null;
	}

	// @Override
	// public Image getIcon() {
	// return getOpenedIcon();
	// }

	// @Override
	public Icon getOpenedIcon() {
		return new ImageIcon(IconManager.getInstance(DetailViewProvider.class,
				"icons").getImage( //$NON-NLS-1$
				"tree." + getClass().getSimpleName()));
		// return IconManager.getInstance(DetailViewProvider.class,
		// "icons").getImage( //$NON-NLS-1$
		// "tree." + getClass().getSimpleName()); //$NON-NLS-1$
	}
}
