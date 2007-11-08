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
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.console.Messages;


/**
 * A simple tablemodel that lists DirectoryObjects and is able to add and remove
 * them.
 * 
 * @author Natalie Bohnert
 */
public class DirObjectsTableModel extends AbstractTableModel {

  protected ArrayList<DirectoryObject> dirObjects;
  protected String[] columnNames;

  public DirObjectsTableModel(Set<? extends DirectoryObject> other) {
    dirObjects = new ArrayList<DirectoryObject>();
    if (other != null) {
      for (DirectoryObject dir : other) {
        this.dirObjects.add(dir);
      }
    }
    String columns = Messages.getString("table.DirObjects"); //$NON-NLS-1$
    if (columns != null) {
      columnNames = columns.split(","); //$NON-NLS-1$
    }
  }

  public DirObjectsTableModel() {
    dirObjects = new ArrayList<DirectoryObject>();
  }

  /*
   * @see javax.swing.table.TableModel#getRowCount()
   */
  public int getRowCount() {
    return dirObjects.size();
  }

  /*
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  public int getColumnCount() {
    return 2;
  }

  /*
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  public Object getValueAt(int rowIndex, int columnIndex) {
    DirectoryObject object = dirObjects.get(rowIndex);
    switch (columnIndex){
      case 0 :
        return object.getName();
      case 1 :
        return object.getDescription();
    }
    return null;
  }

  public String getColumnName(int index) {
    if (columnNames != null) {
      return columnNames[index];
    }
    return null;
  }

  public void addDirectoryObject(DirectoryObject newObj) {
    if (!dirObjects.contains(newObj)) {
      dirObjects.add(newObj);
      fireTableRowsInserted(dirObjects.size() - 1, dirObjects.size() - 1);
    }
  }

  public void removeDirectoryObjectAt(int index) {
    if (dirObjects.remove(index) != null)
      fireTableRowsDeleted(index, index);
  }

  public DirectoryObject getDirectoryObjectAt(int row) {
    return dirObjects.get(row);
  }

  public ArrayList<? extends DirectoryObject> getDirectoryObjects() {
    return dirObjects;
  }

}
