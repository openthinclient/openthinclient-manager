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

import java.util.List;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.apache.log4j.Logger;
import org.openthinclient.common.model.Group;

/**
 * @author levigo
 */
public final class GroupMapping extends TypeMapping {
	private static final Logger logger = Logger.getLogger(GroupMapping.class);
	private final String memberAttribute;

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
			String objectClasses, String keyClass, String memberAttribute)
			throws ClassNotFoundException {
		super(className, baseDN, searchFilter, objectClasses, keyClass);
		this.memberAttribute = memberAttribute;
	}

	public void addMembers(OneToManyMapping memberAttribute) {
		// we just handle the members like any other attribute. This may change in
		// the future.
		add(memberAttribute);
	}

	/**
	 * Add a member to the mapped group
	 * 
	 * @param group the group object
	 * @param memberField the name of the member field
	 * @param memberDN member's DN
	 * @param tx current transaction
	 * @throws DirectoryException
	 * @throws NamingException
	 */
	void addMember(Object group, String memberField, String memberDN,
			Transaction tx) throws DirectoryException, NamingException {
		// FIXME: what is going on in the two lines below?
		if (!memberField.equals("member") && !memberField.equals("memberOf"))
			setDummy(group, memberField, tx);

		memberDN = Util.fixNameCase(memberDN, getConnectionDescriptor());

		// construct modification item and execute the modification
		final ModificationItem mi = new ModificationItem(DirContext.ADD_ATTRIBUTE,
				new BasicAttribute(memberField, memberDN));

		final DirContext ctx = tx.getContext(getConnectionDescriptor());
		final String groupDN = getDN(group);

		if (logger.isDebugEnabled())
			logger.debug("Adding group member to: " + groupDN + " -> " + memberDN);

		ctx.modifyAttributes(Util.makeRelativeName(groupDN,
				getConnectionDescriptor()), new ModificationItem[]{mi});
	}

	/**
	 * Remove a member from the mapped group
	 * 
	 * @param group the group object
	 * @param memberField the member field name
	 * @param memberDN the member DN
	 * @param tx current transaction
	 * @throws DirectoryException
	 * @throws NamingException
	 */
	void removeMember(Object group, String memberField, String memberDN,
			Transaction tx) throws DirectoryException, NamingException {
		// FIXME: what is going on in the two lines below?
		if (!memberField.equals("member") && !memberField.equals("memberOf"))
			setDummy(group, memberField, tx);

		memberDN = Util.fixNameCase(memberDN, getConnectionDescriptor());

		final ModificationItem mi = new ModificationItem(
				DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(memberField, memberDN));

		final DirContext ctx = tx.getContext(getConnectionDescriptor());
		final String groupDN = getDN(group);

		if (logger.isDebugEnabled())
			logger
					.debug("Removing group member from: " + groupDN + " -> " + memberDN);

		ctx.modifyAttributes(Util.makeRelativeName(groupDN,
				getConnectionDescriptor()), new ModificationItem[]{mi});
	}

	public boolean hasDummy(Object group, String memberField, Transaction tx)
			throws DirectoryException, NamingException {

		final boolean hasDummy = isInDirectory(group, memberField, OneToManyMapping
				.getDUMMY_MEMBER(), tx);

		return hasDummy;
	}

	public void setDummy(Object group, String memberField, Transaction tx)
			throws DirectoryException, NamingException {
		if (hasDummy(group, memberField, tx)) {
			// do nothing
		} else {
			// otherwise create a dummy
			final DirContext ctx = tx.getContext(getConnectionDescriptor());
			final String groupDN = getDN(group);

			if (logger.isDebugEnabled())
				logger.debug("Set dummy: " + OneToManyMapping.getDUMMY_MEMBER());

			final String dummy = Util.fixNameCase(OneToManyMapping.getDUMMY_MEMBER(),
					getConnectionDescriptor());

			final Attributes attrs = ctx.getAttributes(Util.makeRelativeName(groupDN,
					getConnectionDescriptor()), new String[]{memberField});
			// create a list of uniqueMembers
			final Attribute a = attrs.getAll().next();
			final ModificationItem[] mods = new ModificationItem[1];
			a.add(dummy);
			// replace the old uniqueMembers
			mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, a);
			ctx.modifyAttributes(Util.makeRelativeName(groupDN,
					getConnectionDescriptor()), mods);
		}
	}

	public boolean isInDirectory(Object group, String memberField, String dn,
			Transaction tx) throws NamingException, DirectoryException {
		// to query if the Object is in the Directory or not
		final DirContext ctx = tx.getContext(getConnectionDescriptor());
		final String groupDN = getDN(group);

		final Attributes attrs = ctx.getAttributes(Util.makeRelativeName(groupDN,
				getConnectionDescriptor()), new String[]{memberField});
		final Attribute a = attrs.getAll().next();
		int k = 0;
		while (k <= a.size() - 1) {
			final String as = a.get(k).toString();
			if (as.equalsIgnoreCase(dn))
				return true;
			k++;
		}
		return false;
	}

	// FIXME why do we need this?
	@Override
	protected void updateAttributes(Attribute currentAttribute,
			Attributes currentAttributes, Attribute a, List<ModificationItem> mods,
			Object o) throws NamingException, DirectoryException {
		if (a.getID().equalsIgnoreCase(memberAttribute))
			updateMembers(currentAttribute, currentAttributes, a, mods, o);
		else
			super.updateAttributes(currentAttribute, currentAttributes, a, mods, o);
	}

	private void updateMembers(Attribute currentAttribute,
			Attributes currentAttributes, Attribute a, List<ModificationItem> mods,
			Object o) throws NamingException, DirectoryException {

		final Group group = (Group) o;

		final Set members = group.getMembers();

		final Attribute attributeToEdit = new BasicAttribute(a.getID());

		for (final Object member : members) {
			final TypeMapping memberMapping = getMapping().getMapping(
					member.getClass());
			String memberDn = memberMapping.getDN(member);
			memberDn = Util.fixNameCase(memberDn, getConnectionDescriptor());
			attributeToEdit.add(memberDn);
		}
		if (attributeToEdit.size() == 0)
			attributeToEdit.add(OneToManyMapping.getDUMMY_MEMBER());

		mods
				.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, attributeToEdit));
	}
}
