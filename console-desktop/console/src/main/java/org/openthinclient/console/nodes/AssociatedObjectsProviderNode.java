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
package org.openthinclient.console.nodes;

import java.awt.Image;
import java.util.Map;
import java.util.Set;

import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openthinclient.common.model.AssociatedObjectsProvider;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.console.DetailViewProvider;

import com.levigo.util.swing.IconManager;

/**
 * @author levigo
 */
public class AssociatedObjectsProviderNode extends AbstractNode {
  private final AssociatedObjectsProvider aop;

  public AssociatedObjectsProviderNode(AssociatedObjectsProvider aop) {
    super(new Children.Array());
    this.aop = aop;

    setName(aop instanceof DirectoryObject ? ((DirectoryObject) aop)
        .getName() : aop.toString());
    Children children = getChildren();
    Map<Class, Set<? extends DirectoryObject>> associatedObjects = aop
        .getAssociatedObjects();
    if (associatedObjects.keySet().size() > 1) {
      // if there is more than one class of associated objects,
      // add child nodes for each class....
      for (Map.Entry<Class, Set<? extends DirectoryObject>> entry : associatedObjects
          .entrySet())
        if (entry.getValue().size() > 0)
          children.add(new Node[]{new DirObjectSetNode(entry.getKey(), entry
              .getValue())});
    } else if (associatedObjects.keySet().size() > 0) {
      // ... otherwise add the associated objects directly
      for (DirectoryObject object : associatedObjects.values().iterator()
          .next()) {
        if (object instanceof AssociatedObjectsProvider)
          getChildren().add(
              new Node[]{new AssociatedObjectsProviderNode(
                  (AssociatedObjectsProvider) object)});
        else
          getChildren().add(new Node[]{new DirObjectNode(this, object)});
      }
    }
  }

  /*
   * @see org.openthinclient.console.nodes.MyAbstractNode#getIcon(int)
   */
  @Override
  public Image getOpenedIcon(int type) {
    return IconManager.getInstance(DetailViewProvider.class, "icons") //$NON-NLS-1$
        .getImage("tree." + aop.getClass().getSimpleName()); //$NON-NLS-1$
  }

  /*
   * @see org.openide.nodes.AbstractNode#getIcon(int)
   */
  @Override
  public Image getIcon(int type) {
    return getOpenedIcon(type);
  }
}
