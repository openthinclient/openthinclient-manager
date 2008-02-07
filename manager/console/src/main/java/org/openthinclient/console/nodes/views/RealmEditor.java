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
package org.openthinclient.console.nodes.views;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;

import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.User;
import org.openthinclient.console.Messages;
import org.openthinclient.console.util.DetailViewFormBuilder;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.Validator;
import com.jgoodies.validation.util.PropertyValidationSupport;

/**
 * @author Natalie Bohnert
 */
public class RealmEditor extends JPanel {
	private static class UserValidator implements Validator {
		private final User user;
		private final PresentationModel model;

		UserValidator(PresentationModel model, User user) {
			this.model = model;
			this.user = user;
		}

		/*
		 * @see com.jgoodies.validation.Validator#validate()
		 */
		public ValidationResult validate() {
			model.triggerCommit();

			final PropertyValidationSupport support = new PropertyValidationSupport(
					user, "User"); //$NON-NLS-1$

			if (null != user.getNewPassword()
					&& !user.getNewPassword().equals(user.getVerifyPassword()))
				support
						.addError(
								"password", Messages.getString("UserEditor.validation.password.mismatch")); //$NON-NLS-1$ //$NON-NLS-2$
			else if (null != user.getNewPassword()
					&& user.getNewPassword().length() > 0)
				support
						.addWarning(
								"password", Messages.getString("RealmEditor.validation.changePWNotice")); //$NON-NLS-1$ //$NON-NLS-2$

			return support.getResult();
		}
	}

	/*
	 * @see org.openthinclient.console.ObjectEditorPart#getMainComponent()
	 */
	public RealmEditor(final Realm realm) {
		// final PresentationModel realmModel = new PresentationModel(new
		// ValueHolder(
		// realm, true));

		final PresentationModel roPrincipalModel = new PresentationModel(
				new ValueHolder(realm.getReadOnlyPrincipal(), true));

		final DetailViewFormBuilder dfb = new DetailViewFormBuilder(new FormLayout(
				"r:p, 3dlu, f:p:g"), Messages.getBundle(), this); //$NON-NLS-1$

		dfb.getPanel().setName(Messages.getString("Settings_title")); //$NON-NLS-1$

		dfb.appendI15d("User.changePassword", BasicComponentFactory //$NON-NLS-1$
				.createPasswordField(roPrincipalModel.getModel("newPassword"), //$NON-NLS-1$
						false));
		dfb.nextLine();

		dfb.appendI15d("User.verifyPassword", BasicComponentFactory //$NON-NLS-1$
				.createPasswordField(roPrincipalModel.getModel("verifyPassword"), //$NON-NLS-1$
						false));
		dfb.nextLine();

		// HACK: forward property changes from the roPrincipal user to the realm,
		// in order to make the validation trigger.
		realm.getReadOnlyPrincipal().addPropertyChangeListener(
				new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent evt) {
						realm.fakePropertyChange();
					}
				});
		putClientProperty(DirObjectEditor.KEY_VALIDATOR, new UserValidator(
				roPrincipalModel, realm.getReadOnlyPrincipal()));
	}
}
