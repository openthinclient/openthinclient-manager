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
 * @author levigo
 */
class InstallationLogEntry {
	public enum Type {
		FILE_INSTALLATION, FILE_REMOVAL, FILE_MODIFICATION, DIRECTORY_CREATION, SYMLINK_INSTALLATION
	}

	final InstallationLogEntry.Type type;

	final File targetFile;

	final File backupFile;

	public InstallationLogEntry(Type type, File targetFile, File backupFile) {
		this.type = type;
		this.targetFile = targetFile;
		this.backupFile = backupFile;
	}

	public InstallationLogEntry(Type type, File targetFile) {
		this(type, targetFile, null);
	}

	public File getBackupFile() {
		return backupFile;
	}

	public File getTargetFile() {
		return targetFile;
	}

	public InstallationLogEntry.Type getType() {
		return type;
	}

	@Override
	public String toString() {
		return type + ": " + targetFile
				+ (null != backupFile ? "backed up in " + backupFile : "");
	}
}
