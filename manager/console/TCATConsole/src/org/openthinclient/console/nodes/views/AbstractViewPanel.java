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
package org.openthinclient.console.nodes.views;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openthinclient.console.util.DetailViewFormBuilder;


/**
 * @author levigo
 */
abstract class AbstractViewPanel extends JPanel {

  /**
   * @param o
   * @return
   */
  protected static JLabel safeMakeLabel(Object o) {
    return new JLabel(o != null ? o.toString() : ""); //$NON-NLS-1$
  }

  protected void appendRow(DetailViewFormBuilder dfb, String label, Object value) {
    dfb.appendI15d(label, safeMakeLabel(value));
    dfb.nextLine();
  }
}
