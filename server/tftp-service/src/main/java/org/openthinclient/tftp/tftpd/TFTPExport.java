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

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

/**
 * The TFTPExport represents a directory made visible by TFTP. A directory is
 * specified by its path and the name of the share as a path prefix.
 */
public class TFTPExport {
  private final String pathPrefix;
  private final Class providerClass;
  private final Map<String, String> options;
  private TFTPProvider providerInstance;

  /**
   * Construct a new TFTPExport which makes the directory at basedir visible
   * with a prefix of "/", i.e. at the TFTP server root.
   * 
   * @param basedir the directory to export.
   */
  public TFTPExport(String basedir) {
    this("/", basedir);
  }

  /**
   * Construct a new TFTPExport which makes the directory at basedir visible
   * with the specified prefix.
   * 
   * @param prefix the path prefix in UNIX notation (i.e. using
   *          forward-slashes), starting with a slash, e.g. "/foo"
   * @param basedir the directory to export.
   */
  public TFTPExport(String prefix, String basedir) {
    // make sure there's a leading /
    if (!prefix.startsWith("/"))
      prefix = "/" + prefix;

    // make sure there's NO trailing /
    if (prefix.endsWith("/") && prefix.length() > 2)
      prefix = prefix.substring(0, prefix.length() - 1);

    this.pathPrefix = prefix;
    this.options = new HashMap<String, String>();
    this.options.put("basedir", basedir);

    this.providerClass = FilesystemProvider.class;
  }

  /**
   * 
   * @param prefix, prefix or triggerprefix, depending on its Context
   * @param basedir,
   * @param triggerClassName, className
   * @param options
   * @throws ClassNotFoundException
   * @throws FileNotFoundException
   */
  public TFTPExport(String prefix, String providerClassName, Map options)
      throws ClassNotFoundException {

    this.providerClass = Class.forName(providerClassName);

    // make sure there's a leading /
    if (!prefix.startsWith("/"))
      prefix = "/" + prefix;

    // make sure there is a trailing /
    if (!prefix.endsWith("/"))
      prefix += "/";

    this.pathPrefix = prefix;
    this.options = options;
  }

  public TFTPProvider getProvider() throws InstantiationException,
      IllegalAccessException {
    if (null == providerInstance)  {
      providerInstance = (TFTPProvider) providerClass.newInstance();
      providerInstance.setOptions(options);
    }
    return providerInstance;
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
    return pathPrefix + "=" + providerClass + "->" + options;
  }
}
