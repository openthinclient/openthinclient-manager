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
import java.text.MessageFormat;

import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.WizardDescriptor;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.Utilities;
import org.openide.util.actions.NodeAction;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.wizards.newdirobject.NewDirObjectTreeWizardIterator;
import org.openthinclient.ldap.DirectoryException;

import com.levigo.util.swing.IconManager;

/**
 * @author bohnerne
 */
public class NewAction extends NodeAction {

	public NewAction() {
		super();
		setIcon(IconManager.getInstance(getClass(), "icons").getIcon("New")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	protected boolean asynchronous() {
		return false;
	}

	/*
	 * @see org.openide.util.actions.NodeAction#performAction(org.openide.nodes.Node[])
	 */
	@Override
	protected void performAction(Node[] activatedNodes) {
		for (final Node node : activatedNodes) {

			final Class childObjectClass = (Class) node.getLookup().lookup(
					Class.class); // Klase
			// des
			// zu
			// erstellnedem
			// Objectes
			// wird
			// bestimmt

			try {
				final DirectoryObject object = (DirectoryObject) childObjectClass
						.newInstance(); // neues DirectoryObjects wird erstellt
				object.setName("New " + childObjectClass.getName()); //$NON-NLS-1$ //

				final Realm realm = (Realm) node.getLookup().lookup(Realm.class); // Realm
				// wird
				// abgefragt
				final WizardDescriptor wd = new WizardDescriptor(
						new NewDirObjectTreeWizardIterator());
				wd.setTitleFormat(new MessageFormat("{0} ({1})")); //$NON-NLS-1$
				wd.setTitle(Messages
						.getString("action.New" + childObjectClass.getSimpleName())); //$NON-NLS-1$    

				// preload properties
				wd.putProperty("type", childObjectClass); //$NON-NLS-1$
				wd.putProperty("realm", node.getLookup().lookup(Realm.class)); //$NON-NLS-1$

				final Dialog dialog = DialogDisplayer.getDefault().createDialog(wd);

				dialog.setIconImage(Utilities.loadImage(
						"org/openthinclient/console/icon.png", true));
				dialog.setSize(830, 600);
				dialog.setVisible(true);
				dialog.toFront();

				if (wd.getValue() == WizardDescriptor.FINISH_OPTION) {
					final DirectoryObject dirObject = (DirectoryObject) wd
							.getProperty("dirObject"); //$NON-NLS-1$
					try {
						realm.getDirectory().save(dirObject);
						if (node != null && node instanceof Refreshable)
							((Refreshable) node).refresh();

					} catch (final DirectoryException e) {
						ErrorManager.getDefault().notify(e);
					}
				}
			} catch (final Exception e) {
				ErrorManager.getDefault().notify(e);
			}
		}

	}

	/*
	 * @see org.openide.util.actions.NodeAction#enable(org.openide.nodes.Node[])
	 */
	@Override
	protected boolean enable(Node[] activatedNodes) {
		for (final Node node : activatedNodes) {
			final Class currentClass = (Class) node.getLookup().lookup(Class.class);
			if (!LDAPDirectory.isMutable(currentClass))
				return false;
		}
		return true;
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getName()
	 */
	@Override
	public String getName() {
		return Messages.getString("New"); //$NON-NLS-1$
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getHelpCtx()
	 */
	@Override
	public HelpCtx getHelpCtx() {
		return null;
	}

}
