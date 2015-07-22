package org.openthinclient.service.dhcp;

import org.openthinclient.ldap.DirectoryException;

public class DhcpImpl implements Dhcp {

	private Dhcp dhcpService;
	
	public DhcpImpl(Dhcp dhcpService) {
		this.dhcpService = dhcpService;
	}

	public boolean reloadRealms() throws DirectoryException {
		return dhcpService.reloadRealms();
	}
}
