package org.openthinclient.console.nodes.pkgmgr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;

import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.nodes.Node;
import org.openide.windows.TopComponent;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.AbstractDetailView;
import org.openthinclient.console.DetailView;
import org.openthinclient.console.DetailViewProvider;
import org.openthinclient.console.Messages;
import org.openthinclient.console.util.StringFilterTableModel;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.util.dpkg.Package;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.levigo.util.swing.IconManager;
import com.levigo.util.swing.table.SunTableSorter;

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

/**
 * Create a new Panel with different data of the chosen PackageListNode
 * 
 * @author tauschfn
 */
public class PackageManagerEditPanel extends JPanel

{
	private static PackageManagerEditPanel packManEdPa;

	public static PackageManagerEditPanel getInstance() {
		packManEdPa = new PackageManagerEditPanel();
		return packManEdPa;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Node node;
	private JComponent footerComponent = null;
	private DialogPackageDetailViewEditorPanel detView;
	private JComponent mainComponent = null;
	private JComponent headerComponent = null;
	private JSplitPane splity = null;
	private CellConstraints cc = new CellConstraints();
	private JComponent infoFooter = null;
	private Node[] selectedPackage;
	private boolean fromPackageNode = false;
	private Node[] selection;
	private TopComponent tc;
	private int tmpselection;

	public void init(Node node, Node[] selectedPackage, Node[] selection,
			TopComponent tc) throws PackageManagerException {
		if (null != selectedPackage)
			fromPackageNode = true;
		else
			fromPackageNode = false;
		this.selectedPackage = selectedPackage;
		this.node = node;
		this.detView = DialogPackageDetailViewEditorPanel.getInstance();
		this.selection = selection;
		this.tc = tc;
		this.footerComponent = null;
		this.mainComponent = null;
		this.headerComponent = null;
		this.splity = new JSplitPane();
		this.cc = new CellConstraints();
		this.infoFooter = null;
		this.tmpselection = -1;
		dataInput();
	}

	public void dataInput() throws PackageManagerException {
		detView.setAllowSelection(Boolean.TRUE);
		detView.init(selection, tc);
		setLayout(new FormLayout("f:p:g", "15dlu,30dlu,f:p:g,30dlu")); //$NON-NLS-1$ //$NON-NLS-2$

		Font f = UIManager.getFont("TitledBorder.font"); //$NON-NLS-1$
		f = f.deriveFont(Font.BOLD, AffineTransform.getScaleInstance(1.5, 1.5));
		if (fromPackageNode)
			for (int i = 0; i < detView.packagesTable.getRowCount(); i++)
				for (int n = 0; n < selectedPackage.length; n++)
					if (((String) detView.packagesTable
							.getValueAt(
									i,
									detView.packagesTable
											.getColumnModel()
											.getColumnIndex(
													Messages
															.getString("node.PackageListNode.getColumnName.name"))))
							.equalsIgnoreCase(selectedPackage[n].getName())) {
						detView.setValueAt(i);
						detView.setRowSelectedInTable(i);
					}
		final JLabel nameLabel = new JLabel(node.getName() != null
				? node.getName()
				: " "); //$NON-NLS-1$
		nameLabel.setForeground(new Color(50, 50, 200));
		nameLabel.setFont(f);
		add(nameLabel, cc.xy(1, 1));
		headerComponent = detView.getHeaderComponent();
		if (null != headerComponent) {
			headerComponent.setBorder(BorderFactory
					.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
							getBackground().darker()), headerComponent.getBorder()));
			add(headerComponent, cc.xy(1, 2));
		}
		mainComponent = detView.getMainComponent();

		mainComponent.setSize(800, detView.packagesTable.getRowCount()
				* detView.packagesTable.getRowHeight());
		detView.setRowSelectedInTable(detView.packagesTable.getSelectedRow());
		refreshFooter();
		detView.packagesTable.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent e) {
						if (e.getValueIsAdjusting())
							return;

						final int previousSelectedRow = detView.getRowSelectedInTable();

						if (tmpselection > -1
								&& detView.packagesTable.getSelectedRow() == -1) {
							if (tmpselection >= detView.packagesTable.getRowCount())
								tmpselection = detView.packagesTable.getSelectedRow();
							else {
								detView.setRowSelectedInTable(tmpselection);
								detView.packagesTable.setRowSelectionInterval(tmpselection,
										tmpselection);
							}
						} else
							tmpselection = detView.packagesTable.getSelectedRow();
						detView.setRowSelectedInTable(detView.packagesTable
								.getSelectedRow());

						if (detView.getRowSelectedInTable() != previousSelectedRow)
							refreshFooter();
					}
				});
		if (null == detView.getFooterComponent())
			footerComponent = new JLabel(Messages
					.getString("PackageManagerEdit.datainput.noPackageSelected"));
		else
			footerComponent = detView.getFooterComponent();
		splity = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, mainComponent,
				footerComponent);
		splity.setDividerLocation(0.5);
		add(splity, cc.xy(1, 3));
		infoFooter = detView.infoFooter();
		add(infoFooter, cc.xy(1, 4));
		detView.packagesTable.getModel().addTableModelListener(
				new TableModelListener() {
					public void tableChanged(TableModelEvent e) {
						try {
							refreshInfoFooter();
						} catch (final PackageManagerException e1) {
							e1.printStackTrace();
							ErrorManager.getDefault().notify(e1);
						}
						refreshFooter();
					}
				});
		setPreferredSize(new Dimension(800, 600));
	}

	/**
	 * @param node
	 * @param dirObject
	 * @return
	 * 
	 */
	@SuppressWarnings("static-access")
	public void doEdit() {
		final JButton cancelButton = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				detView.packagesTable.clearSelection();
			}
		});
		final JButton doSomething = new JButton();
		String buttonDoSomthingText = "";
		doSomething.setVisible(true);
		if (node.getName().equalsIgnoreCase(
				Messages.getString("node.AvailablePackagesNode")))
			buttonDoSomthingText = Messages
					.getString("PackageManagerEditPanel.button.AvailablePackagesNode");
		else if (node.getName().equalsIgnoreCase(
				Messages.getString("node.InstalledPackagesNode")))
			buttonDoSomthingText = Messages
					.getString("PackageManagerEditPanel.button.InstalledPackagesNode");
		else if (node.getName().equalsIgnoreCase(
				Messages.getString("node.UpdatablePackagesNode")))
			buttonDoSomthingText = Messages
					.getString("PackageManagerEditPanel.button.UpdatablePackagesNode");
		else if (node.getName().equalsIgnoreCase(
				Messages.getString("node.AlreadyDeletedPackagesNode")))
			buttonDoSomthingText = Messages
					.getString("PackageManagerEditPanel.button.AlreadyDeletedPackagesNode");
		else if (node.getName().equalsIgnoreCase(
				Messages.getString("node.DebianFilePackagesNode")))
			buttonDoSomthingText = Messages
					.getString("PackageManagerEditPanel.button.DebianFilePackagesNode");

		// DO SOMETHING
		if (buttonDoSomthingText.length() > 0) {
			doSomething.setText(buttonDoSomthingText);
			doSomething.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					doSomethingAction();
				}
			});
			doSomething.setVisible(true);
		}
		final JButton selectAll = new JButton(Messages.getString("selectAll"));

		// SELECT ALL
		selectAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				detView.setBooleanValueAtSearchedPackages(true);
				refreshMain();
			}
		});
		selectAll.setVisible(true);

		// DESELECT ALL
		final JButton deselectAll = new JButton(Messages.getString("deselectAll"));
		deselectAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				detView.setBooleanValueAtSearchedPackages(false);
				refreshMain();
			}
		});
		deselectAll.setVisible(true);
		final CellConstraints cc = new CellConstraints();
		final JPanel jpl = new JPanel(new FormLayout("l:p,c:p:g,r:p", "f:p:g"));
		final ButtonBarBuilder builder1 = new ButtonBarBuilder();
		final ButtonBarBuilder builder2 = new ButtonBarBuilder();
		builder1.addGridded(selectAll);
		builder1.addGridded(deselectAll);
		builder2.addGridded(cancelButton);
		builder2.addGridded(doSomething);
		jpl.add(builder1.getPanel(), cc.xy(1, 1));
		jpl.add(new JPanel(), cc.xy(2, 1));
		jpl.add(builder2.getPanel(), cc.xy(3, 1));
		final DialogDescriptor descriptor = new DialogDescriptor(this, node
				.getName(), true, new Object[]{jpl}, doSomething,
				DialogDescriptor.DEFAULT_ALIGN, null, new ActionListener() {
					public void actionPerformed(ActionEvent e) {

					}
				});
		descriptor.setClosingOptions(new Object[]{cancelButton, doSomething});
		// DIALOG
		final Dialog dialog = DialogDisplayer.getDefault().createDialog(descriptor);
		dialog.addWindowListener(new WindowListener() {

			public void windowOpened(WindowEvent e) {

			}

			public void windowIconified(WindowEvent e) {

			}

			public void windowDeiconified(WindowEvent e) {

			}

			public void windowDeactivated(WindowEvent e) {

			}

			public void windowClosing(WindowEvent e) {
				dialog.setVisible(false);
				dialog.dispose();
				try {
					detView.finalize();
					this.finalize();
				} catch (final Throwable e1) {
					e1.printStackTrace();
					ErrorManager.getDefault().notify(e1);
				}
			}

			public void windowClosed(WindowEvent e) {

			}

			public void windowActivated(WindowEvent e) {

			}
		});

		final int maxSize = dialog.getComponents()[0].getHeight()
				- this.getComponents()[0].getHeight()
				- this.getComponents()[1].getHeight();
		if (maxSize * 0.9 < (detView.packagesTable.getRowCount() + 1)
				* detView.packagesTable.getRowHeight() + 5)
			splity.setDividerLocation(0.5);
		else
			this.splity.setDividerLocation(((detView.packagesTable.getRowCount() + 1)
					* detView.packagesTable.getRowHeight() + 5));
		dialog.setVisible(true);

	}

	/**
	 * Starts the correct Action for the pushed button do somthing button This
	 * means the selected packages will be installed/removed/realyremoved or
	 * updated
	 * 
	 */
	public void doSomethingAction() {

		if (node.getName().equalsIgnoreCase(
				Messages.getString("node.AvailablePackagesNode")))
			InstallAction.installPackages(node, detView.getSlecetedItems());
		else if (node.getName().equalsIgnoreCase(
				Messages.getString("node.InstalledPackagesNode")))
			DeleteAction.deletePackages(detView.getSlecetedItems(), node);
		else if (node.getName().equalsIgnoreCase(
				Messages.getString("node.UpdatablePackagesNode")))
			UpdateAction.updatePackages(node, detView.getSlecetedItems());
		else if (node.getName().equalsIgnoreCase(
				Messages.getString("node.AlreadyDeletedPackagesNode")))
			RealyDeleteAction.realyDeletePackages(detView.getSlecetedItems(), node);
		else if (node.getName().equalsIgnoreCase(
				Messages.getString("node.DebianFilePackagesNode")))
			DebianPackagesDeleteAction.deleteDebianPackages(detView
					.getSlecetedItems(), node);

	}

	/**
	 * refreshes the footer component
	 * 
	 */
	private void refreshFooter() {
		if (null != splity) {
			int tempDivLoc = 0;
			tempDivLoc = this.splity.getDividerLocation();
			this.footerComponent = detView.getFooterComponent();
			this.splity.setBottomComponent(footerComponent);
			this.splity.setDividerLocation(tempDivLoc);
		} else {
			this.footerComponent = detView.getFooterComponent();
			this.splity.setBottomComponent(footerComponent);
		}
	}

	/**
	 * refreshes the main component
	 * 
	 */
	private void refreshMain() {
		if (null != mainComponent) {
			mainComponent.setSize(800, detView.packagesTable.getRowCount()
					* detView.packagesTable.getRowHeight());
			int tempDivLoc = 0;
			tempDivLoc = this.splity.getDividerLocation();
			this.mainComponent = detView.getMainComponent();
			mainComponent.setSize(800, detView.packagesTable.getRowCount()
					* detView.packagesTable.getRowHeight());
			this.splity.setTopComponent(mainComponent);
			this.splity.setDividerLocation(tempDivLoc);
		} else
			this.mainComponent = detView.getMainComponent();

	}

	private void refreshInfoFooter() throws PackageManagerException {

		if (this.getComponents().length > 3) {
			this.remove(3);
			this.infoFooter = detView.infoFooter();
			this.add(infoFooter, cc.xy(1, 4));
			this.revalidate();
			this.validate();
			this.repaint();
		}
	}

	static class DialogPackageDetailViewEditorPanel extends AbstractDetailView {
		private static DialogPackageDetailViewEditorPanel detailView;

		public static DialogPackageDetailViewEditorPanel getInstance() {

			if (null == detailView)
				detailView = new DialogPackageDetailViewEditorPanel();
			return detailView;
		}

		public int getRowSelectedInTable() {
			return rowSelectedInTable;
		}

		private int rowSelectedInTable = -1;
		private JTextField queryField;
		public JTable packagesTable;
		private boolean showDebFile;
		private JComponent mainComponent;
		private StringFilterTableModel tableModel;
		private SunTableSorter sts;
		private Node packnode;
		private Node[] selection;
		private TopComponent tc;
		private PackageManagerDelegation pkgmgr;
		public static final int INSTALL = 0;
		public static final int BOTH = 2;

		private boolean allowSelection = false;

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
					new CellConstraints(1, 1, 1, dfb.getRowCount(),
							CellConstraints.CENTER, CellConstraints.TOP));

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
			} else
				packagesTable.clearSelection();
			return mainComponent;

		}

		/*
		 * @see org.openthinclient.console.DetailView#init(org.openide.nodes.Node[])
		 */
		public void init(Node[] selection, TopComponent tc) {
			this.mainComponent = null;
			this.packagesTable = null;
			this.tableModel = null;
			this.queryField = null;
			this.packnode = null;
			this.selection = null;
			this.sts = null;
			this.tc = null;
			this.selection = selection;
			this.tc = tc;
			for (final Node node : selection)
				if (node instanceof PackageListNode) {
					packnode = node;
					setPackageList((PackageListNode) node, tc, null);

					final Realm realm = (Realm) packnode.getLookup().lookup(Realm.class);
					pkgmgr = realm.getPackageManagerDelegation();

					break;
				}
		}

		/**
		 * @param pln
		 * @param tc
		 * @param node
		 */
		@SuppressWarnings("serial")
		private void setPackageList(final PackageListNode pln,
				final TopComponent tc, final Node node) {
			showDebFile = false;
			if (pln.getName().equalsIgnoreCase(
					Messages.getString("node.AvailablePackagesNode")))
				showDebFile = true;
			getMainComponent();
			tableModel.setTableModel(new PackageListTableModel(pln, allowSelection,
					showDebFile));
			if (showDebFile) {
				packagesTable
						.getColumn(
								Messages
										.getString("node.PackageListNode.getColumnName.isDebLocal"))
						.setCellRenderer(
						// Standard-Renderer erweitern
								new DefaultTableCellRenderer() {
									@Override
									public Component getTableCellRendererComponent(JTable table,
											Object value, boolean isSelected, boolean hasFocus,
											int row, int column) {
										// Label der Oberklasse erweitern
										final JLabel label = (JLabel) super
												.getTableCellRendererComponent(table, value,
														isSelected, hasFocus, row, column);
										// Lediglich Text und Grafik anpassen
										if (value != null) {
											label.setText("");
											label.setIcon((ImageIcon) value);
										}
										return label;
									}
								});
				packagesTable
						.getColumnModel()
						.removeColumn(
								packagesTable
										.getColumn(Messages
												.getString("node.PackageListNode.getColumnName.isDebLocal.DontShow")));
			}
			sts.setSortingStatus(1, SunTableSorter.ASCENDING);
		}

		public void setBooleanValueAtSearchedPackages(boolean b) {
			final int columnValue = packagesTable.getColumnModel().getColumnIndex(
					Messages.getString("node.PackageListNode.getColumnName.tagged"));
			for (int i = 0; i < packagesTable.getRowCount(); i++)
				packagesTable.setValueAt(b, i, columnValue);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.openthinclient.console.AbstractDetailView#getFooterComponent()
		 */
		@Override
		public JComponent getFooterComponent() {
			DetailView detail = null;
			if (tableModel.getTableModel().getClass() == PackageListTableModel.class) {
				if (rowSelectedInTable > -1
						&& rowSelectedInTable < packagesTable.getRowCount()) {
					boolean isSet = false;
					final Node[] nodeArray = new Node[1];
					for (final Node nodele : selection)
						if (null != nodele)
							for (final Node nodelele : nodele.getChildren().getNodes())
								if (null != nodelele)
									if (nodelele
											.getName()
											.equalsIgnoreCase(
													(String) packagesTable
															.getValueAt(
																	rowSelectedInTable,
																	packagesTable
																			.getColumnModel()
																			.getColumnIndex(
																					Messages
																							.getString("node.PackageListNode.getColumnName.name"))))) {
										detail = new PackageNode(packnode,
												((PackageListTableModel) tableModel.getTableModel())
														.getPackageAtRow(rowSelectedInTable))
												.getDetailView();
										nodeArray[0] = nodelele;
										detail.init(nodeArray, tc);
										isSet = true;
									}
					if (isSet) {
						final JComponent jco = detail.getMainComponent();
						return jco;
					} else
						return new JLabel(Messages
								.getString("PackageDetailView.noRowSelected"));
				} else
					return new JLabel(Messages
							.getString("PackageDetailView.noRowSelected"));
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
			jpl.setLayout(new FormLayout("f:p:g", "f:p:g"));
			try {
				if (packnode.getName().equalsIgnoreCase(
						Messages.getString("node.AvailablePackagesNode")))
					jpl.add(getInstallSize(BOTH), cc.xy(1, 1));
				else if (packnode.getName().equalsIgnoreCase(
						Messages.getString("node.DebianFilePackagesNode")))
					jpl.add(getInstallSize(INSTALL), cc.xy(1, 1));
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

		private class SizeInfoPanel extends JPanel {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;
			private final JLabel installedSize;
			private final JLabel cacheSize;
			private JLabel freeDiskLabel;
			private final JLabel installedSizeLabel;
			private final JLabel cacheSizeLabel;

			public SizeInfoPanel(int what) {
				final DefaultFormBuilder dfb = new DefaultFormBuilder(new FormLayout(
						"l:p,15dlu,l:p,15dlu,l:p,15dlu,l:p,15dlu,l:p,15dlu", "f:p:g"), this);

				installedSize = new JLabel();

				installedSizeLabel = dfb.append(Messages
						.getString("size.InstalledSize"), installedSize, false);
				cacheSize = new JLabel();
				cacheSizeLabel = dfb.append(Messages.getString("size.CacheSize"),
						cacheSize, false);
				dfb.nextLine();
				freeDiskLabel = new JLabel((float) Math.round((pkgmgr
						.getFreeDiskSpace() / 1024f))
						+ " " + Messages.getString("size.unit"));
				dfb.append(Messages.getString("size.freeDiskSpace"), freeDiskLabel);
				update(what);
			}

			void update(int what) {
				freeDiskLabel = new JLabel((float) Math.round((pkgmgr
						.getFreeDiskSpace() / 1024f))
						+ " " + Messages.getString("size.unit"));
				installedSize.setText(((PackageListTableModel) tableModel
						.getTableModel()).getUsedInstallSpace());
				cacheSize.setText(((PackageListTableModel) tableModel.getTableModel())
						.getUsedCacheSpace());

				installedSize.setVisible(what == BOTH || what == INSTALL);
				installedSizeLabel.setVisible(what == BOTH || what == INSTALL);

				cacheSize.setVisible(what == BOTH);
				cacheSizeLabel.setVisible(what == BOTH);
				freeDiskLabel.setVisible(true);
			}
		}

		private SizeInfoPanel sip;

		/**
		 * 
		 * @param what describes which space index is needed
		 * @return JComponent with the different capacity informations
		 * @throws IOException
		 * @throws PackageManagerException
		 */
		public JComponent getInstallSize(int what) throws IOException,
				PackageManagerException {
			if (null == sip)
				sip = new SizeInfoPanel(what);

			sip.update(what);

			return sip;
		}

		public void setRowSelectedInTable(int rowSelectedInTable) {
			this.rowSelectedInTable = rowSelectedInTable;
		}

		public int getTableHight() {
			return packagesTable.getRowHeight() * (packagesTable.getRowCount() + 1);
		}

		public void setValueAt(int i) {
			packagesTable.setValueAt(true, i, packagesTable.getColumnModel()
					.getColumnIndex(
							Messages.getString("node.PackageListNode.getColumnName.tagged")));
		}

		@Override
		protected void finalize() throws Throwable {
			super.finalize();
		}
	}
}
