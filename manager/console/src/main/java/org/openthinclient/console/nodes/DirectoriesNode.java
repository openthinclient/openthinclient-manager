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
package org.openthinclient.console.nodes;

import java.awt.Dialog;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.MessageFormat;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.WizardDescriptor;
import org.openide.cookies.InstanceCookie;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.Repository;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Utilities;
import org.openthinclient.console.DetailViewProvider;
import org.openthinclient.console.Messages;
import org.openthinclient.console.wizards.registerdirectory.RegisterDirectoryWizardIterator;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.LDAPConnectionDescriptor;

import com.levigo.util.swing.IconManager;

/** Getting the root node */
public class DirectoriesNode extends FilterNode {
  /** Getting the children of the root node */
  private static class RootChildren extends FilterNode.Children {
    RootChildren(Node folderNode) {
      super(folderNode);
    }

    protected Node[] createNodes(Object key) {
      Node n = (Node) key;
      try {
        LDAPConnectionDescriptor cd = getConnectionDescriptor(n);
        return new Node[]{new DirectoryNode(n, getNode(), cd)};
      } catch (Exception e) {
        ErrorManager.getDefault().notify(e);
        return new Node[]{new ErrorNode(Messages
            .getString("error.OpenRealmFailed"), e)}; //$NON-NLS-1$
      }
    }
  }

  private static LDAPConnectionDescriptor getConnectionDescriptor(Node node) throws IOException,
      ClassNotFoundException, DirectoryException {
    InstanceCookie ck = (InstanceCookie) node.getCookie(InstanceCookie.class);
    if (ck == null) {
      throw new IllegalStateException(Messages.getString("DirectoriesNode.bogus") //$NON-NLS-1$
          + node.getLookup().lookup(FileObject.class));
    }

    return (LDAPConnectionDescriptor) ck.instanceCreate();
  }

  public DirectoriesNode() throws DataObjectNotFoundException {
    super(DataObject.find(
        Repository.getDefault().getDefaultFileSystem().getRoot().getFileObject(
            "Directories")).getNodeDelegate()); //$NON-NLS-1$
    setChildren(new RootChildren(getOriginal()));
    disableDelegation(DELEGATE_GET_DISPLAY_NAME);
  }

  /** An action for adding a realm */
  public static class RegisterDirectoryAction extends AbstractAction {

    private DataFolder folder;

    public RegisterDirectoryAction() {
      super(Messages.getString("action." //$NON-NLS-1$
          + RegisterDirectoryAction.class.getSimpleName()));
    }

    public RegisterDirectoryAction(DataFolder df) {
      super(Messages.getString("action." //$NON-NLS-1$
          + RegisterDirectoryAction.class.getSimpleName()));
      folder = df;
    }

    public void actionPerformed(ActionEvent ae) {
      WizardDescriptor.Iterator iterator = new RegisterDirectoryWizardIterator();
      WizardDescriptor wizardDescriptor = new WizardDescriptor(iterator);
      wizardDescriptor.setTitleFormat(new MessageFormat("{0} ({1})")); //$NON-NLS-1$
      wizardDescriptor.setTitle(Messages.getString("action." //$NON-NLS-1$
          + RegisterDirectoryAction.class.getSimpleName()));
      Dialog dialog = DialogDisplayer.getDefault().createDialog(
          wizardDescriptor);
      
      dialog.setIconImage(Utilities.loadImage(
					"org/openthinclient/console/icon.png", true));
      
      dialog.setVisible(true);
      dialog.toFront();

      if (wizardDescriptor.getValue() == WizardDescriptor.FINISH_OPTION) {
        LDAPConnectionDescriptor cd = (LDAPConnectionDescriptor) wizardDescriptor
            .getProperty("connectionDescriptor"); //$NON-NLS-1$
        assert null != cd;

        FileObject fld = folder.getPrimaryFile();
        String baseName = "DirectoryEnv-" + cd.hashCode(); //$NON-NLS-1$
        if (fld.getFileObject(baseName, "ser") != null) { //$NON-NLS-1$
          DialogDisplayer.getDefault().notify(
              new NotifyDescriptor.Message(
                  Messages.getString("DirectoriesNode.alreadyRegistered"), //$NON-NLS-1$
                  NotifyDescriptor.WARNING_MESSAGE));
          return;
        }

        try {
          FileObject writeTo = fld.createData(baseName, "ser"); //$NON-NLS-1$
          FileLock lock = writeTo.lock();
          try {
            ObjectOutputStream str = new ObjectOutputStream(writeTo
                .getOutputStream(lock));
            try {
              str.writeObject(cd);
            } finally {
              str.close();
            }
          } finally {
            lock.releaseLock();
          }
        } catch (IOException ioe) {
          ErrorManager.getDefault().notify(ioe);
        }
      }
    }
  }

  /** Declaring the Add Feed action and Add Folder action */
  public Action[] getActions(boolean popup) {
    DataFolder df = (DataFolder) getLookup().lookup(DataFolder.class);
    return new Action[]{new RegisterDirectoryAction(df)};
  }

  public String getName() {
    return Messages.getString("node." + getClass().getSimpleName()); //$NON-NLS-1$
  }

  /*
   * @see org.openide.nodes.FilterNode#getIcon(int)
   */
  @Override
  public Image getIcon(int type) {
    return getOpenedIcon(type);
  }

  /*
   * @see org.openide.nodes.FilterNode#getOpenedIcon(int)
   */
  @Override
  public Image getOpenedIcon(int type) {
    return IconManager.getInstance(DetailViewProvider.class, "icons").getImage( //$NON-NLS-1$
        "tree." + getClass().getSimpleName()); //$NON-NLS-1$
  }
}
