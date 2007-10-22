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
package org.openthinclient.common.directory;

import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;

/**
 * @author levigo
 */
public class ACLUtils {
	private final LdapContext ctx;

	/**
	 * 
	 */
	public ACLUtils(LdapContext ctx) {
		this.ctx = ctx;
	}

	public void makeACSA(String dn) throws NamingException {
		// Lookup the administrativeRole specifically since it is operational
		Attributes ap = ctx.getAttributes(dn, new String[]{"administrativeRole"});
		Attribute administrativeRole = ap.get("administrativeRole");

		// If it does not exist or has no ACSA value then add the attribute
		if (administrativeRole == null
				|| !administrativeRole.contains("accessControlSpecificArea")) {
			Attributes changes = new BasicAttributes("administrativeRole",
					"accessControlSpecificArea", true);
			ctx.modifyAttributes(dn, DirContext.ADD_ATTRIBUTE, changes);
		}
	}

	public void enableSearchForAllUsers(String dn) throws NamingException {
		String subentryName = "enableSearchForAllUsers";
		String aciSpec = "{" + " identificationTag \"enableSearchForAllUsers\","
				+ " precedence 14," + " authenticationLevel simple,"
				+ " itemOrUserFirst userFirst: " + " { "
				+ " userClasses { subtree { {} } }, " + " userPermissions " + " { "
				+ " { " + " protectedItems {entry, allUserAttributeTypesAndValues}, "
				+ " grantsAndDenials { grantRead, grantReturnDN, grantBrowse } "
				+ " } } } }";
		String subtreeSpec = "{}";

		createACISubentry(dn, subentryName, aciSpec, subtreeSpec);
	}

	public void enableAdminUsers(String dn) throws NamingException {
		String subentryName = "enableAdmins";
		String subtreeSpec = "{ }";
		String aciSpec = "{"
				+ " identificationTag \"enableAdmins\","
				+ " precedence 20,"
				+ " authenticationLevel simple,"
				+ " itemOrUserFirst userFirst: "
				+ " { "
				+ " userClasses { "//
				+ "userGroup { \"cn=administrators,ou=RealmConfiguration,"
				+ (dn.length() > 0 ? dn + "," : "")
				+ ctx.getNameInNamespace()
				+ "\" }"//
				+ " }, "
				+ " userPermissions "
				+ " { { "
				+ " protectedItems {entry, allUserAttributeTypesAndValues}, "
				+ " grantsAndDenials { grantRead, grantReturnDN, grantBrowse, grantRemove, grantModify, grantAdd, grantRename, grantCompare } "
				+ " } } } }";

		createACISubentry(dn, subentryName, aciSpec, subtreeSpec);
	}

	/**
	 * @param ctx
	 * @param subentryName
	 * @param aciSpec
	 * @param subtreeSpec
	 * @param subtreeSpec2
	 * @throws NamingException
	 */
	private void createACISubentry(String dn, String subentryName,
			String aciSpec, String subtreeSpec) throws NamingException {
		// now add the A/C subentry below dc=example,dc=com
		Attributes subentry = new BasicAttributes("cn", subentryName, true);
		Attribute objectClass = new BasicAttribute("objectClass");
		subentry.put(objectClass);
		objectClass.add("top");
		objectClass.add("subentry");
		objectClass.add("accessControlSubentry");
		subentry.put("subtreeSpecification", subtreeSpec);
		subentry.put("prescriptiveACI", aciSpec);
		final String name = "cn=" + subentryName
				+ (null != dn && dn.length() > 0 ? ("," + dn) : "");

		try {
			ctx.getAttributes(name);

			// oops, ACI already exists
			ctx.unbind(name);
		} catch (NameNotFoundException e) {
			// expected!
		}

		ctx.bind(name, null, subentry);
	}

	public void deleteACI(String dn, String subentryName) throws NamingException {
		ctx.unbind("cn=" + subentryName
				+ (null != dn && dn.length() > 0 ? ("," + dn) : ""));
	}

	public void checkACI() throws NamingException {
		Attributes att = ctx.getAttributes("cn=enableSearchForAllUsers",
				new String[]{"cn", "objectclass", "subtreeSpecification",
						"prescriptiveACI"});
		NamingEnumeration<String> ds = att.getIDs();
		while (ds.hasMore()) {
			String name = ds.next();
			System.out.println(name + " -> " + att.get(name));
		}
	}

	public void listACIs(String dn) throws NamingException {
		// Control[] ctxCtls = new Control[]{new SubentriesControl()};
		// ctx.setRequestControls(ctxCtls);

		NamingEnumeration<NameClassPair> ne = ctx.list(dn);
		while (ne.hasMoreElements()) {
			System.out.println(ne.next());
		}
	}
}
