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
package org.openthinclient.console.wizards.newdirobject;

import java.awt.BorderLayout;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openide.ErrorManager;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.Messages;
import org.openthinclient.console.nodes.views.DirObjectEditor;
import org.openthinclient.console.util.ChildValidator;

import com.jgoodies.validation.ValidationResult;
import com.jgoodies.validation.message.PropertyValidationMessage;

public class ConfigurationPanel implements WizardDescriptor.Panel {

	private WizardDescriptor wd;

	private DirectoryObject newDirectoryObject;

	private final Set<ChangeListener> listeners = new HashSet<ChangeListener>(1);

	private final ChildValidator validator = new ChildValidator();

	private final JPanel component = new JPanel(new BorderLayout());

	public Component getComponent() {
		component.setName(this.getName());
		return component;
	}

	public HelpCtx getHelp() {
		// Show no Help button for this panel:
		return HelpCtx.DEFAULT_HELP;
		// If you have context help:
		// return new HelpCtx(SampleWizardPanel1.class);
	}

	public String getName() {
		return Messages.getString("ConfigurationPanel.name"); //$NON-NLS-1$
	}

	public boolean isValid() {
		final ValidationResult validate = validator.validate();
		if (!validate.hasErrors()) {
			if (validate.hasMessages()) {
				final PropertyValidationMessage m = (PropertyValidationMessage) validate
						.getMessages().get(0);
				wd.putProperty("WizardPanel_errorMessage", m.formattedText()); //$NON-NLS-1$
			} else
				wd.putProperty("WizardPanel_errorMessage", null); //$NON-NLS-1$
			return true;
		} else {
			final PropertyValidationMessage m = (PropertyValidationMessage) validate
					.getErrors().iterator().next();
			wd.putProperty("WizardPanel_errorMessage", m.formattedText()); //$NON-NLS-1$
			return false;
		}
	}

	// You can use a settings object to keep track of state. Normally the
	// settings object will be the WizardDescriptor, so you can use
	// WizardDescriptor.getProperty & putProperty to store information entered
	// by the user.
	public void readSettings(Object settings) {
		wd = (WizardDescriptor) settings;
		final Realm realm = (Realm) wd.getProperty("realm"); //$NON-NLS-1$

		final Class type = (Class) wd.getProperty("type"); //$NON-NLS-1$

		try {
			newDirectoryObject = (DirectoryObject) type.newInstance();
			newDirectoryObject.setName(wd.getProperty("name").toString()); //$NON-NLS-1$
			newDirectoryObject.setDescription(wd.getProperty("description") //$NON-NLS-1$
					.toString());

			if (newDirectoryObject instanceof Profile) {
				final String schemaType = wd.getProperty("schemaType").toString(); //$NON-NLS-1$
				((Profile) newDirectoryObject).setSchema(realm.getSchemaProvider()
						.getSchema(newDirectoryObject.getClass(), schemaType));
			}

			newDirectoryObject
					.addPropertyChangeListener(new PropertyChangeListener() {
						public void propertyChange(PropertyChangeEvent evt) {
							fireChangeEvent();
						}
					});

			final JComponent component = DirObjectEditor.getEditorForDirObject(
					newDirectoryObject, realm);
			setConfigPanel(component);
			validator.addValidatorFrom(component);
		} catch (final Exception e) {
			ErrorManager.getDefault().annotate(e, ErrorManager.EXCEPTION,
					Messages.getString("NewDirObject.schema_not_loaded_error"), null, //$NON-NLS-1$
					null, null);
			ErrorManager.getDefault().notify(e);
		}
	}

	private void setConfigPanel(JComponent panel) {
		component.removeAll();
		component.add(panel, BorderLayout.CENTER);
		component.revalidate();
		component.repaint();
	}

	public void storeSettings(Object settings) {
		wd = (WizardDescriptor) settings;

		wd.putProperty("dirObject", newDirectoryObject); //$NON-NLS-1$
	}

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
}
