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

import java.io.Serializable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.levigo.util.preferences.PreferenceStoreHolder;

/**
 * @author levigo
 */
public class PackageReference implements Serializable {
	private static final long serialVersionUID = 3977016258086907959L;

	private static final Pattern SPECIFIER_PATTERN = Pattern
//	.compile("(\\S+)(?:\\s+\\((<<|<|<=|=|>=|>|>>)\\s+(\\S+)\\))?");
	.compile("(\\S+)(?:\\s+\\((<<|<|<=|=|>=|>|>>)\\s+(\\S+)\\s*\\))?");

	private static enum Relation {
		EARLIER("<<"), EARLIER_OR_EQUAL("<="), EQUAL("="), LATER_OR_EQUAL(">="), LATER(
				">>");

		public static Relation getByTextualRepresentation(String s) {
			for (Relation r : values()) {
				if (r.textualRepresentation.equals(s))
					return r;
			}
			return null;
		}

		private String textualRepresentation;

		private Relation(String s) {
			this.textualRepresentation = s;
		}

		@Override
		public String toString() {
			return textualRepresentation;
		}
	};

	private String packageName;

	private Version version;

	private Relation relation;

	protected int hashCode;
 
	/**
	 * Constructor used by subclass
	 */
	protected PackageReference() {
		this.packageName = "one-of";
	}

	public PackageReference(String specifier) {
		try {
			specifier.trim();
			Matcher m = SPECIFIER_PATTERN.matcher(specifier);
			if (!m.matches())
				throw new IllegalArgumentException(PreferenceStoreHolder.getPreferenceStoreByName("Screen").getPreferenceAsString("PackageReference.IllegalArgument", "No entry found forPackageReference.IllegalArgument")
						+": "
						+ specifier);
			packageName = m.group(1);

			if (m.group(2) != null) {
				// map key to Relation
				relation = Relation.getByTextualRepresentation(m.group(2));
				version = new Version(m.group(3));
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(PreferenceStoreHolder.getPreferenceStoreByName("Screen").getPreferenceAsString("PackageReference.IllegalArgument", "No entry found forPackageReference.IllegalArgument")
					+": ", e);
		}
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer(packageName);
		if (null != relation) {
			sb.append(" (").append(relation).append(" ").append(version)
					.append(")");
		}
		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof PackageReference))
			return false;
		PackageReference r = (PackageReference) obj;

		if (!packageName.equals(r.packageName))
			return false;
		if ((null != version && null == r.version)
				|| (null == version && null != r.version))
			return false;
		return (null == version && null == r.version)
				|| version.equals(r.version);
	}

	@Override
	public int hashCode() {
		if (-1 == hashCode)
			hashCode = 92837421 ^ packageName.hashCode()
					^ (version != null ? version.hashCode() : 0);

		return hashCode;
	}

	/**
	 * Check whether a package version matches this package reference.
	 * 
	 * @param pkg
	 * @return
	 */
	public boolean matches(Package pkg) {
//		System.out.println("packgename "+packageName+" version"+version);
//		System.out.println("pkgname "+pkg.getName()+" version"+pkg.getVersion());
		if (!packageName.equalsIgnoreCase(pkg.getName()))
			return false;
//		if (packageName.equals(pkg.getName()))
//			System.out.println(packageName +" und "+pkg.getName()+" sind gleich");
		// if there is no relation, we're already done
		if (null == relation)
			return true;

		Version v = pkg.getVersion();
		switch (relation) {
		case EARLIER:
			return v.compareTo(this.version) < 0;
		case EARLIER_OR_EQUAL:
			return v.compareTo(this.version) <= 0;
		case EQUAL:
			return v.equals(this.version);
		case LATER_OR_EQUAL:
			return v.compareTo(this.version) >= 0;
		case LATER:
			return v.compareTo(this.version) > 0;
		default:
			return false; // can't happen!
		}
	}

	public String getName() {
		return packageName;
	}

	/**
	 * Check whether this reference is satisfied by one of the packages in the
	 * passed map.
	 * 
	 * @param virtualPackages
	 * @return
	 */
	public boolean isSatisfiedBy(Map<String, Package> pkgs) {
		Package definingPackage = pkgs.get(packageName);
		if (null == definingPackage)
			return false;

		// is the dependency satisfied by the package itself or by a virtual
		// package it provides?
		if (packageName.equals(definingPackage.getName()))
			return matches(definingPackage);
		else if (null == version) {
			// have a look at the "provides" section, if this reference
			// does not specify a version number
			if (definingPackage.getProvides() instanceof ANDReference) {
				for (PackageReference ref : ((ANDReference) definingPackage
						.getProvides()).getRefs())
					if (packageName.equals(ref.getName()))
						return true;
			} else if (packageName.equals(definingPackage.getProvides()
					.getName()))
				return true;
		}

		return false;
	}
}
