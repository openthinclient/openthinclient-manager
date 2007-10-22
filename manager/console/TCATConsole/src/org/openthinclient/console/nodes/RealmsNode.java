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
package org.openthinclient.console.nodes;

import java.awt.EventQueue;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;

import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.cookies.InstanceCookie;
import org.openide.explorer.ExplorerManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.Repository;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.nodes.NodeEvent;
import org.openide.nodes.NodeListener;
import org.openide.nodes.NodeMemberEvent;
import org.openide.nodes.NodeReorderEvent;
import org.openide.util.WeakListeners;
import org.openide.windows.TopComponent;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.AbstractDetailView;
import org.openthinclient.console.AddRealmAction;
import org.openthinclient.console.DetailViewProvider;
import org.openthinclient.console.HTTPLdifImportAction;
import org.openthinclient.console.Messages;
import org.openthinclient.console.NewRealmInitAction;
import org.openthinclient.console.util.StringFilterTableModel;
import org.openthinclient.ldap.DirectoryException;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.levigo.util.swing.IconManager;
import com.levigo.util.swing.table.SunTableSorter;

/** Getting the root node */
// FIXME: gui deadlock here!
// public class RealmsNode extends FilterNode implements
// DetailViewProvider{
public class RealmsNode extends FilterNode {
	/** Getting the children of the root node */
	private static class RealmsChildren extends FilterNode.Children {

		RealmsChildren(Node folderNode) {
			super(folderNode);
		}

		@Override
		protected Node[] createNodes(Object key) {

			final Node n = (Node) key;
			try {
				Realm realm = getRealm(n);
				Node[] node = new Node[]{new RealmNode(n, getNode(), realm)};
				return node;
			} catch (final Exception e) {
				ErrorManager.getDefault().notify(e);
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (DialogDisplayer.getDefault().notify(
								new NotifyDescriptor.Confirmation(Messages
										.getString("RealmsNode.confirmDelete"), //$NON-NLS-1$
										NotifyDescriptor.YES_NO_OPTION)) == NotifyDescriptor.YES_OPTION) {

							// ok, this mumbo-jumbo is necessary, because the Netbeans core is
							// really, really broken:
							// if the InstanceCookie.instanceCreate() fails while
							// deserializing
							// a node instance,
							// it keeps the file open (yes, those suckers can't even use
							// try{...}finally{...} properly! How lame is THAT?).
							// Since there is no way of retrieving the lost handle, we just
							// keep GCing, hoping that
							// the GC closed it somehow, somewhen.
							int trys = 5;
							do {
								try {
									Runtime.getRuntime().gc();
									try {
										Thread.sleep(1000);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									n.destroy();
								} catch (IOException e1) {
									if (trys-- <= 0) {
										ErrorManager.getDefault().annotate(e1,
												Messages.getString("RealmsNode.error.couldNotDelete")); //$NON-NLS-1$
										ErrorManager.getDefault().notify(e1);
									} else
										continue;
								}
							} while (false);
						}
					}
				});

				return new Node[]{new ErrorNode(Messages
						.getString("RealmsNode.error.OpenRealmFailed"), e)}; //$NON-NLS-1$
			}
		}
	}

	private static Realm getRealm(Node node) throws IOException,
			ClassNotFoundException, DirectoryException {
		InstanceCookie ck = (InstanceCookie) node.getCookie(InstanceCookie.class);
		if (ck == null) {
			throw new IllegalStateException(Messages
					.getString("RealmsNode.error.bogus") //$NON-NLS-1$
					+ node.getLookup().lookup(FileObject.class));
		}

		// refresh the realm after it is loaded to reflect changes in the
		// directory.
		Realm realm = (Realm) ck.instanceCreate();
		realm.setNeedsRefresh();

		try {
			updateOnLdifs(realm);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return realm;
	}

	public RealmsNode() throws DataObjectNotFoundException {

		super(DataObject.find(
				Repository.getDefault().getDefaultFileSystem().getRoot().getFileObject(
						"Realms")).getNodeDelegate()); //$NON-NLS-1$

		final DataObject dataObject = DataObject.find(Repository.getDefault()
				.getDefaultFileSystem().getRoot().getFileObject("Realms")); //$NON-NLS-1$

		dataObject.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				dataObject.getPrimaryFile().refresh();
			}
		});

		RealmsChildren rc = new RealmsChildren(getOriginal());
		setChildren(rc);

		disableDelegation(DELEGATE_GET_DISPLAY_NAME);
	}

	// /** An action for adding a realm */
	// public static class AddRealmAction extends AbstractAction {
	//
	// private DataFolder folder;
	//    
	// private static Realm newRealm;
	//    
	// private static boolean automaticRegistration = false;
	//
	// public AddRealmAction() throws DataObjectNotFoundException {
	// super(Messages
	// .getString("action." + AddRealmAction.class.getSimpleName()));
	// //$NON-NLS-1$
	// folder = (DataFolder) DataObject.find(Repository.getDefault()
	// .getDefaultFileSystem().getRoot().getFileObject("Realms")); //$NON-NLS-1$
	// }
	//
	// public AddRealmAction(DataFolder df) {
	// super(Messages.getString("action." +
	// AddRealmAction.class.getSimpleName())); //$NON-NLS-1$
	// folder = df;
	// }
	//
	// public void actionPerformed(ActionEvent ae) {
	// WizardDescriptor.Iterator iterator = new RegisterRealmWizardIterator();
	// WizardDescriptor wizardDescriptor = new WizardDescriptor(iterator);
	// wizardDescriptor.setTitleFormat(new MessageFormat("{0} ({1})"));
	// //$NON-NLS-1$
	// wizardDescriptor.setTitle(Messages.getString("action." //$NON-NLS-1$
	// + AddRealmAction.class.getSimpleName()));
	// Dialog dialog = DialogDisplayer.getDefault().createDialog(
	// wizardDescriptor);
	//      
	// wizardDescriptor.putProperty("enableForward", false);
	// dialog.setSize(830, 600);
	// if(automaticRegistration == false) {
	// dialog.setVisible(true);
	// dialog.toFront();
	// }
	// wizardDescriptor.putProperty("enableForward", true);
	//      
	// if (wizardDescriptor.getValue() == WizardDescriptor.FINISH_OPTION
	// || automaticRegistration == true) {
	//    	
	// Realm realm;
	// if(automaticRegistration == true) {
	// realm = getRealm();
	// }else {
	// realm = (Realm) wizardDescriptor.getProperty("realm"); //$NON-NLS-1$
	// }
	// assert null != realm;
	// automaticRegistration = false;
	//
	// // fix callback handler to use the correct protection domain
	// CallbackHandler callbackHandler =
	// realm.getConnectionDescriptor().getCallbackHandler();
	// if (callbackHandler instanceof UsernamePasswordCallbackHandler)
	// try {
	// ((UsernamePasswordCallbackHandler) callbackHandler)
	// .setProtectionDomain(realm.getConnectionDescriptor().getLDAPUrl());
	// } catch (IOException e) {
	// ErrorManager.getDefault().annotate(e, "Could not update protection
	// domain.");
	// ErrorManager.getDefault().notify(e);
	// }
	// else
	// ErrorManager.getDefault().notify(
	// new IOException(
	// "CallbackHandler was not of the expected type, but "
	// + callbackHandler.getClass()));
	//        
	// FileObject fld = folder.getPrimaryFile();
	// String baseName = "realm-" //$NON-NLS-1$
	// + realm.getConnectionDescriptor().getHostname()
	// + realm.getConnectionDescriptor().getBaseDN(); //$NON-NLS-1$
	// if (fld.getFileObject(baseName, "ser") != null) { //$NON-NLS-1$
	// DialogDisplayer.getDefault().notify(
	// new NotifyDescriptor.Message(Messages
	// .getString("error.RealmAlreadyExists"), //$NON-NLS-1$
	// NotifyDescriptor.WARNING_MESSAGE));
	// return;
	// }
	//
	// try {
	// FileObject writeTo = fld.createData(baseName, "ser"); //$NON-NLS-1$
	// FileLock lock = writeTo.lock();
	// try {
	// ObjectOutputStream str = new ObjectOutputStream(writeTo
	// .getOutputStream(lock));
	// try {
	// str.writeObject(realm);
	// } finally {
	// str.close();
	// }
	// } finally {
	// lock.releaseLock();
	// }
	// } catch (IOException ioe) {
	// ErrorManager.getDefault().notify(ioe);
	// }
	// }
	// }
	//
	// public static boolean isAutomaticRegistration() {
	// return automaticRegistration;
	// }
	//
	// public static void setAutomaticRegistration(boolean automaticRegistration)
	// {
	// AddRealmAction.automaticRegistration = automaticRegistration;
	// }
	//
	// public static Realm getRealm() {
	// return newRealm;
	// }

	// public static void setRealm(Realm realm) {
	// AddRealmAction.newRealm = realm;
	// }
	// }

	/** Declaring the Add Feed action and Add Folder action */
	@Override
	public Action[] getActions(boolean popup) {
		DataFolder df = (DataFolder) getLookup().lookup(DataFolder.class);
		NewRealmInitAction newRealmInitAction = (NewRealmInitAction) NewRealmInitAction
				.findObject(NewRealmInitAction.class, true);
		return new Action[]{new AddRealmAction(df), newRealmInitAction};
	}

	@Override
	public String getName() {
		return Messages.getString("node." + getClass().getSimpleName()); //$NON-NLS-1$
	}

	/*
	 * @see org.openide.nodes.FilterNode#canRename()
	 */
	@Override
	public boolean canRename() {
		return false;
	}

	/*
	 * @see org.openide.nodes.FilterNode#getIcon(int)
	 */
	@Override
	public Image getIcon(int type) {
		return getOpenedIcon(type);
		//    
		// return IconManager.getInstance(DetailViewProvider.class,
		// "icons").getImage(
		// "tree." + getClass().getSimpleName(), IconManager.EFFECT_GRAY50P);
	}

	/*
	 * @see org.openide.nodes.FilterNode#getOpenedIcon(int)
	 */
	@Override
	public Image getOpenedIcon(int type) {
		return IconManager.getInstance(DetailViewProvider.class, "icons").getImage( //$NON-NLS-1$
				"tree." + getClass().getSimpleName()); //$NON-NLS-1$
	}

	public static void updateOnLdifs(Realm realm) throws IOException {
		HTTPLdifImportAction action = new HTTPLdifImportAction(realm
				.getConnectionDescriptor().getHostname());

		if (HTTPLdifImportAction.isEnableAsk())
			action.importAllFromURL(null, realm);
		HTTPLdifImportAction.setEnableAsk(true);
	}

	public DetailView getDetailView() {
		return DetailView.getInstance();
	}

	public static class DetailView extends AbstractDetailView {
		private static DetailView detailView;

		public static DetailView getInstance() {
			if (null == detailView)
				detailView = new DetailView();
			return detailView;
		}

		private JTextField queryField;

		private JTable objectsTable;

		private MouseAdapter listener;

		private JComponent mainComponent;

		private StringFilterTableModel tableModel;

		private SunTableSorter sts;

		private boolean readonly;

		private Class objectClass;

		private static List<RealmsNode> realmsList = new ArrayList<RealmsNode>();

		/*
		 * @see org.openthinclient.console.AbstractDetailView#getHeaderComponent()
		 */
		@Override
		public JComponent getHeaderComponent() {
			// make sure that the main component has been initialized
			getMainComponent();

			DefaultFormBuilder dfb = new DefaultFormBuilder(new FormLayout(
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
			});

			dfb.add(new JLabel(IconManager.getInstance(DetailViewProvider.class,
					"icons").getIcon("tree." + "RealmNode")), //$NON-NLS-1$ //$NON-NLS-2$
					new CellConstraints(1, 1, 1, dfb.getRowCount(),
							CellConstraints.CENTER, CellConstraints.TOP));

			return dfb.getPanel();
		}

		/*
		 * @see org.openthinclient.console.DetailView#getRepresentation()
		 */
		public JComponent getMainComponent() {
			if (null == mainComponent) {
				objectsTable = new JTable();

				tableModel = new StringFilterTableModel();
				sts = new SunTableSorter(tableModel);
				sts.setTableHeader(objectsTable.getTableHeader());

				objectsTable.setModel(sts);

				mainComponent = new JScrollPane(objectsTable);
				mainComponent.setBackground(UIManager.getColor("TextField.background")); //$NON-NLS-1$
				mainComponent.setBorder(BorderFactory.createEmptyBorder());
			}
			return mainComponent;
		}

		/*
		 * @see org.openthinclient.console.DetailView#init(org.openide.nodes.Node[])
		 */
		public void init(Node[] selection, TopComponent tc) {

			// This is for the reload of the Query every time when the Node is
			// called... FT
			if (null != queryField)
				queryField.setText("");
			// find the realm node
			if (null != selection)
				for (Node node : selection) {
					if (node instanceof RealmsNode) {
						setDirObjectList((RealmsNode) node, tc);
						break;
					}
				}
		}

		private static class DirObjectTableModel extends AbstractTableModel
				implements
					NodeListener {
			private static final Pattern SPLIT_PATTERN = Pattern.compile("\\s*,\\s*"); //$NON-NLS-1$

			private final Class objectClass;

			private final Map<String, Method> getters = new HashMap<String, Method>();

			private final String columnNames[];

			private final String getterNames[];

			private final RealmsNode rnode;

			public DirObjectTableModel(RealmsNode dol, Class objectClass) {
				this.rnode = dol;
				this.objectClass = objectClass;

				String tableDesc = Messages.getString("table." //$NON-NLS-1$
						+ "RealmsNode");

				String splitDesc[] = SPLIT_PATTERN.split(tableDesc);

				if (splitDesc.length % 2 != 0)
					ErrorManager.getDefault().log(ErrorManager.WARNING,
							"Table description for " + objectClass + " is malformed"); //$NON-NLS-1$ //$NON-NLS-2$

				columnNames = new String[splitDesc.length / 2];
				getterNames = new String[splitDesc.length / 2];

				int i = 0, j = 0;
				while (i < splitDesc.length - 1) {
					columnNames[j] = splitDesc[i++];
					getterNames[j++] = splitDesc[i++];
				}

				// attach listener
				dol.addNodeListener((NodeListener) WeakListeners.create(
						NodeListener.class, this, dol));
			}

			/*
			 * @see javax.swing.table.TableModel#getValueAt(int, int)
			 */
			public Object getValueAt(int row, int column) {
				Node[] nodes = rnode.getChildren().getNodes();
				if (nodes.length <= row)
					return ""; //$NON-NLS-1$

				Realm realm = (Realm) nodes[row].getLookup().lookup(Realm.class);

				// HACK: lets me get the node without making it visible as a
				// row.
				if (column == -1)
					return nodes[row];

				if (null == realm)
					return "..."; //$NON-NLS-1$

				String getterName = getterNames[column];

				Method getter = getters.get(getterName);
				if (null == getter) {
					try {
						getter = objectClass.getMethod(getterName, new Class[]{});
						getters.put(getterName, getter);
					} catch (Exception e) {
						ErrorManager.getDefault().notify(e);
						return "<" + e.getLocalizedMessage() + ">"; //$NON-NLS-1$ //$NON-NLS-2$
					}
				}

				try {
					Object obj = getter.invoke(realm, new Object[]{});
					if (obj == null) {
						return "";
					}
					if (obj.equals("RealmConfiguration")) {
						String dn = realm.getConnectionDescriptor().getBaseDN();
						dn = dn.replace("\\,", "#%COMMA%#");
						String[] s = dn.split(",");
						String nameRealm = "";
						if (s.length > 0) {
							nameRealm = s[0].replace("ou=", "").trim();
							nameRealm = nameRealm.replace("#%COMMA%#", "\\,").trim();
						}
						return nameRealm;
					}
					return obj;
				} catch (Exception e) {
					ErrorManager.getDefault().notify(e);
					return "<" + e.getLocalizedMessage() + ">"; //$NON-NLS-1$ //$NON-NLS-2$
				}
			}

			Node getNodeAtRow(int row) {
				Node[] nodes = rnode.getChildren().getNodes();
				if (nodes.length <= row)
					return null;
				return nodes[row];
			}

			/*
			 * @see javax.swing.table.TableModel#getColumnCount()
			 */
			public int getColumnCount() {
				return columnNames.length;
			}

			/*
			 * @see javax.swing.table.TableModel#getColumnName(int)
			 */
			@Override
			public String getColumnName(int columnIndex) {
				return columnNames[columnIndex];
			}

			/*
			 * @see javax.swing.table.TableModel#getRowCount()
			 */
			public int getRowCount() {
				return rnode.getChildren().getNodes().length;
			}

			/*
			 * @see org.openide.nodes.NodeListener#childrenAdded(org.openide.nodes.NodeMemberEvent)
			 */
			public void childrenAdded(NodeMemberEvent ev) {
				propagateChangeOnEDT();
			}

			/*
			 * @see org.openide.nodes.NodeListener#childrenRemoved(org.openide.nodes.NodeMemberEvent)
			 */
			public void childrenRemoved(NodeMemberEvent ev) {
				propagateChangeOnEDT();
			}

			/*
			 * @see org.openide.nodes.NodeListener#childrenReordered(org.openide.nodes.NodeReorderEvent)
			 */
			public void childrenReordered(NodeReorderEvent ev) {
				propagateChangeOnEDT();
			}

			/*
			 * @see org.openide.nodes.NodeListener#nodeDestroyed(org.openide.nodes.NodeEvent)
			 */
			public void nodeDestroyed(NodeEvent ev) {
				propagateChangeOnEDT();
			}

			/*
			 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
			 */
			public void propertyChange(PropertyChangeEvent evt) {
				propagateChangeOnEDT();
			}

			/**
			 * 
			 */
			private void propagateChangeOnEDT() {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						fireTableDataChanged();
					}
				});
			}
		}

		/**
		 * @param rnode
		 * @param tc
		 */
		private void setDirObjectList(final RealmsNode rnode, final TopComponent tc) {
			getMainComponent();

			objectClass = Realm.class;
			tableModel.setTableModel(new DirObjectTableModel(rnode, objectClass));
			sts.setSortingStatus(0, SunTableSorter.ASCENDING);
			boolean isIn = false;
			for (RealmsNode ref : realmsList)
				if (ref.getName().equalsIgnoreCase(rnode.getName()))
					isIn = true;
			if (!isIn) {
				if (realmsList.size() > 0) {
					objectsTable
							.removeMouseListener(objectsTable.getMouseListeners()[(objectsTable
									.getMouseListeners().length) - 1]);
					realmsList.remove(realmsList.size() - 1);
				}
				if (null != tc && tc instanceof ExplorerManager.Provider) {
					listener = new MouseAdapter() {
						@Override
						public void mouseClicked(MouseEvent e) {
							if (e.getClickCount() > 1) {
								int selectedRow = objectsTable.getSelectedRow();
								if (selectedRow < 0)
									return;
								final RealmNode nodeAtRow = (RealmNode) (objectsTable
										.getModel()).getValueAt(selectedRow, -1);

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
									} catch (PropertyVetoException e1) {
										ErrorManager.getDefault().notify(e1);
									}
							}
						}
					};
					realmsList.add(rnode);
					objectsTable.addMouseListener((MouseListener) WeakListeners.create(
							MouseListener.class, listener, objectsTable));
				}
			}
		}

		/*
		 * @see org.openthinclient.console.DetailView#setReadonly(boolean)
		 */
		public void setReadonly(boolean readonly) {
			this.readonly = readonly;
		}

		/*
		 * @see org.openthinclient.console.DetailView#isReadonly()
		 */
		public boolean isReadonly() {
			return readonly;
		}

		/*
		 * @see org.openthinclient.console.ObjectEditorPart#getTitle()
		 */
		public String getTitle() {
			return null;
		}

	}

	// public static ArrayList<Node> getListNode() {
	// return listNode;
	// }

}
