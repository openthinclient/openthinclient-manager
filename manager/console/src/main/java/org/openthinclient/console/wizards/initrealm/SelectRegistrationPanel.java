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
/**
 * @author Michael Gold
 */


import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.explorer.ExplorerManager;
import org.openide.util.HelpCtx;
import org.openthinclient.console.Messages;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;


public class SelectRegistrationPanel implements WizardDescriptor.Panel {

  private final ExplorerManager manager = new ExplorerManager();

  private final Set<ChangeListener> listeners = new HashSet<ChangeListener>(1);

  private JCheckBox registrationCheckBox;
  
  private JPanel component;

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

  // Why is this so ridiculously complicated? Because the NetBeans
  // morons prevented the implementation of
  // WizardDescriptor.AsynchronousValidatingPanel
  // by JComponents by calling their validation method validate(). The latter
  // with a exception signature which is incompatible with the signature
  // of the method of the same name in Component.
  private class MyPanel extends JPanel implements ExplorerManager.Provider {
    /*
     * @see org.openide.explorer.ExplorerManager.Provider#getExplorerManager()
     */
    public ExplorerManager getExplorerManager() {
      return manager;
    }
  }

  public JComponent getComponent() {
    if (null == component) {
      final DefaultFormBuilder dfb = new DefaultFormBuilder(new FormLayout(
          "f:p:g"), Messages.getBundle(), new MyPanel()); 

      registrationCheckBox = new JCheckBox();
      registrationCheckBox.setText(Messages.getString("SelectRegistration.text"));
      registrationCheckBox.setVerticalTextPosition(SwingConstants.TOP);
      registrationCheckBox.setSelected(true);

      dfb.append(registrationCheckBox, dfb.getColumnCount());
      dfb.nextLine();

      component = dfb.getPanel();
      component.setName(Messages.getString("SelectRegistration.name")); //$NON-NLS-1$
    }

    return component;
  }
  

  public HelpCtx getHelp() {
    return HelpCtx.DEFAULT_HELP;
  }



  public boolean isValid() {
    return true;
  }

  // You can use a settings object to keep track of state. Normally the
  // settings object will be the WizardDescriptor, so you can use
  // WizardDescriptor.getProperty & putProperty to store information entered
  // by the user.
  public void readSettings(Object settings) {
  }

  public final void removeChangeListener(ChangeListener l) {
    synchronized (listeners) {
      listeners.remove(l);
    }
  }

  public void storeSettings(Object settings) {
    WizardDescriptor wd = (WizardDescriptor) settings;
    wd.putProperty("registration", registrationCheckBox.isSelected()); //$NON-NLS-1$

  }
}
