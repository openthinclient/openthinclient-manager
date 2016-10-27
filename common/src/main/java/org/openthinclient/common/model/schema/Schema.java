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
package org.openthinclient.common.model.schema;

import java.text.Collator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;

import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.Profile;

/**
 * The root node of a configuration template.
 * 
 * @author levigo
 */
public class Schema<T extends Profile> extends Node
		implements
			Comparable<Schema> {
	private static final long serialVersionUID = 109860938274823423L;

	private Class type;

	private final boolean singleInstanceOnly;

	private final String subtype;

	public Schema(String name) {
		super(name);
		this.type = Application.class;
		this.subtype = null;
		this.singleInstanceOnly = false;
	}

	public Schema getSchema() {
		return this;
	}

	/*
	 * @see org.openthinclient.common.model.Profile#contains(java.lang.String)
	 */
	public boolean contains(String key) {
		return null != getNodeForPath(key);
	}

	public Node getNodeForPath(String path) {
		Node node = this;
		final StringTokenizer st = new StringTokenizer(path, ".");
		String nodeName;
		if (st.hasMoreTokens()) {
			// accept a path starting AT the current node or BELOW it
			nodeName = st.nextToken();
			if (nodeName.equals(node.getName()) && st.hasMoreTokens())
				nodeName = st.nextToken();

			do {
				node = node.getChild(nodeName);
				if (st.hasMoreTokens())
					nodeName = st.nextToken();
				else
					break;
			} while (node != null);

			return node;
		} else
			return null;
	}

	/*
	 * @see org.openthinclient.common.model.Profile#getKeys()
	 */
	public Set<String> getKeys() {
		return null;
	}

	/*
	 * @see
	 * org.openthinclient.common.model.Profile#getOverriddenValue(java.lang.String
	 * )
	 */
	public String getOverriddenValue(String key) {
		return null;
	}

	/*
	 * @see org.openthinclient.common.model.Profile#getValue(java.lang.String,
	 * boolean)
	 */
	public String getValue(String key) {
		final Node n = getNodeForPath(key);
		return null != n && n instanceof EntryNode
				? ((EntryNode) n).getValue()
				: null;
	}

	/*
	 * @see org.openthinclient.common.model.Profile#getValues()
	 */
	public Map<String, String> getValues() {
		return null;
	}

	/*
	 * @see org.openthinclient.common.model.Profile#setValue(java.lang.String,
	 * java.lang.String)
	 */
	public void setValue(String path, String value) {
		throw new IllegalArgumentException("Can't set the value here.");
	}

	/*
	 * @see org.openthinclient.common.model.Profile#setValues(java.util.SortedMap)
	 */
	public void setValues(SortedMap<String, String> values) {
		throw new IllegalArgumentException("Can't set the value here.");
	}

	public boolean isSingleInstanceOnly() {
		return singleInstanceOnly;
	}

	public Class<T> getType() {
		return type;
	}

	public void setType(Class type) {
		this.type = type;
	}

	public String getSubtype() {
		return subtype;
	}

	public long getVersion() {
		return getUID();
	}

	public int compareTo(Schema compareSchema) {
		final Collator collator = Collator.getInstance();
		return collator.compare(this.getLabel(), compareSchema.getLabel());
	}

}
