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
package org.openthinclient.console.nodes.views;

import java.beans.PropertyChangeEvent;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.openide.nodes.Node;
import org.openide.nodes.NodeEvent;
import org.openide.nodes.NodeListener;
import org.openide.nodes.NodeMemberEvent;
import org.openide.nodes.NodeReorderEvent;
import org.openide.util.WeakListeners;

/**
 * @author levigo
 */
abstract class NBNodeTreeTableModel extends AbstractTreeTableModel
    implements
      NodeListener {
  private Set<Node> nodesListenedTo = new HashSet<Node>();

  public NBNodeTreeTableModel(Node node) {
    super(node);
  }

  /*
   * @see org.jdesktop.swingx.treetable.TreeTableModel#getValueAt(java.lang.Object,
   *      int)
   */
  public abstract Object getValueAt(Object node, int index);

  /*
   * @see org.jdesktop.swingx.treetable.AbstractTreeTableModel#getColumnName(int)
   */
  @Override
  public abstract String getColumnName(int index);

  /*
   * @see org.jdesktop.swingx.treetable.TreeTableModel#setValueAt(java.lang.Object,
   *      java.lang.Object, int)
   */
  public void setValueAt(Object arg0, Object arg1, int arg2) {
    throw new IllegalArgumentException("not editable"); //$NON-NLS-1$
  }

  /*
   * @see org.jdesktop.swingx.treetable.AbstractTreeTableModel#getColumnCount()
   */
  @Override
  public abstract int getColumnCount();

  /*
   * @see org.jdesktop.swingx.treetable.AbstractTreeTableModel#getChild(java.lang.Object,
   *      int)
   */
  @Override
  public Object getChild(Object parent, int idx) {
    return getNode(parent).getChildren().getNodes()[idx];
  }

  /**
   * @param parent
   * @return
   */
  protected Node getNode(Object parent) {
    final Node node = ((Node) parent);

    if (!nodesListenedTo.contains(node)) {
      node.addNodeListener((NodeListener) WeakListeners.create(
          NodeListener.class, this, node));
      nodesListenedTo.add(node);
    }

    return node;
  }

  /*
   * @see org.jdesktop.swingx.treetable.AbstractTreeTableModel#getChildCount(java.lang.Object)
   */
  @Override
  public int getChildCount(Object parent) {
    return getNode(parent).getChildren().getNodesCount();
  }

  /*
   * @see org.openide.nodes.NodeListener#childrenAdded(org.openide.nodes.NodeMemberEvent)
   */
  public void childrenAdded(NodeMemberEvent ev) {
    fireTreeNodesInserted(this, buildPath(ev.getNode()), ev.getDeltaIndices(),
        ev.getDelta());
  }

  /**
   * @param node TODO
   * @return
   */
  private Object[] buildPath(Node node) {
    List<Node> path = new LinkedList<Node>();
    path.add(node);
    while (path.get(0) != getRoot()) {
      Node parent = path.get(0).getParentNode();
      if (null == parent) {
        System.err.println("Unexpected: parent was null");
        break;
      }
      path.add(0, parent);
    }

    Object p[] = path.toArray(new Object[path.size()]);
    return p;
  }

  /*
   * @see org.openide.nodes.NodeListener#childrenRemoved(org.openide.nodes.NodeMemberEvent)
   */
  public void childrenRemoved(NodeMemberEvent ev) {
    fireTreeNodesRemoved(this, buildPath(ev.getNode()), ev.getDeltaIndices(),
        ev.getDelta());
  }

  /*
   * @see org.openide.nodes.NodeListener#childrenReordered(org.openide.nodes.NodeReorderEvent)
   */
  public void childrenReordered(NodeReorderEvent ev) {
    fireTreeStructureChanged(ev, buildPath(ev.getNode()), ev.getPermutation(),
        new Node[0]);
  }

  /*
   * @see org.openide.nodes.NodeListener#nodeDestroyed(org.openide.nodes.NodeEvent)
   */
  public void nodeDestroyed(NodeEvent ev) {
    fireTreeNodesRemoved(this, buildPath(ev.getNode().getParentNode()),
        new int[0], new Node[]{ev.getNode()});
  }

  /*
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent evt) {
    if (evt.getSource() instanceof Node)
      fireTreeNodesChanged(this, buildPath((Node) evt.getSource()), new int[0],
          new Object[0]);
  }
}
