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

import java.awt.Image;
import java.io.IOException;

import javax.swing.Action;

import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.actions.SystemAction;
import org.openthinclient.console.DetailView;
import org.openthinclient.console.DetailViewProvider;
import org.openthinclient.console.Refreshable;
import org.openthinclient.console.nodes.MyAbstractNode;

import com.levigo.util.swing.IconManager;

/** Getting the root node */
public class PackageManagementNode extends MyAbstractNode
		implements
			Refreshable,
			DetailViewProvider {

	public PackageManagementNode(Node parent) {
		// super(createPackageManager ? Children.LEAF : new Children.Array(),
		// Lookups
		// .fixed(new Object[]{realm}));
		//
		super(new Children.Array(), parent.getLookup());

		getChildren().add(
				new Node[]{new InstalledPackagesNode(this),
						new AvailablePackagesNode(this), new UpdatablePackagesNode(this),
						new AlreadyDeletedPackagesNode(this),
						new DebianFilePackagesNode(this)});
	}

	public void refresh() {
		for (final Node n : getChildren().getNodes())
			if (n instanceof Refreshable)
				((PackageListNode) n).refresh();
	}

	public void refresh(String type) {
	}

	@Override
	public Image getIcon(int type) {
		return getOpenedIcon(type);
	}

	@Override
	public Image getOpenedIcon(int type) {
		return IconManager.getInstance(DetailViewProvider.class, "icons").getImage( //$NON-NLS-1$
				"tree." + getClass().getSimpleName()); //$NON-NLS-1$
	}

	public DetailView getDetailView() {
		return PackageManagementView.getInstance();
	}

	@Override
	public Action[] getActions(boolean arg0) {
		return new Action[]{
				SystemAction.get(RefreshPackageManagerValuesAction.class),
				SystemAction.get(ReloadAction.class)};
	}

	@Override
	public void destroy() throws IOException {
		super.destroy();
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}

}
