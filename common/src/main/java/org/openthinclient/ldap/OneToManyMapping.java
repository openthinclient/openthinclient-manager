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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.LdapContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class maps the outgoing side of one-to-many (which are actually always
 * many-to-many) style mappings. It usually corresponds to the attribute holding
 * the member reference of a group type mapped by {@link GroupMapping}.
 * 
 * @author levigo
 */
public class OneToManyMapping extends ReferenceAttributeMapping {

	private static final Logger logger = LoggerFactory.getLogger(OneToManyMapping.class);

	private final Class memberType;
	private TypeMapping memberMapping;

	private static final Set EMPTY = Collections.unmodifiableSet(new HashSet());

	public OneToManyMapping(String fieldName, String memberType)
			throws ClassNotFoundException {
		super(fieldName, Set.class.getName());
		if (memberType.equals("*"))
			this.memberType = Object.class;
		else
			this.memberType = Class.forName(memberType);

		this.cardinality = Cardinality.ONE_OR_MANY;
	}

	/*
	 * @see org.openthinclient.common.directory.ldap.AttributeMapping#valueFromAttributes(javax.naming.directory.Attribute)
	 */
	@Override
	protected Object valueFromAttributes(final Attributes attributes,
			final Object o, final Transaction tx) throws NamingException,
			DirectoryException {
		// make proxy for lazy loading
		return Proxy.newProxyInstance(o.getClass().getClassLoader(),
				new Class[]{getFieldType()}, new InvocationHandler() {
					private Set realMemberSet;

					public Object invoke(Object proxy, Method method, Object[] args)
							throws Throwable {
						if (null == realMemberSet) {
							if (DiropLogger.LOG.isReadEnabled())
								DiropLogger.LOG.logReadComment(
										"LAZY LOAD: collection for {0}: {1}", fieldName, type
												.getDN(o));

							realMemberSet = loadMemberSet(attributes);
							setValue(o, realMemberSet);
						}
						return method.invoke(realMemberSet, args);
					};
				});
	}

	/**
	 * @param attributes
	 * @param tx TODO
	 * @return
	 * @throws DirectoryException
	 */
	private Set loadMemberSet(Attributes attributes) throws DirectoryException {

		final Attribute membersAttribute = attributes.get(fieldName);

		final Transaction tx = new Transaction(type.getMapping());
		try {
			final Set results = new HashSet();
			if (null != membersAttribute) {
				final NamingEnumeration<?> e = membersAttribute.getAll();
				try {

					while (e.hasMore()) {
						String memberDN = e.next().toString();

						// ignore dummy
						if (type.getDirectoryFacade().isDummyMember(memberDN))
							continue;

						TypeMapping mm = this.memberMapping;
						if (null == mm)
							if (!memberDN.matches("[=,]")
									&& "memberUid".equals(membersAttribute.getID())) {
								// FIXME: evil OpenLDAP hack coz memberUid is never a valid dn
								final LdapContext dc = type.getDirectoryFacade()
										.createDirContext();

								final SearchControls sc = new SearchControls();
								sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

								memberDN = dc.search("", "(uid=" + memberDN + ")", sc).next()
										.getNameInNamespace();
							}

						try {
							mm = type.getMapping().getMapping(memberDN, tx);
						} catch (final NameNotFoundException f) {
							logger
									.warn("Ignoring nonexistant referenced object: " + memberDN);
							continue;
						}

						if (null == mm) {
							logger.warn(this + ": can't determine mapping type for dn="
									+ memberDN);
							continue;
						}

						try {
							results.add(mm.load(memberDN, tx));
						} catch (final DirectoryException f) {
							if (f.getCause() != null
									&& f.getCause() instanceof NameNotFoundException)
								logger.warn("Ignoring nonexistant referenced object: "
										+ memberDN);
							else
								throw f;
						}
					}
				} finally {
					e.close();
				}
			}
			return results;
		} catch (final NamingException e) {
			throw new DirectoryException(
					"Exception during lazy loading of group members", e);
		} finally {
			tx.commit();
		}
	}

	@Override
	protected void cascadePreSave(Object o, Transaction tx)
			throws DirectoryException {
		super.cascadePreSave(o, tx);

		Set memberSet = (Set) getValue(o);
		if (null == memberSet)
			memberSet = EMPTY; // empty set

		// if we still see the unchanged proxy, we're done!
		if (!Proxy.isProxyClass(memberSet.getClass()))
			for (final Object member : memberSet) {
				// make sure that the member has already been saved
				final TypeMapping mappingForMember = getMappingForMember(member);
				final String dn = mappingForMember.getDN(member);
				if (null == dn)
					mappingForMember.save(member, null, tx);
			}
	}

	/*
	 * @see org.openthinclient.common.directory.ldap.AttributeMapping#dehydrate(org.openthinclient.common.directory.Object,
	 *      javax.naming.directory.BasicAttributes)
	 */
	@Override
	public Object dehydrate(Object o, BasicAttributes a)
			throws DirectoryException, NamingException {
		Set memberSet = (Set) getValue(o);

		if (null == memberSet)
			memberSet = EMPTY; // empty set

		// if we still see the unchanged proxy, we're done!
		if (!Proxy.isProxyClass(memberSet.getClass())) {
			// compile list of memberDNs
			// Attribute memberDNs = null;
			final Attribute memberDNs = new BasicAttribute(fieldName);

			if (memberSet.isEmpty()) {
				// do we need a dummy entry?
				if (cardinality == Cardinality.ONE_OR_MANY)
					memberDNs.add(type.getDirectoryFacade().getDummyMember());
			} else
				for (final Object member : memberSet)
					try {
						final TypeMapping mappingForMember = getMappingForMember(member);

						final String memberDN = type.getDirectoryFacade().fixNameCase(
								mappingForMember.getDN(member));

						memberDNs.add(memberDN);
					} catch (final NamingException e) {
						throw new DirectoryException("Can't dehydrate", e);
					}

			// we only add the attribute if it has members
			if (memberDNs.size() > 0)
				a.put(memberDNs);

		} else
			a.put(new BasicAttribute(fieldName,
					TypeMapping.ATTRIBUTE_UNCHANGED_MARKER));

		return memberSet;
	}

	private TypeMapping getMappingForMember(Object member)
			throws DirectoryException {
		TypeMapping mappingForMember = memberMapping;

		// for a generic mapping we have no way of accessing
		// the DN of the member object without fetching at least the default
		// mapping for it.
		if (null == mappingForMember)
			mappingForMember = type.getMapping().getMapping(member.getClass());

		if (null == mappingForMember)
			throw new DirectoryException(
					"One-to-many associaction contains a member of type "
							+ member.getClass() + " for which I don't have a mapping.");

		final String dn = mappingForMember.getDN(member);

		// if the member doesn't have a dn, we must resort to the default mapping
		if (null == dn)
			return mappingForMember;

		// if the mapping we found doesn't match the dn, we need
		// to refine it: the member may point to a non-default directory
		// for the mapped type.
		Name parsedDN;
		try {
			parsedDN = mappingForMember.getDirectoryFacade().getNameParser()
					.parse(dn);
			if (!mappingForMember.getDirectoryFacade().contains(parsedDN)) {
				mappingForMember = type.getMapping().getMapping(member.getClass(), dn);

				// re-parse, because the provider might be different.
				// We may want to get rid of other provider types (besides SUN),
				// because of this unnecessary complexity.
				parsedDN = mappingForMember.getDirectoryFacade().getNameParser().parse(
						dn);
			}

			return mappingForMember;
		} catch (final NamingException e) {
			throw new DirectoryException("Unable to determine mapping for member", e);
		}
	}

	/*
	 * @see org.openthinclient.common.directory.ldap.AttributeMapping#cascadePostSave(org.openthinclient.common.directory.Object)
	 */
	@Override
	protected void cascadePostSave(Object o, Transaction tx, DirContext ctx)
			throws DirectoryException {
		Set memberSet = (Set) getValue(o);
		if (null == memberSet)
			memberSet = EMPTY; // empty set

		// if we still see the unchanged proxy, we're done!
		if (!Proxy.isProxyClass(memberSet.getClass()))
			for (final Object member : memberSet) {
				final TypeMapping mm = getMappingForMember(member);

				if (null == mm)
					throw new DirectoryException(this
							+ ": set contains member of unmapped type: " + member.getClass());

				mm.save(member, null, tx);
			}
	}

	/*
	 * @see org.openthinclient.common.directory.ldap.AttributeMapping#checkNull(javax.naming.directory.Attributes)
	 */
	@Override
	protected boolean checkNull(Attributes a) {
		return false;
	}

	/*
	 * @see org.openthinclient.common.directory.ldap.AttributeMapping#initNewInstance(org.openthinclient.common.directory.Object)
	 */
	@Override
	protected void initNewInstance(Object instance) throws DirectoryException {
		// set new empty collection
		setValue(instance, EMPTY);
	}

	/*
	 * @see org.openthinclient.common.directory.ldap.AttributeMapping#initPostLoad()
	 */
	@Override
	protected void initPostLoad() {
		super.initPostLoad();

		// we don't set up the member mapping, if this group accepts any kind of
		// member
		if (!memberType.equals(Object.class)) {
			final TypeMapping member = type.getMapping().getMapping(memberType);
			if (null == member)
				throw new IllegalStateException(this + ": no mapping for member type "
						+ memberType);

			this.memberMapping = member;

			member.addReferrer(this);
		}
	}

	@Override
	public void setCardinality(String c) {
		super.setCardinality(c);

		if (cardinality != Cardinality.MANY
				&& cardinality != Cardinality.ONE_OR_MANY)
			throw new IllegalArgumentException("Illegal cardinality " + cardinality
					+ " for " + type.getMappedType() + "." + fieldName);
	}

	@Override
	Cardinality getCardinality() {
		return cardinality;
	}
}
