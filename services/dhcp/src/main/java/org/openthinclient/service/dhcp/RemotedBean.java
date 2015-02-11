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
package org.openthinclient.service.dhcp;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemotedBean implements Remoted {
	private static final Logger logger = LoggerFactory.getLogger(RemotedBean.class);

	public boolean dhcpReloadRealms() throws Exception {
//		final ObjectName objectName = new ObjectName("tcat:service=ConfigService");
//		final MBeanServer server = MBeanServerFactory.findMBeanServer(null).get(0);
//
//		if (Boolean.FALSE.equals(server.invoke(objectName, "reloadRealms",
//				new Object[]{}, new String[]{}))) {
//			logger.error("Unable to reloadRealms");
//			return false;
//		} else
//			return true;

    // FIXME this implementation has to be adapted to the new service control mechanism

    throw new UnsupportedOperationException();

  }
}
