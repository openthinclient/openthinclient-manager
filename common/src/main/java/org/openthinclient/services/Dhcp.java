package org.openthinclient.services;

import org.openthinclient.ldap.DirectoryException;

public interface Dhcp {

	public boolean reloadRealms() throws DirectoryException;
	
}
