package org.openthinclient.console;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchResult;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.nodes.Node;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.nodes.DirObjectListNode;
import org.openthinclient.console.util.StringFilterTableModel;
import org.openthinclient.ldap.DirectoryException;

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
 * This Dialog provides the option to start one or more clients at a specific
 * date by means of MagicPacket/WakeOnLan
 */

public class StartAtTimeDialog extends JPanel {

	private static StartAtTimeDialog startAtTime;
	private static CronCellEditor cce;

	public static StartAtTimeDialog getInstance() {
		startAtTime = new StartAtTimeDialog();
		cce = startAtTime.new CronCellEditor(); // TODO wohin sonst ?
		return startAtTime;
	}

	private static final long serialVersionUID = 1L;
	private Node node;
	private JComponent footerComponent = null;
	private DialogClientDetailViewEditorPanel detView;
	private JComponent mainComponent = null;
	private JComponent headerComponent = null;
	private JSplitPane splity = null;
	private CellConstraints cc = new CellConstraints();
	private JComponent infoFooter = null;
	private Node[] selection;
	private TopComponent tc;

	public void init(Node node, Node[] selection, TopComponent tc) {
		this.node = node;
		this.detView = DialogClientDetailViewEditorPanel.getInstance();
		this.selection = selection;
		this.tc = tc;
		this.footerComponent = null;
		this.mainComponent = null;
		this.headerComponent = null;
		this.splity = new JSplitPane();
		this.cc = new CellConstraints();
		this.infoFooter = null;

		dataInput();
		dataInput2(); // TODO umbenennen - beide
	}

	public void dataInput2() {
		// mit getDN arbeiten.....!!!
		final Realm realm = (Realm) node.getLookup().lookup(Realm.class); // TODO
		// nur
		// einmal
		// realm
		// laden
		// !?
		try {
			final DirContext ctx = getContext(realm);
			try {

				final String ouName = "ou=clients";

				final Attributes att = new BasicAttributes(true);
				att.put(new BasicAttribute("manager"));

				final NamingEnumeration ne = ctx.search(ouName, att);
				final TreeMap<Integer, String> map = new TreeMap<Integer, String>();
				Vector vector;
				String clientName = "";
				final HashMap<String, Integer> clientToRow = new HashMap<String, Integer>();
				final TreeSet<String> set = new TreeSet<String>();

				for (int i = 0; i < detView.clientsTable.getRowCount(); i++)
					clientToRow.put((String) detView.clientsTableModel.getValueAt(i, 1),
							i);

				while (ne.hasMoreElements()) {
					final SearchResult sr = (SearchResult) ne.next();
					final Attributes srName = sr.getAttributes();

					// TODO variablen-namen ändern......!!!
					clientName = srName.get("cn").get().toString();
					final String[] x = srName.get("manager").get().toString().split(", ");
					String ids = "";

					if (x != null && x.length != 0)
						for (int i = 0; i < x.length; i++)
							if (i % 2 == 0) {
								set.add(x[i]);
								map.put(Integer.valueOf(x[i]), x[i + 1].trim());
							}

					for (final String str : set) {
						if (ids != "")
							ids = ids.concat(",");
						ids = ids.concat(str);
					}

					detView.clientsTableModel.setValueAt(ids,
							clientToRow.get(clientName), 8);

				}

				if (map != null && !map.isEmpty())
					for (final Map.Entry<Integer, String> entry : map.entrySet()) {
						vector = new Vector();
						final int key = entry.getKey();
						final String value = entry.getValue();
						vector.add(key);
						vector.add(value);
						detView.cronTableModel.addRow(vector);
					}
			} finally {
				ctx.close();
			}
		} catch (final NamingException e) {
			e.printStackTrace();
			ErrorManager.getDefault().notify(e);
		}
	}

	public void dataInput() {
		detView.setAllowSelection(Boolean.TRUE);
		detView.init(selection, tc);
		setLayout(new FormLayout("f:p:g", "15dlu,30dlu,f:d:g,30dlu"));
		Font f = UIManager.getFont("TitledBorder.font");
		f = f.deriveFont(Font.BOLD, AffineTransform.getScaleInstance(1.5, 1.5));
		final JLabel nameLabel = new JLabel(node.getName() != null
				? node.getName()
				: " ");
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
		mainComponent.setSize(800, 5 * detView.clientsTable.getRowHeight());

		refreshFooter();
		detView.clientsTable.setRowSelectionAllowed(false);
		detView.clientsTable.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent e) {
						if (e.getValueIsAdjusting())
							return;
						detView.setRowsSelectedInTable();
					}
				});

		detView.clientsTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				detView.clientsTable.setRowSelectionAllowed(false);
				detView.cronTable.clearSelection();
			}
		});

		// TODO ab hier ((++alle achten ersetzen....))
		// das hier is mein validator.... 2zeilen
		final TableColumn col = detView.clientsTable.getColumnModel().getColumn(8);
		col.setCellEditor(cce);

		footerComponent = detView.getFooterComponent();
		footerComponent.setSize(800, 5 * detView.clientsTable.getRowHeight());
		splity = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, mainComponent,
				footerComponent);
		splity.setDividerLocation(0.5);

		add(splity, cc.xy(1, 3));
		infoFooter = detView.cronButtons();
		add(infoFooter, cc.xy(1, 4));

		setPreferredSize(new Dimension(800, 600));
	}

	public void doEdit() {

		final JButton cancelButton = new JButton(Messages.getString("Cancel"));
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				detView.setBooleanValueAtClients(false);
				detView.setRowsSelectedInTable();
			}
		});

		final JButton okButton = new JButton(Messages.getString("OK"));
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				okButtonAction();
				detView.setBooleanValueAtClients(false);
				detView.setRowsSelectedInTable();
			}
		});

		final JButton selectAll = new JButton(Messages.getString("selectAll"));
		selectAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				detView.setBooleanValueAtClients(true);
				detView.setRowsSelectedInTable();
				refreshMain();
			}
		});

		final JButton deselectAll = new JButton(Messages.getString("deselectAll"));
		deselectAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				detView.setBooleanValueAtClients(false);
				detView.setRowsSelectedInTable();
				refreshMain();
			}
		});

		final CellConstraints cc = new CellConstraints();
		final JPanel jpl = new JPanel(new FormLayout("l:p,c:p:g,r:p", "f:p:g"));
		final ButtonBarBuilder builder1 = new ButtonBarBuilder();
		final ButtonBarBuilder builder2 = new ButtonBarBuilder();

		builder1.addGridded(selectAll);
		builder1.addGridded(deselectAll);
		builder2.addGridded(cancelButton);
		builder2.addGridded(okButton);
		jpl.add(builder1.getPanel(), cc.xy(1, 1));
		jpl.add(new JPanel(), cc.xy(2, 1));
		jpl.add(builder2.getPanel(), cc.xy(3, 1));

		final DialogDescriptor descriptor = new DialogDescriptor(this, node
				.getName(), true, new Object[]{jpl}, okButton,
				DialogDescriptor.DEFAULT_ALIGN, null, new ActionListener() {
					public void actionPerformed(ActionEvent e) {

					}
				});
		descriptor.setClosingOptions(new Object[]{cancelButton, okButton});

		final Dialog dialog = DialogDisplayer.getDefault().createDialog(descriptor);
		dialog.addWindowListener(new WindowAdapter() {
			@Override
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
		});

		// final int maxSize = dialog.getComponents()[0].getHeight()
		// - this.getComponents()[0].getHeight()
		// - this.getComponents()[1].getHeight();
		// if (maxSize * 0.9 < (detView.clientsTable.getRowCount() + 1)
		// * detView.clientsTable.getRowHeight() + 5)
		splity.setDividerLocation(0.5);
		// else
		// this.splity.setDividerLocation(((detView.clientsTable.getRowCount() + 1)
		// * detView.clientsTable.getRowHeight() + 5));

		dialog.setIconImage(Utilities.loadImage(
				"org/openthinclient/console/icon.png", true));
		dialog.setVisible(true);
	}

	private DirContext getContext(Realm realm) throws NamingException {
		final Hashtable env = new Hashtable();
		env
				.put(Context.INITIAL_CONTEXT_FACTORY,
						"com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, realm.getConnectionDescriptor().getLDAPUrl());
		return new InitialDirContext(env);
	}

	public void okButtonAction() {
		// need?
		// if (table.getCellEditor() != null) {
		// table.getCellEditor().stopCellEditing();
		// TableCellEditor tce = null;
		// if (table.isEditing())
		// tce = table.getCellEditor();
		// if (tce != null)
		// tce.stopCellEditing();

		// TODO variablen/methoden umbenennen !!!!

		final Realm realm = (Realm) node.getLookup().lookup(Realm.class);

		// TODO thread!? (was passiert bei "ok" und "close-main" !?)

		String valueX;

		final TreeMap<Integer, String> y = new TreeMap<Integer, String>();
		for (Integer x = 0; x < detView.clientsTableModel.getRowCount(); x++) {
			valueX = (String) detView.clientsTableModel.getValueAt(x, 8);
			if (valueX != null && !valueX.isEmpty())
				y.put(x, valueX);
		}
		final Node[] list = node.getChildren().getNodes();
		Client client;

		if (!y.isEmpty())
			for (final Map.Entry<Integer, String> entry : y.entrySet()) {
				final String value = entry.getValue();
				final Integer key = entry.getKey();
				client = (Client) list[key].getLookup().lookup(Client.class);
				// client.setManager(putTogether(value));

				// for fast delete....
				// final Node[] x = node.getChildren().getNodes();
				// for (final Node node : x) {
				// final Client client = (Client) node.getLookup().lookup(Client.class);
				// client.setManager("");
				try {
					realm.getDirectory().save(client);
				} catch (final DirectoryException e) {
					ErrorManager.getDefault().notify(e);
				}
			}
	}

	public String putTogether(String ids) {
		String x = "";

		// TODO need?
		final String id = "";
		final String cj = "";

		final String[] arr = ids.split(",");

		for (final String arg : arr) {
			if (x != "")
				x = x.concat(", ");
			x = x
					.concat(id
							+ arg
							+ ", "
							+ cj
							+ (String) detView.cronTableModel.getValueAt(
									Integer.valueOf(arg), 1));
		}

		return x;
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
			int tempDivLoc = 0;
			tempDivLoc = this.splity.getDividerLocation();
			this.mainComponent = detView.getMainComponent();
			this.splity.setTopComponent(mainComponent);
			this.splity.setDividerLocation(tempDivLoc);
		} else
			this.mainComponent = detView.getMainComponent();
	}

	private void refreshInfoFooter() {
		if (this.getComponents().length > 3) {
			this.remove(3);
			this.infoFooter = detView.cronButtons();
			this.add(infoFooter, cc.xy(1, 4));
			this.revalidate();
			this.validate();
			this.repaint();
		}
	}

	@SuppressWarnings("serial")
	public class CronCellEditor extends AbstractCellEditor
			implements
				TableCellEditor {

		JComponent component = new JTextField();
		int tables;
		String oldText;

		public Component getTableCellEditorComponent(JTable table, Object value,
				boolean isSelected, int rowIndex, int vColIndex) {

			if (table.equals(detView.clientsTable))
				tables = 1;
			else if (table.equals(detView.cronTable))
				tables = 2;

			if (value != null)
				oldText = value.toString();
			((JTextField) component).setText((String) value);

			return component;
		}

		public Object getCellEditorValue() {
			final String newText = ((JTextField) component).getText();

			switch (tables){
				case 1 :
					final int ids = detView.cronTableModel.getRowCount() - 1;
					if (ids >= 0
							&& newText.matches("[0-" + ids + "]?([,]{1}[0-" + ids + "]+)*"))
						return newText;
					else {
						DialogDisplayer.getDefault().notify(
								new NotifyDescriptor(Messages
										.getString("StartAtTime.error.invalidID"), Messages
										.getString("StartAtTime.error.change"),
										NotifyDescriptor.DEFAULT_OPTION,
										NotifyDescriptor.ERROR_MESSAGE, null, null));
						return oldText;
					}
				case 2 :
					final String min = "(([*]|[0-9]|[1-5][0-9])((|/|-|,)([0-9]|[1-5][0-9]))*)";
					final String hour = "(([*]|[0-9]|1[0-9]|2[0-3])((|/|-|,)([0-9]|1[0-9]|2[0-3]))*)";
					final String day = "(([*]|[1-9]|[12][0-9]|3[01])((|/|-|,)([1-9]|[12][0-9]|3[01]))*)";
					final String months = "jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec";
					final String month = "(([*]|[1-9]|1[012]|" + months
							+ ")((|/|-|,)([1-9]|1[012]|" + months + "))*)";
					final String wdays = "sun|mon|tue|wed|thu|fri|sat";
					final String wday = "(([*]|[0-6]|" + wdays + ")((|/|-|,)([0-6]|"
							+ wdays + "))*)";
					final String blank = "( )";
					final String cron = min + blank + hour + blank + day + blank + month
							+ blank + wday;

					if (newText.matches(cron + "+((\\|)" + cron + "+)*")
							&& !newText.matches(".*(/0|\\*,|\\*-|/.-|/(" + months + ")|/("
									+ wdays + ")).*"))
						return newText;
					else {
						DialogDisplayer.getDefault().notify(
								new NotifyDescriptor(Messages
										.getString("StartAtTime.error.invalidCron"), Messages
										.getString("StartAtTime.error.change"),
										NotifyDescriptor.DEFAULT_OPTION,
										NotifyDescriptor.ERROR_MESSAGE, null, null));
						return oldText;
					}

				default :
					return newText;
			}
		}
	}

	static class DialogClientDetailViewEditorPanel extends AbstractDetailView {
		private static DialogClientDetailViewEditorPanel detailView;

		public static DialogClientDetailViewEditorPanel getInstance() {

			if (null == detailView)
				detailView = new DialogClientDetailViewEditorPanel();
			return detailView;
		}

		private Set rowsSelectedInTable;
		private JTextField queryField;
		public JTable clientsTable;
		private JComponent mainComponent = null;
		private StringFilterTableModel clientsTableModel;
		private SunTableSorter sts;
		private Node clientNode;
		private Node[] selection;
		private TopComponent tc;
		public static final int INSTALL = 0;
		public static final int BOTH = 2;
		private JTable cronTable;
		private DefaultTableModel cronTableModel;
		private boolean globalEmptyList;
		private LinkedHashMap<String, String> cronData;

		private boolean allowSelection = false;

		/*
		 * @see org.openthinclient.console.AbstractDetailView#getHeaderComponent()
		 */
		@Override
		public JComponent getHeaderComponent() {
			// make sure that the main component has been initialized
			getMainComponent();

			final DefaultFormBuilder dfb = new DefaultFormBuilder(new FormLayout(
					"p, 10dlu, r:p, 3dlu, f:p:g"));
			dfb.setDefaultDialogBorder();
			dfb.setLeadingColumnOffset(2);
			dfb.setColumn(3);

			queryField = new JTextField();
			dfb.append(Messages.getString("DirObjectListNode.filter"), queryField);
			dfb.nextLine();

			queryField.getDocument().addDocumentListener(new DocumentListener() {
				public void changedUpdate(DocumentEvent e) {
					clientsTableModel.setFilter(queryField.getText());
					setBooleanValueAtClients(false);
					setRowsSelectedInTable();
				}

				public void removeUpdate(DocumentEvent e) {
					clientsTableModel.setFilter(queryField.getText());
					setBooleanValueAtClients(false);
					setRowsSelectedInTable();
				}

				public void insertUpdate(DocumentEvent e) {
					clientsTableModel.setFilter(queryField.getText());
					setBooleanValueAtClients(false);
					setRowsSelectedInTable();
				}

				@Override
				protected void finalize() throws Throwable {
					clientsTableModel.setFilter("");
					clientsTable.clearSelection();
					super.finalize();
				}

			});

			dfb.add(new JLabel(IconManager.getInstance(DetailViewProvider.class,
					"icons").getIcon("tree." + "binocular")), new CellConstraints(1, 1,
					1, dfb.getRowCount(), CellConstraints.CENTER, CellConstraints.TOP));

			return dfb.getPanel();
		}

		/*
		 * @see org.openthinclient.console.DetailView#getRepresentation()
		 */
		public JComponent getMainComponent() {
			if (null == mainComponent) {
				clientsTable = new JTable();
				clientsTableModel = new StringFilterTableModel();
				sts = new SunTableSorter(clientsTableModel);
				sts.setTableHeader(clientsTable.getTableHeader());
				clientsTable.setModel(sts);
				mainComponent = new JScrollPane(clientsTable);
				mainComponent.setBackground(UIManager.getColor("TextField.background"));
			} else
				clientsTable.clearSelection();
			return mainComponent;

		}

		/*
		 * @see org.openthinclient.console.DetailView#init(org.openide.nodes.Node[])
		 */
		public void init(Node[] selection, TopComponent tc) {
			this.mainComponent = null;
			this.clientsTable = null;
			this.clientsTableModel = null;
			this.queryField = null;
			this.clientNode = null;
			this.selection = null;
			this.sts = null;
			this.tc = null;
			this.selection = selection;
			this.tc = tc;
			for (final Node node : selection)
				if (node instanceof DirObjectListNode) {
					clientNode = node;
					setClientList((DirObjectListNode) node, tc, null);

					break;
				}
		}

		private void setClientList(final DirObjectListNode don,
				final TopComponent tc, final Node node) {
			// if (don.getName().equalsIgnoreCase(
			// Messages.getString("node.AvailablePackagesNode")))
			getMainComponent();
			clientsTableModel.setTableModel(new ClientListTableModel(don,
					allowSelection));
			// TODO

			//

			// if (showDebFile) {
			// clientsTable
			// .getColumn(
			// Messages
			// .getString("node.PackageListNode.getColumnName.isDebLocal"))
			// .setCellRenderer(
			// // Standard-Renderer erweitern
			// new DefaultTableCellRenderer() {
			// @Override
			// public Component getTableCellRendererComponent(JTable table,
			// Object value, boolean isSelected, boolean hasFocus,
			// int row, int column) {
			// // Label der Oberklasse erweitern
			// final JLabel label = (JLabel) super
			// .getTableCellRendererComponent(table, value,
			// isSelected, hasFocus, row, column);
			// // Lediglich Text und Grafik anpassen
			// if (value != null) {
			// label.setText("");
			// label.setIcon((ImageIcon) value);
			// }
			// return label;
			// }
			// });
			// clientsTable
			// .getColumnModel()
			// .removeColumn(
			// clientsTable
			// .getColumn(Messages
			// .getString("node.PackageListNode.getColumnName.isDebLocal.DontShow")));
			// }
			sts.setSortingStatus(1, SunTableSorter.ASCENDING);
		}

		public void setBooleanValueAtClients(boolean b) {
			final int columnValue = clientsTable.getColumnModel().getColumnIndex(
					Messages.getString("node.PackageListNode.getColumnName.tagged"));
			// TODO Messages.... und sonstige package....
			for (int i = 0; i < clientsTable.getRowCount(); i++)
				clientsTableModel.setValueAt(b, i, columnValue);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.openthinclient.console.AbstractDetailView#getFooterComponent()
		 */
		@SuppressWarnings("serial")
		@Override
		public JComponent getFooterComponent() {
			// DetailView detail = null;
			// if (tableModel.getTableModel().getClass() ==
			// ClientListTableModel.class) {
			// if (rowSelectedInTable > -1
			// && rowSelectedInTable < clientsTable.getRowCount()) {
			// boolean isSet = false;
			// final Node[] nodeArray = new Node[1];
			//
			// for (final Node node : selection)
			// if (null != node)
			// for (final Node childNode : node.getChildren().getNodes())
			// if (null != childNode)
			// if (childNode
			// .getName()
			// .equalsIgnoreCase(
			// (String) clientsTable
			// .getValueAt(
			// rowSelectedInTable,
			// clientsTable
			// .getColumnModel()
			// .getColumnIndex(
			// Messages
			// .getString("node.PackageListNode.getColumnName.name"))))) {
			// detail = new DirObjectNode(clientNode,
			// ((ClientListTableModel) tableModel.getTableModel())
			// .getClientsAtRow(rowSelectedInTable))
			// .getDetailView();
			// nodeArray[0] = childNode;
			// detail.init(nodeArray, tc);
			// isSet = true;
			// }
			// if (isSet) {
			// final JComponent jco = detail.getMainComponent();
			// return jco;
			// } else
			// return new JLabel(Messages
			// .getString("PackageDetailView.noRowSelected"));
			// } else
			// return new JLabel(Messages
			// .getString("PackageDetailView.noRowSelected"));
			// } else
			// return new
			// JLabel(Messages.getString("PackageDetailView.noRowSelected"));

			cronTableModel = new DefaultTableModel();

			cronTableModel.addColumn(Messages.getString("StartAtTime.table.id"));
			cronTableModel.addColumn(Messages.getString("StartAtTime.table.cronJob"));

			cronTable = new JTable(cronTableModel) {
				@Override
				public boolean isCellEditable(int row, int col) {
					switch (col){
						case 0 :
							return false;
						default :
							return true;
					}
				}
			};

			final TableColumn col = cronTable.getColumnModel().getColumn(1);
			col.setCellEditor(cce);

			cronTable.getColumnModel().getColumn(0).setMaxWidth(200);

			cronTable.getSelectionModel().addListSelectionListener(
					new ListSelectionListener() {
						public void valueChanged(ListSelectionEvent e) {
							if (!e.getValueIsAdjusting()) {
								int id;
								String value;

								clientsTable.clearSelection();
								clientsTable.setRowSelectionAllowed(true);

								if (cronTable.getSelectedRow() != -1)
									for (int i = 0; i < clientsTable.getRowCount(); i++) {
										value = (String) clientsTableModel.getValueAt(i, 8);
										if (value != null && !value.isEmpty())
											for (final Integer iter : cronTable.getSelectedRows()) {
												id = (Integer) cronTableModel.getValueAt(iter, 0);
												if (value.contains(String.valueOf(id)))
													clientsTable.addRowSelectionInterval(i, i);
											}
									}
							}
						}
					});

			cronTable.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getButton() == 3 || e.isPopupTrigger()) {
						final int row = cronTable.rowAtPoint(e.getPoint());
						final int[] rows = cronTable.getSelectedRows();
						final int last = rows.length - 1;

						boolean containsSelected = false;
						for (final int i : rows)
							if (i == row)
								containsSelected = true;

						if (containsSelected)
							cronTable.setRowSelectionInterval(rows[0], rows[last]);
						else
							cronTable.setRowSelectionInterval(row, row);

						final JPopupMenu popupMenu = new JPopupMenu();
						final JMenuItem addItem = new JMenuItem(Messages
								.getString("StartAtTime.item.add"));
						final JMenuItem removeItem = new JMenuItem(Messages
								.getString("StartAtTime.item.remove"));

						final Set selectedRows = detailView.getRowsSelectedInTable();
						final int column = clientsTableModel.findColumn(Messages
								.getString("StartAtTime.table.id"));

						addItem.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								if (selectedRows != null && !selectedRows.isEmpty())
									for (int i = last; i >= 0; i--)
										for (final Object obj : selectedRows) {
											final int row = (Integer) obj;
											String value = (String) clientsTableModel.getValueAt(row,
													column);

											if (value == null || value.isEmpty())
												value = String.valueOf(rows[i]);
											else if (!value.contains(String.valueOf(rows[i])))
												value = value.concat("," + rows[i]);

											clientsTableModel.setValueAt(value, row, column);
										}
							}
						});

						removeItem.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								for (int i = last; i >= 0; i--) {
									cronTableModel.removeRow(rows[i]);
									checkID(rows[i]);
								}
							}
						});
						popupMenu.add(addItem);
						popupMenu.add(removeItem);
						popupMenu.show(e.getComponent(), e.getX(), e.getY());
					}
				}
			});

			cronTable.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (e.getKeyChar() == KeyEvent.VK_DELETE
							&& cronTable.getSelectedRow() != -1) {
						final int[] rows = cronTable.getSelectedRows();
						for (int i = rows.length - 1; i >= 0; i--) {
							cronTableModel.removeRow(rows[i]);

							checkID(rows[i]);
						}
						e.consume();
					}
				}
			});

			return new JScrollPane(cronTable);
		}

		private int checkID(int... vars) {
			if (vars != null && vars.length != 0) {
				String valueX;

				final TreeMap<Integer, String> y = new TreeMap<Integer, String>();
				for (Integer x = 0; x < clientsTableModel.getRowCount(); x++) {
					valueX = (String) clientsTableModel.getValueAt(x, 8);
					if (valueX != null && !valueX.isEmpty())
						y.put(x, valueX);
				}

				// TODO nochmal schön neu und so komprimiert...
				// TODO kein ids <0 !!!
				// TODO ganz löschen falls <0 !!!
				// TODO komisches verhalten bei 4,5,6 -> 0 !???

				Object[] arr = null;
				if (!y.isEmpty())
					for (final Map.Entry<Integer, String> entry : y.entrySet()) {
						final String value = entry.getValue();
						final Integer key = entry.getKey();
						final String[] val = value.split(",");
						final List list = Arrays.asList(val);
						for (int x = 0; x < val.length; x++)
							if (Integer.valueOf(val[x]) >= Integer.valueOf(vars[0])) {
								final TreeSet set = new TreeSet(list);
								arr = set.toArray();
								for (int i = 0; i < arr.length; i++)
									if (Integer.valueOf((String) arr[i]) >= Integer
											.valueOf(vars[0])) {
										for (int j = i; j < arr.length; j++)
											if (arr[i].equals(String.valueOf(vars[0])))
												arr[j] = "";
											else if (Integer.valueOf((String) arr[j]) - 1 >= 0)
												arr[j] = String.valueOf(Integer
														.valueOf((String) arr[j]) - 1);
											else
												arr[j] = "";
										break;
									}
								final Set sett = new TreeSet(Arrays.asList(arr));
								String str = "";
								for (final Object obj : sett) {
									if (str != "")
										str = str.concat(",");
									str = str.concat((String) obj);
								}
								clientsTableModel.setValueAt(str, key, 8);
							}

					}
			}
			final int oldID = cronTableModel.getRowCount() - 1;

			for (int i = 0; i <= oldID; i++)
				if (!cronTableModel.getValueAt(i, 0).equals(i))
					for (int j = i; j <= oldID; j++)
						cronTableModel.setValueAt(j, j, 0);

			return cronTableModel.getRowCount();
		}

		public JComponent cronButtons() {
			final CellConstraints cc = new CellConstraints();
			final JPanel jpl = new JPanel();
			jpl.setLayout(new FormLayout("f:p:g,f:p:g,f:p:g,f:p:g,f:p:g,f:p:g,f:p:g",
					"f:p:g")); // TODO bitte anstÃ¤ndig...

			final JButton addButton = new JButton(Messages
					.getString("StartAtTime.buttons.add"));
			addButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					final Vector rowData = new Vector();
					String rowDat = "";

					final Iterator it = cronData.keySet().iterator();
					while (it.hasNext()) {
						final Object key = it.next();
						final Object val = cronData.get(key);
						if (rowDat.equals(""))
							rowDat = "" + val;
						else
							rowDat = rowDat.concat(" " + val);
					}

					rowData.add(checkID()); // TODO checkID und rowDat einfügen etc......
					rowData.add(rowDat);

					final Set selectedRows = detailView.getRowsSelectedInTable();
					final int column = clientsTableModel.findColumn(Messages
							.getString("StartAtTime.table.id"));

					if (selectedRows != null && !selectedRows.isEmpty()) {
						cronTableModel.addRow(rowData);

						for (final Object obj : selectedRows) {
							final int row = (Integer) obj;
							String value;

							if (clientsTableModel.getValueAt(row, column) == null)
								value = rowData.firstElement().toString();
							else {
								value = (String) clientsTableModel.getValueAt(row, column);
								value = value.concat("," + rowData.firstElement());
							}
							clientsTableModel.setValueAt(value, row, column);
						}
					}
					initCronData();
				}
			});

			initCronData();

			final String[] minutes = new String[60];
			for (int i = 1; i <= 60; i++)
				minutes[i - 1] = String.valueOf(i);

			final String[] hours = new String[24];
			for (int i = 1; i <= 24; i++)
				hours[i - 1] = String.valueOf(i);

			final String[] days = new String[31];
			for (int i = 1; i <= 31; i++)
				days[i - 1] = String.valueOf(i);

			final String[] months = new String[12];
			for (int i = 1; i <= 12; i++)
				months[i - 1] = String.valueOf(i);

			final String[] weekDays = new String[7];
			for (int i = 1; i <= 7; i++)
				weekDays[i - 1] = String.valueOf(i);

			jpl.add(generateDropDown(Messages
					.getString("StartAtTime.dropDowns.minutes"), minutes), cc.xy(1, 1));
			jpl.add(generateDropDown(Messages
					.getString("StartAtTime.dropDowns.hours"), hours), cc.xy(2, 1));
			jpl.add(generateDropDown(
					Messages.getString("StartAtTime.dropDowns.days"), days), cc.xy(3, 1));
			jpl.add(generateDropDown(Messages
					.getString("StartAtTime.dropDowns.months"), months), cc.xy(4, 1));
			jpl.add(generateDropDown(Messages
					.getString("StartAtTime.dropDowns.weekdays"), weekDays), cc.xy(5, 1));

			jpl.add(addButton, cc.xy(7, 1));

			return jpl;
		}

		private void initCronData() {

			cronData = new LinkedHashMap<String, String>();
			cronData.put(Messages.getString("StartAtTime.dropDowns.minutes"), "*");
			cronData.put(Messages.getString("StartAtTime.dropDowns.hours"), "*");
			cronData.put(Messages.getString("StartAtTime.dropDowns.days"), "*");
			cronData.put(Messages.getString("StartAtTime.dropDowns.months"), "*");
			cronData.put(Messages.getString("StartAtTime.dropDowns.weekdays"), "*");
			globalEmptyList = true;
		}

		private Component generateDropDown(final String name, String[] contents) {
			final JToggleButton button = new JToggleButton(name);
			final JPopupMenu popup = new JPopupMenu();
			final DefaultListModel listModel = new DefaultListModel();
			final JList list = new JList();

			final MouseAdapter mouseAdapter = new MouseAdapter() {
				// TODO button-pressed-problematik
				@Override
				public void mouseClicked(MouseEvent e) {

					// if (globalEmptyList)
					// list.clearSelection();
					// TODO gehirn einschalten

					if (button.isSelected())
						popup.show(button, 0, button.getSize().height);
					else
						popup.setVisible(false);
				}
			};

			button.addMouseListener(mouseAdapter);
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					if (button.isSelected())
						popup.show(button, 0, button.getSize().height);
					else
						popup.setVisible(false);
				}
			});

			popup.setBorder(new MatteBorder(0, 1, 1, 1, Color.DARK_GRAY));
			popup.setAutoscrolls(true);

			popup.addPopupMenuListener(new PopupMenuListener() {
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					popup.setPopupSize(button.getBounds().width, 147); // TODO height
				}

				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					EventQueue.invokeLater(new Runnable() {
						public void run() {
							button.getModel().setArmed(false);
							button.setSelected(false);
						}
					});
				}

				public void popupMenuCanceled(PopupMenuEvent e) {
				}
			});

			listModel.addElement("*");
			for (final String content : contents)
				listModel.addElement(content);

			list.setModel(listModel);

			list.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(ListSelectionEvent e) {

					String value = "";

					if (!e.getValueIsAdjusting()) {

						if (list.isSelectedIndex(0) && list.getMaxSelectionIndex() != 0)
							list.removeSelectionInterval(1, list.getMaxSelectionIndex());

						for (final Object item : list.getSelectedValues())
							if (value.equals(""))
								value = (String) item;
							else
								value = value.concat("," + (String) item);

						cronData.put(name, value);
						System.out.println(cronData);
					}
				}

			});

			popup.add(new JScrollPane(list));

			return button;
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

		public Collection<Client> getSelectedItems() {
			return ((ClientListTableModel) clientsTableModel.getTableModel())
					.getSelectedClients();
		}

		public Set getRowsSelectedInTable() {
			return rowsSelectedInTable;
		}

		public void setRowsSelectedInTable() {
			rowsSelectedInTable = new TreeSet();
			for (int i = 0; i < clientsTableModel.getRowCount(); i++)
				if (clientsTableModel.getValueAt(i, 0).equals(true))
					rowsSelectedInTable.add(i);
		}

		@Override
		protected void finalize() throws Throwable {
			setBooleanValueAtClients(false);
			setRowsSelectedInTable();
			super.finalize();
		}
	}

}
