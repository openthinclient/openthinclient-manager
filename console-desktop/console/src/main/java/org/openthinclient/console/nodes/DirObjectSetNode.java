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
import java.util.Set;

import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openthinclient.common.model.AssociatedObjectsProvider;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.console.DetailViewProvider;
import org.openthinclient.console.Messages;

import com.levigo.util.swing.IconManager;

/**
 * @author levigo
 */
public class DirObjectSetNode extends AbstractNode {
  private final Class clazz;

  private static class MyChildren extends Children.Keys {
    /**
     * @param entries
     */
    public MyChildren(Set<? extends DirectoryObject> entries) {
      setKeys(entries);
    }

    /*
     * @see org.openide.nodes.Children.Keys#createNodes(java.lang.Object)
     */
    @Override
    protected Node[] createNodes(Object key) {
      if (key instanceof AssociatedObjectsProvider)
        return new Node[]{new AssociatedObjectsProviderNode(
            (AssociatedObjectsProvider) key)};
      else
        return new Node[]{new DirObjectNode(getNode(), (DirectoryObject) key)};
    }
  }

  public DirObjectSetNode(Class clazz, Set<? extends DirectoryObject> entries) {
    super(new MyChildren(entries));
    this.clazz = clazz;
  }

  public String getName() {
    return Messages.getString("types.plural." + clazz.getSimpleName()); //$NON-NLS-1$
  }

  /*
   * @see org.openthinclient.console.nodes.MyAbstractNode#getIcon(int)
   */
  @Override
  public Image getOpenedIcon(int type) {
    return IconManager.getInstance(DetailViewProvider.class, "icons").getImage( //$NON-NLS-1$
        "tree.list." + clazz.getSimpleName()); //$NON-NLS-1$
  }

  /*
   * @see org.openide.nodes.AbstractNode#getIcon(int)
   */
  @Override
  public Image getIcon(int type) {
    return getOpenedIcon(type);
  }
}
