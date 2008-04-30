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
package org.openthinclient.console.ui;

import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.SortOrder;
import org.openide.ErrorManager;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.AssociatedObjectsProvider;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Group;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.User;
import org.openthinclient.common.model.UserGroup;
import org.openthinclient.console.Messages;
import org.openthinclient.console.util.ExcludeFilterTableModel;
import org.openthinclient.ldap.DirectoryException;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.ButtonStackBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;
import com.levigo.util.swing.table.JTableFormatter;

/**
 * A GUI component that allows adding and removing of DirectoryObjects to other
 * DirectoryObjects. The component shows two tables. The left one contains the
 * DirectoryObjects that are already choosen and the right all other
 * DirectoryObjects of given class existing or this Realm.
 * 
 * @author Natalie Bohnert
 */
public class AssociationEditor extends JPanel {
	/**
	 * The action used to open the frame to select objects to be added.
	 */
	private final class OpenAddFrameAction extends AbstractAction {
		/**
		 * @param table
		 */
		private OpenAddFrameAction(JTable table) {
			super(Messages.getString("AssociationEditor.addButton"));
		}

		public void actionPerformed(ActionEvent e) {
			final JDialog d = getAddDialog();
			availableObjectsTableModel.updateFilteredRows();
			d.setSize(830, 600);
			locateDialog(d);
			d.setVisible(true);
		}

		/**
		 * Locates the dialog on screen
		 */
		private void locateDialog(JDialog d) {
			int x;
			int y;
			// locate the dialog in the center of the parent
			final Container parent = d.getParent();
			if (parent.getWidth() >= d.getWidth())
				x = parent.getX() + (parent.getWidth() - d.getWidth()) / 2;
			else
				x = parent.getX() + 20;
			if (parent.getHeight() >= getHeight())
				y = parent.getY() + (parent.getHeight() - d.getHeight()) / 2;
			else
				y = parent.getY() + 20;

			d.setLocation(x, y);
		}
	}

	/**
	 * @author levigo
	 * 
	 * To change this generated comment go to Window>Preferences>Java>Code
	 * Generation>Code Template
	 */
	private final class AddObjectsAction extends AbstractAction
			implements
				ListSelectionListener {
		private final JXTable availableTable;
		private final DirObjectsTableModel membersTableModel;
		private final JDialog dialog;

		/**
		 * @param availableTable
		 * @param dialog
		 */
		private AddObjectsAction(JXTable availableTable,
				DirObjectsTableModel membersTableModel, JDialog dialog) {
			super(Messages.getString("AssociationEditor.ok"));
			this.availableTable = availableTable;
			this.membersTableModel = membersTableModel;
			this.dialog = dialog;

			setEnabled(availableTable.getSelectedRowCount() > 0);
			availableTable.getSelectionModel().addListSelectionListener(this);
		}

		public void actionPerformed(ActionEvent e) {
			final int[] selectedRows = availableTable.getSelectedRows();
			final DirObjectsTableModel availableTableModel = (DirObjectsTableModel) availableObjectsTableModel
					.getTableModel();

			// undo effects of sorting!
			for (int i = 0; i < selectedRows.length; i++)
				selectedRows[i] = availableTable
						.convertRowIndexToModel(selectedRows[i]);

			for (final int i : selectedRows)
				membersTableModel.addDirectoryObject(availableTableModel
						.getDirectoryObjectAt(availableObjectsTableModel
								.getUnfilteredRowIndex(i)));

			commit();

			dialog.setVisible(false);
		}

		/*
		 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
		 */
		public void valueChanged(ListSelectionEvent e) {
			setEnabled(availableTable.getSelectedRowCount() > 0);
		}
	}

	/**
	 * @author levigo
	 * 
	 * To change this generated comment go to Window>Preferences>Java>Code
	 * Generation>Code Template
	 */
	private final class RemoveAction extends AbstractAction
			implements
				ListSelectionListener {
		private final JXTable table;

		/**
		 * @param table
		 */
		private RemoveAction(JXTable table) {
			super(Messages.getString("AssociationEditor.deleteButton"));
			this.table = table;

			setEnabled(table.getSelectedRowCount() > 0);
			table.getSelectionModel().addListSelectionListener(this);
		}

		public void actionPerformed(ActionEvent e) {
			final int[] selectedRows = table.getSelectedRows();
			final DirObjectsTableModel memberTableModel = (DirObjectsTableModel) table
					.getModel();

			// undo effects of sorting!
			for (int i = 0; i < selectedRows.length; i++)
				selectedRows[i] = table.convertRowIndexToModel(selectedRows[i]);

			// remove in descending order lest we don't break the indices
			Arrays.sort(selectedRows);

			for (int i = selectedRows.length - 1; i >= 0; i--)
				memberTableModel.removeDirectoryObjectAt(selectedRows[i]);
			commit();
		}

		/*
		 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
		 */
		public void valueChanged(ListSelectionEvent e) {
			setEnabled(table.getSelectedRowCount() > 0);
		}
	}

	private final Set<? extends DirectoryObject> members;
	private final Realm realm;
	private final Class memberClass;
	private final DirectoryObject dirObject;

	private final int type;

	public final static int TYPE_MEMBERS = 1;
	public final static int TYPE_ASSOC_OBJECTS = 2;
	private JXTable availableObjectsTable;
	private DirObjectsTableModel membersTableModel;
	private ExcludeFilterTableModel availableObjectsTableModel;

	/**
	 * 
	 */
	public AssociationEditor(Set<? extends DirectoryObject> members, Realm realm,
			Class memberClass, DirectoryObject dirObject, int type) {

		this.members = members;
		this.realm = realm;
		this.memberClass = memberClass;
		this.dirObject = dirObject;
		this.type = type;

		setName(getTitle());
		init();
	}

	/**
	 * 
	 */
	protected JDialog getAddDialog() {
		final JDialog f = new JDialog((Dialog) SwingUtilities.getRoot(this),
				Messages.getString("AssociationEditor.choice"), true);
		final DefaultFormBuilder dfb = new DefaultFormBuilder(new FormLayout(
				"f:p:g"));
		dfb.setDefaultDialogBorder();

		dfb.appendTitle(Messages.getString("AssociationEditor.select"));
		dfb.nextLine();
		dfb.appendRelatedComponentsGapRow();
		dfb.nextLine();
		dfb.appendRow("f:max(100dlu;min):g");
		dfb.append(new JScrollPane(getAvailableObjectsTable()));
		dfb.nextLine();

		final ButtonBarBuilder bbb = new ButtonBarBuilder();
		final JButton ok = new JButton(new AddObjectsAction(
				getAvailableObjectsTable(), membersTableModel, f));
		bbb.addGridded(ok);
		bbb.addRelatedGap();

		final AbstractAction cancelAction = new AbstractAction(Messages
				.getString("AssociationEditor.cancel")) {
			public void actionPerformed(ActionEvent e) {
				f.setVisible(false);
			}
		};
		bbb.addGridded(new JButton(cancelAction));

		dfb.appendUnrelatedComponentsGapRow();
		dfb.nextLine();
		dfb.append(bbb.getPanel());
		dfb.nextLine();

		f.setContentPane(dfb.getPanel());
		f.getRootPane().setDefaultButton(ok);

		f.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke("ESCAPE"), "ESCAPE");
		f.getRootPane().getActionMap().put("ESCAPE", cancelAction);

		f.pack();

		return f;
	}

	/**
	 * 
	 */
	private void init() {
		setLayout(new FormLayout("f:p:g, 5dlu, p", //$NON-NLS-1$
				"f:p:g")); //$NON-NLS-1$
		setBorder(Borders.DIALOG_BORDER);

		final JXTable memberTable = createMemberTable();

		add(new JScrollPane(memberTable), "1,1");

		if (canModify(memberClass))
			add(initButtonPanel(memberTable), "3,1");
	}

	/**
	 * @return
	 */
	private JXTable createMemberTable() {
		membersTableModel = new DirObjectsTableModel(members);
		final JXTable memberTable = new JXTable(membersTableModel);
		memberTable.setSortable(true);
		memberTable.setSortOrder(0, SortOrder.ASCENDING);
		memberTable.setPreferredScrollableViewportSize(new Dimension(300, 300));
		JTableFormatter.initColumnSizes(memberTable);
		return memberTable;

	}

	/**
	 * @param cc
	 * @return
	 */
	private JXTable getAvailableObjectsTable() {
		if (null == availableObjectsTable) {
			final Set<DirectoryObject> other = new HashSet();
			final Set<DirectoryObject> obj = new HashSet();
			try {
				final LDAPDirectory directory = realm.getDirectory();
				if (directory != null)
					other.addAll(directory.list(memberClass));

				for (final DirectoryObject o : other)
					if (!o.equals(this.dirObject))
						obj.add(o);

			} catch (final DirectoryException e) {
				ErrorManager.getDefault().notify(e);
			}

			availableObjectsTableModel = new ExcludeFilterTableModel(

			new DirObjectsTableModel(obj), membersTableModel);
			availableObjectsTable = new JXTable(availableObjectsTableModel);
			availableObjectsTable.setSortable(true);
			availableObjectsTable.setSortOrder(0, SortOrder.ASCENDING);

			JTableFormatter.initColumnSizes(availableObjectsTable);
		}

		return availableObjectsTable;
	}

	/**
	 * @param memberTable
	 * @return
	 */
	protected JPanel initButtonPanel(final JXTable memberTable) {
		final ButtonStackBuilder bsb = new ButtonStackBuilder();

		bsb.addGridded(new JButton(new OpenAddFrameAction(memberTable)));
		bsb.addRelatedGap();
		bsb.addGridded(new JButton(new RemoveAction(memberTable)));
		bsb.addRelatedGap();

		return bsb.getPanel();
	}

	private void commit() {
		if (type == TYPE_ASSOC_OBJECTS)
			((AssociatedObjectsProvider) dirObject).setAssociatedObjects(memberClass,
					new HashSet(membersTableModel.getDirectoryObjects()));
		else if (type == TYPE_MEMBERS) {
			final Set newMembers = new HashSet(((Group) dirObject).getMembers());
			for (final Iterator i = newMembers.iterator(); i.hasNext();) {
				final Object member = i.next();
				if (member.getClass().equals(memberClass))
					i.remove();
			}

			newMembers.addAll(membersTableModel.getDirectoryObjects());

			((Group) dirObject).setMembers(newMembers);
		}
	}

	/*
	 * @see org.openthinclient.console.ObjectEditorPart#getTitle()
	 */
	public String getTitle() {
		final String prefix = ""; //$NON-NLS-1$
		final String postfix = ""; //$NON-NLS-1$
		// if (type == AssociationEditor.TYPE_MEMBERS) {
		// prefix = "<html>" + Messages.getString("AssociationEditor.referenced") +
		// "<br>";
		// //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		// postfix = "</html>"; //$NON-NLS-1$
		// }
		return prefix + Messages.getString(memberClass.getSimpleName() + "s_title") //$NON-NLS-1$
				+ postfix;
	}

	private boolean canModify(Class object) {
		final boolean isUser = this.dirObject.getClass() == User.class;
		final boolean isGroup = this.dirObject.getClass() == UserGroup.class;
		final boolean isMutable = LDAPDirectory.isMutable(UserGroup.class);

		if (realm.getAdministrators() == this.dirObject)
			return true;

		if (isUser && object == UserGroup.class && false == isMutable)
			return false;

		if (isGroup && (object == UserGroup.class || object == User.class)
				&& false == isMutable)
			return false;

		return true;
	}
}
