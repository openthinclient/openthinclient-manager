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
public class RealyDeleteAction extends NodeAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public RealyDeleteAction() {
		super();
		setIcon(IconManager.getInstance(getClass(), "icons").getIcon("Refresh")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * @see org.openide.util.actions.NodeAction#performAction(org.openide.nodes.Node[])
	 */
	@Override
	protected void performAction(Node[] activatedNodes) {
		List<Package> deleteList = new ArrayList<Package>();
		Node node2 = null;
		for (Node node : activatedNodes)
			if (node instanceof PackageNode) {
				Package pkg = (Package) ((PackageNode) node).getLookup().lookup(
						Package.class);
				deleteList.add(pkg);
				if (node2 == null)
					node2 = node.getParentNode();

			}
		realyDeletePackages(deleteList, node2);
		deleteList.removeAll(deleteList);
	}

	/**
	 * realy delete the files from the given package list
	 * 
	 * @param deleteList
	 * @param node
	 */
	public static void realyDeletePackages(Collection<Package> deleteList,
			Node node) {

		PackageManagerJobQueue.Job job = new PackageManagerJobQueue.Job(node,
				deleteList) {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.openthinclient.console.nodes.pkgmgr.PackageManagerJobQueue.Job#doJob()
			 */
			@Override
			void doJob() {
				ModifyDialog mody = new ModifyDialog();
				int retValue = mody.shouldPackagesBeUsed(packageList, node.getName());
				if (retValue == 1) {
					loadDialog();
				} else if (retValue == 0) {
					dontWantToInstall();
				} else {
					List<Node> activatedNodes = new ArrayList<Node>();
					for (Node packnode : node.getChildren().getNodes()) {
						for (Package pkg : packageList) {
							if (packnode.getName().equalsIgnoreCase(pkg.getName())){
								activatedNodes.add(packnode);
							}
						}
					}
					Node[] nodeArray = new Node[activatedNodes.size()];
					for (int i = 0; i < activatedNodes.size(); i++)
						nodeArray[i] = activatedNodes.get(i);
					new PackageListNodeActionForPackageNode().performAction(nodeArray);
					packageList = new ArrayList<Package>();
				}
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.openthinclient.console.nodes.pkgmgr.PackageManagerJobQueue.Job#doPMJob()
			 */
			@Override
			Object doPMJob() throws PackageManagerException {
				if (pm.deleteOldPackages(packageList))
					createInformationOptionPane(false);
				else
					throw new PackageManagerException(Messages
							.getString("error.RealyDeleteAction"));
				packageList.removeAll(packageList);

				return null;
			}

		};
		PackageManagerJobQueue.getInstance().addPackageManagerJob(job);

	}

	/*
	 * @see org.openide.util.actions.NodeAction#enable(org.openide.nodes.Node[])
	 */
	private Node[] nodes;

	@Override
	protected boolean enable(Node[] activatedNodes) {
		nodes = activatedNodes;
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
		for (Node node : nodes) {
			if (node.getClass().equals(PackageNode.class))
				return Messages.getString("realyDeleteAction.getName"); //$NON-NLS-1$
		}

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
