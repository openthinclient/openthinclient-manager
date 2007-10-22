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

import org.openide.nodes.Children;
import org.openide.nodes.Node;

/** Getting the feed node and wrapping it in a FilterNode */
class ManagementNode extends MyAbstractNode {
  public ManagementNode(Node parent) {
    super(new Children.Array(), parent.getLookup());

    // for (Class c : OBJECT_CLASSES) {
    // try {
    // getChildren()
    // .add(new Node[]{new DirObjectListNode(this, c)});
    // } catch (Exception e) {
    // ErrorManager.getDefault().notify(e);
    // getChildren().add(
    // new Node[]{new ErrorNode("Kann Kind nicht erzeugen.", e)});
    // }
    // }
  }
}
