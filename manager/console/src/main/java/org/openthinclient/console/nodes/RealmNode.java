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
package org.openthinclient.console.nodes;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.openide.ErrorManager;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.Children.Array;
import org.openide.util.Lookup;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.DetailView;
import org.openthinclient.console.DetailViewProvider;
import org.openthinclient.console.EditAction;
import org.openthinclient.console.EditorProvider;
import org.openthinclient.console.Messages;
import org.openthinclient.console.RefreshAction;
import org.openthinclient.console.Refreshable;
import org.openthinclient.console.ServerLogAction;
import org.openthinclient.console.nodes.pkgmgr.PackageManagementNode;
import org.openthinclient.console.nodes.views.DirObjectDetailView;
import org.openthinclient.console.nodes.views.DirObjectEditor;
import org.openthinclient.ldap.DirectoryException;

import com.levigo.util.swing.IconManager;

public class RealmNode extends MyAbstractNode
		implements
			DetailViewProvider,
			EditorProvider,
			Refreshable {
	public static Class CHILD_NODE_CLASSES[] = new Class[]{DirObjectsNode.class};

	/**
	 * @param realm
	 * @param hideChildren TODO
	 */
	public RealmNode(Realm realm, boolean hideChildren) {
		super(hideChildren ? Children.LEAF : new Children.Array(), Lookups
				.fixed(new Object[]{realm}));

		if (!hideChildren)
			createChildren(Node.EMPTY, realm);
	}

	public RealmNode(Node parent, Realm realm) {
		super(new Children.Array(), new ProxyLookup(new Lookup[]{
				Lookups.fixed(new Object[]{realm}), parent.getLookup()}));

		createChildren(parent, realm);
	}

	/**
	 * @param parent
	 * @param realm
	 */
	private void createChildren(Node parent, Realm realm) {
		final List<Node> children = new ArrayList<Node>();
		try {
			for (final Node node : DirObjectsNode.createChildren(this))
				children.add(node);
		} catch (final Exception e) {
			ErrorManager.getDefault().notify(e);
			children.add(new ErrorNode(Messages
					.getString("error.ChildCreationFailed.dirObjects"), e)); //$NON-NLS-1$
		}

		try {
			children.add(new DirectoryViewNode(parent, realm
					.getConnectionDescriptor(), ""));
		} catch (final Exception e) {
			ErrorManager.getDefault().notify(e);
			children.add(new ErrorNode(Messages
					.getString("error.ChildCreationFailed.dirView"), e)); //$NON-NLS-1$
		}

		// FIXME: refactor secondary conection handling:
		// - Secondary node is visible only if there IS a secondary directory
		// - Realm returns two LCDs, one for the primary, one for the secondary DS
		// - the same type of view node is used to visualize the two
		//
		// try {
		// children.add(new SecondaryDirectoryViewNode(parent,
		// realm.getDirectory()
		// .createNewConnection(), ""));
		// } catch (Exception e) {
		// // FIXME
		// e.printStackTrace();
		// }

		try {
			children.add(new PackageManagementNode(this));
		} catch (final Exception e) {
			ErrorManager.getDefault().notify(e);
			children.add(new ErrorNode(Messages
					.getString("error.ChildCreationFailed.pkgMgr"), e)); //$NON-NLS-1$
		}

		final Array c = new org.openide.nodes.Children.Array();
		c.add(children.toArray(new Node[children.size()]));

		setChildren(c);
	}

	@Override
	public String getName() {
		final Realm realm = (Realm) getLookup().lookup(Realm.class);
		if (null != realm.getDescription())
			return realm.getDescription();

		String base = realm.getConnectionDescriptor().getBaseDN();

		int idx = base.indexOf(',');
		if (idx > 0)
			base = base.substring(0, idx);

		idx = base.lastIndexOf('=');
		if (idx < 0)
			return "???";

		return base.substring(idx + 1);
	}

	/*
	 * @see org.openide.nodes.FilterNode#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return ((Realm) getLookup().lookup(Realm.class)).getDn();
	}

	@Override
	public Action[] getActions(boolean context) {
		return new Action[]{SystemAction.get(EditAction.class),
				SystemAction.get(RefreshAction.class),
				// FIXME: enable when fixed
				// SystemAction.get(DeleteAction.class),
				// SystemAction.get(DeleteRealmAction.class),
				SystemAction.get(ServerLogAction.class)};
	}

	/*
	 * @see org.openthinclient.console.DetailViewProvider#getDetailView()
	 */
	public DetailView getDetailView() {
		return new DirObjectDetailView();
	}

	/*
	 * @see org.openide.nodes.FilterNode#getIcon(int)
	 */
	@Override
	public Image getIcon(int type) {
		return getOpenedIcon(type);
		// return IconManager.getInstance(DetailViewProvider.class,
		// "icons").getImage(
		// "tree." + getClass().getSimpleName(), IconManager.EFFECT_GRAY50P);
	}

	/*
	 * @see org.openide.nodes.FilterNode#getOpenedIcon(int)
	 */
	@Override
	public Image getOpenedIcon(int type) {
		return IconManager.getInstance(DetailViewProvider.class, "icons").getImage( //$NON-NLS-1$
				"tree." + getClass().getSimpleName()); //$NON-NLS-1$
	}

	@Override
	public SystemAction getDefaultAction() {
		return SystemAction.get(EditAction.class);
	}

	/*
	 * @see org.openthinclient.console.EditorProvider#getEditor()
	 */
	public DetailView getEditor() {
		return new DirObjectEditor();
	}

	/*
	 * @see org.openthinclient.console.Refreshable#refresh()
	 */
	public void refresh() {
		// MainTreeTopComponent.expandThisNode(this);
		final Realm realm = (Realm) getLookup().lookup(Realm.class);
		try {
			// FIXME: deadlock if enabled
			// createChildren(this.getParentNode(), realm);
			realm.refresh();

			for (final Node node : getChildren().getNodes())
				if (node instanceof Refreshable)
					((Refreshable) node).refresh();

			fireCookieChange();
		} catch (final DirectoryException e) {
			ErrorManager.getDefault().notify(e);
		}
	}
}
