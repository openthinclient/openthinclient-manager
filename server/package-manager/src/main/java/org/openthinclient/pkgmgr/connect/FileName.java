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
package org.openthinclient.pkgmgr.connect;


/**
 *
 * 
 * @author tauschfn
 *
 */
public class FileName {

	private String fileName;
	private String serverFile;
	private String localFile;
	private String[] ret;
	/**
	 * 
	 * @param packFileName
	 * @param serverPath
	 * @param archivesPath
	 * @return String array with the server adress and the local File
	 */
	public String[] getLocationsForDownload (String packFileName, String serverPath,String archivesPath){
		fileName = packFileName;
		
		if (fileName.charAt(0) == '.') {
			fileName = fileName.substring(1);
			serverFile =fileName;
			localFile = archivesPath+fileName;
			ret = new String[2];
			ret[0] =serverPath+serverFile;
			ret[1] =localFile;
			return(ret);
		}
		else {
			serverFile = fileName;
			int lastSlashInName=fileName.lastIndexOf("/");
			fileName = fileName.substring(lastSlashInName+1);
			localFile = archivesPath + fileName;
			ret = new String[2];
			ret[0] =serverPath+serverFile;
			ret[1] =localFile;
			return(ret);
		}
	}
}
