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
import java.util.Properties;

import javax.swing.Action;

import org.openide.ErrorManager;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.actions.SystemAction;
import org.openthinclient.common.model.Realm;
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

	private PackageManagerDelegation packageManagerDelegation;

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

	/**
	 * Connect to schemaprovider, hostname or localhost to instantiate a new
	 * PackageManagerDelegation if not already done
	 * 
	 * @return PackageManagerDelegation
	 */
	public PackageManagerDelegation getPackageManagerDelegation() {
		if (null == packageManagerDelegation) {
			String homeServer = null;
			try {
				final Properties p = new Properties();
				final Realm realm = (Realm) getLookup().lookup(Realm.class);

				if (null != realm.getSchemaProviderName())
					homeServer = realm.getSchemaProviderName();
				else if (null != realm.getConnectionDescriptor().getHostname())
					homeServer = realm.getConnectionDescriptor().getHostname();
				else
					homeServer = "localhost";
				p.setProperty("java.naming.factory.initial",
						"org.jnp.interfaces.NamingContextFactory");
				p.setProperty("java.naming.provider.url", "jnp://" + homeServer
						+ ":1099");
				packageManagerDelegation = new PackageManagerDelegation(p);
				return packageManagerDelegation;
			} catch (final Exception e) {
				// e.printStackTrace();
				// ErrorManager
				// .getDefault()
				// .annotate(
				// e,
				// Messages
				// .getString(
				// "node.PackageManagementNode.createPackageManager.ServerNotFound",
				// homeServer));
				// ErrorManager.getDefault().notify(e);
				ErrorManager.getDefault().notify(e);
				return null;
			}
		} else
			return packageManagerDelegation;
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
