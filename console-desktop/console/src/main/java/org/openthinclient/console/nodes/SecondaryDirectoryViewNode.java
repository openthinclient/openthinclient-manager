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

import org.openide.nodes.Node;
import org.openthinclient.console.Messages;
import org.openthinclient.ldap.LDAPConnectionDescriptor;


/** Getting the feed node and wrapping it in a FilterNode */
public class SecondaryDirectoryViewNode extends PartitionNode {
  /**
   * @param node
   * @param lcd
   * @param rdn
   */
  public SecondaryDirectoryViewNode(Node node, LDAPConnectionDescriptor lcd, String dn) {
    super(node, lcd, dn);
  }

  /**
   * @param connectionDescriptor
   */
  public SecondaryDirectoryViewNode(LDAPConnectionDescriptor connectionDescriptor) {
    this(Node.EMPTY, connectionDescriptor, ""); //$NON-NLS-1$
  }

  /*
   * @see org.openthinclient.console.nodes.DirectoryEntryNode#getName()
   */
  @Override
  public String getName() {
    return Messages.getString("SecondaryDirectoryViewNode.name"); //$NON-NLS-1$
  }

  /*
   * @see org.openthinclient.console.nodes.DirectoryEntryNode#getDisplayName()
   */
  @Override
  public String getDisplayName() {
    return getName();
  }
}
