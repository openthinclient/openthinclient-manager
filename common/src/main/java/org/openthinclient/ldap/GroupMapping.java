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
package org.openthinclient.ldap;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.apache.log4j.Logger;

/**
 * @author levigo
 */
public final class GroupMapping extends TypeMapping {
	private static final Logger logger = Logger.getLogger(GroupMapping.class);

	/**
	 * @param className
	 * @param baseDN
	 * @param searchFilter
	 * @param objectClasses
	 * @param canUpdate
	 * @param keyClass
	 * @throws ClassNotFoundException
	 */
	public GroupMapping(String className, String baseDN, String searchFilter,
			String objectClasses, String canUpdate, String keyClass)
			throws ClassNotFoundException {
		super(className, baseDN, searchFilter, objectClasses, canUpdate, keyClass);
	}

	public void addMembers(OneToManyMapping memberAttribute) {
		// we just handle the members like any other attribute. This may change in
		// the future.
		add(memberAttribute);
	}

	/**
	 * @param group
	 * @param memberField
	 * @param dn
	 * @param tx TODO
	 * @throws DirectoryException
	 * @throws NamingException
	 */
	void addMember(Object group, String memberField, String dn, Transaction tx)
			throws DirectoryException, NamingException {

		if (logger.isDebugEnabled())
			logger.debug("Add member and maybe a dummy");

		if (!memberField.equals("member") && !memberField.equals("memberOf"))
			setDummy(group, memberField, tx);

		dn = Util.idToUpperCase(dn);

		// construct modification item and execute the modification
		ModificationItem mi = new ModificationItem(DirContext.ADD_ATTRIBUTE,
				new BasicAttribute(memberField, dn));
		DirContext ctx = tx.getContext(group.getClass());
		final String groupDN = getDN(group);

		if (logger.isDebugEnabled())
			logger.debug("Adding group member to: " + groupDN + " -> " + dn);
		ctx.modifyAttributes(Util.makeRelativeName(groupDN, ctx),
				new ModificationItem[]{mi});
	}

	/**
	 * @param group
	 * @param memberField
	 * @param dn
	 * @param tx TODO
	 * @throws DirectoryException
	 * @throws NamingException
	 */
	void removeMember(Object group, String memberField, String dn, Transaction tx)
			throws DirectoryException, NamingException {
		// construct modification item and execute the modification

		if (logger.isDebugEnabled())
			logger.debug("Add maybe a dummy and delete the member");

		if (!memberField.equals("member") && !memberField.equals("memberOf"))
			setDummy(group, memberField, tx);

		dn = Util.idToUpperCase(dn);

		ModificationItem mi = new ModificationItem(DirContext.REMOVE_ATTRIBUTE,
				new BasicAttribute(memberField, dn));
		DirContext ctx = tx.getContext(group.getClass());
		final String groupDN = getDN(group);

		if (logger.isDebugEnabled())
			logger.debug("Removing group member from: " + groupDN + " -> " + dn);

		ctx.modifyAttributes(Util.makeRelativeName(groupDN, ctx),
				new ModificationItem[]{mi});
	}

	public boolean hasDummy(Object group, String memberField, Transaction tx)
			throws DirectoryException, NamingException {

		boolean hasDummy = isInDirectory(group, memberField, OneToManyMapping
				.getDUMMY_MEMBER(), tx);

		return hasDummy;
	}

	public void setDummy(Object group, String memberField, Transaction tx)
			throws DirectoryException, NamingException {
		if (hasDummy(group, memberField, tx)) {
			// do nothing
		} else {
			// otherwise create a dummy
			DirContext ctx = tx.getContext(group.getClass());
			final String groupDN = getDN(group);

			if (logger.isDebugEnabled())
				logger.debug("Set dummy: " + OneToManyMapping.getDUMMY_MEMBER());

			String dummy = Util.idToUpperCase(OneToManyMapping.getDUMMY_MEMBER());

			Attributes attrs = ctx.getAttributes(Util.makeRelativeName(groupDN, ctx),
					new String[]{memberField});
			// create a list of uniqueMembers
			Attribute a = attrs.getAll().next();
			ModificationItem[] mods = new ModificationItem[1];
			a.add(dummy);
			// replace the old uniqueMembers
			mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, a);
			ctx.modifyAttributes(Util.makeRelativeName(groupDN, ctx), mods);
		}
	}

	public boolean isInDirectory(Object group, String memberField, String dn,
			Transaction tx) throws NamingException, DirectoryException {
		// to query if the Object is in the Directory or not
		DirContext ctx = tx.getContext(group.getClass());
		final String groupDN = getDN(group);

		Attributes attrs = ctx.getAttributes(Util.makeRelativeName(groupDN, ctx),
				new String[]{memberField});
		Attribute a = attrs.getAll().next();
		int k = 0;
		while (k <= a.size() - 1) {
			String as = a.get(k).toString();
			if (as.equalsIgnoreCase(dn)) {
				return (true);
			}
			k++;
		}
		return (false);
	}

}
