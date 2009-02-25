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

import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.openthinclient.console.Messages;
import org.openthinclient.console.Refreshable;
import org.openthinclient.pkgmgr.PackageManagerException;

import com.levigo.util.swing.IconManager;

/**
 */
public class RefreshPackageManagerValuesAction extends NodeAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3089422954495094295L;

	public RefreshPackageManagerValuesAction() {
		super();
		setIcon(IconManager.getInstance(getClass(), "icons").getIcon("Refresh")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * @see org.openide.util.actions.NodeAction#performAction(org.openide.nodes.Node[])
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void performAction(Node[] activatedNodes) {
		Node node = null;
		for (final Node tmp : activatedNodes)
			if (tmp instanceof PackageManagementNode
					|| tmp instanceof PackageListNode) {
				node = tmp;
				break;
			}
		final PackageManagerJobQueue.Job job = new PackageManagerJobQueue.Job(node) {

			/*
			 * @see org.openthinclient.console.nodes.pkgmgr.PackageManagerJobQueue.Job#doJob()
			 */
			@Override
			void doJob() {
				// do nothing
				// loadDialog(pkgmgr);
			}

			/*
			 * @see org.openthinclient.console.nodes.pkgmgr.PackageManagerJobQueue.Job#doPMJob()
			 */
			@Override
			Object doPMJob() throws PackageManagerException {
				int what;
				if (node instanceof PackageManagementNode)
					what = REFRESH_ALL_PACKAGES;
				else if (node instanceof InstalledPackagesNode)
					what = REFRESH_INSTALLED_PACKAGES;
				else if (node instanceof AvailablePackagesNode)
					what = REFRESH_INSTALLABLE_PACKAGES;
				else if (node instanceof UpdatablePackagesNode)
					what = REFRESH_UPDATEABLE_PACKAGES;
				else if (node instanceof AlreadyDeletedPackagesNode)
					what = REFRESH_REMOVED_PACKAGES;
				else if (node instanceof DebianFilePackagesNode)
					what = REFRESH_DEBIAN_PACKAGES;
				else
					// FIXME
					throw new PackageManagerException("bad error requested!");
				pkgmgr.refresh(what);
				return null;
			}
		};
		PackageManagerJobQueue.getInstance().addPackageManagerJob(job);
		for (final Node tmpnode : activatedNodes)
			if (tmpnode instanceof Refreshable)
				((Refreshable) tmpnode).refresh();
	}

	/*
	 * @see org.openide.util.actions.NodeAction#enable(org.openide.nodes.Node[])
	 */
	private Node[] nodes;

	@Override
	protected boolean enable(Node[] activatedNodes) {
		nodes = activatedNodes;
		for (final Node node : activatedNodes)
			if (node instanceof PackageManagementNode
					|| node instanceof PackageListNode)
				return true;
		return false;
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getName()
	 */
	@Override
	public String getName() {
		for (final Node node : nodes)
			if (node instanceof PackageManagementNode
					|| node instanceof PackageListNode)
				return Messages.getString("RefreshAction.name"); //$NON-NLS-1$
		return "";

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
