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
package org.openthinclient.util.dpkg;

import java.io.File;
/**
 * only contains an File and the URL for this File not less and not more
 * @author tauschfn
 *
 */
public class UrlAndFile {
	private String url;

	private File file;
	
	private String changelogDir;

	public UrlAndFile(String url, File file, String changelogDir) {
		this.file = file;
		this.url = url;
		this.changelogDir=changelogDir;
	}
	public UrlAndFile() {
		
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUrl() {
		return (this.url);
	}

	public void setFile(File file) {
		this.file = file;
	}

	public File getFile() {
		return (this.file);
	}
	public String getChangelogDir() {
		return changelogDir;
	}

}
