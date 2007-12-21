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

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;

import org.apache.log4j.Logger;

/**
 * @author levigo
 */
public class ChildMapping extends AttributeMapping implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(Cardinality.class);

	private String filter;
	private final Class childType;
	private TypeMapping childMapping;

	public ChildMapping(String fieldName, String fieldType)
			throws ClassNotFoundException {
		super(fieldName, fieldType);
		this.childType = Class.forName(fieldType);

		if (!Object.class.isAssignableFrom(this.childType))
			throw new IllegalArgumentException("The field " + fieldName
					+ " is not a subclass of Object");
	}

	/*
	 * @see org.openthinclient.ldap.AttributeMapping#initPostLoad()
	 */
	@Override
	protected void initPostLoad() {
		super.initPostLoad();
		final TypeMapping child = type.getMapping().getMapping(childType);
		if (null == child)
			throw new IllegalStateException(this + ": no mapping for peer type "
					+ childType);

		this.childMapping = child;

		child.addReferrer(this);
	}

	/*
	 * @see org.openthinclient.common.directory.ldap.AttributeMapping#hydrate(java.lang.Object,
	 *      javax.naming.directory.Attributes)
	 */
	@Override
	public void hydrate(Object o, Attributes a, Transaction tx)
			throws DirectoryException {
		// nothing to do here
	}

	/*
	 * @see org.openthinclient.common.directory.ldap.AttributeMapping#cascadePostLoad(java.lang.Object)
	 */
	@Override
	protected void cascadePostLoad(final Object parent, Transaction tx)
			throws DirectoryException {
		if (cardinality == Cardinality.MANY
				|| cardinality == Cardinality.ONE_OR_MANY)
			setValue(parent, Proxy.newProxyInstance(parent.getClass()
					.getClassLoader(), new Class[]{Set.class}, new InvocationHandler() {
				private Object childSet;

				public Object invoke(Object proxy, Method method, Object[] args)
						throws Throwable {
					if (null == childSet) {
						final Transaction tx = new Transaction(type.getMapping());
						try {
							DiropLogger.LOG.logReadComment("LAZY LOAD: children for {0}",
									fieldName);

							childSet = loadChildren(parent, tx);

							// set real loaded object to original instance.
							setValue(parent, childSet);
						} finally {
							tx.commit();
						}
					}
					return method.invoke(childSet, args);
				};
			}));
		else
			setValue(parent, loadChildren(parent, tx));
	}

	/**
	 * Load the referenced children for the given parent.
	 * 
	 * @param parent the parent
	 * @param tx the current transaction
	 * @return
	 * @throws DirectoryException
	 */
	private Object loadChildren(Object parent, Transaction tx)
			throws DirectoryException {
		final String dn = type.getDN(parent);

		final Set set = childMapping.list(null != filter
				? new Filter(filter, dn)
				: null, dn, null, tx);

		switch (cardinality){
			case ONE :
				if (set.size() == 0) {
					logger.warn("No child for " + this
							+ " with cardinality ONE found at " + dn + ", filter: " + filter);
					return null;
				}
				// fall through!
			case ZERO_OR_ONE :
				if (set.size() == 0)
					return null;
				if (set.size() == 1)
					return set.iterator().next();
				throw new DirectoryException("More than one child for " + this
						+ " with cardinality ONE found at " + dn + ", filter: " + filter);
			case ONE_OR_MANY :
				if (set.size() == 0)
					throw new DirectoryException("No child for " + this
							+ " with cardinality ONE_OR_MANY found at " + dn + ", filter: "
							+ filter);
				// fall through
			case MANY :
			default :
				return set;
		}
	}

	/*
	 * @see org.openthinclient.common.directory.ldap.AttributeMapping#checkNull(javax.naming.directory.Attributes)
	 */
	@Override
	protected boolean checkNull(Attributes a) {
		return false;
	}

	@Override
	public void setCardinality(String cardinality) {
		super.setCardinality(cardinality);

		if (this.cardinality == Cardinality.ONE_OR_MANY
				|| this.cardinality == Cardinality.MANY)
			setFieldType(Set.class);
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	/*
	 * @see org.openthinclient.common.directory.ldap.AttributeMapping#initNewInstance(org.openthinclient.common.directory.Object)
	 */
	@Override
	protected void initNewInstance(Object instance) throws DirectoryException {
		if (cardinality == Cardinality.ONE
				|| cardinality == Cardinality.ONE_OR_MANY) {
			Object v = type.getMapping().create(childType);

			if (cardinality == Cardinality.ONE_OR_MANY) {
				final Set tmp = new HashSet();
				tmp.add(v);
				v = tmp;
			}

			setValue(instance, v);
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
		switch (cardinality){
			case ONE :
				final Object child = getValue(o);
				if (null == child)
					throw new DirectoryException(
							"No child for child mapping with cardinality ONE present");
				save(o, child, tx);
				break;
			case ZERO_OR_ONE :
				save(o, getValue(o), tx);
				break;
			case ONE_OR_MANY :
				Set set = (Set) getValue(o);
				if (Proxy.isProxyClass(set.getClass())) {
					if (logger.isDebugEnabled())
						logger.debug("Still got the dynamic proxy for " + o);
				} else {
					if (set.size() == 0)
						throw new DirectoryException(
								"No child for child mapping with cardinality ONE_OR_MANY present");
					save(o, set, tx);
				}
				break;
			case MANY :
			default :
				set = (Set) getValue(o);
				if (Proxy.isProxyClass(set.getClass())) {
					if (logger.isDebugEnabled())
						logger.debug("Still got the dynamic proxy for " + o);
				} else
					save(o, set, tx);
				break;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openthinclient.ldap.AttributeMapping#cascadeRDNChange(java.lang.Object,
	 *      javax.naming.Name, javax.naming.Name,
	 *      org.openthinclient.ldap.Transaction)
	 */
	@Override
	public void cascadeRDNChange(Object parent, String oldDN, String newDN,
			Transaction tx) throws DirectoryException, NamingException {
		switch (cardinality){
			case ONE :
			case ZERO_OR_ONE :
				final Object child = getValue(parent);
				childMapping.handleParentNameChange(child, oldDN, newDN, tx);
				break;

			case ONE_OR_MANY :
			case MANY :
				final Set children = (Set) getValue(parent);
				if (Proxy.isProxyClass(children.getClass()))
					// still got the proxy - just replace it
					cascadePostLoad(parent, tx);
				else
					for (final Object c : children)
						childMapping.handleParentNameChange(c, oldDN, newDN, tx);
				break;
		}
	}

	/**
	 * @param child
	 * @throws DirectoryException
	 */
	private void save(Object parent, Set children, Transaction tx)
			throws DirectoryException {
		final TypeMapping parentMapping = getParentMapping(parent);

		String parentDNrelative;
		String parentDNabsolute;
		try {
			parentDNrelative = type.getDirectoryFacade().makeRelativeName(
					parentMapping.getDN(parent)).toString();
			parentDNabsolute = type.getDirectoryFacade().makeAbsoluteName(
					parentDNrelative).toString();
		} catch (final NamingException e) {
			throw new DirectoryException(
					"Parent DN can't be turned into relative one", e);
		}

		final Set existing = type.getMapping().list(childType,
				null != filter ? new Filter(filter, parentDNrelative) : null,
				parentDNrelative, null);

		// FIXME: This will prevent saveNewObject() in TypeMapping.save():
		// make sure that all child DNs are set. Otherwise we might try to do
		// spurious saves
		// for (final Object child : children)
		// childMapping.ensureDNSet(child, parentDNabsolute, tx);

		// sync existing children with the ones contained in the object.
		final Set missing = new HashSet();
		if (null != children)
			missing.addAll(children);

		for (final Iterator i = missing.iterator(); i.hasNext();)
			if (existing.remove(i.next()))
				i.remove();

		// missing now has the missing ones, existing the ones to be
		// removed
		for (final Object object : existing)
			childMapping.delete(object, tx);
		for (final Object missingObject : missing)
			childMapping.save(missingObject, parentDNrelative, tx);
	}

	// private void ensureDNSet(Object child, String baseDN, Transaction tx)
	// throws DirectoryException {
	// if (null == childMapping.getDN(child))
	// try {
	// childMapping.fillEmptyDN(child, tx.getContext(childMapping
	// .getDirectoryFacade()), baseDN);
	// } catch (final Exception e) {
	// throw new DirectoryException("Can't fill DN", e);
	// }
	// }

	/**
	 * @param child
	 * @param tx
	 * @throws DirectoryException
	 */
	private void save(Object parent, Object child, Transaction tx)
			throws DirectoryException {
		final TypeMapping parentMapping = getParentMapping(parent);

		final String parentDN = parentMapping.getDN(parent);

		if (null == child) {
			// child is null - just delete the existing children
			final Set set = type.getMapping().list(childType,
					null != filter ? new Filter(filter, parentDN) : null, parentDN, null);
			for (final Object existingChild : set)
				childMapping.delete(existingChild, tx);
		} else
			// just save it (let the type mapping for the child do the chores)
			childMapping.save(child, parentDN, tx);
	}

	private TypeMapping getParentMapping(Object parent) {
		final TypeMapping parentMapping = type.getMapping().getMapping(
				parent.getClass(), type.getDirectoryFacade());
		if (null == parentMapping)
			throw new IllegalStateException("Parent " + parent.getClass() + " for "
					+ this + " is not mapped");
		return parentMapping;
	}

	/*
	 * @see org.openthinclient.ldap.AttributeMapping#toString()
	 */
	@Override
	public String toString() {
		return "[ChildMapping name=" + fieldName + ", cardinality=" + cardinality
				+ ", type=" + childType + "]";
	}
}
