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

import javax.swing.BorderFactory;
import javax.swing.JComponent;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.gradient.BasicGradientPainter;
import org.openide.ErrorManager;
import org.openide.nodes.Node;
import org.openide.nodes.NodeEvent;
import org.openide.nodes.NodeListener;
import org.openide.nodes.NodeMemberEvent;
import org.openide.nodes.NodeReorderEvent;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;

final public class DetailViewTopComponent extends TopComponent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private class MyNodeListener implements NodeListener {
		/*
		 * @see org.openide.nodes.NodeListener#childrenAdded(org.openide.nodes.NodeMemberEvent)
		 */
		public void childrenAdded(NodeMemberEvent arg0) {
			updateDetailView(currentDetailViewProvider, lastTopComponent
					.getActivatedNodes(), lastTopComponent);
		}

		/*
		 * @see org.openide.nodes.NodeListener#childrenRemoved(org.openide.nodes.NodeMemberEvent)
		 */
		public void childrenRemoved(NodeMemberEvent arg0) {
			updateDetailView(currentDetailViewProvider, lastTopComponent
					.getActivatedNodes(), lastTopComponent);
		}

		/*
		 * @see org.openide.nodes.NodeListener#childrenReordered(org.openide.nodes.NodeReorderEvent)
		 */
		public void childrenReordered(NodeReorderEvent arg0) {
			updateDetailView(currentDetailViewProvider, lastTopComponent
					.getActivatedNodes(), lastTopComponent);
		}

		/*
		 * @see org.openide.nodes.NodeListener#nodeDestroyed(org.openide.nodes.NodeEvent)
		 */
		public void nodeDestroyed(NodeEvent arg0) {
			detachDetailView();

			revalidate();
			repaint();
		}

		/*
		 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
		 */
		public void propertyChange(PropertyChangeEvent evt) {
			updateDetailView(currentDetailViewProvider, lastTopComponent
					.getActivatedNodes(), lastTopComponent);
		}
	}

	/**
	 * 
	 */
	private static final int MAX_TITLE_LENGTH = 60;
	private static DetailViewTopComponent instance;
	private final PropertyChangeListener propertyChangeListener;
	private final MyNodeListener listener = new MyNodeListener();
	private TopComponent lastTopComponent;
	private DetailViewProvider currentDetailViewProvider;

	private DetailViewTopComponent() {
		setName(Messages.getString("DetailViewTopComponent.name")); //$NON-NLS-1$
		final Image loadImage = Utilities.loadImage(
				"org/openthinclient/console/rss16.gif", //$NON-NLS-1$
				true);
		setIcon(loadImage);
		setLayout(new BorderLayout());
		setBorder(null);

		propertyChangeListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().equals(Registry.PROP_CURRENT_NODES)) {
					final Object src = evt.getSource();
					nodeSelectionChanged(evt.getNewValue(), src instanceof Registry
							? ((Registry) src).getActivated()
							: null);
				}
			}
		};

	}

	/*
	 * @see org.openide.windows.TopComponent#componentOpened()
	 */
	@Override
	protected void componentOpened() {
		getRegistry().addPropertyChangeListener(propertyChangeListener);
	}

	/*
	 * @see org.openide.windows.TopComponent#componentClosed()
	 */
	@Override
	protected void componentClosed() {
		super.componentClosed();

		if (null != propertyChangeListener)
			getRegistry().removePropertyChangeListener(propertyChangeListener);
	}

	/**
	 * @param newValue
	 * @param topComponent
	 */
	protected void nodeSelectionChanged(Object newValue, TopComponent topComponent) {
		this.lastTopComponent = topComponent;

		if (newValue == null || topComponent != null)
			newValue = topComponent.getActivatedNodes();

		if (newValue != null && newValue instanceof Node[]
				&& ((Node[]) newValue).length > 0) {
			final Node[] selection = (Node[]) newValue;

			// find a DetailViewProvider among the selected Nodes
			DetailViewProvider dvp = null;
			for (final Node node : selection)
				if (node instanceof DetailViewProvider) {
					dvp = (DetailViewProvider) node;
					break;
				}

			if (null != dvp) {
				setTitle((Node) dvp);
				// setIcon(((Node) dvp).getIcon(0));
				updateDetailView(dvp, selection, topComponent);
			}
		}
	}

	/**
	 * @param node
	 */
	private void setTitle(Node node) {
		final StringBuffer sb = new StringBuffer(node.getDisplayName());

		Node parent = node.getParentNode();
		while (null != parent) {
			sb.insert(0, " > "); //$NON-NLS-1$
			sb.insert(0, parent.getDisplayName());
			parent = parent.getParentNode();
		}

		if (sb.length() > MAX_TITLE_LENGTH) {
			// try to find a sensible location to split at
			int idx = -1, nextIdx = sb.length();
			while (sb.length() - nextIdx < MAX_TITLE_LENGTH && nextIdx > 0) {
				idx = nextIdx;
				nextIdx = sb.lastIndexOf(">", idx - 1); //$NON-NLS-1$
			}

			if (idx < 0)
				idx = sb.length() - MAX_TITLE_LENGTH;

			sb.replace(0, idx + 1, "..."); //$NON-NLS-1$
		}

		// sb.insert(0, " - "); //$NON-NLS-1$
		// sb.insert(0, getName());

		setName(sb.toString());
	}

	/**
	 * @param dvp
	 * @param selection
	 * @param topComponent
	 */
	private void updateDetailView(DetailViewProvider dvp, Node[] selection,
			TopComponent topComponent) {
		ConsoleFrame.getINSTANCE().hideObjectDetails();
		detachDetailView();

		if (null == dvp || 0 == selection.length)
			return; // detach-only!

		this.currentDetailViewProvider = dvp;

		try {
			final DetailView detailView = dvp.getDetailView();
			detailView.init(selection, topComponent);

			if (dvp instanceof Node)
				((Node) dvp).addNodeListener(listener);

			reloadDetailView(detailView);
		} catch (final RuntimeException e) {
			ErrorManager.getDefault().notify(e);
		}
	}

	private void detachDetailView() {
		if (null != currentDetailViewProvider
				&& currentDetailViewProvider instanceof Node)
			((Node) currentDetailViewProvider).removeNodeListener(listener);

		currentDetailViewProvider = null;

		removeAll();
	}

	/**
	 * Reloads the given DetailView (revalidate() and repaint())
	 * 
	 * @param detailView
	 */
	private void reloadDetailView(DetailView detailView) {
		removeAll();
		final JComponent headerComponent = detailView.getHeaderComponent();
		if (null != headerComponent) {
			final JXPanel p = new JXPanel(new BorderLayout());

			final BasicGradientPainter gradient = new BasicGradientPainter(
					BasicGradientPainter.GRAY);
			p.setBackgroundPainter(gradient);
			p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, getBackground()
					.darker()));

			headerComponent.setOpaque(false);
			p.add(headerComponent, BorderLayout.CENTER);
			add(p, BorderLayout.NORTH);
		}

		final JComponent mainComponent = detailView.getMainComponent();
		if (null != mainComponent)
			add(mainComponent, BorderLayout.CENTER);

		final JComponent footerComponent = detailView.getFooterComponent();

		if (footerComponent != null)
			add(footerComponent, BorderLayout.SOUTH);

		revalidate();
		repaint();
	}

	public static synchronized DetailViewTopComponent getDefault() {
		if (instance == null)
			instance = new DetailViewTopComponent();
		return instance;
	}

	@Override
	public int getPersistenceType() {
		return TopComponent.PERSISTENCE_ALWAYS;
	}

	@Override
	protected String preferredID() {
		return "MainTreeTopComponent"; //$NON-NLS-1$
	}

	@Override
	protected Object writeReplace() {
		return new ResolvableHelper();
	}

	private final static class ResolvableHelper implements Serializable {
		private static final long serialVersionUID = 1L;

		public Object readResolve() {
			return DetailViewTopComponent.getDefault();
		}
	}

	/*
	 * @see org.openide.windows.TopComponent#canClose()
	 */
	@Override
	public boolean canClose() {
		return false;
	}
	// public void setNewDetailView(DetailView detView){
	// reloadDetailView(detView);
	// instance.repaint();
	// }
}
