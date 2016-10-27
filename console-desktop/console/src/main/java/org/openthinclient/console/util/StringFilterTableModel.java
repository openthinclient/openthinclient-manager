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
 * @author levigo
 */
public class StringFilterTableModel extends FilterTableModel
    implements
      TableModelListener {
  private String filter;

  /**
   * 
   */
  public StringFilterTableModel() {
  }

  /**
   * 
   */
  public StringFilterTableModel(TableModel delegate) {
    setTableModel(delegate);
  }

  public String getFilter() {
    return filter;
  }

  public void setFilter(String filter) {
    this.filter = filter;
    updateFilteredRows();

    fireTableDataChanged();
  }

  /*
   * @see org.openthinclient.console.util.FilterTableModel#filterRow(int)
   */
  @Override
  protected boolean filterRow(int i) {
    if (null != filter && filter.length() > 0) {
      for (int j = 0; j < tableModel.getColumnCount(); j++) {
        Object v = tableModel.getValueAt(i, j);
        if (null != v && v.toString().contains(filter))
          return true;
      }
      return false;
    }
    return true;
  }
}
