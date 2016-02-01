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
package org.openthinclient.console.nodes.pkgmgr;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.levigo.util.swing.IconManager;
import com.levigo.util.swing.table.SunTableSorter;
import org.openide.ErrorManager;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Node;
import org.openide.util.WeakListeners;
import org.openide.windows.TopComponent;
import org.openthinclient.console.AbstractDetailView;
import org.openthinclient.console.DetailView;
import org.openthinclient.console.DetailViewProvider;
import org.openthinclient.console.Messages;
import org.openthinclient.console.util.StringFilterTableModel;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.pkgmgr.db.Package;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.Collection;

public class DialogPackageDetailView extends AbstractDetailView {

	public static final int INSTALL = 0;
	public static final int CACHE = 1;
	public static final int BOTH = 2;
	private static DialogPackageDetailView detailView;

	public static DialogPackageDetailView getInstance() {

		if (null == detailView)
			detailView = new DialogPackageDetailView();
		return detailView;
	}

	public JTable packagesTable;
	private int rowSelectedInTable = -1;
	private JTextField queryField;
	private MouseAdapter listener;
	private boolean showDebFile;
	private JComponent mainComponent;
	private StringFilterTableModel tableModel;
	private SunTableSorter sts;
	private Node packnode;
	private Node[] selection;
	private TopComponent tc;
	private boolean allowSelection = false;
	private PackageManagerDelegation pkgmgr;

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
				tableModel.setFilter("");
				packagesTable.clearSelection();
				super.finalize();
			}

		});

		dfb.add(new JLabel(IconManager.getInstance(DetailViewProvider.class,
				"icons").getIcon("tree." + "PackageListQuery")), //$NON-NLS-1$ //$NON-NLS-2$
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

		this.selection = selection;
		this.tc = tc;
		for (final Node node : selection)
			if (node instanceof PackageListNode) {
				packnode = node;
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

		showDebFile = false;
		if (pln.getName().equalsIgnoreCase(
				Messages.getString("node.AvailablePackagesNode")))
			showDebFile = true;
		getMainComponent();
		tableModel.setTableModel(new PackageListTableModel(pln, allowSelection,
				showDebFile));
		sts.setSortingStatus(1, SunTableSorter.ASCENDING);
		if (null != tc && tc instanceof ExplorerManager.Provider) {
			listener = new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() > 1) {
						final int selectedRow = packagesTable.getSelectedRow();
						if (selectedRow < 0)
							return;
						final Node nodeAtRow = (Node) packagesTable.getModel().getValueAt(
								selectedRow, -1);

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
			packagesTable.addMouseListener((MouseListener) WeakListeners.create(
					MouseListener.class, listener, packagesTable));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openthinclient.console.AbstractDetailView#getFooterComponent()
	 */
	@Override
	public JComponent getFooterComponent() {
		if (tableModel.getTableModel().getClass() == PackageListTableModel.class) {
			if (rowSelectedInTable > -1
					&& rowSelectedInTable < packagesTable.getRowCount()) {
				boolean isSet = false;
				final Node[] droehnung = new Node[1];
				for (final Node nodele : selection)
					if (null != nodele)
						for (final Node nodelele : nodele.getChildren().getNodes())
							if (null != nodelele)
								if (nodelele.getName().equalsIgnoreCase(
										(String) packagesTable.getValueAt(rowSelectedInTable, 1))) {
									droehnung[0] = nodelele;
									isSet = true;
								}
				if (isSet) {
					Package pkg = null;
					for (int i = 0; i < packagesTable.getRowCount(); i++)
						if (droehnung[0].getName().equalsIgnoreCase(
								(String) ((PackageListTableModel) tableModel.getTableModel())
										.getValueAt(i, 1)))
							pkg = ((PackageListTableModel) tableModel.getTableModel())
									.getPackageAtRow(i);
					final DetailView detail = new PackageNode(packnode, pkg)
							.getDetailView();

					detail.init(droehnung, tc);

					final JComponent jco = detail.getMainComponent();
					return jco;
				} else
					return new JLabel(Messages
							.getString("PackageDetailView.noRowSelected"));
			} else
				return new JLabel(Messages.getString("PackageDetailView.noRowSelected"));
		} else
			return new JLabel(Messages.getString("PackageDetailView.noRowSelected"));
	}

	/**
	 * 
	 * @return JComponent with informations about the used Space of the selected
	 *         Package item's
	 * @throws PackageManagerException
	 */
	public JComponent infoFooter() throws PackageManagerException {
		final CellConstraints cc = new CellConstraints();
		final JPanel jpl = new JPanel();
		jpl.setLayout(new FormLayout("f:p:g", "30dlu"));
		try {
			if (packnode.getName().equalsIgnoreCase(
					Messages.getString("node.AvailablePackagesNode")))
				jpl.add(getInstallSize(BOTH), cc.xy(1, 1));
			else if (packnode.getName().equalsIgnoreCase(
					Messages.getString("node.DebianFilePackagesNode")))
				jpl.add(getInstallSize(CACHE), cc.xy(1, 1));
			else
				jpl.add(getInstallSize(INSTALL), cc.xy(1, 1));
		} catch (final IOException e) {
			e.printStackTrace();
			ErrorManager.getDefault().notify(e);
		}
		return jpl;
	}

	/*
	 * @see org.openthinclient.console.ObjectEditorPart#getTitle()
	 */
	public String getTitle() {
		return null;
	}

	public boolean isAllowSelection() {
		return allowSelection;
	}

	public void setAllowSelection(boolean allowSelection) {
		this.allowSelection = allowSelection;
	}

	public Collection<Package> getSlecetedItems() {
		return ((PackageListTableModel) tableModel.getTableModel())
				.getSelectedPackages();
	}

	/**
	 * 
	 * @param i describes which space index is needed
	 * @return JComponent with the different capacity informations
	 * @throws IOException
	 * @throws PackageManagerException
	 */
	public JComponent getInstallSize(int i) throws IOException,
			PackageManagerException {
		final CellConstraints cc = new CellConstraints();
		final JPanel jpl = new JPanel();

		if (i == BOTH) {
			jpl.setLayout(new FormLayout("85dlu,60dlu,85dlu,60dlu", "15dlu,15dlu"));
			jpl
					.add(new JLabel(Messages.getString("size.InstalledSize")), cc
							.xy(1, 1));
			jpl.add(new JLabel(((PackageListTableModel) tableModel.getTableModel())
					.getUsedInstallSpace()), cc.xy(2, 1));
			jpl.add(new JLabel(Messages.getString("size.CacheSize")), cc.xy(3, 1));
			jpl.add(new JLabel(((PackageListTableModel) tableModel.getTableModel())
					.getUsedCacheSpace()), cc.xy(4, 1));
		} else {
			jpl.setLayout(new FormLayout("85dlu,60dlu", "15dlu,15dlu"));
			if (i == INSTALL) {
				jpl.add(new JLabel(Messages.getString("size.InstalledSize")), cc.xy(1,
						1));
				jpl.add(new JLabel(((PackageListTableModel) tableModel.getTableModel())
						.getUsedInstallSpace()), cc.xy(2, 1));
			} else if (i == CACHE) {
				jpl.add(new JLabel(Messages.getString("size.CacheSize")), cc.xy(1, 1));
				jpl.add(new JLabel(((PackageListTableModel) tableModel.getTableModel())
						.getUsedCacheSpace()), cc.xy(2, 1));
			}
		}
		jpl.add(new JLabel(Messages.getString("size.freeDiskSpace")), cc.xy(1, 2));
		// try{
		pkgmgr = ((PackageManagementNode) packnode.getParentNode().getParentNode()).getPackageManagerDelegation();
		jpl.add(new JLabel(String.valueOf((float) Math.round((pkgmgr
				.getFreeDiskSpace() / 1024f)))
				+ " " + Messages.getString("size.unit")), cc.xy(2, 2));
		// }
		// catch (PackageManagerException e) {
		// e.printStackTrace();
		// jpl.add(new JLabel(e.toString()));
		// ErrorManager.getDefault().notify(e);
		// }
		return jpl;
	}

	public void setRowSelectedInTable(int rowSelectedInTable) {
		this.rowSelectedInTable = rowSelectedInTable;
	}

	public int getTableHight() {
		return packagesTable.getRowHeight() * (packagesTable.getRowCount() + 1);
	}

	public void setValueAt(int i) {
		tableModel.setValueAt(true, i, 0);
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}
}
