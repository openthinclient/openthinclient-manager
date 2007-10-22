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
package org.openthinclient.console.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.UIManager;
import javax.swing.border.Border;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXHyperlink;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Stacks components vertically in boxes. Each box is created with a title and a
 * component.<br>
 * 
 * <p>
 * The <code>StackedBox</code> can be added to a
 * {@link javax.swing.JScrollPane}.
 * 
 * <p>
 * Note: this class is not part of the SwingX core classes. It is just an
 * example of what can be achieved with the components.
 * 
 * @author <a href="mailto:fred@L2FProd.com">Frederic Lavigne</a>
 */
public class SectionPanel extends JPanel implements Scrollable {

  /**
   * 
   */
  private static final String BOTTOM_INDENT = "8dlu"; //$NON-NLS-1$
  /**
   * 
   */
  private static final String LEFT_INDENT = "10dlu"; //$NON-NLS-1$
  private Color titleBackgroundColor;
  private Color titleForegroundColor;
  private Color separatorColor;
  private JXHyperlink link;
  private JXCollapsiblePane collapsible;

  public SectionPanel(String title, Component component, boolean collapsed) {
    FormLayout layout = new FormLayout("0dlu, fill:default:grow", //$NON-NLS-1$
        "2dlu, default, 0dlu, fill:default:grow, 0dlu"); //$NON-NLS-1$
    setLayout(layout);
    setOpaque(false);

    if (UIFactory.DEBUG)
      setBorder(BorderFactory.createEtchedBorder());

    FormLayout collapsibleLayout = new FormLayout(LEFT_INDENT
        + ", fill:default:grow", "fill:default:grow, " + BOTTOM_INDENT); //$NON-NLS-1$ //$NON-NLS-2$
    CellConstraints cc = new CellConstraints();
    collapsible = new JXCollapsiblePane();
    collapsible.setAnimated(false);
    collapsible.setCollapsed(collapsed);
    collapsible.getContentPane().setLayout(collapsibleLayout);
    collapsible.getContentPane().add(component, cc.xy(2, 1));
    collapsible.setOpaque(false);
    ((JPanel) collapsible.getContentPane()).setOpaque(false);
    ((JPanel) collapsible.getComponent(0)).setOpaque(false);

    if (UIFactory.DEBUG)
      ((JPanel) collapsible.getContentPane()).setBorder(BorderFactory
          .createEtchedBorder());

    Action toggleAction = collapsible.getActionMap().get(
        JXCollapsiblePane.TOGGLE_ACTION);
    // // use the collapse/expand icons from the JTree UI
    toggleAction.putValue(JXCollapsiblePane.COLLAPSE_ICON, UIManager
        .getIcon("Tree.expandedIcon")); //$NON-NLS-1$
    toggleAction.putValue(JXCollapsiblePane.EXPAND_ICON, UIManager
        .getIcon("Tree.collapsedIcon")); //$NON-NLS-1$

    link = new JXHyperlink(toggleAction);
    link.setText(title);
    link.setFont(link.getFont().deriveFont(Font.BOLD));
    link.setOpaque(false);
    link.setFocusPainted(false);

    link.setUnclickedColor(getTitleForegroundColor());
    link.setClickedColor(getTitleForegroundColor());

    add(link, cc.xy(2, 2));
    add(collapsible, cc.xywh(1, 4, 2, 2));
  }

  public Color getSeparatorColor() {
    return separatorColor;
  }

  public void setSeparatorColor(Color separatorColor) {
    this.separatorColor = separatorColor;
  }

  public Color getTitleForegroundColor() {
    return titleForegroundColor;
  }

  public void setTitleForegroundColor(Color titleForegroundColor) {
    this.titleForegroundColor = titleForegroundColor;
  }

  public Color getTitleBackgroundColor() {
    return titleBackgroundColor;
  }

  public void setTitleBackgroundColor(Color titleBackgroundColor) {
    this.titleBackgroundColor = titleBackgroundColor;
  }

  /**
   * @see Scrollable#getPreferredScrollableViewportSize()
   */
  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }

  /**
   * @see Scrollable#getScrollableBlockIncrement(java.awt.Rectangle, int, int)
   */
  public int getScrollableBlockIncrement(Rectangle visibleRect,
      int orientation, int direction) {
    return 10;
  }

  /**
   * @see Scrollable#getScrollableTracksViewportHeight()
   */
  public boolean getScrollableTracksViewportHeight() {
    if (getParent() instanceof JViewport) {
      return (((JViewport) getParent()).getHeight() > getPreferredSize().height);
    } else {
      return false;
    }
  }

  /**
   * @see Scrollable#getScrollableTracksViewportWidth()
   */
  public boolean getScrollableTracksViewportWidth() {
    return true;
  }

  /**
   * @see Scrollable#getScrollableUnitIncrement(java.awt.Rectangle, int, int)
   */
  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation,
      int direction) {
    return 10;
  }

  /**
   * The border between the stack components. It separates each component with a
   * fine line border.
   */
  class SeparatorBorder implements Border {

    boolean isFirst(Component c) {
      return c.getParent() == null || c.getParent().getComponent(0) == c;
    }

    public Insets getBorderInsets(Component c) {
      // if the collapsible is collapsed, we do not want its border to be
      // painted.
      if (c instanceof JXCollapsiblePane) {
        if (((JXCollapsiblePane) c).isCollapsed()) {
          return new Insets(0, 0, 0, 0);
        }
      }
      return new Insets(isFirst(c) ? 4 : 1, 0, 1, 0);
    }

    public boolean isBorderOpaque() {
      return true;
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width,
        int height) {
      g.setColor(getSeparatorColor());
      if (isFirst(c)) {
        g.drawLine(x, y + 2, x + width, y + 2);
      }
      g.drawLine(x, y + height - 1, x + width, y + height - 1);
    }
  }

  @Override
  public void setOpaque(boolean isOpaque) {
    super.setOpaque(isOpaque);
    if (link != null)
      link.setOpaque(isOpaque);
    if (collapsible != null)
      collapsible.setOpaque(isOpaque);

  }

}
