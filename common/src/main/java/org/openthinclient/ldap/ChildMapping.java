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

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;

import org.apache.log4j.Logger;

/**
 * @author levigo
 */
public class ChildMapping extends AttributeMapping implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(Cardinality.class);

	private enum Cardinality {
		ONE, ZERO_OR_ONE, ONE_OR_MANY, MANY
	}

	private String filter;
	private final Class childType;
	private Cardinality cardinality = Cardinality.ONE;
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
		TypeMapping child = type.getMapping().getMapping(childType);
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
	protected void cascadePostLoad(final Object o, final Transaction tx)
			throws DirectoryException {
		if (cardinality == Cardinality.MANY
				|| cardinality == Cardinality.ONE_OR_MANY) {
			setValue(o, Proxy.newProxyInstance(o.getClass().getClassLoader(),
					new Class[]{Set.class}, new InvocationHandler() {
						public Object invoke(Object proxy, Method method, Object[] args)
								throws Throwable {
							if (logger.isDebugEnabled())
								logger.debug("Loading lazily: children for "
										+ ChildMapping.this);

							// set real loaded object to original instance.
							Object childSet = loadChildren(o, tx);
							setValue(o, childSet);
							return method.invoke(childSet, args);
						};
					}));
		} else
			setValue(o, loadChildren(o, tx));
	}

	/**
	 * @param o
	 * @param tx TODO
	 * @return
	 * @throws DirectoryException
	 */
	private Object loadChildren(Object o, Transaction tx)
			throws DirectoryException {
		String dn = type.getDN(o);

		Set set = childMapping.list(null != filter ? new Filter(filter, dn) : null,
				dn, null, tx);

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

	public void setCardinality(String cardinality) {
		this.cardinality = Cardinality.valueOf(cardinality);
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
				Set tmp = new HashSet();
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
				Object child = getValue(o);
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
				if (set.size() == 0)
					throw new DirectoryException(
							"No child for child mapping with cardinality ONE_OR_MANY present");
				save(o, set, tx);
				break;
			case MANY :
			default :
				save(o, (Set) getValue(o), tx);
				break;
		}
	}

	/**
	 * @param child
	 * @throws DirectoryException
	 */
	private void save(Object parent, Set children, Transaction tx)
			throws DirectoryException {
		TypeMapping parentMapping = type.getMapping().getMapping(parent.getClass());
		if (null == parentMapping)
			throw new IllegalStateException("Parent " + parent.getClass() + " for "
					+ this + " is not mapped");

		String parentDN = parentMapping.getDN(parent);

		Set existing = type.getMapping().list(childType, null,
				null != filter ? new Filter(filter, parentDN) : null, parentDN, null);

		// sync existing children with the ones contained in the object.
		Set missing = new HashSet();
		if (null != children)
			missing.addAll(children);

		for (Iterator i = missing.iterator(); i.hasNext();)
			if (existing.remove(i.next()))
				i.remove();

		// missing now has the missing ones, existing the ones to be
		// removed
		for (Object object : existing)
			childMapping.delete(object, tx);
		for (Object missingObject : missing)
			childMapping.save(missingObject, parentDN, tx);

	}

	/**
	 * @param child
	 * @param tx
	 * @throws DirectoryException
	 */
	private void save(Object parent, Object child, Transaction tx)
			throws DirectoryException {
		TypeMapping parentMapping = type.getMapping().getMapping(parent.getClass());
		if (null == parentMapping)
			throw new IllegalStateException("Parent " + parent.getClass() + " for "
					+ this + " is not mapped");

		String parentDN = parentMapping.getDN(parent);

		if (null == child) {
			// child is null - just delete the existing children
			Set set = type.getMapping().list(childType, null,
					null != filter ? new Filter(filter, parentDN) : null, parentDN, null);
			for (Object existingChild : set)
				childMapping.delete(existingChild, tx);
		} else {
			// just save it (let the type mapping for the child do the chores)
			childMapping.save(child, parentDN, tx);
		}
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
