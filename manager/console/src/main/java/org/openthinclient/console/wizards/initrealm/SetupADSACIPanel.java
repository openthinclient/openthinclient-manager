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
package org.openthinclient.console.wizards.initrealm;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;
import org.openthinclient.console.Messages;
import org.openthinclient.ldap.Util;
import org.openthinclient.ldap.LDAPConnectionDescriptor.DirectoryType;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class SetupADSACIPanel
		implements
			WizardDescriptor.Panel,
			EnableableWizardPanel {

	private final Set<ChangeListener> listeners = new HashSet<ChangeListener>(1);

	private JPanel component;
	private WizardDescriptor wizardDescriptor;
	private String baseDN;
	private JCheckBox baseCheckBox;
	private JCheckBox searchForAllCheckBox;
	private JCheckBox adminAccessCheckBox;

	public final void addChangeListener(ChangeListener l) {
		synchronized (listeners) {
			listeners.add(l);
		}
	}

	protected final void fireChangeEvent() {
		Iterator<ChangeListener> it;
		synchronized (listeners) {
			it = new HashSet<ChangeListener>(listeners).iterator();
		}
		final ChangeEvent ev = new ChangeEvent(this);
		while (it.hasNext())
			it.next().stateChanged(ev);
	}

	public JComponent getComponent() {
		if (null == component) {
			final DefaultFormBuilder dfb = new DefaultFormBuilder(new FormLayout(
					"15dlu, 15dlu, fill:max(200dlu;pref)"), Messages.getBundle()); //$NON-NLS-1$
			final ActionListener changeForwarder = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					updateComponentStates();
					fireChangeEvent();
				}
			};

			baseCheckBox = new JCheckBox();
			baseCheckBox.setVerticalTextPosition(SwingConstants.TOP);
			baseCheckBox.setSelected(true);
			baseCheckBox.addActionListener(changeForwarder);
			dfb.append(baseCheckBox, dfb.getColumnCount());
			dfb.nextLine();

			dfb.setLeadingColumnOffset(1);

			adminAccessCheckBox = new JCheckBox();
			adminAccessCheckBox.setVerticalAlignment(SwingConstants.TOP);
			adminAccessCheckBox.setVerticalTextPosition(SwingConstants.TOP);
			adminAccessCheckBox.addActionListener(changeForwarder);
			adminAccessCheckBox.setSelected(true);
			dfb.append(adminAccessCheckBox, 2);
			dfb.nextLine();

			searchForAllCheckBox = new JCheckBox();
			searchForAllCheckBox.setVerticalTextPosition(SwingConstants.TOP);
			searchForAllCheckBox.setSelected(true);
			dfb.append(searchForAllCheckBox, 2);
			dfb.nextLine();

			updateComponentStates();

			component = dfb.getPanel();
			component.setName(Messages.getString("SetupADSACIPanel.name")); //$NON-NLS-1$
		}

		return component;
	}

	/**
	 * 
	 */
	protected void updateComponentStates() {
		baseCheckBox.setText(Messages.getString("SetupADSACIPanel.base", baseDN)); //$NON-NLS-1$

		final boolean enabled = baseCheckBox.isSelected();
		searchForAllCheckBox.setEnabled(enabled);
		searchForAllCheckBox.setText(Messages.getString(
				"SetupADSACIPanel.enableSearchForAll", baseDN)); //$NON-NLS-1$
		adminAccessCheckBox.setEnabled(enabled);
		adminAccessCheckBox.setText(Messages.getString(
				"SetupADSACIPanel.enableAdminAccess", baseDN)); //$NON-NLS-1$

		if (null != wizardDescriptor)
			wizardDescriptor.putProperty("ACISetupTakenCareOf", baseCheckBox //$NON-NLS-1$
					.isSelected());
	}

	public HelpCtx getHelp() {
		return HelpCtx.DEFAULT_HELP;
	}

	public boolean isValid() {
		wizardDescriptor.putProperty("WizardPanel_errorMessage", null); //$NON-NLS-1$
		return true;
	}

	// You can use a settings object to keep track of state. Normally the
	// settings object will be the WizardDescriptor, so you can use
	// WizardDescriptor.getProperty & putProperty to store information entered
	// by the user.
	public void readSettings(Object settings) {
		wizardDescriptor = (WizardDescriptor) settings;

		baseDN = (String) wizardDescriptor.getProperty("selectedBaseDN"); //$NON-NLS-1$

		updateComponentStates();

		fireChangeEvent();
	}

	public final void removeChangeListener(ChangeListener l) {
		synchronized (listeners) {
			listeners.remove(l);
		}
	}

	public void storeSettings(Object settings) {
		final WizardDescriptor wd = (WizardDescriptor) settings;
		wd.putProperty("createADSACIs", baseCheckBox.isSelected()); //$NON-NLS-1$
		wd.putProperty("enableSearchForAll", searchForAllCheckBox.isSelected()); //$NON-NLS-1$
		wd.putProperty("enableAdminAccess", adminAccessCheckBox.isSelected()); //$NON-NLS-1$
	}

	/*
	 * @see org.openthinclient.console.wizards.initrealm.EnableableWizardPanel#isEnabled(org.openide.WizardDescriptor)
	 */
	public boolean isEnabled(WizardDescriptor wd) {
		if (null == wd)
			return false;

		final DirectoryType serverType = (DirectoryType) wd
				.getProperty("serverType"); //$NON-NLS-1$

		if (null != serverType && serverType == DirectoryType.GENERIC_RFC)
			try {
				final DirContext ctx = (DirContext) wd.getProperty("schema"); //$NON-NLS-1$
				final boolean isADS = Util.hasObjectClass(ctx, "apacheCatalogEntry"); //$NON-NLS-1$

				return isADS;
			} catch (final NamingException e) {
				// fall through
			}

		return false;
	}
}
