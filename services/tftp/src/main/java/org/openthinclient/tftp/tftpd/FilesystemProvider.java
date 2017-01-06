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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;

/**
 * @author levigo
 */
public class FilesystemProvider implements TFTPProvider {
  private final String basedir;

  public FilesystemProvider(String basedir) {
    this.basedir = basedir;
  }

  /*
   * @see org.openthinclient.tftp.tftpd.TFTPProvider#getLength(java.lang.String,
   *      java.lang.String)
   */
  public long getLength(SocketAddress peer, SocketAddress local, String prefix,
      String filename) throws IOException {
    File file = new File(basedir, filename);
    if (file.exists() && file.isFile() && file.canRead())
      return file.length();
    throw new FileNotFoundException(file.getPath());
  }

  /*
   * @see org.openthinclient.tftp.tftpd.TFTPProvider#getStream(java.lang.String,
   *      java.lang.String)
   */
  public InputStream getStream(SocketAddress peer, SocketAddress local,
      String prefix, String filename) throws IOException {
    File file = new File(basedir, filename);
    if (file.exists() && file.isFile() && file.canRead())
      return new FileInputStream(file);
    throw new FileNotFoundException(file.getPath());
  }
}
