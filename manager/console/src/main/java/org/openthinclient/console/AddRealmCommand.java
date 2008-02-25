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
import java.util.Collection;

import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.wizards.registerrealm.RegisterRealmWizardIterator;

import com.levigo.util.swing.action.AbstractCommand;

/** An action for adding a realm */
public class AddRealmCommand extends AbstractCommand {
	@Override
	protected void doExecute(Collection args) {
		final WizardDescriptor.Iterator iterator = new RegisterRealmWizardIterator();
		final WizardDescriptor wizardDescriptor = new WizardDescriptor(iterator);
		wizardDescriptor.setTitleFormat(new MessageFormat("{0} ({1})")); //$NON-NLS-1$
		wizardDescriptor.setTitle(Messages.getString("action.AddRealmAction")); //$NON-NLS-1$
		final Dialog dialog = DialogDisplayer.getDefault().createDialog(
				wizardDescriptor);
		wizardDescriptor.putProperty("enableForward", false);
		// Dialog dialog = DialogDisplayer.getDefault().createDialog(
		// wizardDescriptor);
		dialog.setSize(830, 600);
		dialog.setVisible(true);
		dialog.toFront();

		wizardDescriptor.putProperty("enableForward", true);

		if (wizardDescriptor.getValue() == WizardDescriptor.FINISH_OPTION) {

			final Realm realm = (Realm) wizardDescriptor.getProperty("realm"); //$NON-NLS-1$

			RealmManager.registerRealm(realm);
		}
	}

	@Override
	public boolean checkDeeply(Collection args) {
		return true;
	}
}
