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

import java.awt.Font;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;

import org.jdesktop.swingx.JXTable;
import org.openide.nodes.Node;
import org.openide.windows.TopComponent;
import org.openthinclient.console.AbstractDetailView;
import org.openthinclient.console.Messages;
import org.openthinclient.console.nodes.DirectoryEntryNode;
import org.openthinclient.console.nodes.DirectoryNode;
import org.openthinclient.console.util.DetailViewFormBuilder;
import org.openthinclient.ldap.LDAPConnectionDescriptor;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Natalie Bohnert
 */
public class DirectoryEntryDetailView extends AbstractDetailView {

	private String dn;
	private String rdn;
	private LDAPConnectionDescriptor connectionDescriptor;
	private Image icon;
	private String displayName;

	@Override
	public JComponent getHeaderComponent() {
		final DetailViewFormBuilder dfb = new DetailViewFormBuilder(new FormLayout(
				"p, 10dlu, p, 0dlu, f:p:g"), Messages.getBundle()); //$NON-NLS-1$
		dfb.setLeadingColumnOffset(2);
		dfb.setColumn(3);

		Font f = UIManager.getFont("TitledBorder.font"); //$NON-NLS-1$
		f = f.deriveFont(Font.BOLD, AffineTransform.getScaleInstance(1.5, 1.5));

		final JLabel nameLabel = new JLabel(displayName);
		nameLabel.setForeground(UIManager.getColor("textHighlight")); //$NON-NLS-1$
		nameLabel.setFont(f);

		final JLabel nameField = new JLabel(dn.substring(rdn.length()));
		nameField.setForeground(UIManager.getColor("textHighlight").brighter()); //$NON-NLS-1$
		nameField.setFont(f);

		final JLabel descField = new JLabel(connectionDescriptor.getLDAPUrl());

		dfb.append(nameLabel, nameField);
		dfb.append(descField, 3);

		dfb.add(new JLabel(new ImageIcon(icon)), new CellConstraints(1, 1, 1, dfb
				.getRowCount(), CellConstraints.CENTER, CellConstraints.TOP));

		return dfb.getPanel();
	}

	private static class AttributesTableModel extends AbstractTableModel {
		private class Row {
			public final Attribute a;
			public final Object val;
			public final boolean isFirst;

			Row(Attribute a, Object val, boolean isFirst) {
				this.a = a;
				this.val = val;
				this.isFirst = isFirst;
			}
		}

		private final List<Row> rows = new ArrayList<Row>();

		public AttributesTableModel(Attributes attributes) throws NamingException {
			final ArrayList<String> ids = new ArrayList<String>();
			final NamingEnumeration<String> i = attributes.getIDs();
			while (i.hasMore())
				ids.add(i.next());
			Collections.sort(ids);

			for (final String id : ids) {
				final Attribute a = attributes.get(id);
				if (a.size() == 0)
					rows.add(new Row(a, null, true));
				else
					for (int j = 0; j < a.size(); j++)
						rows.add(new Row(a, a.get(j), j == 0));
			}
		}

		/*
		 * @see javax.swing.table.TableModel#getRowCount()
		 */
		public int getRowCount() {
			return rows.size();
		}

		/*
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		public int getColumnCount() {
			return 2;
		}

		/*
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		public Object getValueAt(int rowIndex, int columnIndex) {
			final Row r = rows.get(rowIndex);
			switch (columnIndex){
				case 0 :
					return r.isFirst ? r.a.getID() : ""; //$NON-NLS-1$
				default :
					return r.val != null ? r.val : ""; //$NON-NLS-1$
			}
		}

		/*
		 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
		 */
		@Override
		public String getColumnName(int column) {
			switch (column){
				case 0 :
					return Messages.getString("DirectoryEntryDetailView.name"); //$NON-NLS-1$

				default :
					return Messages.getString("DirectoryEntryDetailView.value"); //$NON-NLS-1$
			}
		}
	}

	/*
	 * @see org.openthinclient.console.DetailView#getMainComponent()
	 */
	public JComponent getMainComponent() {
		try {
			final DirContext ctx = connectionDescriptor.createDirectoryFacade()
					.createDirContext();
			final JXTable table = new JXTable(new AttributesTableModel(ctx
					.getAttributes(dn)));
			table.setShowGrid(true);

			final JScrollPane mainComponent = new JScrollPane(table);
			mainComponent.setBackground(UIManager.getColor("TextField.background")); //$NON-NLS-1$
			mainComponent.setBorder(BorderFactory.createEmptyBorder());

			return mainComponent;
		} catch (final NamingException e) {
			return new JLabel(e.toString());
		}
	}

	/*
	 * @see org.openthinclient.console.DetailView#init(org.openide.nodes.Node[],
	 *      org.openide.windows.TopComponent)
	 */
	public void init(Node[] selection, TopComponent tc) {
		for (final Node node : selection)
			if (node instanceof DirectoryEntryNode) {
				final DirectoryEntryNode den = (DirectoryEntryNode) node;
				this.dn = den.getDn();
				this.rdn = den.getRdn();
				this.connectionDescriptor = den.getConnectionDescriptor();
				this.icon = den.getIcon(0);
				this.displayName = den.getDisplayName();
			} else if (node instanceof DirectoryNode) {
				final DirectoryNode dirn = (DirectoryNode) node;
				this.dn = ""; //$NON-NLS-1$
				this.rdn = ""; //$NON-NLS-1$
				this.connectionDescriptor = dirn.getConnectionDescriptor();
				this.icon = dirn.getIcon(0);
				this.displayName = dirn.getDisplayName();
			}
	}

	/*
	 * @see org.openthinclient.console.SubDetailView#getTitle()
	 */
	public String getTitle() {
		return null;
	}

}
