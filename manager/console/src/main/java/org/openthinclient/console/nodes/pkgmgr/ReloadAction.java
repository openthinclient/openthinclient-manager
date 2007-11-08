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

import java.util.Collections;

import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.openthinclient.console.Messages;
import org.openthinclient.pkgmgr.PackageManagerException;

import com.levigo.util.swing.IconManager;

/**
 */
public class ReloadAction extends NodeAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3089422954495094295L;

	public ReloadAction() {
		super();
		setIcon(IconManager.getInstance(getClass(), "icons").getIcon("Refresh")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * @see org.openide.util.actions.NodeAction#performAction(org.openide.nodes.Node[])
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void performAction(Node[] activatedNodes) {
		Node actuallNode = null;
		for (Node node : activatedNodes)
			if (node instanceof PackageManagementNode)
				actuallNode = node;
		PackageManagerJobQueue.Job job = new PackageManagerJobQueue.Job(
				actuallNode, Collections.EMPTY_LIST) {
			/*
			 * @see org.openthinclient.console.nodes.pkgmgr.PackageManagerJobQueue.Job#doJob()
			 */

			@Override
			void doJob() {
				loadDialog();
			}

			/*
			 * 
			 * @see org.openthinclient.console.nodes.pkgmgr.PackageManagerJobQueue.Job#doPMJob()
			 */
			@Override
			Object doPMJob() throws PackageManagerException {
				if(pm.updateCacheDB())
				createInformationOptionPane(false);
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
			if (node instanceof PackageManagementNode)
				return true;
		return false;
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getName()
	 */
	@Override
	public String getName() {
		for (Node node : nodes) {
			if (node instanceof PackageManagementNode)
				return Messages.getString("reloadAction.getName"); //$NON-NLS-1$
		}
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
