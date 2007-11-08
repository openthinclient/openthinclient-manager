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
import org.openide.nodes.FilterNode.Children;
import org.openthinclient.console.DetailViewProvider;

import com.levigo.util.swing.IconManager;

/**
 * @author levigo
 */
class OperationPendingNode extends AbstractNode {
  private final String message;

  /**
   * @param e
   * @param children
   */
  public OperationPendingNode(String message) {
    super(Children.LEAF);
    this.message = message;
  }

  /*
   * @see java.beans.FeatureDescriptor#getDisplayName()
   */
  @Override
  public String getName() {
    return message;
  }

  @Override
  public Image getIcon(int type) {
    return IconManager.getInstance(DetailViewProvider.class, "icons").getImage( //$NON-NLS-1$
        "tree.pending"); //$NON-NLS-1$
  }
}
