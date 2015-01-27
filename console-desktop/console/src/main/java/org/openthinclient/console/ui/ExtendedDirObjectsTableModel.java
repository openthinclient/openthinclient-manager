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
package org.openthinclient.console.ui;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.log4j.Logger;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.console.Messages;


/**
 * @author Natalie Bohnert
 */
public class ExtendedDirObjectsTableModel extends DirObjectsTableModel {

  private ArrayList<JTree> paths = new ArrayList();

  /**
   * @param other
   */
  public ExtendedDirObjectsTableModel(Set<? extends DirectoryObject> other) {
    super(other);
  }

  public ExtendedDirObjectsTableModel(
      Map<String, Set<? extends DirectoryObject>> dirObjects) {
    Set<String> keys = dirObjects.keySet();

    for (String key : keys) {
      Set<? extends DirectoryObject> currDirObjects = dirObjects.get(key);
      for (DirectoryObject dirObject : currDirObjects) {
        addDirectoryObject(dirObject);
        int index = this.dirObjects.indexOf(dirObject);
        String[] nodes = key.split(";"); //$NON-NLS-1$

        JTree tree = null;
        DefaultMutableTreeNode parent = null;
        for (int i = 0; i < nodes.length; i++) {
          if (nodes[i].length() > 0) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodes[i],
                true);
            if (i == 0) {
              tree = new JTree(node);
              tree.setShowsRootHandles(true);
              tree.setRootVisible(true);
              tree.setVisible(true);
              tree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                  Logger.getLogger(this.getClass()).debug(
                      "tree selection changed"); //$NON-NLS-1$
                  ((JTree) e.getSource()).repaint();
                }
              });
              tree.addTreeExpansionListener(new TreeExpansionListener() {

                public void treeExpanded(TreeExpansionEvent event) {
                  Logger.getLogger(this.getClass()).debug("tree expanded"); //$NON-NLS-1$
                  fireTableDataChanged();
                  fireTableCellUpdated(paths.indexOf(event.getSource()), 2);
                }

                public void treeCollapsed(TreeExpansionEvent event) {
                  fireTableDataChanged();
                }
              });
            } else {
              parent.add(node);
            }
            parent = node;
          }
          paths.add(index, tree);
        }
      }
    }
    String columns = Messages.getString("table.ExtendedDirObjects"); //$NON-NLS-1$
    if (columns != null) {
      columnNames = columns.split(","); //$NON-NLS-1$
    }

  }

  public int getColumnCount() {
    return 3;
  }

  /*
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  public Object getValueAt(int rowIndex, int columnIndex) {
    DirectoryObject object = dirObjects.get(rowIndex);
    switch (columnIndex){
      case 0 : {
        return object.getName();
      }
      case 1 : {
        return object.getDescription();
      }
      case 2 : {
        return paths.get(rowIndex);
      }
    }
    return null;
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    if (columnIndex == 2) {
      return true;
    }
    return super.isCellEditable(rowIndex, columnIndex);
  }

  @Override
  public void fireTableDataChanged() {
    super.fireTableDataChanged();
  }

}
