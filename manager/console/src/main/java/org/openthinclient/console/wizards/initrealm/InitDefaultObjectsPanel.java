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
package org.openthinclient.console.wizards.initrealm;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;
import org.openthinclient.console.Messages;
import org.openthinclient.ldap.LDAPConnectionDescriptor;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class InitDefaultObjectsPanel implements WizardDescriptor.Panel {

  private final Set<ChangeListener> listeners = new HashSet<ChangeListener>(1);

  private JPanel component;
  private WizardDescriptor wizardDescriptor;
  private String baseDN;
  private JCheckBox ousCheckBox;
  private JCheckBox adminCheckBox;
  private JLabel adminDNLabel;
  private JCheckBox locationCheckBox;
  private JCheckBox hwtypeAndDevicesCheckBox;
  private JTextField adminNameField;

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
    ChangeEvent ev = new ChangeEvent(this);
    while (it.hasNext()) {
      it.next().stateChanged(ev);
    }
  }

  public JComponent getComponent() {
    if (null == component) {
      final DefaultFormBuilder dfb = new DefaultFormBuilder(new FormLayout(
          "15dlu, 15dlu, p:g, 3dlu, p:g"), Messages.getBundle()); //$NON-NLS-1$
      final ActionListener changeForwarder = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          updateComponentStates();
          fireChangeEvent();
        }
      };

      DocumentListener documentForwarder = new DocumentListener() {
        public void changedUpdate(DocumentEvent e) {
          updateComponentStates();
          fireChangeEvent();
        }

        public void insertUpdate(DocumentEvent e) {
          updateComponentStates();
          fireChangeEvent();
        }

        public void removeUpdate(DocumentEvent e) {
          updateComponentStates();
          fireChangeEvent();
        }
      };

      ousCheckBox = new JCheckBox(Messages
          .getString("InitDefaultObjectsPanel.ous")); //$NON-NLS-1$
      ousCheckBox.setSelected(true);
      ousCheckBox.addActionListener(changeForwarder);
      dfb.append(ousCheckBox, dfb.getColumnCount());
      dfb.nextLine();

      dfb.setLeadingColumnOffset(1);

      adminCheckBox = new JCheckBox(Messages
          .getString("InitDefaultObjectsPanel.admin")); //$NON-NLS-1$
      ousCheckBox.addActionListener(changeForwarder);
      adminCheckBox.setSelected(true);
      dfb.append(adminCheckBox, dfb.getColumnCount() - dfb.getColumn() - 2);
      adminNameField = new JTextField(Messages.getString("InitDefaultObjectsPanel.defaultAdminUserName")); //$NON-NLS-1$
      adminNameField.getDocument().addDocumentListener(documentForwarder);
      dfb.append(adminNameField);
      dfb.nextLine();

      dfb.setLeadingColumnOffset(2);
      adminDNLabel = new JLabel();
      dfb.append(adminDNLabel, dfb.getColumnCount() - 2);
      dfb.nextLine();
      dfb.setLeadingColumnOffset(1);

      locationCheckBox = new JCheckBox(Messages
          .getString("InitDefaultObjectsPanel.location")); //$NON-NLS-1$
      locationCheckBox.setSelected(true);
      dfb.append(locationCheckBox, dfb.getColumnCount() - 1);
      dfb.nextLine();
      
      hwtypeAndDevicesCheckBox = new JCheckBox(Messages
              .getString("InitDefaultObjectsPanel.hwtypeAndDevices")); //$NON-NLS-1$
      hwtypeAndDevicesCheckBox.setSelected(true);
      dfb.append(hwtypeAndDevicesCheckBox, dfb.getColumnCount() - dfb.getColumn());
      dfb.nextLine();

      updateComponentStates();

      component = dfb.getPanel();
      component.setName(Messages.getString("InitDefaultObjectsPanel.name")); //$NON-NLS-1$
    }

    return component;
  }

  /**
   * 
   */
  protected void updateComponentStates() {
    boolean enabled = ousCheckBox.isSelected();
    adminCheckBox.setEnabled(enabled);
    adminDNLabel.setEnabled(enabled && adminCheckBox.isSelected());
    adminDNLabel.setText(Messages.getString("InitDefaultObjectsPanel.adminDN", //$NON-NLS-1$
        adminNameField.getText(), baseDN));
    adminNameField.setEnabled(enabled && adminCheckBox.isSelected());
    locationCheckBox.setEnabled(enabled);
    hwtypeAndDevicesCheckBox.setEnabled(enabled);
  }

  public HelpCtx getHelp() {
    return HelpCtx.DEFAULT_HELP;
  }

  public boolean isValid() {
    if (adminNameField.isEnabled() && adminNameField.getText().length() == 0) {
      wizardDescriptor
          .putProperty(
              "WizardPanel_errorMessage", Messages.getString("InitDefaultObjectsPanel.validation.error.adminNameMandatory")); //$NON-NLS-1$ //$NON-NLS-2$
      return false;
    }
    wizardDescriptor.putProperty("WizardPanel_errorMessage", null); //$NON-NLS-1$
    return true;
  }

  // You can use a settings object to keep track of state. Normally the
  // settings object will be the WizardDescriptor, so you can use
  // WizardDescriptor.getProperty & putProperty to store information entered
  // by the user.
  public void readSettings(Object settings) {
    wizardDescriptor = (WizardDescriptor) settings;
    LDAPConnectionDescriptor lcd = (LDAPConnectionDescriptor) wizardDescriptor.getProperty("connectionDescriptor");

    if((String) wizardDescriptor.getProperty("selectedBaseDN") != null)
    	baseDN = "ou=users," + (String) wizardDescriptor.getProperty("selectedBaseDN") + ","+ lcd.getBaseDN(); //$NON-NLS-1$
    else 
    	baseDN = "ou=users," + (String) wizardDescriptor.getProperty("oldSelectedBaseDN") + ","+ lcd.getBaseDN(); //$NON-NLS-1$
    
    updateComponentStates();
  }

  public final void removeChangeListener(ChangeListener l) {
    synchronized (listeners) {
      listeners.remove(l);
    }
  }

  public void storeSettings(Object settings) {
    WizardDescriptor wd = (WizardDescriptor) settings;
    wd.putProperty("initOUs", ousCheckBox.isSelected()); //$NON-NLS-1$
    wd.putProperty("initAdmin", adminCheckBox.isSelected()); //$NON-NLS-1$
    wd.putProperty("initLocation", locationCheckBox.isSelected()); //$NON-NLS-1$
    wd.putProperty("initHwtypeAndDevices",hwtypeAndDevicesCheckBox.isSelected()); //$NON-NLS-1$
    wd.putProperty("adminName", adminNameField.getText()); //$NON-NLS-1$
    wd.putProperty("adminBaseDN", baseDN); //$NON-NLS-1$
  }
}
