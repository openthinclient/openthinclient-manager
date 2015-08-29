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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.openthinclient.console.Messages;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.util.dpkg.Package;

import com.levigo.util.swing.IconManager;

/**
 */
public class UpdateAction extends NodeAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public UpdateAction() {
		super();
		setIcon(IconManager.getInstance(getClass(), "icons").getIcon("Refresh")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * @see
	 * org.openide.util.actions.NodeAction#performAction(org.openide.nodes.Node[])
	 */
	@Override
	protected void performAction(Node[] activatedNodes) {
		final List<Package> updateList = new ArrayList<Package>();
		Node node2 = null;
		for (final Node node : activatedNodes)
			if (node instanceof PackageNode) {
				final Package pkg = (Package) ((PackageNode) node).getLookup().lookup(
						Package.class);
				if (node2 == null)
					node2 = node.getParentNode();
				updateList.add(pkg);
			}
		updatePackages(node2, updateList);
		updateList.remove(updateList);
		for (Node node : activatedNodes)
			while (node != null) {
				if (node instanceof PackageNode)
					((PackageNode) node).refresh();

				node = node.getParentNode();
			}
	}

	/**
	 * update the given package list
	 * 
	 * @param node
	 * @param updateList
	 */
	public static void updatePackages(Node node, Collection<Package> updateList) {

		final PackageManagerJob job = new PackageManagerJob(node,
				updateList) {
			/*
			 * @see
			 * org.openthinclient.console.nodes.pkgmgr.PackageManagerJobQueue.Job#
			 * doJob()
			 */
			@Override
			void doJob() {
				final ModifyDialog mody = new ModifyDialog();
				if (mody.shouldPackagesBeUsed(packageList, node.getName())) {
					loadDialog(pkgmgr);
					// TODO: Why is that needed?
					pkgmgr.removeConflicts();
					pkgmgr.refreshSolveDependencies();
				} else
					dontWantToInstall();
			}

			/*
			 * @see
			 * org.openthinclient.console.nodes.pkgmgr.PackageManagerJobQueue.Job#
			 * doPMJob()
			 */
			@Override
			Object doPMJob() throws PackageManagerException {
				if (pkgmgr.update(packageList))
					createInformationOptionPane(true);
				else
					throw new PackageManagerException(
							Messages.getString("error.UpdateAction"));

				packageList.removeAll(packageList);
				return null;
			}

		};
		PackageManagerJobQueue.getInstance().addPackageManagerJob(job);

		((PackageListNode) node).refresh();
	}

	/*
	 * @see org.openide.util.actions.NodeAction#enable(org.openide.nodes.Node[])
	 */
	private Node[] nodes;

	@Override
	protected boolean enable(Node[] activatedNodes) {
		nodes = activatedNodes;
		for (final Node node : activatedNodes)
			if (node instanceof PackageNode)
				return true;
		return false;
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getName()
	 */
	@Override
	public String getName() {
		for (final Node node : nodes)
			if (node.getClass().equals(PackageNode.class))
				return Messages.getString("updateAction.getName"); //$NON-NLS-1$

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
