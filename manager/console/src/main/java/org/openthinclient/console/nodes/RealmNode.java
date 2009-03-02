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
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.openide.ErrorManager;
import org.openide.nodes.Children;
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
import org.openthinclient.console.DisconnectEnvironmentAction;
import org.openthinclient.console.EditAction;
import org.openthinclient.console.EditorProvider;
import org.openthinclient.console.HTTPLdifImportAction;
import org.openthinclient.console.MainTreeTopComponent;
import org.openthinclient.console.Messages;
import org.openthinclient.console.RefreshAction;
import org.openthinclient.console.Refreshable;
import org.openthinclient.console.ServerLogAction;
import org.openthinclient.console.nodes.pkgmgr.PackageManagementNode;
import org.openthinclient.console.nodes.views.DirObjectDetailView;
import org.openthinclient.console.nodes.views.DirObjectEditor;
import org.openthinclient.console.nodes.views.RealmConnectionErrorView;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.TypeMapping;

import com.levigo.util.swing.IconManager;

public class RealmNode extends MyAbstractNode
		implements
			DetailViewProvider,
			EditorProvider,
			Refreshable {
	public static Class CHILD_NODE_CLASSES[] = new Class[]{DirObjectsNode.class};

	private static final Logger logger = Logger.getLogger(TypeMapping.class);

	private final Realm realm;

	private boolean actionLocked = false;
	private Exception exception;

	/**
	 * @param realm
	 * @param hideChildren TODO
	 */
	public RealmNode(Realm realm, boolean hideChildren) {
		super(hideChildren ? Children.LEAF : new Children.Array(), Lookups
				.fixed(new Object[]{realm}));
		this.realm = realm;
	}

	public RealmNode(Node parent, Realm realm) {
		super(new Children.Array(), new ProxyLookup(new Lookup[]{
				Lookups.fixed(new Object[]{realm}), parent.getLookup()}));

		this.realm = realm;
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

	private void createChildren(Node child) {
		final List<Node> children = new ArrayList<Node>();
		try {
			children.add(child);
		} catch (final Exception e) {
			ErrorManager.getDefault().notify(e);
			children.add(new ErrorNode(Messages
					.getString("error.ChildCreationFailed.dirObjects"), e)); //$NON-NLS-1$
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
		if (this.actionLocked == false)
			return new Action[]{SystemAction.get(EditAction.class),
					SystemAction.get(ServerLogAction.class),
					SystemAction.get(DisconnectEnvironmentAction.class),
					SystemAction.get(DeleteRealmAction.class), null,
					SystemAction.get(RefreshAction.class)};
		else
			return new Action[]{SystemAction.get(RefreshAction.class),
					SystemAction.get(DisconnectEnvironmentAction.class)};
	}

	/*
	 * @see org.openthinclient.console.DetailViewProvider#getDetailView()
	 */
	public DetailView getDetailView() {
		if (this.actionLocked == false)
			return new DirObjectDetailView();
		else
			return new RealmConnectionErrorView(exception);
	}

	/*
	 * @see org.openide.nodes.FilterNode#getIcon(int)
	 */
	@Override
	public Image getIcon(int type) {
		return IconManager.getInstance(DetailViewProvider.class, "icons").getImage( //$NON-NLS-1$
				"tree." + getClass().getSimpleName()); //$NON-NLS-1$

		// FIXME: Why not?
		// return IconManager.getInstance(DetailViewProvider.class,
		// "icons").getImage(
		// "tree." + getClass().getSimpleName(), IconManager.EFFECT_GRAY50P);
	}

	/*
	 * @see org.openide.nodes.FilterNode#getOpenedIcon(int)
	 */
	@Override
	public Image getOpenedIcon(int type) {
		if (getChildren().getNodes().length == 0)
			loadChildren();
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
		if (this.actionLocked == true) {
			this.actionLocked = false;
			loadChildren();
		}

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

	public void updateOnLdifs() {
		final Realm realm = (Realm) getLookup().lookup(Realm.class);
		try {

			realm.getDirectory().refresh(realm);

		} catch (final DirectoryException e) {
			logger.error("Could not import", e);
			ErrorManager.getDefault().annotate(e, "Could not import");
			ErrorManager.getDefault().notify(e);
			e.printStackTrace();
		}
		try {

			final HTTPLdifImportAction action = new HTTPLdifImportAction(realm
					.getConnectionDescriptor().getHostname());

			if (HTTPLdifImportAction.isEnableAsk())
				action.importAllLdifFolder(null, realm);
			HTTPLdifImportAction.setEnableAsk(true);

		} catch (final MalformedURLException e) {
			logger.error("Could not import", e);
			ErrorManager.getDefault().annotate(e, "Could not import");
			ErrorManager.getDefault().notify(e);

		} catch (final IOException e) {
			logger.error("Could not import", e);
			ErrorManager.getDefault().annotate(e, "Could not import");
			ErrorManager.getDefault().notify(e);
			e.printStackTrace();
		}
	}

	public void loadChildren() {
		MainTreeTopComponent.expandThisNode(this);
		if (this.actionLocked == true)
			return;
		try {
			LDAPDirectory.assertBaseDNReachable(realm.getConnectionDescriptor());
			realm.getDirectory().refresh(realm);
		} catch (final Exception e) {
			createErrorNode(e);
			return;
		}

		SwingUtilities.invokeLater(new Runnable() {

			final Realm needed = realm;

			public void run() {

				createChildren(Node.EMPTY, needed);
			}
		});
		updateOnLdifs();
	}

	private void createErrorNode(Exception e) {
		this.exception = e;
		this.actionLocked = true;
		final RealmNode thisNode = this;
		e.printStackTrace();
		logger.error("Can't load Realm", e);

		final Node c = new ErrorNode(Messages.getString("RealmsNode.cantDisplay"),
				e) {
		};

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				thisNode.createChildren(c);
			}
		});
	}
}
