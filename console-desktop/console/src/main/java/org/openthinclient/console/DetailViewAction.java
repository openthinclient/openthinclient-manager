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
package org.openthinclient.console;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import org.openide.ErrorManager;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Action which shows Feed component.
 */
public class DetailViewAction extends AbstractAction {

  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DetailViewAction() {
    super(Messages.getString("DetailViewAction.name"), new ImageIcon(Utilities.loadImage( //$NON-NLS-1$
        "org/openthinclient/console/icon.png", true))); //$NON-NLS-1$
  }

  public void actionPerformed(ActionEvent evt) {
    TopComponent win = WindowManager.getDefault().findTopComponent(
        "DetailViewTopComponent"); //$NON-NLS-1$
    if (win == null) {
      ErrorManager.getDefault().log(ErrorManager.WARNING,
          Messages.getString("DetailViewAction.notFound")); //$NON-NLS-1$
      return;
    }
    win.open();
    win.requestActive();
  }

}
