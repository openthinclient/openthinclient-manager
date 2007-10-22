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
package org.openthinclient.ldap;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;

import org.apache.log4j.Logger;

/**
 * @author levigo
 */
public class OneToManyMapping extends AttributeMapping {
	/**
	 * 
	 */
	private static String DUMMY_MEMBER = "";

	private static final Logger logger = Logger.getLogger(OneToManyMapping.class);

	private final Class memberType;
	private TypeMapping memberMapping;

	private boolean createDummyForEmptyMemberList = true;

	public OneToManyMapping(String fieldName, String memberType)
			throws ClassNotFoundException {
		super(fieldName, Set.class.getName());
		if (memberType.equals("*")) {
			this.memberType = Object.class;
		} else {
			this.memberType = Class.forName(memberType);
		}
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
					public Object invoke(Object proxy, Method method, Object[] args)
							throws Throwable {
						if (logger.isDebugEnabled())
							logger.debug("Loading lazily: collection for "
									+ OneToManyMapping.this);
						// set real loaded object to original instance.

						Set realMemberSet = loadMemberSet(attributes, tx);
						setValue(o, realMemberSet);
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
	private Set loadMemberSet(Attributes attributes, Transaction tx)
			throws DirectoryException {

		Attribute membersAttribute = attributes.get(fieldName);
		Attribute objectClasses = attributes.get("objectClass");

		try {
			Set results = new HashSet();
			if (null != membersAttribute) {
				NamingEnumeration<?> e = membersAttribute.getAll();
				try {

					while (e.hasMore()) {
						String memberDN = e.next().toString();

						// ignore dummy
						if (memberDN.equals(getDUMMY_MEMBER()))
							continue;

						TypeMapping mm = this.memberMapping;

						if (!memberDN.equalsIgnoreCase(OneToManyMapping.getDUMMY_MEMBER())
								&& !memberDN.equalsIgnoreCase("DC=dummy")) {
							if (null == mm) {
								mm = type.getMapping().getMappingByDN(memberDN,
										objectClasses, tx);
							}
							
							if (null == mm) {
								mm = type.getMapping().getMappingByAttributes(memberDN,
										objectClasses, tx);
							}

							if (null == mm) {
								logger.warn(this + ": can't determine mapping type for dn="
										+ memberDN);
								continue;
							}

							try {
								results.add(mm.load(memberDN, tx));
							} catch (DirectoryException f) {
								if (f.getCause() != null
										&& f.getCause() instanceof NameNotFoundException) {
									logger.warn("Ignoring nonexistant referenced object: "
											+ memberDN);
								} else
									throw f;
							}
						}
					}
				} finally {

					e.close();
				}
			}
			return results;
		} catch (NamingException e) {
			throw new DirectoryException(
					"Exception during lazy loading of group members", e);
		}
	}

	/*
	 * @see org.openthinclient.common.directory.ldap.AttributeMapping#dehydrate(org.openthinclient.common.directory.Object,
	 *      javax.naming.directory.BasicAttributes)
	 */
	@Override
	public Object dehydrate(Object o, BasicAttributes a)
			throws DirectoryException {
		Set memberSet = (Set) getValue(o);
		if (null == memberSet) {
			memberSet = new HashSet(); // empty set
		}

		// if we still see the unchanged proxy, we're done!
		if (!Proxy.isProxyClass(memberSet.getClass())) {
			// compile list of memberDNs
			// Attribute memberDNs = null;
			Attribute memberDNs = new BasicAttribute(fieldName);

			// String name = "";
			// if(a.size() >0) {
			// name = a.get("cn").toString();
			// }

			if (memberSet.isEmpty()) {
				logger.debug("empty member set");

				// dummy entry
				if (createDummyForEmptyMemberList) {
					memberDNs.add(getDUMMY_MEMBER());

					logger.debug("adding dummy entry");
				}

			} else
				for (Object member : memberSet) {
					TypeMapping mappingForMember = memberMapping;

					if (null == mappingForMember)
						mappingForMember = type.getMapping().getMapping(member.getClass());

					if (null == mappingForMember) {
						logger.warn("One-to-many associaction contains a member of type "
								+ member.getClass() + " for which I don't have a mapping.");
						continue;
					}
					// String dn =
					// TypeMapping.idToUpperCase(mappingForMember.getDN(member));
					// //Standort toUpperCase ???
					// memberDNs.add(dn);
					memberDNs.add(getDUMMY_MEMBER());
					
					String memberDN = TypeMapping.idToUpperCase(mappingForMember.getDN(member));
					
					memberDNs.add(memberDN);
				}

			// we only add the attribute if it has members
			if (memberDNs.size() > 0) {
				a.put(memberDNs);
			}

		} else
			a.put(new BasicAttribute(fieldName,
					TypeMapping.ATTRIBUTE_UNCHANGED_MARKER));

		return memberSet;
	}

	/*
	 * @see org.openthinclient.common.directory.ldap.AttributeMapping#cascadePostSave(org.openthinclient.common.directory.Object)
	 */
	@Override
	protected void cascadePostSave(Object o, Transaction tx, DirContext ctx)
			throws DirectoryException {
		Set memberSet = (Set) getValue(o);
		if (null == memberSet)
			memberSet = new HashSet(); // empty set

		// if we still see the unchanged proxy, we're done!
		if (!Proxy.isProxyClass(memberSet.getClass()))
			for (Object member : memberSet) {
				TypeMapping mm = this.memberMapping;

				// do we have a mapping for a particular kind of member, or
				// is this a general ("*") Mapping?
				if (null == mm)
					mm = type.getMapping().getMapping(member.getClass());

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
		setValue(instance, new HashSet());
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
			TypeMapping member = type.getMapping().getMapping(memberType);
			if (null == member)
				throw new IllegalStateException(this + ": no mapping for member type "
						+ memberType);

			this.memberMapping = member;

			member.addReferrer(this);
		}
	}

	public static String getDUMMY_MEMBER() {
		if(DUMMY_MEMBER.equals(""))
			DUMMY_MEMBER = "DC=dummy";
		return DUMMY_MEMBER;
	}

	public static void setDUMMY_MEMBER(String dummy_member) {
		DUMMY_MEMBER = TypeMapping.idToUpperCase(dummy_member);
	}
}
