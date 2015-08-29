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

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

/**
 * This filter table model is able to filter based on a second table model. A
 * row is excluded, if it is contained in the second model. Both models must
 * have the same number of columns.
 * 
 * @author levigo
 */
public class ExcludeFilterTableModel extends FilterTableModel
    implements
      TableModelListener {
  private final TableModel exclusionModel;

  /**
   * 
   */
  public ExcludeFilterTableModel(TableModel delegate, TableModel exclusionModel) {
    this.exclusionModel = exclusionModel;
    setTableModel(delegate);
  }

  /*
   * @see org.openthinclient.console.util.FilterTableModel#updateFilteredRows()
   */
  @Override
  public void updateFilteredRows() {
    super.updateFilteredRows();
  }

  /*
   * @see org.openthinclient.console.util.FilterTableModel#filterRow(int)
   */
  @Override
  protected boolean filterRow(int i) {
    outer:for (int j = 0; j < exclusionModel.getRowCount(); j++) {
      for (int k = 0; k < getColumnCount(); k++) {
        Object mine = tableModel.getValueAt(i, k);
        Object exclude = exclusionModel.getValueAt(j, k);
        
        if(mine.equals(exclude))
        	return false;

        if(mine == null && exclude != null)
          continue outer;
        
        if(mine == null && exclude == null) {
        	continue outer;
        }

        if(!mine.equals(exclude))
             continue outer;
      } 
      return false;
    }
    return true;
  }
}
