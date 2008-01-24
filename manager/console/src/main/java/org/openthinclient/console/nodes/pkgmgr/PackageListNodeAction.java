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
 ******************************************************************************/
package org.openthinclient.console.nodes.pkgmgr;

import org.openide.ErrorManager;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.openide.windows.TopComponent;
import org.openthinclient.console.Messages;
import org.openthinclient.pkgmgr.PackageManagerException;

import com.levigo.util.swing.IconManager;

/**
 */
public class PackageListNodeAction extends NodeAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PackageListNodeAction() {
		super();
		setIcon(IconManager.getInstance(getClass(), "icons").getIcon("Refresh")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * @see org.openide.util.actions.NodeAction#performAction(org.openide.nodes.Node[])
	 */
	@Override
	protected void performAction(Node[] activatedNodes) {
		boolean b = true;
		for (final Node node : activatedNodes) {
			final Node node2 = node;
			while (b) {
				if (!node2.getClass().equals(PackageListNode.class))
					if (null != node2.getParentNode())
						b = false;
				try {
					final PackageManagerEditPanel paMaEdPa = PackageManagerEditPanel
							.getInstance();
					paMaEdPa.init(node, null, activatedNodes, new TopComponent());
					paMaEdPa.doEdit();
				} catch (final PackageManagerException e) {
					e.printStackTrace();
					ErrorManager.getDefault().notify(e);
				}
			}
		}
	}

	/*
	 * @see org.openide.util.actions.NodeAction#enable(org.openide.nodes.Node[])
	 */
	private Node[] nodes;

	@Override
	protected boolean enable(Node[] activatedNodes) {
		nodes = activatedNodes;
		for (final Node node : activatedNodes)
			if (node instanceof PackageListNode)
				return true;
		return false;
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getName()
	 */
	@Override
	public String getName() {
		for (final Node node : nodes)
			if (node instanceof PackageListNode)
				return Messages.getString("edit"); //$NON-NLS-1$
		return null;

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

	// public DetailView getDetailView() {
	//		
	// return DetailView.getInstance();
	// }
}
