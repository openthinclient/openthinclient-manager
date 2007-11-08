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

import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;

import com.levigo.util.swing.IconManager;

/**
 * Action which shows Feed component.
 */
public class RefreshAction extends NodeAction {

  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RefreshAction() {
    super();
    setIcon(IconManager.getInstance(getClass(), "icons").getIcon("Refresh")); //$NON-NLS-1$ //$NON-NLS-2$
  }

  /*
   * @see org.openide.util.actions.NodeAction#performAction(org.openide.nodes.Node[])
   */
  @Override
  protected void performAction(Node[] activatedNodes) {
    for (Node node : activatedNodes)
      if (node instanceof Refreshable)
        ((Refreshable) node).refresh();
  }

  /*
   * @see org.openide.util.actions.NodeAction#enable(org.openide.nodes.Node[])
   */
  @Override
  protected boolean enable(Node[] activatedNodes) {
	  
    for (Node node : activatedNodes)
      if (node instanceof Refreshable)
        return true;
    return false;
  }

  /*
   * @see org.openide.util.actions.SystemAction#getName()
   */
  @Override
  public String getName() {
    return Messages.getString("RefreshAction.name"); //$NON-NLS-1$
  }

  /*
   * @see org.openide.util.actions.SystemAction#getHelpCtx()
   */
  @Override
  public HelpCtx getHelpCtx() {
    return null;
  }
  
  /* 
   * @see org.openide.util.actions.CallableSystemAction#asynchronous()
   */
  @Override
  protected boolean asynchronous() {
    return true;
  }
}
