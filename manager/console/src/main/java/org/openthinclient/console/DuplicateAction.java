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
package org.openthinclient.console;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.openide.ErrorManager;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.Properties;
import org.openthinclient.common.model.Realm;

/**
 * @author levigo
 */

public class DuplicateAction extends NodeAction {

	public DuplicateAction() {
		super();
	}

	@Override
	protected void performAction(Node[] arg0) {
		final Set<Node> parentNodesToRefresh = new HashSet<Node>();
		for (final Node node : arg0) {
			final DirectoryObject dirObject = (DirectoryObject) node.getLookup()
					.lookup(DirectoryObject.class);
			final Realm realm = (Realm) node.getLookup().lookup(Realm.class);

			try {
				final DirectoryObject copyObj = dirObject.getClass().newInstance();
				final String duplicateName = Messages.getString("DuplicateOf",
						dirObject.getName());
				copyObj.setName(duplicateName);

				// copy profile
				if (dirObject instanceof Profile) {
					((Profile) copyObj).getProperties().setDescription(
							((Profile) dirObject).getProperties().getDescription());

					final Properties props = ((Profile) dirObject).getProperties();
					final SortedMap<String, String> pmap = props.getMap();
					for (final Map.Entry<String, String> e : pmap.entrySet())
						((Profile) copyObj).setValue(e.getKey(), e.getValue());
				}

				// copy description
				copyObj.setDescription(dirObject.getDescription());

				// save new directory object
				realm.getDirectory().save(copyObj);

				final Node parentNode = node.getParentNode();
				if (null != parentNode && parentNode instanceof Refreshable)
					parentNodesToRefresh.add(parentNode);

			} catch (final Exception e) {
				e.printStackTrace();
				ErrorManager.getDefault().notify(e);
			}
		}

		for (final Iterator iterator = parentNodesToRefresh.iterator(); iterator
				.hasNext();) {
			final Node node = (Node) iterator.next();
			((Refreshable) node).refresh();
		}
	}

	@Override
	protected boolean enable(Node[] activatedNodes) {
		for (final Node node : activatedNodes) {
			final Class currentClass = (Class) node.getLookup().lookup(Class.class);
			if (!LDAPDirectory.isMutable(currentClass))
				return false;
		}
		return true;
	}

	@Override
	public String getName() {
		return Messages.getString("action." + this.getClass().getSimpleName());
	}

	@Override
	public HelpCtx getHelpCtx() {
		return null;
	}
}
