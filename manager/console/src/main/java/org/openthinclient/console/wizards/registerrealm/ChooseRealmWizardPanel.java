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
package org.openthinclient.console.wizards.registerrealm;

import java.awt.BorderLayout;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openide.WizardDescriptor;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.Messages;
import org.openthinclient.console.nodes.RealmNode;
import org.openthinclient.ldap.LDAPConnectionDescriptor;

public class ChooseRealmWizardPanel extends JPanel
		implements
			ExplorerManager.Provider,
			WizardDescriptor.Panel {

	public Component getComponent() {
		return this;
	}

	public HelpCtx getHelp() {
		return HelpCtx.DEFAULT_HELP;
	}

	@Override
	public boolean isValid() {
		for (final Node node : manager.getSelectedNodes())
			if (node instanceof RealmNode)
				return true;
		return false;
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

	protected final void fireChangeEvent() {
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
		final WizardDescriptor wd = (WizardDescriptor) settings;
		connectionDescriptor = (LDAPConnectionDescriptor) wd
				.getProperty("connectionDescriptor"); //$NON-NLS-1$

		Node root;
		if (!connectionDescriptor.isBaseDnSet())
			root = new SearchRealmDirectoryNode(connectionDescriptor);
		else
			root = new SearchRealmDirectoryViewNode(connectionDescriptor);
		manager.setRootContext(root);
	}

	public void storeSettings(Object settings) {
		Realm realm = null;
		for (final Node node : manager.getSelectedNodes())
			if (node instanceof RealmNode)
				realm = (Realm) ((RealmNode) node).getLookup().lookup(Realm.class);

		((WizardDescriptor) settings).putProperty("realm", realm); //$NON-NLS-1$
	}

	private final ExplorerManager manager = new ExplorerManager();
	private LDAPConnectionDescriptor connectionDescriptor;

	/**
	 * Creates new form ConnectionSettingsVisualPanel
	 * 
	 * @param panel1
	 */
	public ChooseRealmWizardPanel() {
		final BeanTreeView view = new BeanTreeView();

		setLayout(new BorderLayout());
		add(view, BorderLayout.CENTER);
		view.setDefaultActionAllowed(false);
		view.setRootVisible(true);
		view.setPopupAllowed(false);

		// view.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		manager.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				fireChangeEvent();
			}
		});
	}

	@Override
	public String getName() {
		return Messages.getString("RegisterRealm2_name"); //$NON-NLS-1$
	}

	/*
	 * @see org.openide.explorer.ExplorerManager.Provider#getExplorerManager()
	 */
	public ExplorerManager getExplorerManager() {
		return manager;
	}
}
