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
 *******************************************************************************/
package org.openthinclient.console.nodes;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;

import javax.swing.AbstractAction;

import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.WizardDescriptor;
import org.openide.loaders.DataFolder;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Utilities;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.ApplicationGroup;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Device;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.Printer;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.UnrecognizedClient;
import org.openthinclient.common.model.User;
import org.openthinclient.common.model.UserGroup;
import org.openthinclient.console.MainTreeTopComponent;
import org.openthinclient.console.Messages;
import org.openthinclient.console.Refreshable;
import org.openthinclient.console.wizards.newdirobject.NewDirObjectWizardIterator;
import org.openthinclient.ldap.DirectoryException;


/** Getting the feed node and wrapping it in a FilterNode */
class DirObjectsNode extends MyAbstractNode {
	public static Class OBJECT_CLASSES[] = new Class[]{Client.class, User.class,
			UserGroup.class, Application.class, ApplicationGroup.class, Device.class,
			Location.class, Printer.class, HardwareType.class,
			UnrecognizedClient.class};

	public DirObjectsNode(Node parent) {
		super(new Children.Array(), parent.getLookup());

		// createChildren(getChildren(), this);
	}

	/**
	 * 
	 */
	public static Node[] createChildren(Node parent) {
		Node children[] = new Node[OBJECT_CLASSES.length];
		for (int i = 0; i < OBJECT_CLASSES.length; i++)
			children[i] = new DirObjectListNode(parent, OBJECT_CLASSES[i]);
		return children;
	}

	public static class AddDirObjectAction extends AbstractAction {

		public AddDirObjectAction() {
			super(Messages.getString("action." //$NON-NLS-1$
					+ AddDirObjectAction.class.getSimpleName()));
		}

		public AddDirObjectAction(DataFolder df) {
			super(Messages.getString("action." //$NON-NLS-1$
					+ AddDirObjectAction.class.getSimpleName()));
		}

		/*
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent e) {
			WizardDescriptor.Iterator iterator = new NewDirObjectWizardIterator();
			WizardDescriptor wizardDescriptor = new WizardDescriptor(iterator);
			wizardDescriptor.setTitleFormat(new MessageFormat("{0} ({1})")); //$NON-NLS-1$
			wizardDescriptor.setTitle(Messages.getString("action." //$NON-NLS-1$
					+ AddDirObjectAction.class.getSimpleName()));
			Dialog dialog = DialogDisplayer.getDefault().createDialog(
					wizardDescriptor);
			dialog.setIconImage(Utilities.loadImage(
					"org/openthinclient/console/icon.png", true));
			dialog.setVisible(true);
			dialog.toFront();

			if (wizardDescriptor.getValue() == WizardDescriptor.FINISH_OPTION) {
				DirectoryObject dirObject = (DirectoryObject) wizardDescriptor
						.getProperty("dirObject"); //$NON-NLS-1$
				Realm realm = (Realm) wizardDescriptor.getProperty("realm"); //$NON-NLS-1$
				try {
					realm.getDirectory().save(dirObject);
				} catch (DirectoryException e1) {
					ErrorManager.getDefault().annotate(e1, ErrorManager.EXCEPTION,
							Messages.getString("error.AddDirObjectFailed"), null, null, null); //$NON-NLS-1$
					ErrorManager.getDefault().notify(e1);
				}
			}
			Node[] nodes = MainTreeTopComponent.getDefault().getActivatedNodes();
			for (Node node : nodes)
				if (node instanceof Refreshable)
					((Refreshable) node).refresh();
		}

	}
}
