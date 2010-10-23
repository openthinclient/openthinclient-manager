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
package org.openthinclient.console.nodes.views;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.text.MessageFormat;

import javax.swing.JPanel;

import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.WizardDescriptor;
import org.openide.util.Utilities;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.UnrecognizedClient;
import org.openthinclient.console.Messages;
import org.openthinclient.console.wizards.newdirobject.NewDirObjectTreeWizardIterator;
import org.openthinclient.ldap.DirectoryException;

/**
 * @author Michael Gold
 */
public class UnrecognizedClientEditor extends JPanel {

	private static String ipAddress;

	private static String macAddress;

	private static boolean unrecognize = false;

	public UnrecognizedClientEditor(final UnrecognizedClient unrecognizedClient,
			final Realm realm) {
		unrecognize = true;

		final Client newClient = new Client();

		newClient.setName("New " + newClient.getClass().getName());

		setMacAddress(unrecognizedClient.getMacAddress());
		setIpAddress(unrecognizedClient.getIpHostNumber());

		final WizardDescriptor wd = new WizardDescriptor(
				new NewDirObjectTreeWizardIterator());
		wd.setTitleFormat(new MessageFormat("{0} ({1})"));
		wd.setTitle(Messages.getString("UnrecognizedClientEditor.create"));

		// preload properties
		wd.putProperty("type", newClient.getClass());
		wd.putProperty("realm", realm);

		final Dialog dialog = DialogDisplayer.getDefault().createDialog(wd);

		dialog.setIconImage(Utilities.loadImage(
				"org/openthinclient/console/icon.png", true));

		dialog.setPreferredSize(new Dimension(830, 600));
		dialog.pack();
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		dialog.setLocation((screenSize.width - dialog.getWidth()) / 2,
				(screenSize.height - dialog.getHeight()) / 2);

		dialog.setVisible(true);
		dialog.toFront();

		if (wd.getValue() == WizardDescriptor.FINISH_OPTION) {
			final DirectoryObject dirObject = (DirectoryObject) wd
					.getProperty("dirObject");

			try {
				realm.getDirectory().save(dirObject);
			} catch (final DirectoryException e2) {
				ErrorManager.getDefault().notify(e2);
			}
			try {
				realm.getDirectory().delete(unrecognizedClient);
			} catch (final DirectoryException e1) {
				e1.printStackTrace();
			}
		}
		unrecognize = false;
	}

	public static String getIpAddress() {
		return ipAddress;
	}

	public static String getMacAddress() {
		return macAddress;
	}

	public static void setIpAddress(String ipAddress) {
		UnrecognizedClientEditor.ipAddress = ipAddress;
	}

	public static void setMacAddress(String macAddress) {
		UnrecognizedClientEditor.macAddress = macAddress;
	}

	public static boolean isUnrecognize() {
		return unrecognize;
	}

	public static void setUnrecognize(boolean unrecognize) {
		UnrecognizedClientEditor.unrecognize = unrecognize;
	}
}
