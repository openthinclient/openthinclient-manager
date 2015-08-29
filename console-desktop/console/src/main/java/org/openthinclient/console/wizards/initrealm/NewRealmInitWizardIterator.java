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

import java.awt.Component;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openide.WizardDescriptor;
import org.openthinclient.console.Messages;
import org.openthinclient.console.wizards.ConnectionSettingsWizardPanel;


public final class NewRealmInitWizardIterator
    implements
      WizardDescriptor.Iterator {
  private static int index;

  private List<WizardDescriptor.Panel> availablePanels = new ArrayList<WizardDescriptor.Panel>();
  private static List<WizardDescriptor.Panel> activePanels = new ArrayList<WizardDescriptor.Panel>();
  private List<WizardDescriptor.Panel> lastActivePanels = new ArrayList<WizardDescriptor.Panel>();

  private WizardDescriptor wizardDescriptor;

  public NewRealmInitWizardIterator() {
	index = 0;
    ConnectionSettingsWizardPanel connectionSettingsWizardPanel = new ConnectionSettingsWizardPanel();
    connectionSettingsWizardPanel.getComponent().putClientProperty(
        "WizardPanel_errorMessage", //$NON-NLS-1$
        Messages.getString("NewRealmInit_baseDN_host_error")); //$NON-NLS-1$
    availablePanels.add(connectionSettingsWizardPanel);

    availablePanels.add(new SelectBasePanel());
    availablePanels.add(new InitEnvironmentPanel());
    availablePanels.add(new InitDefaultObjectsPanel());
    availablePanels.add(new SetupADSACIPanel());
    availablePanels.add(new OtherDSNoticePanel());
    availablePanels.add(new SelectRegistrationPanel());

    initPanels();

    updateActivePanels();
  }

  /**
   * Initialize panels representing individual wizard's steps and sets various
   * properties for them influencing wizard appearance.
   */
  private void initPanels() {
    for (WizardDescriptor.Panel panel : availablePanels) {
      Component c = panel.getComponent();
      if (c instanceof JComponent) { // assume Swing components
        JComponent jc = (JComponent) c;
        // Turn on subtitle creation on each step
        jc.putClientProperty("WizardPanel_autoWizardStyle", Boolean.TRUE); //$NON-NLS-1$
        // Show steps on the left side with the image on the background
        jc.putClientProperty("WizardPanel_contentDisplayed", Boolean.TRUE); //$NON-NLS-1$
        // Turn on numbering of all steps
        jc.putClientProperty("WizardPanel_contentNumbered", Boolean.TRUE); //$NON-NLS-1$
      }

      // add listener for enable/disable
      panel.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          updateActivePanels();
        }
      });
    }
  }

  /**
   * Initialize panels representing individual wizard's steps and sets various
   * properties for them influencing wizard appearance.
   */
  private boolean updateActivePanels() {
    activePanels.clear();
    List<String> stepNames = new ArrayList<String>();
    for (WizardDescriptor.Panel panel : availablePanels) {
      if (panel instanceof EnableableWizardPanel && !((EnableableWizardPanel) panel).isEnabled(wizardDescriptor))
        continue;

      activePanels.add(panel);
      stepNames.add(panel.getComponent().getName());
    }

    String stepNamesA[] = stepNames.toArray(new String[stepNames.size()]);

    int i = 0;
    for (WizardDescriptor.Panel panel : activePanels) {
      Component c = panel.getComponent();
      if (c instanceof JComponent) { // assume Swing components
        JComponent jc = (JComponent) c;
        // Sets steps names for a panel
        jc.putClientProperty("WizardPanel_contentData", stepNamesA); //$NON-NLS-1$
        // Sets step number of a component
        jc.putClientProperty("WizardPanel_contentSelectedIndex", i); //$NON-NLS-1$
        // Turn on subtitle creation on each step
        jc.putClientProperty("WizardPanel_autoWizardStyle", Boolean.TRUE); //$NON-NLS-1$
        // Show steps on the left side with the image on the background
        jc.putClientProperty("WizardPanel_contentDisplayed", Boolean.TRUE); //$NON-NLS-1$
        // Turn on numbering of all steps
        jc.putClientProperty("WizardPanel_contentNumbered", Boolean.TRUE); //$NON-NLS-1$

        jc.putClientProperty("WizardPanel_errorMessage", Messages //$NON-NLS-1$
            .getString("NewRealmInit_baseDN_host_error")); //$NON-NLS-1$

        i++;
      }
    }

    if (!activePanels.equals(lastActivePanels)) {
      lastActivePanels.clear();
      lastActivePanels.addAll(activePanels);
      return true;
    }

    return false;
  }

  public WizardDescriptor.Panel current() {
    return activePanels.get(index);
  }
  
  public static WizardDescriptor.Panel current(int index) {
	    return activePanels.get(index);
  }

  public String name() {
    return Messages.getString("Wizards.xofy", index + 1, activePanels.size()); //$NON-NLS-1$
  }

  public boolean hasNext() {
    return index < activePanels.size() - 1;
  }

  public boolean hasPrevious() {
	 return index > 0;
  }

  public void nextPanel() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    index++;
  }

  public void previousPanel() {
    if (!hasPrevious()) {
      throw new NoSuchElementException();
    } 
    	index--;
  }

  // If something changes dynamically (besides moving between panels), e.g.
  // the number of panels changes in response to user input, then uncomment
  // the following and call when needed: fireChangeEvent();
  private transient Set<ChangeListener> listeners = new HashSet<ChangeListener>(
      1);

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
    ChangeEvent ev = new ChangeEvent(this);
    while (it.hasNext()) {
      it.next().stateChanged(ev);
    }
  }

  private void readObject(ObjectInputStream in) throws IOException,
      ClassNotFoundException {
    in.defaultReadObject();
    listeners = new HashSet<ChangeListener>(1);
  }

  public void setWizardDescriptor(WizardDescriptor wizardDescriptor) {
    this.wizardDescriptor = wizardDescriptor;
  }

  public static int getIndex() {
		return index;
  }
	
  public static void setIndex(int index) {
		NewRealmInitWizardIterator.index = index;
  } 
}
