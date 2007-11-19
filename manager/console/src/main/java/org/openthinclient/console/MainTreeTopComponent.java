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

import java.awt.BorderLayout;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;

import javax.swing.ActionMap;

import org.openide.ErrorManager;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.BeanTreeView;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.Node;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;
import org.openthinclient.console.nodes.RootNode;

public final class MainTreeTopComponent extends TopComponent
		implements
			ExplorerManager.Provider {

	private static MainTreeTopComponent instance;

	private final ExplorerManager manager = new ExplorerManager();

	private final BeanTreeView view = new BeanTreeView();

	Node nodeToExpand;

	private MainTreeTopComponent() {
		setName(Messages.getString("CTL_MainTreeTopComponent"));//$NON-NLS-1$
		setToolTipText(Messages.getString("HINT_MainTreeTopComponent"));//$NON-NLS-1$
		final Image loadImage = Utilities.loadImage(
				"org/openthinclient/console/rss16.gif",//$NON-NLS-1$
				true);
		setIcon(loadImage);
		setLayout(new BorderLayout());
		add(view, BorderLayout.CENTER);
		view.setDefaultActionAllowed(true);
		view.setRootVisible(false);

		try {
			manager.setRootContext(new RootNode());
		} catch (final DataObjectNotFoundException ex) {
			ErrorManager.getDefault().notify(ex);
		}
		final ActionMap map = getActionMap();
		map.put("delete", ExplorerUtils.actionDelete(manager, true));//$NON-NLS-1$
		map.put("copy", ExplorerUtils.actionCopy(manager));//$NON-NLS-1$
		map.put("paste", ExplorerUtils.actionPaste(manager));//$NON-NLS-1$
		map.put("cut", ExplorerUtils.actionCut(manager));//$NON-NLS-1$
		associateLookup(ExplorerUtils.createLookup(manager, map));

		manager.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().equals("exploredContext"))
					// System.out.println("Explored: " + evt.getNewValue());
					view.expandNode((Node) evt.getNewValue());
			}
		});

		manager.setExploredContext(manager.getRootContext().getChildren()
				.getNodes()[0]);
	}

	public static void expandThisNode(Node node) {
		getDefault().view.expandNode(node);
	}

	public static synchronized MainTreeTopComponent getDefault() {
		if (instance == null)
			instance = new MainTreeTopComponent();
		return instance;
	}

	@Override
	public int getPersistenceType() {
		return TopComponent.PERSISTENCE_ALWAYS;
	}

	@Override
	protected String preferredID() {
		return "MainTreeTopComponent";//$NON-NLS-1$
	}

	@Override
	protected Object writeReplace() {
		return new ResolvableHelper();
	}

	private final static class ResolvableHelper implements Serializable {
		private static final long serialVersionUID = 1L;

		public Object readResolve() {
			return MainTreeTopComponent.getDefault();
		}
	}

	public ExplorerManager getExplorerManager() {
		return manager;
	}

	/*
	 * @see org.openide.windows.TopComponent#canClose()
	 */
	@Override
	public boolean canClose() {
		return false;
	}
}
