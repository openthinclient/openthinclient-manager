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

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.apache.log4j.Logger;

/**
 * This class maps a group type (group, groupOfNames, groupOfUniqueNames, etc.)
 * where the group members are represented by a multi-valued attribute.
 * 
 * @author levigo
 */
public final class GroupMapping extends TypeMapping {
	private static final Logger logger = Logger.getLogger(GroupMapping.class);
	/**
	 * The name of the attribute holding the member references
	 */
	private final String memberAttribute;
	private AttributeMapping memberMapping;

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

	public void addMembers(AttributeMapping memberAttribute) {
		// we just handle the members like any other attribute. This may change in
		// the future.
		add(memberAttribute);
	}

	/**
	 * Add a member to the mapped group. This method is called by the mapper of a
	 * member object if it detects a missing association from this group to
	 * itself.
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
		memberDN = getDirectoryFacade().fixNameCase(memberDN);

		final DirContext ctx = tx.getContext(getDirectoryFacade());
		final String groupDN = getDN(group);

		if (logger.isDebugEnabled())
			logger.debug("   ADD MEMBER TO " + groupDN + ": " + memberDN);

		final Name groupName = getDirectoryFacade().makeRelativeName(groupDN);

		// construct modification item and execute the modification
		final ModificationItem mi = new ModificationItem(DirContext.ADD_ATTRIBUTE,
				new BasicAttribute(memberField, memberDN));

		ModificationItem[] mods = null;

		// if the member attribute requires a dummy and the dummy is present, remove
		// it.
		if (memberMapping.cardinality == Cardinality.ONE_OR_MANY
				&& !memberField.equals("member") && !memberField.equals("memberOf")) {
			final Attribute membersAttribute = ctx.getAttributes(groupName,
					new String[]{memberField}).get(memberAttribute);

			if (null != membersAttribute) {
				final String dummy = getDirectoryFacade().getDummyMember();
				if (membersAttribute.contains(dummy))
					mods = new ModificationItem[]{
							mi,
							new ModificationItem(DirContext.REMOVE_ATTRIBUTE,
									new BasicAttribute(memberField, dummy))};
			}
		}

		if (null == mods)
			mods = new ModificationItem[]{mi};

		ctx.modifyAttributes(getDirectoryFacade().makeRelativeName(groupDN), mods);
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
		memberDN = getDirectoryFacade().fixNameCase(memberDN);

		final DirContext ctx = tx.getContext(getDirectoryFacade());
		final String groupDN = getDN(group);

		if (logger.isDebugEnabled())
			logger.debug("   REMOVE MEMBER FROM " + groupDN + ": " + memberDN);

		final Name groupName = getDirectoryFacade().makeRelativeName(groupDN);

		// construct modification item and execute the modification
		final ModificationItem mi = new ModificationItem(
				DirContext.REMOVE_ATTRIBUTE, new BasicAttribute(memberField, memberDN));

		ModificationItem[] mods = null;

		// if the member attribute requires a dummy and the last member is removed,
		// re-add dummy.
		if (memberMapping.cardinality == Cardinality.ONE_OR_MANY
				&& !memberField.equals("member") && !memberField.equals("memberOf")) {
			final Attribute membersAttribute = ctx.getAttributes(groupName,
					new String[]{memberField}).get(memberAttribute);

			// if we are about to remove the last member, or there aren't any members
			// to begin with, add dummy.
			if (null == membersAttribute //
					|| null != membersAttribute
					&& membersAttribute.contains(memberDN)
					&& membersAttribute.size() <= 1)
				mods = new ModificationItem[]{
						new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute(
								memberField, getDirectoryFacade().getDummyMember())), mi};
		}

		if (null == mods)
			mods = new ModificationItem[]{mi};

		ctx.modifyAttributes(getDirectoryFacade().makeRelativeName(groupDN), mods);
	}

	public boolean isInDirectory(Object group, String memberField, String dn,
			Transaction tx) throws NamingException, DirectoryException {
		// to query if the Object is in the Directory or not
		final DirContext ctx = tx.getContext(getDirectoryFacade());
		final String groupDN = getDN(group);

		final Attributes attrs = ctx.getAttributes(getDirectoryFacade()
				.makeRelativeName(groupDN), new String[]{memberField});
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

	@Override
	protected void initPostLoad() {
		super.initPostLoad();

		// make sure that the member attribute points to a OneToManyMapping
		for (final AttributeMapping am : attributes)
			if (am.fieldName.equals(memberAttribute))
				if (am instanceof OneToManyMapping) {
					this.memberMapping = am;
					break;
				} else
					throw new IllegalStateException("MemberAttribute " + memberAttribute
							+ " of GroupMapping " + getMappedType()
							+ " is not mapped using one-to-many");

		if (null == memberMapping)
			throw new IllegalStateException("MemberAttribute " + memberAttribute
					+ " of GroupMapping missing corresponding one-to-many mapping");
	}

	//
	// @Override
	// protected void updateAttributes(Attributes currentAttributes,
	// Attribute currentValues, Attribute newValues,
	// List<ModificationItem> mods, Object o) throws NamingException,
	// DirectoryException {
	// if (newValues.getID().equalsIgnoreCase(memberAttribute))
	// updateMembers(currentValues, currentAttributes, newValues, mods, o);
	// else
	// super.updateAttributes(currentAttributes, currentValues, newValues, mods,
	// o);
	// }
	//
	// /**
	// * Update the members-attribute.
	// *
	// * @param currentValues
	// * @param currentAttributes
	// * @param newValues
	// * @param mods
	// * @param o
	// * @throws NamingException
	// * @throws DirectoryException
	// */
	// private void updateMembers(Attribute currentValues,
	// Attributes currentAttributes, Attribute newValues,
	// List<ModificationItem> mods, Object o) throws NamingException,
	// DirectoryException {
	//
	// final Group group = (Group) o;
	//
	// final Set members = group.getMembers();
	// if (!Proxy.isProxyClass(members.getClass())) {
	// final Attribute attributeToEdit = new BasicAttribute(newValues.getID());
	//
	// for (final Object member : members) {
	// final TypeMapping memberMapping = getMapping().getMapping(
	// member.getClass());
	// String memberDn = memberMapping.getDN(member);
	// memberDn = getDirectoryFacade().fixNameCase(memberDn);
	// attributeToEdit.add(memberDn);
	// }
	// if (attributeToEdit.size() == 0)
	// attributeToEdit.add(OneToManyMapping.getDUMMY_MEMBER());
	//
	// mods.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
	// attributeToEdit));
	// }
	// }
}
