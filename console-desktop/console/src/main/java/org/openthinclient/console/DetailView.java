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
package org.openthinclient.console;

import javax.swing.JComponent;

import org.openide.nodes.Node;
import org.openide.windows.TopComponent;

/**
 * @author levigo
 */
public interface DetailView {
  /**
   * Set the current node selection and top component. Used to initialize the
   * detail view to the current selection before it is displayed in the detail
   * view top component.
   * 
   * @param selection
   * @param tc
   */
  void init(Node[] selection, TopComponent tc);

  /**
   * Get the component to be displayed in the center area of the detail view.
   * 
   * @return
   */
  public abstract JComponent getMainComponent();

  /**
   * Get the component to be displayed above the main component.
   * 
   * @return the component or <code>null</code>, if there is no header
   *         component.
   */
  public abstract JComponent getHeaderComponent();

  /**
   * Get the component to be displayed below the main component.
   * 
   * @return the component or <code>null</code>, if there is no footer
   *         component.
   */
  public abstract JComponent getFooterComponent();
}
