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
package org.openthinclient.tftp.tftpd;

/**
 * The TFTPExport represents a directory made visible by TFTP. A directory is
 * specified by its path and the name of the share as a path prefix.
 */
public class TFTPExport {
  private final String pathPrefix;
  private final TFTPProvider provider;

  public TFTPExport(String prefix, TFTPProvider provider) {

    // make sure there's a leading /
    if (!prefix.startsWith("/"))
      prefix = "/" + prefix;

    // make sure there is a trailing /
    if (!prefix.endsWith("/"))
      prefix += "/";

    this.pathPrefix = prefix;
    this.provider = provider;
  }

  public TFTPProvider getProvider() {
    return provider;
  }

  /**
   * 
   * @return
   */
  public String getPathPrefix() {
    return pathPrefix;
  }

  /**
   * The hashCode is based on the pathPrexif hash code.
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return 98475935 ^ pathPrefix.hashCode();
  }

  /**
   * Two exports are considered equal if, and only if their pathPrefixes are
   * equal.
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    return obj instanceof TFTPExport
        && ((TFTPExport) obj).getPathPrefix().equals(pathPrefix);
  }

  @Override
  public String toString() {
    return pathPrefix + "=" + provider;
  }
}
