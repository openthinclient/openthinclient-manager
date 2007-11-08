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
package org.openthinclient.console;

import org.openide.ErrorManager;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.nodes.DirObjectNode;
import org.openthinclient.console.ui.DirObjectEditPanel;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.TypeMapping;


/**
 * @author bohnerne
 */
public class EditAction extends NodeAction {
	/*
	 * @see org.openide.util.actions.CallableSystemAction#asynchronous()
	 */
	@Override
	protected boolean asynchronous() {
		return true;
	}

	/*
	 * @see org.openide.util.actions.NodeAction#performAction(org.openide.nodes.Node[])
	 */
	@Override
	protected void performAction(Node[] nodes) {

		TypeMapping.setIsNewAction(false);

		// Node[] nodes = MainTreeTopComponent.getDefault().getActivatedNodes();
		for (Node node : nodes) {
			if (node instanceof EditorProvider) {
				final DirectoryObject dirObject = (DirectoryObject) node.getLookup()
						.lookup(DirectoryObject.class);

				// reload the object so that we work on a copy.
				Realm realm = (Realm) node.getLookup().lookup(Realm.class);
				DirectoryObject copy = null;
				try {
					// disable caching!
					copy = realm.getDirectory().load(dirObject.getClass(),
							dirObject.getDn(), true);
				} catch (DirectoryException e) {
					ErrorManager.getDefault().notify(e);
				}

				// copy connection descriptor for realm
				if (dirObject instanceof Realm)
					((Realm) copy).setConnectionDescriptor(((Realm) dirObject)
							.getConnectionDescriptor());

				DetailView editor = ((EditorProvider) node).getEditor();
				editor.init(new Node[]{new DirObjectNode(node.getParentNode(), copy)},
						MainTreeTopComponent.getDefault());

				if (new DirObjectEditPanel(editor).doEdit(copy, node)) {
					try {
						realm.getDirectory().save(copy);

						if (!dirObject.getDn().equals(copy.getDn())) {
							// DN change. Refresh the parent instead.
							Node parentNode = node.getParentNode();
							if (null != parentNode && parentNode instanceof Refreshable)
								((Refreshable) parentNode).refresh();
						} else if (node instanceof Refreshable)
							((Refreshable) node).refresh();
					} catch (DirectoryException e) {
						ErrorManager.getDefault().notify(e);
					}
				}
			}
		}
	}

	/*
	 * @see org.openide.util.actions.NodeAction#enable(org.openide.nodes.Node[])
	 */
	@Override
	protected boolean enable(Node[] arg0) {
		return true;
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getName()
	 */
	@Override
	public String getName() {
		return Messages.getString("action." + this.getClass().getSimpleName()); //$NON-NLS-1$
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getHelpCtx()
	 */
	@Override
	public HelpCtx getHelpCtx() {
		return null;
	}
}
