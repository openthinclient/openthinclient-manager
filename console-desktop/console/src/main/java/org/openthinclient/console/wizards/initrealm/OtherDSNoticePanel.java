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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;
import org.openthinclient.console.Messages;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class OtherDSNoticePanel
    implements
      WizardDescriptor.Panel,
      EnableableWizardPanel {

  private final Set<ChangeListener> listeners = new HashSet<ChangeListener>(1);

  private JPanel component;
  private WizardDescriptor wizardDescriptor;
  private String baseDN;

  private JLabel explanationLabel;

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
          "fill:max(250dlu;pref):grow"), Messages.getBundle()); //$NON-NLS-1$

      explanationLabel = new JLabel(Messages.getString(
          "OtherDSNoticePanel.explanation", baseDN)); //$NON-NLS-1$
      dfb.append(explanationLabel);

      component = dfb.getPanel();
      component.setName(Messages.getString("OtherDSNoticePanel.name")); //$NON-NLS-1$
    }

    return component;
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
    Object tco = wizardDescriptor.getProperty("ACISetupTakenCareOf"); //$NON-NLS-1$;

    explanationLabel.setEnabled(tco == null || !((Boolean) tco).booleanValue());
    explanationLabel.setText(Messages.getString(
        "OtherDSNoticePanel.explanation", baseDN)); //$NON-NLS-1$
  }

  public final void removeChangeListener(ChangeListener l) {
    synchronized (listeners) {
      listeners.remove(l);
    }
  }

  public void storeSettings(Object settings) {
  }

  /*
   * @see org.openthinclient.console.wizards.initrealm.EnableableWizardPanel#isEnabled(org.openide.WizardDescriptor)
   */
  public boolean isEnabled(WizardDescriptor wd) {
    if (null == wd)
      return true;

    Object tco = wd.getProperty("ACISetupTakenCareOf"); //$NON-NLS-1$
    return tco == null || !((Boolean) tco).booleanValue();
  }
}
