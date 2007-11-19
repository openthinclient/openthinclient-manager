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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openide.WizardDescriptor;
import org.openide.explorer.ExplorerManager;
import org.openide.util.HelpCtx;
import org.openthinclient.console.Messages;
import org.openthinclient.ldap.LDAPConnectionDescriptor;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class InitEnvironmentPanel implements WizardDescriptor.Panel,EnableableWizardPanel {

  private final ExplorerManager manager = new ExplorerManager();
  
  private LDAPConnectionDescriptor connectionDescriptor;
  
  private JTextField descriptionField;

  private final Set<ChangeListener> listeners = new HashSet<ChangeListener>(1);

  private JTextField nameField;

  private JLabel baseDNLabel;
  
  private String baseDN;
  
  private String newBaseDN;

  private JPanel component;
  
  private WizardDescriptor wizardDescriptor;
  
  private static boolean allowed = true;

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
      "r:p,3dlu,f:p:g,3dlu,p,3dlu,p"), Messages.getBundle(), new MyPanel()); //$NON-NLS-1$
    int DEFAULT_COLSPAN = 5; 

      manager.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          fireChangeEvent();
        }
      });
      
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
      
      dfb.appendI15dSeparator(Messages.getString("InitEnvironmentPanel.separator.baseDN")); //$NON-NLS-1$
      dfb.nextLine();
        
      baseDNLabel= new JLabel();
      dfb.append(baseDNLabel, dfb.getColumnCount() - 2);
      dfb.nextLine();
      
      dfb.appendI15dSeparator(Messages.getString("InitEnvironmentPanel.separator.settings")); //$NON-NLS-1$
    	      dfb.nextLine();
      
      nameField = new JTextField();
      nameField.setText(Messages.getString("NewRealmInit.new_folder.defaultName")); 
      nameField.getDocument().addDocumentListener(documentForwarder);
      
      dfb.appendI15d(Messages.getString("NewRealmInit.new_folder_name"), nameField, DEFAULT_COLSPAN); //$NON-NLS-1$
      dfb.nextLine();

      descriptionField = new JTextField();
      
      dfb.appendI15d("NewRealmInit.description", descriptionField, DEFAULT_COLSPAN); //$NON-NLS-1$
      dfb.nextLine();
      

      descriptionField.getDocument().addDocumentListener(
          new DocumentListener() {
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
      });
      
      updateComponentStates();
      
      component = dfb.getPanel();
      component.setName(Messages.getString("InitEnvironmentPanel.name")); //$NON-NLS-1$
    }

    return component;
   
    
  }
  protected void updateComponentStates() 
  {	
  	newBaseDN = "ou=" +nameField.getText();
  	if(baseDN != null){
  		if(baseDN.equals("")) {
  			baseDNLabel.setText(Messages.getString("InitEnvironmentPanel.baseDN", newBaseDN,connectionDescriptor.getBaseDN()) ); //$NON-NLS-1$
  		}
  		else {	
  			baseDNLabel.setText(Messages.getString("InitEnvironmentPanel.baseDN.long", newBaseDN, baseDN, connectionDescriptor.getBaseDN())); //$NON-NLS-1$
	 			newBaseDN = newBaseDN +"," + baseDN;	
  		}
  	}	
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
    connectionDescriptor = (LDAPConnectionDescriptor) (wizardDescriptor).getProperty("connectionDescriptor"); //$NON-NLS-1$
    baseDN = (wizardDescriptor).getProperty("oldSelectedBaseDN").toString(); //$NON-NLS-1$
    
    updateComponentStates();
  }

  public final void removeChangeListener(ChangeListener l) {
    synchronized (listeners) {
      listeners.remove(l);
    }
  }

public void storeSettings(Object settings) {
    WizardDescriptor wd = (WizardDescriptor) settings;
    wd.putProperty("newFolderName", nameField.getText()); //$NON-NLS-
    wd.putProperty("treeSelection", manager.getSelectedNodes()); //$NON-NLS-1$
    wd.putProperty("selectedBaseDN", newBaseDN); //$NON-NLS-1$
    wd.putProperty("description", descriptionField.getText()); //$NON-NLS-1$
    wd.putProperty("ConnectionDescriptor", connectionDescriptor); //$NON-NLS-1$
  }

  public boolean isEnabled(WizardDescriptor wd) {
	  if (null == wd)
	      return true;
	  Object tco = wd.getProperty("newFolderBox"); //$NON-NLS-1$
	  return tco == null || ((Boolean) tco).booleanValue();
  }
}
