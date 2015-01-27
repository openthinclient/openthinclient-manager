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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openide.ErrorManager;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.Messages;
import org.openthinclient.console.util.DetailViewFormBuilder;
import org.openthinclient.ldap.DirectoryException;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.Validator;
import com.jgoodies.validation.util.PropertyValidationSupport;
import com.jgoodies.validation.util.ValidationUtils;

/**
 * @author Natalie Bohnert
 */
public class ClientEditor extends JPanel {
	private static class ClientValidator implements Validator {

		private final Client client;

		private static final Pattern MAC_ADDRESS_PATTERN = Pattern
				.compile("(\\p{XDigit}{2}:){5}\\p{XDigit}{2}"); //$NON-NLS-1$
		private final PresentationModel model;

		ClientValidator(PresentationModel model, Client client) {
			this.model = model;
			this.client = client;
		}

		/*
		 * @see com.jgoodies.validation.Validator#validate()
		 */
		public ValidationResult validate() {

			if (UnrecognizedClientEditor.isUnrecognize()) {
				client.setIpHostNumber(UnrecognizedClientEditor.getIpAddress());
				client.setMacAddress(UnrecognizedClientEditor.getMacAddress());
				// set unrecognized to false to be able to change the IP address
				UnrecognizedClientEditor.setUnrecognize(false);
			}
			model.triggerCommit();

			final PropertyValidationSupport support = new PropertyValidationSupport(
					client, "Client"); //$NON-NLS-1$

			support.addWarning("ipaddress", Messages
					.getString("ClientEditor.validation.ipaddress.forInformation"));

			if (null == client.getIpHostNumber()
					|| client.getIpHostNumber().equals(""))
				support
						.addError(
								"IP Address", Messages.getString("ClientEditor.validation.ipaddress.mandatory")); //$NON-NLS-1$ //$NON-NLS-2$

			if (null == client.getLocation())
				support
						.addError(
								"location", Messages.getString("ClientEditor.validation.location.mandatory")); //$NON-NLS-1$ //$NON-NLS-2$

			if (null == client.getHardwareType())
				support
						.addError(
								"hwtype", Messages.getString("ClientEditor.validation.hwtype.mandatory")); //$NON-NLS-1$ //$NON-NLS-2$

			if (ValidationUtils.isEmpty(client.getMacAddress()))
				support
						.addError(
								"macaddress", Messages.getString("ClientEditor.validation.macaddress.mandatory")); //$NON-NLS-1$ //$NON-NLS-2$
			else if (!MAC_ADDRESS_PATTERN.matcher(client.getMacAddress()).matches())
				support.addError("macaddress", //$NON-NLS-1$
						Messages.getString("ClientEditor.validation.macaddress.invalid")); //$NON-NLS-1$

			if (!ValidationUtils.isEmpty(client.getIpHostNumber()))
				try {
					final InetAddress addr = InetAddress.getByName(client
							.getIpHostNumber());

					if (addr.isLinkLocalAddress() || addr.isMulticastAddress())
						support
								.addError(
										"ipaddress", Messages.getString("ClientEditor.validation.ipaddress.islocal")); //$NON-NLS-1$ //$NON-NLS-2$
					else if (addr.isLoopbackAddress())
						support
								.addError(
										"ipaddress", Messages.getString("ClientEditor.validation.ipaddress.isloopback")); //$NON-NLS-1$ //$NON-NLS-2$
					else
						client.setIpHostNumber(addr.getHostAddress());
				} catch (final UnknownHostException e) {
					support
							.addWarning(
									"ipaddress", Messages.getString("ClientEditor.validation.ipaddress.hostunknown")); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (final NumberFormatException e) {
					support.addError("ipaddress", //$NON-NLS-1$
							Messages.getString("ClientEditor.validation.ipaddress.invalod")); //$NON-NLS-1$
				}

			// validate the name. we can use the rules for a java identifier as a
			// starting point, but add some details.
			final char[] chars = client.getName().toCharArray();
			if (chars.length > 0)
				if (!Character.isJavaIdentifierStart(chars[0]))
					support
							.addWarning(
									"name", Messages.getString("DirObjectEditor.validation.name.discouraged")); //$NON-NLS-1$ //$NON-NLS-2$
				else
					for (final char c : chars)
						if (!(Character.isJavaIdentifierPart(c) || c == '-')
								|| "_.$".indexOf(c) >= 0) {
							support
									.addWarning(
											"name", Messages.getString("DirObjectEditor.validation.name.discouraged")); //$NON-NLS-1$ //$NON-NLS-2$
							break;
						}
			return support.getResult();
		}

	}

	/*
	 * @see org.openthinclient.console.ObjectEditorPart#getMainComponent()
	 */
	public ClientEditor(final Client client, Realm realm) {
		final PresentationModel model = new PresentationModel(new ValueHolder(
				client, true));
		final DetailViewFormBuilder dfb = new DetailViewFormBuilder(new FormLayout(
				"r:p, 3dlu, f:p:g"), Messages.getBundle(), this); //$NON-NLS-1$

		dfb.getPanel().setName(Messages.getString("Settings_title")); //$NON-NLS-1$

		dfb.appendI15d("Client.ipHostNumber", //$NON-NLS-1$
				BasicComponentFactory.createTextField(model.getModel("ipHostNumber"), //$NON-NLS-1$
						true));
		dfb.nextLine();

		if (UnrecognizedClientEditor.isUnrecognize()) {
			// Disable editing of unrecognized client MAC addresses
			final JTextField macAdressTextField = BasicComponentFactory
					.createTextField(model.getModel("macAddress"));
			macAdressTextField.setEditable(false);
			dfb.appendI15d("Client.macAddress", //$NON-NLS-1$
					macAdressTextField, //$NON-NLS-1$
					false);
		} else
			dfb.appendI15d("Client.macAddress", //$NON-NLS-1$
					BasicComponentFactory.createTextField(model.getModel("macAddress"), //$NON-NLS-1$
							false));
		dfb.nextLine();

		// FIXME: den richtigen Realm benutzen!
		Set<Location> locations;
		try {
			locations = realm.getDirectory().list(Location.class);
		} catch (final DirectoryException e) {
			ErrorManager.getDefault().notify(e);
			locations = new HashSet<Location>();
		}

		final JComboBox locationBox = BasicComponentFactory
				.createComboBox(new SelectionInList(new ArrayList(locations), model
						.getModel("location")));

		dfb.appendI15d("Client.location", locationBox);
		if (locations.size() == 1 && locationBox.getSelectedIndex() < 0)
			locationBox.setSelectedIndex(0);

		dfb.nextLine();

		Set<HardwareType> hwtypes;
		try {
			hwtypes = realm.getDirectory().list(HardwareType.class);
		} catch (final DirectoryException e) {
			ErrorManager.getDefault().notify(e);
			hwtypes = new HashSet<HardwareType>();
		}

		final JComboBox hwtBox = BasicComponentFactory
				.createComboBox(new SelectionInList(new ArrayList(hwtypes), model
						.getModel("hardwareType")));

		dfb.appendI15d("Client.hardwareType", hwtBox);
		if (hwtypes.size() == 1 && hwtBox.getSelectedIndex() < 0)
			hwtBox.setSelectedIndex(0);

		dfb.nextLine();

		putClientProperty(DirObjectEditor.KEY_VALIDATOR, new ClientValidator(model,
				client));
	}
}
