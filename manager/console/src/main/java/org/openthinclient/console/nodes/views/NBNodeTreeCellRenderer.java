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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.openide.nodes.Node;

/**
 * @author levigo
 */
class NBNodeTreeCellRenderer extends DefaultTreeCellRenderer {
  private JTree tree;

  private static ImageIcon ii = new ImageIcon();

  /*
   * @see javax.swing.tree.DefaultTreeCellRenderer#getTreeCellRendererComponent(javax.swing.JTree,
   *      java.lang.Object, boolean, boolean, boolean, int, boolean)
   */
  public Component getTreeCellRendererComponent(JTree tree, Object value,
      boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
    this.tree = tree;

    JLabel r = (JLabel) super.getTreeCellRendererComponent(tree,
        value instanceof Node ? ((Node) value).getName() : value, sel,
        expanded, leaf, row, hasFocus);

    if (value instanceof Node) {
      if (expanded)
        ii.setImage(((Node) value).getOpenedIcon(0));
      else
        ii.setImage(((Node) value).getIcon(0));
      setIcon(ii);
    } else
      setIcon(null);

    return r;
  }

  public void paint(Graphics g) {
    String fullText = super.getText();
    // getText() calls tree.convertValueToText();
    // tree.convertValueToText() should call treeModel.convertValueToText(),
    // if possible

    String shortText = SwingUtilities.layoutCompoundLabel(this, g
        .getFontMetrics(), fullText, getIcon(), getVerticalAlignment(),
        getHorizontalAlignment(), getVerticalTextPosition(),
        getHorizontalTextPosition(), getItemRect(itemRect), iconRect, textRect,
        getIconTextGap());

    /**
     * TODO: setText is more heavyweight than we want in this situation. Make
     * JLabel.text protected instead of private.
     */

    setText(shortText); // temporarily truncate text
    super.paint(g);
    setText(fullText); // restore full text
  }

  private Rectangle getItemRect(Rectangle itemRect) {
    tree.getBounds(itemRect);
    itemRect.width = tree.getWidth() - itemRect.x;
    return itemRect;
  }

  // Rectangles filled in by SwingUtilities.layoutCompoundLabel();
  private final Rectangle iconRect = new Rectangle();
  private final Rectangle textRect = new Rectangle();
  // Rectangle filled in by this.getItemRect();
  private final Rectangle itemRect = new Rectangle();
}
