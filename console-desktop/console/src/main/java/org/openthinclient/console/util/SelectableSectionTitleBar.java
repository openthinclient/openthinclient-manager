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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.UIManager;

import org.openthinclient.console.DetailViewProvider;

import com.levigo.util.swing.IconManager;

/**
 * @author Natalie Bohnert
 */
public class SelectableSectionTitleBar extends SectionTitleBar {

  protected JButton toggleVisibility;

  protected Icon contentVisible;
  protected Icon contentInVisible;

  protected boolean visible = false;

  protected static final String DEFAULT_ICON_CONTENT_VISIBLE = "list.hide"; //$NON-NLS-1$
  protected static final String DEFAULT_ICON_CONTENT_INVISIBLE = "list.unhide"; //$NON-NLS-1$

  /**
   * 
   */
  public SelectableSectionTitleBar() {
    this(""); //$NON-NLS-1$
  }

  /**
   * @param title
   */
  public SelectableSectionTitleBar(String title) {
    this(title, null, null);
  }

  public SelectableSectionTitleBar(String title, Icon contentVisible,
      Icon contentInvisible) {
    super();

    if (contentVisible != null) {
      this.contentVisible = contentVisible;
    } else {
      this.contentVisible = IconManager.getInstance(DetailViewProvider.class,
          "icons").getIcon(DEFAULT_ICON_CONTENT_VISIBLE); //$NON-NLS-1$
    }
    if (contentInvisible != null) {
      this.contentInVisible = contentInvisible;
    } else {
      this.contentInVisible = IconManager.getInstance(DetailViewProvider.class,
          "icons").getIcon(DEFAULT_ICON_CONTENT_INVISIBLE); //$NON-NLS-1$
    }

    toggleVisibility = new JButton();
    toggleVisibility.setIcon(this.contentInVisible);
    toggleVisibility.setBorder(null);

    toggleVisibility.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        firePropertyChange("contentVisibility", visible, !visible); //$NON-NLS-1$
        visible = !visible;
        updateButton();
      }

    });

    add(toggleVisibility);

    add(Box.createGlue());

    titleLabel = new JLabel(title);
    titleLabel.setFont(UIManager.getFont("TitledBorder.font")); //$NON-NLS-1$
    add(titleLabel);

    add(Box.createGlue());

    updateUI();
  }

  private void updateButton() {
    if (visible) {
      toggleVisibility.setIcon(contentVisible);
    } else {
      toggleVisibility.setIcon(contentInVisible);
    }
  }

  /**
   * @param visible2
   */
  public void setContentVisible(boolean visible) {
    this.visible = visible;
    updateButton();
  }

}
