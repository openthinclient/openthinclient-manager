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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;

import org.apache.log4j.Logger;

/**
 * @author levigo
 */
public class AttributeMapping implements Cloneable {
	private static final Logger logger = Logger.getLogger(AttributeMapping.class);

	protected final String fieldName;

	private Class fieldType;

	private Method getMethod;

	private String getMethodName;

	private Method setMethod;

	private String setMethodName;

	protected TypeMapping type;

	protected Cardinality cardinality = Cardinality.MANY;

	public AttributeMapping(String fieldName, String fieldType)
			throws ClassNotFoundException {
		this.fieldName = fieldName;
		this.setFieldType(Class.forName(fieldType));
	}

	/**
	 * @param targetName
	 * @param tx
	 */
	protected void cascadeDelete(Name targetName, Transaction tx)
			throws DirectoryException {
		// nothing to do here
	}

	/**
	 * @param o
	 * @param tx TODO
	 * @throws DirectoryException
	 */
	protected void cascadePostLoad(Object o, Transaction tx)
			throws DirectoryException {
		// nothing to do here
	}

	/**
	 * @param o
	 * @param tx
	 * @param ctx TODO
	 * @throws DirectoryException
	 */
	protected void cascadePostSave(Object o, Transaction tx, DirContext ctx)
			throws DirectoryException {
		// nothing to do
	}

	/**
	 * @param targetName
	 * @param newName
	 */
	public void cascadeRDNChange(Name oldName, Name newName)
			throws DirectoryException {
		// nothing to do here
	}

	/**
	 * @return
	 */
	protected boolean checkNull(Attributes a) {
		return null == a.get(fieldName);
	}

	/**
	 * Dehydrates the attribute value into the supplied attribute container and
	 * returns the value.
	 * 
	 * @param o
	 * @param a
	 * @return the dehydrated value
	 * @throws DirectoryException
	 */
	public Object dehydrate(Object o, BasicAttributes a)
			throws DirectoryException {
		if (logger.isTraceEnabled())
			logger.trace("dehydrating " + fieldName + " for instance of "
					+ o.getClass());

		try {
			final Object v = getValue(o);
			return valueToAttributes(a, v);
		} catch (final Exception e) {
			throw new DirectoryException("Can't read attribute from object", e);
		}
	}

	/**
	 * @param a
	 * @param v
	 * @return
	 */
	protected Object valueToAttributes(BasicAttributes a, Object v) {
		if (null != v)
			if (fieldType.equals(String.class)) {
				if (((String) v).length() > 0)
					a.put(fieldName, v);
			} else if (fieldType.equals(byte[].class)) {
				if (((byte[]) v).length > 0)
					a.put(fieldName, v);
			} else
				a.put(fieldName, v.toString());
		return v;
	}

	protected Class getFieldType() {
		return fieldType;
	}

	protected Method getGetter() throws NoSuchMethodException {
		if (null == this.getMethod) {
			if (null == getMethodName)
				getMethodName = "get" + fieldName.substring(0, 1).toUpperCase()
						+ fieldName.substring(1);
			this.getMethod = getMethod(type.getMappedType(), getMethodName,
					new Class[]{});
		}
		return getMethod;
	}

	/**
	 * @param modelClass
	 * @param getMethod2
	 * @param classes
	 * @return
	 * @throws NoSuchMethodException
	 */
	protected Method getMethod(Class targetClass, String methodName,
			Class[] parameterTypes) throws NoSuchMethodException {
		NoSuchMethodException firstException = null;
		while (null != targetClass) {
			try {
				return targetClass.getMethod(methodName, parameterTypes);
			} catch (final NoSuchMethodException e) {
				if (null == firstException)
					firstException = e;
			}

			targetClass = targetClass.getSuperclass();
		}

		throw firstException;
	}

	protected Method getSetter() throws NoSuchMethodException {
		if (null == this.setMethod) {
			if (null == setMethodName)
				setMethodName = "set" + fieldName.substring(0, 1).toUpperCase()
						+ fieldName.substring(1);
			this.setMethod = getMethod(type.getMappedType(), setMethodName,
					new Class[]{getFieldType()});
		}
		return setMethod;
	}

	/**
	 * @param o
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 */
	protected Object getValue(Object o) throws DirectoryException {
		try {
			return getGetter().invoke(o, new Object[]{});
		} catch (final Exception e) {
			throw new DirectoryException("Can't get value for " + this, e);
		}
	}

	/**
	 * @param o
	 * @param a
	 * @param tx TODO
	 * @throws NamingException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws NoSuchMethodException
	 * @throws DirectoryException
	 */
	public void hydrate(Object o, Attributes a, Transaction tx)
			throws DirectoryException {
		if (logger.isTraceEnabled())
			logger.trace("hydrating " + this + " for object of type " + o.getClass()
					+ " from " + a);

		if (checkNull(a))
			return;

		try {
			setValue(o, valueFromAttributes(a, o, tx));
		} catch (final DirectoryException e) {
			throw e;
		} catch (final Exception e) {
			throw new DirectoryException("Can't hydrate attribute " + fieldName, e);
		}
	}

	/**
	 * @param instance
	 * @throws DirectoryException
	 */
	protected void initNewInstance(Object instance) throws DirectoryException {
		// nothing to do
	}

	/**
	 * 
	 */
	protected void initPostLoad() {
		// nothing to do
	}

	protected void setFieldType(Class fieldType) {
		this.fieldType = fieldType;
		this.getMethod = this.setMethod = null;
	}

	public void setGetMethod(String getMethodName) {
		this.getMethodName = getMethodName;
	}

	public void setSetMethod(String setMethodName) {
		this.setMethodName = setMethodName;
	}

	public void setTypeMapping(TypeMapping type) {
		this.type = type;
	}

	/**
	 * @param o
	 * @param a
	 * @param dn
	 * @return
	 * @throws DirectoryException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws NamingException
	 * @throws DirectoryException
	 */
	Object setValue(Object o, Object value) throws DirectoryException {
		try {
			return getSetter().invoke(o, new Object[]{value});
		} catch (final Exception e) {
			throw new DirectoryException("Can't set value for " + this, e);
		}
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "[AttributeMapping name=" + fieldName + " type=" + fieldType + "]";
	}

	/**
	 * @param a
	 * @param o TODO
	 * @param tx TODO
	 * @return
	 * @throws NamingException
	 * @throws DirectoryException
	 */
	protected Object valueFromAttributes(Attributes a, Object o, Transaction tx)
			throws NamingException, DirectoryException {
		final Attribute attribute = a.get(fieldName);
		if (null != attribute) {
			Object v = attribute.get();
			if (null != v)
				// handle various value types
				if (fieldType.equals(Integer.class))
					try {
						v = new Integer(v.toString());
					} catch (final NumberFormatException e) {
						logger.error("Can't convert this value to an Integer: "
								+ v.toString());
						v = null;
					}
			return v;
		} else
			return null;
	}

	/**
	 * @param o
	 * @param tx
	 * @throws DirectoryException
	 */
	protected void cascadePreSave(Object o, Transaction tx)
			throws DirectoryException {
		// nothing to be done for basic attributes
	}

	/*
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected AttributeMapping clone() throws CloneNotSupportedException {
		return (AttributeMapping) super.clone();
	}

	protected void collectRefererAttributes(Set<String> refererAttributes) {
		// default: nothing to do
	}

	public void setCardinality(String cardinality) {
		this.cardinality = Cardinality.valueOf(cardinality);
	}
}
