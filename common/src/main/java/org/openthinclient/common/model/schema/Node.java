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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * The node base class. Handles basic properties like the node name, parent and
 * children.
 * 
 * @author levigo
 */
public class Node implements Iterable<Node>, Serializable {

	private static final long serialVersionUID = 1L;

	protected static final List<Node> EMPTY_COLLECTION = new LinkedList<Node>();

	/** The node's name */
	private String name;

	/** The node's parent */
	private transient Node parent;

	/**
	 * The node's key consisting of the names of the nodes along the path to this
	 * node, joined using "."s. Lazily initialized.
	 */
	private transient String key;

	/** Initialized only on demand */
	private List<Node> children = EMPTY_COLLECTION;

	/**
	 * Map of language->label.
	 */
	private ArrayList<Label> labels = new ArrayList<Label>();

	/**
	 * Map of language->tip.
	 */
	private ArrayList<Label> tips = new ArrayList<Label>();

	/**
	 * Create a new node with just a name.
	 * 
	 * @param name
	 */
	public Node(String name) {
		// if (name.indexOf('.') >= 0)
		// throw new IllegalArgumentException(
		// "The period (.) character is not allowed in node names.");
		this.name = name;
	}

	/**
	 * Get the Node's childern.
	 * 
	 * @return
	 */
	public List<Node> getChildren() {
		return children;
	}

	/**
	 * Get the child with a given name or <code>null</code> if no child with the
	 * given name exists.
	 * 
	 * @param name
	 * @return
	 */
	public Node getChild(String name) {
		for (Node child : children) {
			if (child.getName().equals(name))
				return child;
		}

		return null;
	}

	/**
	 * Add a child to this node.
	 * 
	 * @param child
	 */
	public void addChild(Node child) {
		if (children == EMPTY_COLLECTION)
			children = new ArrayList<Node>();
		child.setParent(this);
		children.add(child);
	}

	/**
	 * Add a child to this node.
	 * 
	 * @param child
	 */
	protected void addChild(Node child, int index) {
		if (children == EMPTY_COLLECTION)
			children = new ArrayList<Node>();
		child.setParent(this);
		children.add(index, child);
	}

	public boolean removeChild(Node child) {
		if (!children.contains(child))
			return false;
		child.setParent(null);
		children.remove(child);
		return true;
	}

	/**
	 * Get the node's name.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the node's parent. The parent is <code>null</code> for root nodes.
	 * 
	 * @return
	 */
	public Node getParent() {
		return parent;
	}

	/**
	 * Set the node's parent.
	 * 
	 * @param parent
	 */
	protected void setParent(Node parent) {
		this.parent = parent;

		clearPathCache();
	}

	/**
	 * Clear the path cache of this node as well as of all of the children.
	 * 
	 * @deprecated
	 */
	@Deprecated
	public void clearPathCache() {
		// if I don't have a cached path, my children don't have one either
		if (null != key) {
			key = null;
			for (Node child : children)
				child.clearPathCache();
		}
		// if (null != path) {
		// path = null;
		// for (Node child : children)
		// child.clearPathCache();
		// }
	}

	// /**
	// * Get the node's path. Initialize the path if necessary. Calling this
	// method
	// * repeatedly will return cached paths.
	// *
	// * @return
	// */
	// public TreePath getPath() {
	// if (null == path)
	// if (null == parent)
	// path = new TreePath(this);
	// else
	// path = parent.getPath().pathByAddingChild(this);
	//
	// return path;
	// }

	/**
	 * Get the node's key. Initialize the path if necessary. Calling this method
	 * repeatedly will return cached paths.
	 * 
	 * @return
	 */
	public String getKey() {
		if (null == key)
			if (null == parent || parent instanceof Schema)
				key = name;
			else
				key = parent.getKey() + "." + name;

		return key;
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getLabel();
	}

	/**
	 * Remove all children of this node.
	 */
	public void removeAllChildren() {
		children = EMPTY_COLLECTION;
	}

	public void setName(String name) {
		if (name.contains("/"))
			throw new IllegalArgumentException("Node name may not contain a '/'");
		this.name = name;
	}

	public Iterator<Node> iterator() {
		return children.iterator();
	}

	public ArrayList<Label> getLabels() {
		return labels;
	}

	public void setLabels(ArrayList<Label> labels) {
		this.labels = labels;
	}

	public String getLabel() {
		for (Label label : labels) {
			if (label.getLang().equals(Locale.getDefault().getLanguage())) {
				String labelText = label.getLabel();
				if (labelText != null) {
					return labelText;
				}
			}
		}
		return name;
	}

	public ArrayList<Label> getTips() {
		return tips;
	}

	public void setTips(ArrayList<Label> tips) {
		this.tips = tips;
	}

	public String getTip() {
		for (Label tip : tips)
			if (tip.getLang().equals(Locale.getDefault().getLanguage()))
				return tip.getLabel();

		return null;
	}

	protected long getUID() {
		long uid = getClass().getSimpleName().hashCode();

		for (Iterator i = children.iterator(); i.hasNext();) {
			Node child = (Node) i.next();
			uid ^= child.getUID();
		}

		uid ^= getKey().hashCode() ^ getName().hashCode();

		return uid;
	}
}
