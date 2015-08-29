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

import javax.swing.JPanel;

import org.openthinclient.common.directory.LDAPDirectory;
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
public class UserEditor extends JPanel {
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

			// if (null == user.getLocation())
			// support
			// .addError(
			// "location",
			// Messages.getString("UserEditor.validation.location.mandatory"));
			// //$NON-NLS-1$ //$NON-NLS-2$

			if (null == user.getSn() || user.getSn().equals(""))
				support
						.addError(
								"surname", Messages.getString("UserEditor.validation.surname.mandatory"));//$NON-NLS-2$

			if (null != user.getNewPassword()
					&& !user.getNewPassword().equals(user.getVerifyPassword()))
				support
						.addError(
								"password", Messages.getString("UserEditor.validation.password.mismatch")); //$NON-NLS-1$ //$NON-NLS-2$

			return support.getResult();
		}
	}

	/*
	 * @see org.openthinclient.console.ObjectEditorPart#getMainComponent()
	 */
	public UserEditor(final User user, Realm realm) {

		if (!LDAPDirectory.isMutable(User.class)) {

			final PresentationModel model = new PresentationModel(new ValueHolder(
					user, true));

			final DetailViewFormBuilder dfb = new DetailViewFormBuilder(
					new FormLayout("r:p, 3dlu, f:p:g"), Messages.getBundle(), this);

			dfb.getPanel().setName(Messages.getString("Settings_title"));

			dfb.appendI15d("User.givenName", BasicComponentFactory.createLabel(model
					.getModel("givenName")));
			dfb.nextLine();

			dfb.appendI15d("User.surname", BasicComponentFactory.createLabel(model
					.getModel("sn")));
			dfb.nextLine();

			// dfb.appendI15d("User.stringLocation",
			// BasicComponentFactory.createLabel(
			// model.getModel("stringLocation")));
			// dfb.nextLine();

			dfb.appendUnrelatedComponentsGapRow();
			dfb.nextLine();

			dfb.appendI15d("User.changePassword", BasicComponentFactory
					.createLabel(model.getModel("newPassword")));
			dfb.nextLine();

			dfb.appendI15d("User.verifyPassword", BasicComponentFactory
					.createLabel(model.getModel("verifyPassword")));
			dfb.nextLine();

			putClientProperty(DirObjectEditor.KEY_VALIDATOR, new UserValidator(model,
					user));
		} else {
			final PresentationModel model = new PresentationModel(new ValueHolder(
					user, true));
			final DetailViewFormBuilder dfb = new DetailViewFormBuilder(
					new FormLayout("r:p, 3dlu, f:p:g"), Messages.getBundle(), this); //$NON-NLS-1$

			dfb.getPanel().setName(Messages.getString("Settings_title")); //$NON-NLS-1$

			dfb.appendI15d("User.givenName", BasicComponentFactory.createTextField( //$NON-NLS-1$
					model.getModel("givenName"), //$NON-NLS-1$
					true));
			dfb.nextLine();

			dfb.appendI15d(
					"User.surname", BasicComponentFactory.createTextField(model //$NON-NLS-1$
							.getModel("sn"), //$NON-NLS-1$
							false));
			dfb.nextLine();

			// Set<Location> locations;
			// try {
			// locations =
			// realm.getDirectory(user.getClass().getName()).list(Location.class);
			// } catch (DirectoryException e) {
			// ErrorManager.getDefault().notify(e);
			// locations = new HashSet<Location>();
			// }

			// dfb.appendI15d("User.location", //$NON-NLS-1$
			// BasicComponentFactory.createComboBox(new SelectionInList(new ArrayList(
			// locations), model.getModel("location")))); //$NON-NLS-1$
			// dfb.nextLine();

			dfb.appendUnrelatedComponentsGapRow();
			dfb.nextLine();

			dfb.appendI15d("User.changePassword", BasicComponentFactory //$NON-NLS-1$
					.createPasswordField(model.getModel("newPassword"), //$NON-NLS-1$
							false));
			dfb.nextLine();

			dfb.appendI15d("User.verifyPassword", BasicComponentFactory //$NON-NLS-1$
					.createPasswordField(model.getModel("verifyPassword"), //$NON-NLS-1$
							false));
			dfb.nextLine();

			putClientProperty(DirObjectEditor.KEY_VALIDATOR, new UserValidator(model,
					user));
		}

	}
}
