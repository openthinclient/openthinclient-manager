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
package org.openthinclient.nfs;

import java.io.File;
import java.net.UnknownHostException;

import org.jboss.system.ServiceMBean;
import org.openthinclient.mountd.NFSExport;

/**
 * @author levigo
 */
public interface NFSServiceMBean extends ServiceMBean {
  /**
   * Get a list of active exports
   * 
   * @return
   */
  public Exports getExports();

  /**
   * Add a new export by a textual export spec.
   * 
   * @param exportSpec
   * @throws UnknownHostException 
   */
  public void addExport(String exportSpec) throws UnknownHostException;

  /**
   * Add a new export.
   * 
   * @param export
   */
  public void addExport(NFSExport export);

  /**
   * Remove an export by its name.
   * 
   * @param name
   */
  public boolean removeExport(String name);
  
  /**
   * Set Xports (DOM-Representation of Exports)
   * @return
   */
  void setExports(org.w3c.dom.Element o);
  
  public boolean moveLocalFile(File from, File to);
}
