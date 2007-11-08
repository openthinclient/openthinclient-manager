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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


import org.openide.WizardDescriptor;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.Node;
import org.openide.nodes.NodeAdapter;
import org.openide.nodes.NodeListener;
import org.openide.nodes.NodeMemberEvent;
import org.openide.util.HelpCtx;
import org.openide.util.WeakListeners;
import org.openthinclient.common.directory.LDAPConnectionDescriptor;
import org.openthinclient.console.Messages;
import org.openthinclient.console.nodes.DirectoryEntryNode;
import org.openthinclient.console.nodes.DirectoryNode;
import org.openthinclient.console.nodes.DirectoryViewNode;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class SelectBasePanel implements WizardDescriptor.Panel {

  private final ExplorerManager manager = new ExplorerManager();
  private LDAPConnectionDescriptor connectionDescriptor;

  private final Set<ChangeListener> listeners = new HashSet<ChangeListener>(1);
  
  private JCheckBox newFolderBox;
 
  private JPanel component;
  private WizardDescriptor wizardDescriptor;

  private final NodeAdapter nodeEventForwarder = new NodeAdapter() {
    public void childrenRemoved(NodeMemberEvent e) {
      forward();
    }

    private void forward() {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          fireChangeEvent();
        }
      });
    }

    public void childrenAdded(NodeMemberEvent e) {
      forward();
    }
  };

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
          "f:p:g"), Messages.getBundle(), new MyPanel()); //$NON-NLS-1$


      manager.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          fireChangeEvent(); 
        }
      });
      
      final ActionListener changeForwarder = new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            updateComponentStates();
            fireChangeEvent();
          }
        };

      dfb.appendI15dTitle("NewRealmInit.select_location"); //$NON-NLS-1$
      dfb.nextLine();

      BeanTreeView view = new BeanTreeView();
      view.setDefaultActionAllowed(false);
      view.setRootVisible(true);

      dfb.appendRow("min(140dlu;pref)"); //$NON-NLS-1$
      dfb.append(view);
      dfb.nextLine();
     
      newFolderBox = new JCheckBox();
      newFolderBox.setText(Messages.getString("NewRealmInit.new_folder.checkBox"));
      newFolderBox.setVerticalTextPosition(SwingConstants.TOP);
      newFolderBox.addActionListener(changeForwarder);
      newFolderBox.setSelected(true);

      dfb.append(newFolderBox, dfb.getColumnCount());
      dfb.nextLine();

      component = dfb.getPanel();
      component.setName(Messages.getString("NewRealmInit.name")); //$NON-NLS-1$
      
      updateComponentStates();
    }

    return component;
  }
  
  protected void updateComponentStates() {

	    if (null != wizardDescriptor)
	      wizardDescriptor.putProperty("newFolderBox", newFolderBox //$NON-NLS-1$
	          .isSelected());
	  }

  public HelpCtx getHelp() {
    return HelpCtx.DEFAULT_HELP;
  }

  private String getSelectedBaseDN() {
    Node[] selectedNodes = manager.getSelectedNodes();
    if (selectedNodes.length > 0
        && selectedNodes[0] instanceof DirectoryEntryNode) {
    	return ((DirectoryEntryNode) selectedNodes[0]).getDn();
    }
    return null;
  }


  public boolean isValid() {
    if (manager.getSelectedNodes().length != 1 || getSelectedBaseDN() == null) {
      wizardDescriptor.putProperty("WizardPanel_errorMessage", Messages //$NON-NLS-1$
          .getString("NewRealmInit.validation.error.no_node_selected")); //$NON-NLS-1$
      return false;
    }

    // make sure the OU is empty
    final DirectoryEntryNode den = (DirectoryEntryNode) manager
        .getSelectedNodes()[0];

    if(newFolderBox.isSelected() == false && getSelectedBaseDN().equals("")) {
    	wizardDescriptor.putProperty("WizardPanel_errorMessage", Messages //$NON-NLS-1$
    	          .getString("NewRealmInit.validation.error.no_node_selected")); //$NON-NLS-1$
    	return false;	
    }

    if (den.getChildren().getNodes().length != 0 && newFolderBox.isSelected() == false) {
      // we MUST attach a listener here, because the child list of the DEN is
      // dynamic.
      den.addNodeListener((NodeListener) WeakListeners.create(
          NodeListener.class, nodeEventForwarder, den));

      wizardDescriptor.putProperty("WizardPanel_errorMessage", Messages //$NON-NLS-1$
          .getString("NewRealmInit.validation.error.path_not_empty")); //$NON-NLS-1$
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
    connectionDescriptor = (LDAPConnectionDescriptor) (wizardDescriptor)
        .getProperty("connectionDescriptor"); //$NON-NLS-1$
    
    updateComponentStates();

    Node root;
    if (!connectionDescriptor.isBaseDnSet())
      root = new DirectoryNode(connectionDescriptor);
    else
    root = new DirectoryViewNode(connectionDescriptor);
    manager.setRootContext(root);
    
    try {
		manager.setSelectedNodes(new Node[]{root});
	} catch (PropertyVetoException e) {
		e.printStackTrace();
	}
  }

  public final void removeChangeListener(ChangeListener l) {
    synchronized (listeners) {
      listeners.remove(l);
    }
  }

  public void storeSettings(Object settings) {
    WizardDescriptor wd = (WizardDescriptor) settings;
    wd.putProperty("newFolderBox", newFolderBox.isSelected()); //$NON-NLS-1$
    wd.putProperty("oldSelectedBaseDN", getSelectedBaseDN()); //$NON-NLS-1$
  }
}
