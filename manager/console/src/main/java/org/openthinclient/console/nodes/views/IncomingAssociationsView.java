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
package org.openthinclient.console.nodes.views;

import java.awt.BorderLayout;
import java.util.HashSet;
import java.util.Set;

import javax.swing.tree.TreePath;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.decorator.AlternateRowHighlighter;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.decorator.HighlighterPipeline;
import org.jdesktop.swingx.treetable.TreeTableModel;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Group;
import org.openthinclient.console.Messages;
import org.openthinclient.console.nodes.DirObjectNode;
import org.openthinclient.console.nodes.DirObjectSetNode;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.Sizes;
import com.jgoodies.forms.util.LayoutStyle;

/**
 * @author levigo
 */
public class IncomingAssociationsView extends JXPanel {
	private static class MyNBNodeTreeTableModel extends NBNodeTreeTableModel {

		/**
		 * @param node
		 */
		public MyNBNodeTreeTableModel(Node node) {
			super(node);
		}

		/*
		 * @see org.openthinclient.console.ui.NBNodeTreeTableModel#getValueAt(java.lang.Object,
		 *      int)
		 */
		@Override
		public Object getValueAt(Object node, int index) {
			switch (index){
				case 1 :
					if (node instanceof DirObjectNode)
						return ((DirectoryObject) ((DirObjectNode) node).getLookup()
								.lookup(DirectoryObject.class)).getDescription();
				default :
					return ""; //$NON-NLS-1$
			}
		}

		/*
		 * @see org.openthinclient.console.ui.NBNodeTreeTableModel#getColumnName(int)
		 */
		@Override
		public String getColumnName(int index) {
			switch (index){
				case 0 :
					return Messages.getString("IncomingAssociationsView.typeName"); //$NON-NLS-1$
				case 1 :
					return Messages.getString("IncomingAssociationsView.description"); //$NON-NLS-1$
				default :
					return ""; //$NON-NLS-1$
			}
		}

		/*
		 * @see org.openthinclient.console.ui.NBNodeTreeTableModel#getColumnCount()
		 */
		@Override
		public int getColumnCount() {
			return 2;
		}

		/*
		 * @see org.jdesktop.swingx.treetable.AbstractTreeTableModel#isLeaf(java.lang.Object)
		 */
		@Override
		public boolean isLeaf(Object arg0) {
			return arg0 instanceof DirObjectNode;
		}
	}

	private static class GroupNode extends AbstractNode {
		private final Class[] memberClasses;
		private static final Logger logger = Logger
				.getLogger(IncomingAssociationsView.class);

		/**
		 * @param arg0
		 */
		public GroupNode(Group group) {

			super(new Children.Array());

			memberClasses = group.getMemberClasses();
			final Set<DirectoryObject> allMembers = group.getMembers();

			for (final Class memberClass : memberClasses) {
				final Set<DirectoryObject> members = new HashSet<DirectoryObject>();
				for (final DirectoryObject o : allMembers)
					if (memberClass.isAssignableFrom(o.getClass()))
						members.add(o);

				if (logger.isDebugEnabled())
					logger.debug((memberClass + " -> " + members.size()));
				getChildren().add(
						new Node[]{new DirObjectSetNode(memberClass, members)});
			}
		}
	}

	public IncomingAssociationsView(Group<DirectoryObject> group) {
		setLayout(new BorderLayout());

		final JXTreeTable tt = new JXTreeTable(new MyNBNodeTreeTableModel(
				new GroupNode(group)));
		tt.setShowHorizontalLines(false);
		tt.setShowVerticalLines(false);
		tt.setTreeCellRenderer(new NBNodeTreeCellRenderer());
		tt.setHighlighters(new HighlighterPipeline(
				new Highlighter[]{AlternateRowHighlighter.genericGrey}));

		expandOneLevel(tt);

		add(tt.getTableHeader(), BorderLayout.NORTH);
		add(tt, BorderLayout.CENTER);

		setBorder(Borders.createEmptyBorder(LayoutStyle.getCurrent()
				.getRelatedComponentsPadY(), Sizes.ZERO, LayoutStyle.getCurrent()
				.getRelatedComponentsPadY(), Sizes.ZERO));
		setOpaque(false);
		if (group instanceof DirectoryObject) {
			final DirectoryObject dirObject = (DirectoryObject) group;
			setName(Messages.getString(
					"IncomingAssociationsView.assigned", dirObject.getName())); //$NON-NLS-1$
			// setName(Messages.getString("IncomingAssociationsView.assigned_object")
			// + " " + dirObject.getName() +" "
			// + Messages.getString("IncomingAssociationsView.assigned_use")
			// ); //$NON-NLS-1$
		}
	}

	/**
	 * @param tt
	 */
	private void expandOneLevel(JXTreeTable tt) {
		final TreeTableModel model = tt.getTreeTableModel();
		final int childCount = model.getChildCount(model.getRoot());
		final TreePath rootPath = new TreePath(model.getRoot());
		for (int i = 0; i < childCount; i++)
			tt.expandPath(rootPath.pathByAddingChild(model.getChild(model.getRoot(),
					i)));
	}
}
