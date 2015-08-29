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
package org.openthinclient.console.util;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.apache.commons.collections.primitives.ArrayIntList;
import org.apache.commons.collections.primitives.IntList;

/**
 * @author levigo
 */
public abstract class FilterTableModel extends AbstractTableModel
    implements
      TableModelListener {
  protected TableModel tableModel;
  private IntList filteredRowIndices = new ArrayIntList();
  
  /**
   * 
   */
  public FilterTableModel() {
  }

  /**
   * 
   */
  public FilterTableModel(TableModel delegate) {
    setTableModel(delegate);
  }

  /**
   * 
   */
  protected void updateFilteredRows() {
    filteredRowIndices.clear();
    int rowCount = tableModel.getRowCount();
    for (int i = 0; i < rowCount; i++)
      if (filterRow(i)) 
        filteredRowIndices.add(i);

    fireTableDataChanged();
  }

  /**
   * Return whether to filter the row.
   * 
   * @param i
   * @return true if the row should be IN the output.
   */
  protected abstract boolean filterRow(int i);

  public Class<?> getColumnClass(int columnIndex) {
    return null != tableModel
        ? tableModel.getColumnClass(columnIndex)
        : Object.class;
  }

  public int getColumnCount() {
    return null != tableModel ? tableModel.getColumnCount() : 0;
  }

  public String getColumnName(int columnIndex) {
    return null != tableModel ? tableModel.getColumnName(columnIndex) : ""; //$NON-NLS-1$
  }

  public int getRowCount() {
    return filteredRowIndices.size();
  }

  public Object getValueAt(int rowIndex, int columnIndex) {
    return null != tableModel ? tableModel.getValueAt(filteredRowIndices
        .get(rowIndex), columnIndex) : null;
  }

  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return null != tableModel ? tableModel.isCellEditable(filteredRowIndices
        .get(rowIndex), columnIndex) : false;
  }

  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    if (null != tableModel)
      tableModel.setValueAt(aValue, filteredRowIndices.get(rowIndex),
          columnIndex);
  }

  /*
   * @see javax.swing.event.TableModelListener#tableChanged(javax.swing.event.TableModelEvent)
   */
  public void tableChanged(TableModelEvent e) {
    updateFilteredRows();

    if (e.getFirstRow() == TableModelEvent.HEADER_ROW)
      fireTableStructureChanged();
    else
      fireTableDataChanged();
  }

  public TableModel getTableModel() {
    return tableModel;
  }

  public void setTableModel(TableModel delegate) {
    if (null != delegate)
      delegate.removeTableModelListener(this);

    this.tableModel = delegate;

    if (null != delegate) {
      delegate.addTableModelListener(this);
      updateFilteredRows();
    } else
      filteredRowIndices.clear();

    fireTableStructureChanged();
  }
  
  public int getUnfilteredRowIndex(int i) {
    return filteredRowIndices.get(i);
  }
}
