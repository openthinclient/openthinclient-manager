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
package org.openthinclient.console.util;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.UIManager;

import com.jgoodies.forms.util.DefaultUnitConverter;

/**
 * @author levigo
 */
public class SectionTitleBar extends JComponent {
  private static final long serialVersionUID = 1L;

  protected JLabel titleLabel;

  public SectionTitleBar() {
    setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    DefaultUnitConverter duc = DefaultUnitConverter.getInstance();
    int leftRight = duc.dialogUnitXAsPixel(4, this);
    int topBottom = duc.dialogUnitYAsPixel(2, this);
    setBorder(BorderFactory.createEmptyBorder(topBottom, leftRight, topBottom,
        leftRight));

    updateUI();
  }

  public SectionTitleBar(String title) {
    this();
    titleLabel = new JLabel(title);
    titleLabel.setFont(UIManager.getFont("TitledBorder.font")); //$NON-NLS-1$
    add(titleLabel);

    add(Box.createGlue());

    updateUI();
  }

  /*
   * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
   */
  protected void paintComponent(Graphics g) {
    DefaultUnitConverter duc = DefaultUnitConverter.getInstance();

    int w = getWidth() - 1;
    int h = getHeight();
    int bevelSize = duc.dialogUnitXAsPixel(6, this);

    Graphics2D g2 = (Graphics2D) g;

    GeneralPath clip = new GeneralPath();
    clip.moveTo(0, h);
    clip.lineTo(0, bevelSize);
    clip.curveTo(0, 0, 0, 0, bevelSize, 0);
    clip.lineTo(w - bevelSize, 0);
    clip.curveTo(w, 0, w, 0, w, bevelSize);
    clip.lineTo(w, h);

    Color fillColor = UIManager.getColor("InternalFrame.activeTitleGradient"); //$NON-NLS-1$
    if (null == fillColor)
      fillColor = UIManager.getColor("InternalFrame.activeTitleBackground"); //$NON-NLS-1$

    Object rhAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
    GradientPaint painter = new GradientPaint(0, h / 2, new Color(fillColor
        .getRed(), fillColor.getGreen(), fillColor.getBlue(), 128), 0, h,
        new Color(fillColor.getRed(), fillColor.getGreen(),
            fillColor.getBlue(), 0));
    g2.setPaint(painter);
    g2.fill(clip);

    Color lineColor = getBackground().darker();
    painter = new GradientPaint(0, h / 2, lineColor, 0f, h, new Color(lineColor
        .getRed(), lineColor.getGreen(), lineColor.getBlue(), 0));
    g2.setPaint(painter);
    g2.draw(clip);
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, rhAA);

    super.paintComponent(g);
  }
}
