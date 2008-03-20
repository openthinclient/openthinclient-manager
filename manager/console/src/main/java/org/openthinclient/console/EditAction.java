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

import javax.swing.SwingUtilities;

import org.openide.ErrorManager;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.NodeAction;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.UnrecognizedClient;
import org.openthinclient.console.nodes.DirObjectNode;
import org.openthinclient.console.ui.DirObjectEditPanel;
import org.openthinclient.ldap.DirectoryException;

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
	protected void performAction(final Node[] nodes) {
		// invoke later to prevent deadlocks - did I mention that NetBeans RCP sucks
		// badly?
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Node[] nodes = MainTreeTopComponent.getDefault().getActivatedNodes();
				for (final Node node : nodes)
					if (node instanceof EditorProvider) {
						final DirectoryObject dirObject = (DirectoryObject) node
								.getLookup().lookup(DirectoryObject.class);

						// reload the object so that we work on a copy.
						final Realm realm = (Realm) node.getLookup().lookup(Realm.class);
						DirectoryObject copy = null;
						try {
							// ensure realm is initialized
							realm.ensureInitialized();
							
							// disable caching!
							copy = realm.getDirectory().load(dirObject.getClass(),
									dirObject.getDn(), true);

							// copy connection descriptor for realm
							if (dirObject instanceof Realm)
								((Realm) copy).setConnectionDescriptor(((Realm) dirObject)
										.getConnectionDescriptor());

							final DetailView editor = ((EditorProvider) node).getEditor();
							editor.init(new Node[]{new DirObjectNode(node.getParentNode(),
									copy)}, MainTreeTopComponent.getDefault());

							final Node parentNode = node.getParentNode();

							// FIXME: UnrecognizedClient EditAction -> NewAction
							if (dirObject.getClass().equals(UnrecognizedClient.class)) {
								new DirObjectEditPanel(editor);
								if (null != parentNode && parentNode instanceof Refreshable)
									((Refreshable) parentNode).refresh();
								final Node[] parentParentNodeList = parentNode.getParentNode()
										.getChildren().getNodes();
								for (final Node topNode : parentParentNodeList)
									if (topNode.getName().equalsIgnoreCase(
											Messages.getString("types.plural.Client"))) {
										((Refreshable) topNode).refresh();
										break;
									}
							} else if (new DirObjectEditPanel(editor).doEdit(copy, node))
								try {
									realm.getDirectory().save(copy);
									// realm.getDn() returns RDN so dirObject.getDn() always not equals copy.getDn()
									if (!dirObject.getDn().equals(copy.getDn())
											&& !(dirObject instanceof Realm)) {
										// DN change. Refresh the parent instead.
										if (null != parentNode && parentNode instanceof Refreshable)
											((Refreshable) parentNode).refresh();
									} else if (node != null && node instanceof Refreshable)
										((Refreshable) node).refresh();
								} catch (final DirectoryException e) {
									ErrorManager.getDefault().notify(e);
								}
						} catch (final DirectoryException e) {
							ErrorManager.getDefault().notify(e);
						}
					}
			}
		});
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
