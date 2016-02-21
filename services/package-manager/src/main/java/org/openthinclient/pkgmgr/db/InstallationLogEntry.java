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
package org.openthinclient.pkgmgr.db;

import javax.persistence.*;
import java.nio.file.Path;

@Entity
@Table(name="otc_installation_log")
public class InstallationLogEntry {

	@Id
	private int id;
	@ManyToOne(fetch = FetchType.LAZY)
	private Installation installation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "package")
	private Package pkg;

	@Column
	private InstallationLogEntry.Type type;
	@Column
	private Path path;

	/**
	 * Use the factory methods instead.
	 */
	@Deprecated
	public InstallationLogEntry() {
	}

	InstallationLogEntry(final Installation installation, final Package pkg, final Type type, final Path path) {
		this.installation = installation;
		this.pkg = pkg;
		this.type = type;
		this.path = path;
	}

	public static InstallationLogEntry file(Installation installation, Package pkg, Path path) {
		return new InstallationLogEntry(installation, pkg, Type.FILE, path);
	}

	public static InstallationLogEntry symlink(Installation installation, Package pkg, Path path) {
		return new InstallationLogEntry(installation, pkg, Type.SYMLINK, path);
	}

	public static InstallationLogEntry dir(Installation installation, Package pkg, Path path) {
		return new InstallationLogEntry(installation, pkg, Type.DIR, path);
	}

	public Installation getInstallation() {
		return installation;
	}

	public Path getPath() {
		return path;
	}

	public int getId() {
		return id;
	}

	public InstallationLogEntry.Type getType() {
		return type;
	}

	public Package getPackage() {
		return pkg;
	}

	@Override public String toString() {
		return "InstallationLogEntry{" +
				"type=" + type +
				", path=" + path +
				'}';
	}

	// IMPORTANT: keep in mind that the database table has a limited size to represent the type.
	public enum Type {
		FILE, DIR, SYMLINK
	}
}
