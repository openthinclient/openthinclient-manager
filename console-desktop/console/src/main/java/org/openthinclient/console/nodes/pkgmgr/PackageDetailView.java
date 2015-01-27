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
package org.openthinclient.console.nodes.pkgmgr;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openide.ErrorManager;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Node;
import org.openide.util.WeakListeners;
import org.openide.windows.TopComponent;
import org.openthinclient.console.AbstractDetailView;
import org.openthinclient.console.DetailViewProvider;
import org.openthinclient.console.Messages;
import org.openthinclient.console.util.StringFilterTableModel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.levigo.util.swing.IconManager;
import com.levigo.util.swing.table.SunTableSorter;

public class PackageDetailView extends AbstractDetailView {
	private static PackageDetailView detailView;

	public static PackageDetailView getInstance() {
		if (null == detailView)
			detailView = new PackageDetailView();
		return detailView;
	}

	private static List<PackageListNode> plns = new ArrayList<PackageListNode>();

	private JTextField queryField;

	private JTable packagesTable;

	private MouseAdapter listener;

	private JComponent mainComponent;

	private StringFilterTableModel tableModel;

	private SunTableSorter sts;

	private final boolean allowSelection = false;

	/*
	 * @see org.openthinclient.console.AbstractDetailView#getHeaderComponent()
	 */
	@Override
	public JComponent getHeaderComponent() {
		// make sure that the main component has been initialized

		getMainComponent();

		final DefaultFormBuilder dfb = new DefaultFormBuilder(new FormLayout(
				"p, 10dlu, r:p, 3dlu, f:p:g")); //$NON-NLS-1$
		dfb.setDefaultDialogBorder();
		dfb.setLeadingColumnOffset(2);
		dfb.setColumn(3);

		queryField = new JTextField();
		dfb.append(Messages.getString("DirObjectListNode.filter"), queryField); //$NON-NLS-1$
		dfb.nextLine();

		queryField.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				tableModel.setFilter(queryField.getText());
			}

			public void removeUpdate(DocumentEvent e) {
				tableModel.setFilter(queryField.getText());
			}

			public void insertUpdate(DocumentEvent e) {
				tableModel.setFilter(queryField.getText());
			}

			@Override
			protected void finalize() throws Throwable {
				// System.out.println("PackageDetailView/DocumentListener/finalize");
				tableModel.setFilter("");
				super.finalize();
			}
		});

		dfb.add(
				new JLabel(
						IconManager
								.getInstance(DetailViewProvider.class, "icons").getIcon("tree.PackageListQuery")), //$NON-NLS-1$ //$NON-NLS-2$
				new CellConstraints(1, 1, 1, dfb.getRowCount(), CellConstraints.CENTER,
						CellConstraints.TOP));

		return dfb.getPanel();
	}

	/*
	 * @see org.openthinclient.console.DetailView#getRepresentation()
	 */
	public JComponent getMainComponent() {
		if (null == mainComponent) {
			packagesTable = new JTable();

			tableModel = new StringFilterTableModel();
			sts = new SunTableSorter(tableModel);
			sts.setTableHeader(packagesTable.getTableHeader());

			packagesTable.setModel(sts);

			mainComponent = new JScrollPane(packagesTable);
			mainComponent.setBackground(UIManager.getColor("TextField.background")); //$NON-NLS-1$

		}

		return mainComponent;
	}

	/*
	 * @see org.openthinclient.console.DetailView#init(org.openide.nodes.Node[])
	 */
	public void init(Node[] selection, TopComponent tc) {
		if (null != queryField)
			queryField.setText(" ");
		for (final Node node : selection)
			if (node instanceof PackageListNode) {
				setPackageList((PackageListNode) node, tc, null);
				break;

			}

	}

	/**
	 * @param pln
	 * @param tc
	 * @param node TODO
	 */
	private void setPackageList(final PackageListNode pln, final TopComponent tc,
			final Node node) {
		getMainComponent();
		tableModel.setTableModel(new PackageListTableModel(pln, allowSelection,
				false));
		sts.setSortingStatus(0, SunTableSorter.ASCENDING);
		boolean isIn = false;
		for (final PackageListNode ref : plns)
			if (ref.getName().equalsIgnoreCase(pln.getName()))
				isIn = true;
		if (!isIn) {
			if (plns.size() > 0) {
				packagesTable
						.removeMouseListener(packagesTable.getMouseListeners()[packagesTable
								.getMouseListeners().length - 1]);
				plns.remove(plns.size() - 1);
			}
			if (null != tc && tc instanceof ExplorerManager.Provider) {
				listener = new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						if (e.getClickCount() > 1) {
							final int selectedRow = packagesTable.getSelectedRow();
							if (selectedRow < 0)
								return;
							final Node nodeAtRow = (Node) packagesTable.getModel()
									.getValueAt(selectedRow, -1);
							// navigate explorer to node and, if it was a
							// double-click,
							// execute the default action
							if (null != nodeAtRow)
								try {
									((ExplorerManager.Provider) tc).getExplorerManager()
											.setSelectedNodes(new Node[]{nodeAtRow});
									if (e.getClickCount() > 1)
										SwingUtilities.invokeLater(new Runnable() {
											public void run() {
												if (null != nodeAtRow.getPreferredAction())
													nodeAtRow.getPreferredAction().actionPerformed(
															new ActionEvent(nodeAtRow, 1, "open")); //$NON-NLS-1$
											}
										});
								} catch (final PropertyVetoException e1) {
									e1.printStackTrace();
									ErrorManager.getDefault().notify(e1);
								}
						}
					}
				};
				plns.add(pln);
				packagesTable.addMouseListener((MouseListener) WeakListeners.create(
						MouseListener.class, listener, packagesTable));
			}
		}
	}

	/*
	 * @see org.openthinclient.console.ObjectEditorPart#getTitle()
	 */
	public String getTitle() {
		// FIXME
		return null;
	}

	public JTable getpackagesTable() {
		return packagesTable;
	}
}
