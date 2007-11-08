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

import java.awt.Image;

import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.Lookup;
import org.openthinclient.console.DetailViewProvider;
import org.openthinclient.console.Messages;

import com.levigo.util.swing.IconManager;

public class MyAbstractNode extends AbstractNode {
  /**
   * @param children
   * @param lookup
   */
  public MyAbstractNode(Children children, Lookup lookup) {
    super(children, lookup);
  }

  /**
   * @param children
   */
  public MyAbstractNode(Children children) {
    super(children);
  }


  /* 
   * @see java.beans.FeatureDescriptor#getName()
   */
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
        "tree." + getIconName()); //$NON-NLS-1$
  }

  /**
   * @return String which contains the SimpleClassNAme which is used for icon
   */
  protected String getIconName() {
    return getClass().getSimpleName();
  }
}
