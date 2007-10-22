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
package org.openthinclient.console.nodes;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import org.openide.ErrorManager;
import org.openide.actions.DeleteAction;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.nodes.Children.Array;
import org.openide.util.Lookup;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.DeleteRealmAction;
import org.openthinclient.console.DetailView;
import org.openthinclient.console.DetailViewProvider;
import org.openthinclient.console.EditAction;
import org.openthinclient.console.EditorProvider;
import org.openthinclient.console.MainTreeTopComponent;
import org.openthinclient.console.Messages;
import org.openthinclient.console.RefreshAction;
import org.openthinclient.console.Refreshable;
import org.openthinclient.console.ServerLogAction;
import org.openthinclient.console.nodes.pkgmgr.PackageManagementNode;
import org.openthinclient.console.nodes.views.DirObjectDetailView;
import org.openthinclient.console.nodes.views.DirObjectEditor;
import org.openthinclient.ldap.DirectoryException;

import com.levigo.util.swing.IconManager;

public class RealmNode extends FilterNode
		implements
			DetailViewProvider,
			EditorProvider,
			Refreshable {
	public static Class CHILD_NODE_CLASSES[] = new Class[]{DirObjectsNode.class};

	// wird gebraucht f�r komplexere Struktur (post-V1.0)

	// public static Class CHILD_NODE_CLASSES[] = new
	// Class[]{DirObjectsNode.class,
	// RealmServerNode.class, ManagementNode.class, PackageManagementNode.class};

	// private static class Children extends AbstractAsyncArrayChildren {
	// /*
	// * @see
	// org.openthinclient.console.nodes.AbstractAsyncArrayChildren#asyncInitChildren()
	// */
	// @Override
	// protected void asyncInitChildren() {
	// try {
	// Realm realm = (Realm) getNode().getLookup().lookup(Realm.class);
	// realm.refresh();
	//
	// removeAllChildren();
	//
	// Object constructorArgs[] = new Object[]{getNode()};
	// for (Class c : CHILD_NODE_CLASSES) {
	// try {
	// Class<? extends Node> nodeClass = c;
	// Constructor<? extends Node> constructor = nodeClass
	// .getConstructor(new Class[]{Node.class});
	// if (null == constructor)
	// add(new Node[]{new ErrorNode("Kann Kind f�r Klasse " + c
	// + " nicht erzeugen")});
	// else
	// add(new Node[]{constructor.newInstance(constructorArgs)});
	// } catch (Exception e) {
	// ErrorManager.getDefault().notify(e);
	// add(new Node[]{new ErrorNode("Kann Kind nicht erzeugen.", e)});
	// }
	// }
	// } catch (Exception e) {
	// removeAllChildren();
	// ErrorManager.getDefault().notify(e);
	// add(new Node[]{new ErrorNode("Kann Umgebung nicht �ffnen.", e)});
	// }
	// }
	// }

	public RealmNode(Node dataNode, Node parent, Realm realm) {
		super(dataNode, /* new Children() */
		new Children.Array(), new ProxyLookup(new Lookup[]{
				Lookups.fixed(new Object[]{realm}), parent.getLookup()}));

		createChildren(parent, realm);

		disableDelegation(DELEGATE_GET_DISPLAY_NAME);
	}

	/**
	 * @param realm
	 * @param hideChildren TODO
	 */
	public RealmNode(Realm realm, boolean hideChildren) {
		super(Node.EMPTY, hideChildren ? Children.LEAF : new Children.Array(),
				Lookups.fixed(new Object[]{realm}));

		if (!hideChildren)
			createChildren(Node.EMPTY, realm);

		disableDelegation(DELEGATE_GET_DISPLAY_NAME);
	}

	/**
	 * @param parent
	 * @param realm
	 */
	private void createChildren(Node parent, Realm realm) {
		try {
			List<Node> children = new ArrayList<Node>();
			for (Node node : DirObjectsNode.createChildren(this))
				children.add(node);
			// children.addAll(children);
			children.add(new DirectoryViewNode(parent, realm
					.getConnectionDescriptor(), ""));

			try {
				LDAPDirectory dir = realm.getDirectory();
				if (LDAPDirectory.isSettingFine()) {
					children.add(new SecondaryDirectoryViewNode(parent, LDAPDirectory
							.createNewConnection(), ""));
				}
			} catch (Exception e) {
				// FIXME
				e.printStackTrace();
			}

			children.add(new PackageManagementNode(this));
			Array c = new org.openide.nodes.Children.Array();
			c.add(children.toArray(new Node[children.size()]));

			setChildren(c);
		} catch (Exception e) {
			ErrorManager.getDefault().notify(e);
			getChildren().add(
					new Node[]{new ErrorNode(Messages
							.getString("error.ChildCreationFailed"), e)}); //$NON-NLS-1$
		}
	}

	public String getName() {
		Realm realm = (Realm) getLookup().lookup(Realm.class);
		String description = realm.getDescription();
		if (description == null) {
			return realm.getShortName();
		} else if (description.equals(""))
			return realm.getShortName();
		else {
			return realm.getShortName() + " (" + realm.getDescription() + ")"; //$NON-NLS-1$ //$NON-NLS-2$;
		}
	}

	/*
	 * @see org.openide.nodes.FilterNode#getShortDescription()
	 */
	@Override
	public String getShortDescription() {
		return ((Realm) getLookup().lookup(Realm.class)).getDn();
	}

	public Action[] getActions(boolean context) {
		return new Action[]{SystemAction.get(EditAction.class),
				SystemAction.get(RefreshAction.class),
				SystemAction.get(DeleteAction.class),
				SystemAction.get(DeleteRealmAction.class),
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
		MainTreeTopComponent.expandThisNode(this);
		Realm realm = (Realm) getLookup().lookup(Realm.class);
		try {
			// FIXME ?
			createChildren(this.getParentNode(), realm);
			realm.refresh();
			fireCookieChange();
		} catch (DirectoryException e) {
			ErrorManager.getDefault().notify(e);
		}
	}
}
