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
package org.openthinclient.console.wizards.registerrealm;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.openide.ErrorManager;
import org.openide.nodes.Node;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.Messages;
import org.openthinclient.console.nodes.AbstractAsyncArrayChildren;
import org.openthinclient.console.nodes.DirectoryEntryNode;
import org.openthinclient.console.nodes.ErrorNode;
import org.openthinclient.console.nodes.RealmNode;
import org.openthinclient.ldap.LDAPConnectionDescriptor;


/**
 * @author levigo
 */
class RealmsInPartition extends AbstractAsyncArrayChildren {
	private final String dn;

	public RealmsInPartition(String dn) {
		this.dn = dn;
	}

	protected Collection asyncInitChildren() {
		try {
			LDAPConnectionDescriptor lcd = new LDAPConnectionDescriptor(
					((DirectoryEntryNode) getNode()).getConnectionDescriptor());
			if(lcd == null) {
		  		  return Collections.EMPTY_LIST;
		    }
			
			lcd.setBaseDN(dn);

			final Set<Realm> realms = LDAPDirectory.listRealms(lcd);
			
			return realms;
		} catch (Exception e) {
			ErrorManager.getDefault().notify(e);
			add(new Node[] { new ErrorNode(Messages
					.getString("RealmsInPartition.cantDisplay"), e) }); //$NON-NLS-1$
			
			return Collections.EMPTY_LIST;
		}
	}

	@Override
	protected Node[] createNodes(Object key) {
		return new Node[] { new RealmNode((Realm) key, true) };
	}
}
