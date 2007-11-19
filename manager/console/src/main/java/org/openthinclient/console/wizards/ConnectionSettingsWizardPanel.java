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
package org.openthinclient.console.wizards;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openide.WizardDescriptor;
import org.openide.WizardValidationException;
import org.openide.util.HelpCtx;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.Messages;
import org.openthinclient.ldap.LDAPConnectionDescriptor;

public class ConnectionSettingsWizardPanel
		implements
			WizardDescriptor.AsynchronousValidatingPanel {

	/**
	 * The visual component that displays this panel. If you need to access the
	 * component from this class, just use getComponent().
	 */
	ConnectionSettingsVisualPanel component;
	private List<String> partitions;
	private Set<Realm> realms;
	private WizardDescriptor wd;

	// Get the visual component for the panel. In this template, the component
	// is kept separate. This can be more efficient: if the wizard is created
	// but never displayed, or not all panels are displayed, it is better to
	// create only those which really need to be visible.
	public ConnectionSettingsVisualPanel getComponent() {
		if (component == null)
			component = new ConnectionSettingsVisualPanel(this);
		return component;
	}

	public HelpCtx getHelp() {
		// Show no Help button for this panel:
		return HelpCtx.DEFAULT_HELP;
		// If you have context help:
		// return new HelpCtx(SampleWizardPanel1.class);
	}

	public boolean isValid() {
		return getComponent().valid(wd);
	}

	private final Set<ChangeListener> listeners = new HashSet<ChangeListener>(1);

	public final void addChangeListener(ChangeListener l) {
		synchronized (listeners) {
			listeners.add(l);
		}
	}

	public final void removeChangeListener(ChangeListener l) {
		synchronized (listeners) {
			listeners.remove(l);
		}
	}

	/*
	 * @see org.openthinclient.console.wizards.WizardPanelAdapter#fireChangeEvent()
	 */
	protected void fireChangeEvent() {
		Iterator<ChangeListener> it;
		synchronized (listeners) {
			it = new HashSet<ChangeListener>(listeners).iterator();
		}
		final ChangeEvent ev = new ChangeEvent(this);
		while (it.hasNext())
			it.next().stateChanged(ev);
	}

	// You can use a settings object to keep track of state. Normally the
	// settings object will be the WizardDescriptor, so you can use
	// WizardDescriptor.getProperty & putProperty to store information entered
	// by the user.
	public void readSettings(Object settings) {
		wd = (WizardDescriptor) settings;
		getComponent().readSettings((WizardDescriptor) settings);
	}

	public void storeSettings(Object settings) {
		wd = (WizardDescriptor) settings;

		getComponent().storeSettings(wd);

		wd.putProperty("partitions", partitions); //$NON-NLS-1$
		wd.putProperty("realms", realms); //$NON-NLS-1$
		wd.putProperty("connectionDescriptor", component //$NON-NLS-1$
				.createLDAPConnectionDescriptor());
	}

	/*
	 * @see org.openide.WizardDescriptor.ValidatingPanel#validate()
	 */
	public void validate() throws WizardValidationException {
		final LDAPConnectionDescriptor lcd = component
				.createLDAPConnectionDescriptor();

		try {
			// try to fetch the attributes of the DN pointed to by the descriptor
			final DirContext ctx = lcd.createDirContext();
			try {
				ctx.getAttributes(""); //$NON-NLS-1$

				wd.putProperty("serverType", lcd.guessDirectoryType()); //$NON-NLS-1$
				wd.putProperty("schema", ctx.getSchema("")); //$NON-NLS-1$ //$NON-NLS-2$
			} finally {
				ctx.close();
			}
		} catch (final AuthenticationException e) {
			throwValidationException(Messages
					.getString("ConnectionSettings.error.auth_failed")); //$NON-NLS-1$
		} catch (final NameNotFoundException e) {
			throwValidationException(Messages.getString(
					"ConnectionSettings.error.name_not_found", lcd.getBaseDN())); //$NON-NLS-1$
		} catch (final CommunicationException e) {
			throwValidationException(Messages.getString(
					"ConnectionSettings.error.comm_failed", e.getLocalizedMessage())); //$NON-NLS-1$
		} catch (final NamingException e) {
			throwValidationException(Messages.getString(
					"ConnectionSettings.error.connection_failed", e //$NON-NLS-1$
							.getLocalizedMessage()));
		}
	}

	/**
	 * @param msg
	 * @throws WizardValidationException
	 */
	private void throwValidationException(String msg)
			throws WizardValidationException {
		throw new WizardValidationException(getComponent(), msg, msg);
	}

	/*
	 * @see org.openide.WizardDescriptor.AsynchronousValidatingPanel#prepareValidation()
	 */
	public void prepareValidation() {
		// FIXME: this isn't sufficient, I know.
		component.setEnabled(false);
	}
}
