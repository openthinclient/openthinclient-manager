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
import org.openide.util.actions.SystemAction;
import org.openthinclient.console.EditAction;
import org.openthinclient.ldap.LDAPConnectionDescriptor;


/** Getting the feed node and wrapping it in a FilterNode */
public class PartitionNode extends DirectoryEntryNode {
  /**
   * @param c
   * @param node
   * @param lcd
   * @param dn
   */
  public PartitionNode(Children c, Node node, LDAPConnectionDescriptor lcd,
      String dn) {
    super(c, node, lcd, dn);
  }

  /**
   * @param node
   * @param lcd
   * @param rdn
   */
  public PartitionNode(Node node, LDAPConnectionDescriptor lcd, String dn) {
    super(node, lcd, dn);
  }

  @Override
  public SystemAction getDefaultAction() {
    return SystemAction.get(EditAction.class);
  }

  /*
   * @see org.openide.nodes.FilterNode#canCopy()
   */
  @Override
  public boolean canCopy() {
    return true;
  }

  /*
   * @see org.openide.nodes.FilterNode#canDestroy()
   */
  @Override
  public boolean canDestroy() {
    return false;
  }

  /*
   * @see org.openide.nodes.FilterNode#canRename()
   */
  @Override
  public boolean canRename() {
    return false;
  }
}
