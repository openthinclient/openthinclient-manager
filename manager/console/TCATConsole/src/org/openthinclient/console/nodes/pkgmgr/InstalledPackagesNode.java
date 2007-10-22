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

import java.awt.Image;
import java.util.Collection;
import javax.swing.Action;
import org.openide.nodes.Node;
import org.openide.util.actions.SystemAction;
import org.openthinclient.console.DetailViewProvider;
import org.openthinclient.util.dpkg.Package;


import com.levigo.util.swing.IconManager;

public class InstalledPackagesNode extends PackageListNode {

	public InstalledPackagesNode(Node parent) {
		super(parent);
	}

	@Override
	protected Collection<Package> getPackageList(PackageManagerDelegation pkgmgr) {
		return pkgmgr.getInstalledPackages();
	}

	@Override
	public Action[] getActions(boolean context) {
		return getActions(context  , SystemAction.get(RefreshPackageManagerValuesAction.class),SystemAction.get(DeleteAction.class),SystemAction.get(PackageListNodeAction.class) );
	}

	public void refresh(String type) {
		super.refresh();
		
	}
	@Override
	public Image getIcon(int type) {
		return getOpenedIcon(type);
	}

	public Image getOpenedIcon(int type) {
		return IconManager.getInstance(DetailViewProvider.class, "icons").getImage( //$NON-NLS-1$
				"tree." + this.getClass().getSimpleName()); //$NON-NLS-1$
	}
	

}
