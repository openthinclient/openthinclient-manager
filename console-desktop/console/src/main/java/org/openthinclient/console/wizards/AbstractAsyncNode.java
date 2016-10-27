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
package org.openthinclient.console.wizards;

import java.awt.Cursor;
import java.util.Enumeration;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

/**
 * @author levigo
 */
public abstract class AbstractAsyncNode extends DefaultMutableTreeNode {
  private boolean realChildrenInitialized = false;
  protected final JTree tree;

  protected AbstractAsyncNode(Object userObject, JTree tree) {
    super(userObject, true);
    this.tree = tree;
    // add a fake child which is replaced by the real children
    // during lazy loading
    insert(new DefaultMutableTreeNode(getPendingMessage()), 0);
  }

  /**
   * @return
   */
  protected abstract String getPendingMessage();

  /*
   * @see javax.swing.tree.DefaultMutableTreeNode#children()
   */
  @Override
  public Enumeration children() {
    ensureChildrenInitialized();
    return super.children();
  }

  /**
   * 
   */
  private void ensureChildrenInitialized() {
    if (!realChildrenInitialized) {
      realChildrenInitialized = true;
      refreshChildren();
    }
  }

  /*
   * @see javax.swing.tree.DefaultMutableTreeNode#getChildAt(int)
   */
  @Override
  public TreeNode getChildAt(int index) {
    ensureChildrenInitialized();
    return super.getChildAt(index);
  }

  /*
   * @see javax.swing.tree.DefaultMutableTreeNode#getChildCount()
   */
  @Override
  public int getChildCount() {
    ensureChildrenInitialized();
    return super.getChildCount();
  }

  /*
   * @see javax.swing.tree.DefaultMutableTreeNode#isLeaf()
   */
  @Override
  public boolean isLeaf() {
    return false; // never!
  }

  /**
   * 
   */
  protected void refreshChildren() {
    final Cursor plainCursor = tree.getCursor();
    tree.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    new Thread("Partition child loader") { //$NON-NLS-1$
      public void run() {
        try {
          doInitChildren();
        } finally {
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              ((DefaultTreeModel) tree.getModel())
                  .nodeStructureChanged(AbstractAsyncNode.this);
              tree.setCursor(plainCursor);
            }
          });
        }
      }
    }.start();
  }

  /**
   */
  protected abstract void doInitChildren();
}
