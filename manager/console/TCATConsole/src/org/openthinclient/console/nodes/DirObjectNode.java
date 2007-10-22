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
import java.beans.IntrospectionException;
import java.io.IOException;

import javax.swing.Action;

import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.actions.DeleteAction;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.ClientLogAction;
import org.openthinclient.console.DetailView;
import org.openthinclient.console.DetailViewProvider;
import org.openthinclient.console.EditAction;
import org.openthinclient.console.EditorProvider;
import org.openthinclient.console.Messages;
import org.openthinclient.console.Refreshable;
import org.openthinclient.console.nodes.views.DirObjectDetailView;
import org.openthinclient.console.nodes.views.DirObjectEditor;
import org.openthinclient.ldap.DirectoryException;

import com.levigo.util.swing.IconManager;

/** Getting the feed node and wrapping it in a FilterNode */
public class DirObjectNode extends AbstractNode
		implements
			DetailViewProvider,
			EditorProvider,
			Refreshable {

	/**
	 * @param node
	 * @param keys
	 * @throws IntrospectionException
	 */
	public DirObjectNode(Node node, DirectoryObject object) {
		super(Children.LEAF, new ProxyLookup(new Lookup[]{
				Lookups.fixed(new Object[]{object}), node.getLookup()}));
	}

	public String getName() {
		return ((DirectoryObject) getLookup().lookup(DirectoryObject.class))
				.getName();
	}

	public Action[] getActions(boolean context) {
		if ((DirectoryObject) getLookup().lookup(DirectoryObject.class) instanceof Client)
			return new Action[]{SystemAction.get(EditAction.class),
					SystemAction.get(DeleteAction.class),
					SystemAction.get(ClientLogAction.class)};
		else
			return new Action[]{SystemAction.get(EditAction.class),
					SystemAction.get(DeleteAction.class)};
	}

	@Override
	public SystemAction getDefaultAction() {
		return SystemAction.get(EditAction.class);
	}

	/*
	 * @see org.openide.nodes.FilterNode#canCopy()
	 */
	@Override
	public boolean canCopy() {
		return true;
	}

	/*
	 * @see org.openide.nodes.FilterNode#canDestroy()
	 */
	@Override
	public boolean canDestroy() {
		// FIXME: ask directory whether this type is mutable
		Class currentClass = (Class) this.getLookup().lookup(Class.class);
		if (!LDAPDirectory.isMutable(currentClass)) {
			return false;
		}
		return true;
	}

	/*
	 * @see org.openide.nodes.Node#destroy()
	 */
	@Override
	public void destroy() throws IOException {
		DirectoryObject object = (DirectoryObject) getLookup().lookup(
				DirectoryObject.class);
		Realm realm = (Realm) getLookup().lookup(Realm.class);

		if (null == realm || null == object)
			throw new IllegalStateException("Don't have a directory or object"); //$NON-NLS-1$

		try {
			realm.getDirectory().delete(object);
			super.destroy();
		} catch (DirectoryException e) {
			ErrorManager.getDefault().annotate(e, ErrorManager.EXCEPTION,
					Messages.getString("DirObjectNode.cantDelete"), null, null, null); //$NON-NLS-1$
			ErrorManager.getDefault().notify(e);
		}
	}

	/*
	 * @see org.openide.nodes.FilterNode#canRename()
	 */
	@Override
	public boolean canRename() {
		Class currentClass = (Class) this.getLookup().lookup(Class.class);
		if (!LDAPDirectory.isMutable(currentClass)) {
			return false;
		}
		return true;
	}

	/*
	 * @see org.openide.nodes.AbstractNode#setName(java.lang.String)
	 */
	@Override
	public void setName(String s) {
		if (null == s || s.length() == 0) {
			DialogDisplayer.getDefault().notify(
					new NotifyDescriptor(
							Messages.getString("DirObjectNode.nameInvalid", s), //$NON-NLS-1$ //$NON-NLS-2$
							Messages.getString("DirObjectNode.cantChangeName"), //$NON-NLS-1$
							NotifyDescriptor.DEFAULT_OPTION, NotifyDescriptor.ERROR_MESSAGE,
							null, null));
			return;
		}

		Node[] nodes = getParentNode().getChildren().getNodes();
		for (Node node : nodes) {
			if (node instanceof DirObjectNode) {
				DirObjectNode don = (DirObjectNode) node;
				DirectoryObject object = (DirectoryObject) don.getLookup().lookup(
						DirectoryObject.class);
				if (null != object && object.getName().equals(s)) {
					DialogDisplayer.getDefault().notify(
							new NotifyDescriptor(
									Messages.getString("DirObjectNode.alreadyExists"), //$NON-NLS-1$
									Messages.getString("DirObjectNode.cantChangeName"), //$NON-NLS-1$
									NotifyDescriptor.DEFAULT_OPTION,
									NotifyDescriptor.ERROR_MESSAGE, null, null));
					return;
				}
			}
		}

		DirectoryObject object = (DirectoryObject) getLookup().lookup(
				DirectoryObject.class);
		Realm realm = (Realm) getLookup().lookup(Realm.class);

		if (null == realm || null == object)
			throw new IllegalStateException("Don't have a directory or object"); //$NON-NLS-1$

		String oldName = object.getName();

		// reload the object so that we work on a copy.
		DirectoryObject copy = null;
		try {
			// disable caching!
			copy = realm.getDirectory().load(object.getClass(), object.getDn(), true);
		} catch (DirectoryException e) {
			ErrorManager.getDefault().notify(e);
		}
		// copy connection descriptor for realm
		if (object instanceof Realm)
			((Realm) copy).setConnectionDescriptor(((Realm) object)
					.getConnectionDescriptor());

		copy.setName(s);

		try {
			realm.getDirectory().save(copy);
			// fireNameChange(oldName, s);

			for (Node node : nodes) {
				if (!object.getDn().equals(copy.getDn())) {
					// DN change. Refresh the parent instead.
					Node parentNode = node.getParentNode();
					if (null != parentNode && parentNode instanceof Refreshable)
						((Refreshable) parentNode).refresh();
				} else if (node instanceof Refreshable)
					((Refreshable) node).refresh();
			}
		} catch (DirectoryException e) {
			e.printStackTrace();

			object.setName(oldName);

			ErrorManager.getDefault().annotate(e, ErrorManager.ERROR, null,
					Messages.getString("DirObjectNode.cantSave"), null, null); //$NON-NLS-1$
			ErrorManager.getDefault().notify(e);
		}
	}

	/*
	 * @see org.openthinclient.console.nodes.MyAbstractNode#getIcon(int)
	 */
	@Override
	public Image getIcon(int type) {
		DirectoryObject o = (DirectoryObject) getLookup().lookup(
				DirectoryObject.class);
		return IconManager.getInstance(DetailViewProvider.class, "icons").getImage( //$NON-NLS-1$
				"tree." + o.getClass().getSimpleName()); //$NON-NLS-1$
	}

	/*
	 * @see org.openthinclient.console.DetailViewProvider#getDetailView()
	 */
	public DetailView getDetailView() {
		return new DirObjectDetailView();
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
	public void refresh(String type) {
		Realm realm = (Realm) getLookup().lookup(Realm.class);
		DirectoryObject o = (DirectoryObject) getLookup().lookup(
				DirectoryObject.class);

		try {
			realm.getDirectory().refresh(o);
			fireCookieChange();
		} catch (DirectoryException e) {
			ErrorManager.getDefault().notify(e);
		}
	}

	public void refresh() {
		// TODO Auto-generated method stub

	}
}
