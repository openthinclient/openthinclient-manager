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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.AttributeInUseException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The inverse side of a {@link GroupMapping}, i.e. the attribute pointing to
 * the groups containing the object to which this attribute belongs.
 * 
 * @author levigo
 */
public class ManyToManyMapping extends AttributeMapping {
	private static final Logger logger = LoggerFactory.getLogger(ManyToManyMapping.class);

	private String filter;
	private String memberField;
	private final Class peerType;

	private GroupMapping peerMapping;

	public ManyToManyMapping(String fieldName, String fieldType)
			throws ClassNotFoundException {
		super(fieldName, Set.class.getName());
		this.peerType = Class.forName(fieldType);

		if (!Object.class.isAssignableFrom(this.peerType))
			throw new IllegalArgumentException("The field " + fieldName
					+ " is not a subclass of Object");
	}

	/*
	 * @see org.openthinclient.common.directory.ldap.AttributeMapping#valueFromAttributes(javax.naming.directory.Attribute)
	 */
	@Override
	protected Object valueFromAttributes(Attributes attributes, final Object o,
			final Transaction tx) throws NamingException, DirectoryException {
		// make proxy for lazy loading
		return Proxy.newProxyInstance(o.getClass().getClassLoader(),
				new Class[]{Set.class}, new InvocationHandler() {
					private Set realObjectSet;

					public Object invoke(Object proxy, Method method, Object[] args)
							throws Throwable {
						if (null == realObjectSet) {
							final String dn = type.getDN(o);

							DiropLogger.LOG.logReadComment("LAZY LOAD: {0} containing {1}",
									peerType.getSimpleName(), dn);

							realObjectSet = loadObjectSet(dn);

							// set real loaded object to original instance.
							setValue(o, realObjectSet);
						}
						return method.invoke(realObjectSet, args);
					};
				});
	}

	/**
	 * @param referencedDN
	 * @param tx TODO
	 * @return
	 * @throws DirectoryException
	 */
	private Set loadObjectSet(String referencedDN) throws DirectoryException {
		final Transaction tx = new Transaction(type.getMapping());
		try {
			referencedDN = peerMapping.getDirectoryFacade().fixNameCase(referencedDN);

			return wrapValueSet(peerMapping.list(null != filter ? new Filter(filter,
					referencedDN) : null, null, null, tx));
		} catch (final NamingException e) {
			throw new DirectoryException("Can't fix DN case for " + peerMapping);
		} finally {
			tx.commit();
		}
	}

	/*
	 * @see org.openthinclient.common.directory.ldap.AttributeMapping#dehydrate(org.openthinclient.common.directory.Object,
	 *      javax.naming.directory.BasicAttributes)
	 */
	@Override
	public Object dehydrate(Object o, BasicAttributes a)
			throws DirectoryException {
		// nothing to do
		return null;
	}

	/*
	 * @see org.openthinclient.common.directory.ldap.AttributeMapping#cascadePostSave(org.openthinclient.common.directory.Object)
	 */
	@Override
	protected void cascadePostSave(Object o, Transaction tx, DirContext ctx)
			throws DirectoryException {
		// In preparation for SUITE-69: Check whether client code has modified the
		// value set. Warn if a modification is detected.
		// final Set newAssociations = (Set) getValue(o);
		//
		// if (null != newAssociations
		// && !Proxy.isProxyClass(newAssociations.getClass())
		// && newAssociations.size() > 0) {
		// // Set is defined and not a proxy. Detect whether it is modifiable
		// // by attempting to add a member to it.
		// final Object something = newAssociations.iterator().next();
		//
		// try {
		// // should be null-operation due to set-semantics
		// newAssociations.add(something);
		//
		// // warn about transient change
		// logger.warn("Changes to the field " + fieldName + " of type "
		// + type.getMappedType() + " will not be persisted!");
		// } catch (final UnsupportedOperationException e) {
		// // expected/hoped for
		// }
		// }

		// The following code has been commented out due to SUITE-69. It has,
		// however, been left in place should there be the need to resurrect this
		// functionality.

		try {
			// compare existing associations with the associations the saved object
			// has
			final Set newAssociations = (Set) getValue(o);

			// if the associations aren't set at all, we don't care
			if (null == newAssociations)
				return;

			if (null != newAssociations) {
				// if the content is a proxy class, we don't have to save anything,
				// since the association is unmodified.
				if (Proxy.isProxyClass(newAssociations.getClass()))
					return;

				// save the association's members
				for (final Object peer : newAssociations)
					peerMapping.save(peer, null, tx);
			}

			// load existing associations
			final String dn = peerMapping.getDirectoryFacade().fixNameCase(
					type.getDN(o));
			final Transaction nested = new Transaction(tx);
			Set existing;
			try {
				existing = peerMapping.list(null != filter
						? new Filter(filter, dn)
						: null, null, null, nested);
			} catch (final DirectoryException e) {
				nested.rollback();
				throw e;
			} catch (final RuntimeException e) {
				nested.rollback();
				throw e;
			} finally {
				nested.commit();
			}

			final List missing = new LinkedList();
			if (null != newAssociations)
				missing.addAll(newAssociations);

			for (final Iterator i = missing.iterator(); i.hasNext();)
				if (existing.remove(i.next()))
					i.remove();

			// missing now has the missing ones, existing the ones to be removed
			for (final Iterator i = existing.iterator(); i.hasNext();) {
				final Object group = i.next();
				if (logger.isDebugEnabled())
					logger.debug("Remove: " + group);
				peerMapping.removeMember(group, memberField, dn, tx);
			}
			for (final Iterator i = missing.iterator(); i.hasNext();)
				try {
					final Object group = i.next();
					if (logger.isDebugEnabled())
						logger.debug("Save: " + group);
					if (!peerMapping.isInDirectory(group, memberField, dn, tx))
						peerMapping.addMember(group, memberField, dn, tx);
					else
						logger.error("Object already exists !!!");
				} catch (final AttributeInUseException a) {
					logger.error("Object already exists !!!", a);
				}

		} catch (final DirectoryException e) {
			throw e;
		} catch (final Exception e) {
			throw new DirectoryException("Can't update many-to-many association", e);
		}
	}

	/*
	 * @see org.openthinclient.common.directory.ldap.AttributeMapping#checkNull(javax.naming.directory.Attributes)
	 */
	@Override
	protected boolean checkNull(Attributes a) {
		return false;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	/*
	 * @see org.openthinclient.common.directory.ldap.AttributeMapping#initNewInstance(org.openthinclient.common.directory.Object)
	 */
	@Override
	protected void initNewInstance(Object instance) throws DirectoryException {
		setValue(instance, wrapValueSet(new HashSet()));
	}

	private Set wrapValueSet(Set s) {
		// commented-out until SUITE-69 is implemented.
		// return Collections.unmodifiableSet(s);

		return s;
	}

	public void setMemberField(String memberField) {
		this.memberField = memberField;
	}

	/*
	 * @see org.openthinclient.common.directory.ldap.AttributeMapping#initPostLoad()
	 */
	@Override
	protected void initPostLoad() {
		super.initPostLoad();
		final TypeMapping peer = type.getMapping().getMapping(peerType);
		if (null == peer)
			throw new IllegalStateException(this + ": no mapping for peer type "
					+ peerType);

		if (!(peer instanceof GroupMapping))
			throw new IllegalStateException("many-to-many-mapping " + this
					+ " needs a group as a partner, not a " + peer);

		this.peerMapping = (GroupMapping) peer;

		peer.addReferrer(this);
	}
}
