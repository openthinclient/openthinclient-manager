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
package org.openthinclient.server;

import java.util.Iterator;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

/**
 * @author levigo
 */
public class ServerConfigurationFactory {
  /**
   * Get the current server configuration. This method fails, if it is invoked
   * anywhere but on the server.
   * 
   * @return
   * @throws TCATServerException
   */
  public static ServerConfiguration getOnServer() throws TCATServerException {
    try {
      // find the local MBeanServer
      MBeanServer server = locateJBoss();
      if (null == server)
        throw new IllegalStateException("Can't locate JBoss MBean server");
      // target MBean
      ObjectName objectName = new ObjectName("tcat:service=ConfigService");

      return (ServerConfiguration) server.invoke(objectName,
          "getConfiguration", new Object[]{}, new String[]{});
    } catch (Exception e) {
      throw new TCATServerException(
          "Could not get current client configuration", e);
    }
  }

  public static MBeanServer locateJBoss() {
    for (Iterator i = MBeanServerFactory.findMBeanServer(null).iterator(); i
        .hasNext();) {
      MBeanServer server = (MBeanServer) i.next();
      if (server.getDefaultDomain().equals("jboss"))
        return server;
    }
    throw new IllegalStateException("No 'jboss' MBeanServer found!");
  }
}
