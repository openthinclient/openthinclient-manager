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
/*
 * This code is based on: JNFSD - Free NFSD. Mark Mitchell 2001
 * markmitche11@aol.com http://hometown.aol.com/markmitche11
 */
package org.openthinclient.service.nfs;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.File;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author levigo
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class NFSExport implements Serializable, Cloneable {

	private static final long serialVersionUID = 3257846571638207028L;

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Group implements Cloneable {
		private InetAddress address;
		private int mask;
		private boolean readOnly;
		private boolean wildcard;

		public InetAddress getAddress() {
			return address;
		}

		public int getMask() {
			return mask;
		}

		public boolean isReadOnly() {
			return readOnly;
		}

		public boolean isWildcard() {
			return wildcard;
		}

		public void setAddress(InetAddress address) {
			this.address = address;
		}

		public void setMask(int mask) {
			this.mask = mask;
		}

		public void setReadOnly(boolean readOnly) {
			this.readOnly = readOnly;
		}

		public void setWildcard(boolean wildcard) {
			this.wildcard = wildcard;
		}

		@Override
		public String toString() {
			if (isWildcard())
				return "*" + (readOnly ? "(ro)" : "(rw)");
			else
				return (null != address ? address.toString() : "")
						+ (0 != mask ? "/" + mask : "") + (readOnly ? "(ro)" : "(rw)");
		}

		@Override
		public Group clone() {
			try {
				return (Group) super.clone();
			} catch (CloneNotSupportedException e) {
				// shall never happen, as this class is cloneable
				throw new Error(e);
			}
		}
	}

	@XmlAttribute
	private String name;
	@XmlElement
	private File root;
	@XmlElement
	private final List<Group> groups = new ArrayList<>();

	@XmlTransient
	private boolean revoked;

	@XmlTransient
	private int cacheTimeout = 15000;

	public void setName(String name) {
		this.name = name;
	}

	public void setRoot(File root) {
		this.root = root;
	}

	public File getRoot() {
		return root;
	}

	public List<Group> getGroups() {
		return groups;
	}

	public String getName() {
		return name;
	}

	public boolean isRevoked() {
		return revoked;
	}

	public void setRevoked(boolean revoked) {
		this.revoked = revoked;
	}

	public int getCacheTimeout() {
		return cacheTimeout;
	}

	public void setCacheTimeout(int cacheTimeout) {
		this.cacheTimeout = cacheTimeout;
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer(root.getAbsolutePath());
		sb.append("|").append(name);
		if (null != groups)
			for (final Group group : groups)
				sb.append("|").append(group);
		else
			sb.append("|*(rw)");

		return sb.toString();
	}

	@Override
	public NFSExport clone() {
		final NFSExport clone = new NFSExport();
		clone.setName(name);
		clone.setRevoked(revoked);
		clone.setRoot(root);
		clone.setCacheTimeout(cacheTimeout);

		clone.getGroups().addAll(groups.stream().map(Group::clone).collect(Collectors.toList()));

		return clone;
	}
}
