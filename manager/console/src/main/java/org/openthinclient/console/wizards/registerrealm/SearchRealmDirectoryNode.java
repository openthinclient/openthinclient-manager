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
 ******************************************************************************/
package org.openthinclient.console.wizards.registerrealm;

import org.openide.nodes.Node;
import org.openthinclient.console.nodes.DirectoryNode;
import org.openthinclient.console.nodes.PartitionNode;
import org.openthinclient.ldap.LDAPConnectionDescriptor;

/**
 * A special kind of DirectoryNode with the goal of locating Realms in the DIT.
 * 
 * @author levigo
 */
class SearchRealmDirectoryNode extends DirectoryNode {

	/**
	 * @param dataNode
	 * @param parent
	 * @param cd
	 */
	public SearchRealmDirectoryNode(LDAPConnectionDescriptor cd) {
		super(Node.EMPTY, Node.EMPTY, cd);
	}

	/*
	 * @see org.openthinclient.console.nodes.DirectoryNode#createChild(org.openthinclient.common.directory.LDAPConnectionDescriptor,
	 *      java.lang.String)
	 */
	@Override
	protected PartitionNode createChild(LDAPConnectionDescriptor lcd, String rdn) {
		return new SearchRealmPartitionNode(this, lcd, rdn);
	}
}
