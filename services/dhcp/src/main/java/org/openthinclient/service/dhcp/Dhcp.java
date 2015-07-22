package org.openthinclient.service.dhcp;

import org.openthinclient.ldap.DirectoryException;

public interface Dhcp {

	public boolean reloadRealms() throws DirectoryException;
	
}
