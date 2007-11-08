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

import java.awt.BorderLayout;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.openthinclient.console.util.SelectableSectionTitleBar;


/**
 * @author Natalie Bohnert
 */
public class TitledTreePanel extends JPanel {

  private boolean contentVisible = false;

  private SelectableSectionTitleBar titleBar;
  private JComponent contentPanel;

  /**
   * @param contentPanel
   */
  public TitledTreePanel(JComponent contentPanel) {
    this(contentPanel, ""); //$NON-NLS-1$
  }

  /**
   * @param title
   */
  public TitledTreePanel(String title) {
    this(null, title);
  }

  /**
   * @param contentPanel
   * @param title
   */
  public TitledTreePanel(JComponent contentPanel, String title) {
    setBackground(Color.WHITE);
    this.contentPanel = contentPanel;
    this.titleBar = new SelectableSectionTitleBar(title);
    init();
  }

  private void init() {
    titleBar.addPropertyChangeListener(new PropertyChangeListener() {

      public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName() == "contentVisibility") { //$NON-NLS-1$
          update();
        }

      }

    });
    setLayout(new BorderLayout());
    contentPanel.setBackground(UIManager.getColor("TextField.background")); //$NON-NLS-1$
    // titleBar.setBackground(UIManager.getColor("TextField.background"));
    add(titleBar, BorderLayout.NORTH);
    // add(contentPanel, BorderLayout.CENTER);
    updateUI();
  }

  private void update() {
    if (contentVisible) {
      contentVisible = false;
      remove(contentPanel);
    } else {
      contentVisible = true;
      add(contentPanel);
    }
    revalidate();
    repaint();
  }

  public void setContentVisible(boolean visible) {
    update();
    titleBar.setContentVisible(visible);
  }
}
