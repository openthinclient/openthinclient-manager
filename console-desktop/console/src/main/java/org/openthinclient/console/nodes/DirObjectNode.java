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
import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.Action;

import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Device;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.Printer;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.ClientLogAction;
import org.openthinclient.console.DeleteNodeAction;
import org.openthinclient.console.DetailView;
import org.openthinclient.console.DetailViewProvider;
import org.openthinclient.console.DuplicateAction;
import org.openthinclient.console.EditAction;
import org.openthinclient.console.EditorProvider;
import org.openthinclient.console.Messages;
import org.openthinclient.console.OpenVNCConnectionAction;
import org.openthinclient.console.Refreshable;
import org.openthinclient.console.nodes.views.DirObjectDetailView;
import org.openthinclient.console.nodes.views.DirObjectEditor;
import org.openthinclient.ldap.DirectoryException;

import com.levigo.util.swing.IconManager;

/** Getting the feed node and wrapping it in a FilterNode */
public class DirObjectNode extends MyAbstractNode
		implements
			DetailViewProvider,
			EditorProvider,
			Refreshable {

	// list of valid classes to duplicate
	private final Set validDuplicateClassesSet = new HashSet(Arrays.asList(
			Application.class, Device.class, HardwareType.class, Location.class,
			Printer.class));

	/**
	 * @param node
	 * @param keys
	 * @throws IntrospectionException
	 */
	public DirObjectNode(Node node, DirectoryObject object) {
		super(Children.LEAF, new ProxyLookup(new Lookup[]{
				Lookups.fixed(new Object[]{object}), node.getLookup()}));
	}

	@Override
	public String getName() {
		return ((DirectoryObject) getLookup().lookup(DirectoryObject.class))
				.getName();
	}

	@Override
	public Action[] getActions(boolean context) {
		final DirectoryObject dirObject = (DirectoryObject) getLookup().lookup(
				DirectoryObject.class);
		final Class<? extends DirectoryObject> dirObjectClass = dirObject
				.getClass();
		if (dirObjectClass.equals(Client.class)) {
			if (isWritable())
				return new Action[]{SystemAction.get(EditAction.class),
						SystemAction.get(ClientLogAction.class),
						SystemAction.get(OpenVNCConnectionAction.class),
						SystemAction.get(DeleteNodeAction.class)};
			else
				return new Action[]{SystemAction.get(ClientLogAction.class)};
		} else if (isWritable()) {
			if (validDuplicateClassesSet.contains(dirObjectClass))
				return new Action[]{SystemAction.get(EditAction.class),
						SystemAction.get(DuplicateAction.class),
						SystemAction.get(DeleteNodeAction.class)};
			else
				return new Action[]{SystemAction.get(EditAction.class),
						SystemAction.get(DeleteNodeAction.class)};
		} else
			return new Action[]{};

	}

	@Override
	public SystemAction getDefaultAction() {
		if (isWritable())
			return SystemAction.get(EditAction.class);
		else
			return null;
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
		final Class currentClass = (Class) this.getLookup().lookup(Class.class);
		if (!LDAPDirectory.isMutable(currentClass))
			return false;
		return true;
	}

	/*
	 * @see org.openide.nodes.Node#destroy()
	 */
	@Override
	public void destroy() throws IOException {
		final DirectoryObject object = (DirectoryObject) getLookup().lookup(
				DirectoryObject.class);
		final Realm realm = (Realm) getLookup().lookup(Realm.class);

		if (null == realm || null == object)
			throw new IllegalStateException("Don't have a directory or object"); //$NON-NLS-1$

		try {
			realm.getDirectory().delete(object);
			super.destroy();
			final Node parentNode = getParentNode();
		} catch (final DirectoryException e) {
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
		return false;
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

		final Node[] nodes = getParentNode().getChildren().getNodes();
		for (final Node node : nodes)
			if (node instanceof DirObjectNode) {
				final DirObjectNode don = (DirObjectNode) node;
				final DirectoryObject object = (DirectoryObject) don.getLookup()
						.lookup(DirectoryObject.class);
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

		final DirectoryObject object = (DirectoryObject) getLookup().lookup(
				DirectoryObject.class);
		final Realm realm = (Realm) getLookup().lookup(Realm.class);

		if (null == realm || null == object)
			throw new IllegalStateException("Don't have a directory or object"); //$NON-NLS-1$

		final String oldName = object.getName();

		// reload the object so that we work on a copy.
		DirectoryObject copy = null;
		try {
			// disable caching!
			copy = realm.getDirectory().load(object.getClass(), object.getDn(), true);
		} catch (final DirectoryException e) {
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

			// DN change. Refresh the parent.
			final Node parentNode = getParentNode();
			if (null != parentNode && parentNode instanceof Refreshable)
				((Refreshable) parentNode).refresh();

		} catch (final DirectoryException e) {
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
		final DirectoryObject o = (DirectoryObject) getLookup().lookup(
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
	public void refresh() {
		final Realm realm = (Realm) getLookup().lookup(Realm.class);
		final DirectoryObject o = (DirectoryObject) getLookup().lookup(
				DirectoryObject.class);

		try {
			realm.getDirectory().refresh(o);
			fireCookieChange();
		} catch (final DirectoryException e) {
			ErrorManager.getDefault().notify(e);
		}
	}
}
