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
package org.openthinclient.pkgmgr.connect;

import java.io.File;

public class FileName {

  public static class DownloadItem {
    private final String serverPath;
    private final File localFile;

    public DownloadItem(String serverPath, File localFile) {
      this.serverPath = serverPath;
      this.localFile = localFile;
    }

    public String getServerPath() {
      return serverPath;
    }

    public File getLocalFile() {
      return localFile;
    }
  }

  /**
	 * 
	 * @param packFileName
	 * @param serverPath
	 * @param archivesPath
	 * @return String array with the server address and the local File
	 */
	public DownloadItem getLocationsForDownload(String packFileName,
			String serverPath, File archivesPath) {
    String fileName = packFileName;

    String serverFile;
    File localFile;
    if (fileName.charAt(0) == '.') {
			fileName = fileName.substring(1);
			serverFile = fileName;
			localFile = new File(archivesPath, fileName);
      return new DownloadItem(serverPath + serverFile, localFile);
		} else {
			serverFile = fileName;
			final int lastSlashInName = fileName.lastIndexOf("/");
			fileName = fileName.substring(lastSlashInName + 1);
			localFile = new File(archivesPath, fileName);
      return new DownloadItem(serverPath + serverFile, localFile);
		}
	}
}
