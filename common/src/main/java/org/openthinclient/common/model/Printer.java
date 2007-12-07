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
package org.openthinclient.common.model;

import java.util.Collections;
import java.util.Set;

/**
 * @author levigo
 */
public class Printer extends Profile implements Group<DirectoryObject> {
	private static final long serialVersionUID = 1L;

	private static final Class[] MEMBER_CLASSES = new Class[]{Location.class,
			Client.class, User.class, UserGroup.class};

	private Set<DirectoryObject> members;

	/*
	 * @see org.openthinclient.common.model.Group#getMemberClasses()
	 */
	public Class[] getMemberClasses() {
		return MEMBER_CLASSES;
	}

	/*
	 * @see org.openthinclient.common.model.Group#getMembers()
	 */
	public Set<DirectoryObject> getMembers() {
		return members;
	}

	/*
	 * @see org.openthinclient.common.model.Group#setMembers(java.util.Set)
	 * @deprecated for LDAP mapping only
	 */
	public void setMembers(Set<DirectoryObject> members) {
		this.members = Collections.unmodifiableSet(members);
	}
}
