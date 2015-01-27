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
public class PackageListNodeActionForPackageNode extends NodeAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PackageListNodeActionForPackageNode() {
		super();
		setIcon(IconManager.getInstance(getClass(), "icons").getIcon("Refresh")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * @see org.openide.util.actions.NodeAction#performAction(org.openide.nodes.Node[])
	 */
	@Override
	protected void performAction(Node[] activatedNodes) {
		Node node2=null;
		Node[] packListNode = new Node[1];
		boolean b = true;
		for (Node node : activatedNodes) {
			node2 = node;
			while (b) {
				if (node2.getName().equalsIgnoreCase(
						Messages.getString("node.InstalledPackagesNode"))
						|| node2.getName().equalsIgnoreCase(
								Messages.getString("node.AvailablePackagesNode"))
						|| node2.getName().equalsIgnoreCase(
								Messages.getString("node.UpdatablePackagesNode"))
						|| node2.getName().equalsIgnoreCase(
								Messages.getString("node.AlreadyDeletedPackagesNode"))
						|| node2.getName().equalsIgnoreCase(
								Messages.getString("node.DebianFilePackagesNode"))) {
					packListNode[0] = node2;
					b = false;
				}
				if (null == node2.getParentNode()) {
					b = false;
				} else
					node2 = node2.getParentNode();

			}

		}
		try {
			PackageManagerEditPanel paMaEdPaTe = PackageManagerEditPanel.getInstance();
			paMaEdPaTe.init(packListNode[0],activatedNodes,packListNode, new TopComponent());
			paMaEdPaTe.doEdit();
		} catch (PackageManagerException e) {
			e.printStackTrace();
			ErrorManager.getDefault().notify(e);
		}
	}

	/*
	 * @see org.openide.util.actions.NodeAction#enable(org.openide.nodes.Node[])
	 */
	@Override
	protected boolean enable(Node[] activatedNodes) {
		for (Node node : activatedNodes)
			if (node instanceof PackageNode)
				return true;
		return false;

	}

	/*
	 * @see org.openide.util.actions.SystemAction#getName()
	 */
	@Override
	public String getName() {
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

}
