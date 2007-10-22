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
/**
 * 
 */
package org.openthinclient.console.nodes.pkgmgr;

import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;

import org.openide.nodes.Node;
import org.openide.nodes.NodeEvent;
import org.openide.nodes.NodeListener;
import org.openide.nodes.NodeMemberEvent;
import org.openide.nodes.NodeReorderEvent;
import org.openide.util.WeakListeners;
import org.openthinclient.console.DetailViewProvider;
import org.openthinclient.console.Messages;
import org.openthinclient.util.dpkg.Package;
import org.openthinclient.util.dpkg.Version;

// import org.openthinclient.pkgmgr.PackageManager;
import com.levigo.util.swing.IconManager;

class PackageListTableModel extends AbstractTableModel implements NodeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final PackageListNode pln;

	private Set<Package> toBeSelectable = new HashSet<Package>();

	private Set<Package> toBeSelectableDEB = new HashSet<Package>();

	private final boolean allowSelection;

	private final boolean existsaDebFile;

	private PackageManagerDelegation pkgmgr;

	private static final int IN_BYTE = 0;

	private static final int IN_KBYTE = 1;

	public PackageListTableModel(PackageListNode dol, boolean allowSelection,
			boolean existsaDebFile) {
		this.pln = dol;
		this.allowSelection = allowSelection;
		this.existsaDebFile = existsaDebFile;
		// attach listener
		dol.addNodeListener((NodeListener) WeakListeners.create(NodeListener.class,
				this, dol));
		if (existsaDebFile) {
			this.pkgmgr = (PackageManagerDelegation) pln.getLookup().lookup(
					PackageManagerDelegation.class);

		}
	}

	/*
	 * @see javax.swing.table.TableModel#getValueAt(int, int)
	 */
	public Object getValueAt(int row, int column) {
		Node[] nodes = pln.getChildren().getNodes();
		if (nodes.length <= row || null == nodes[row])
			return ""; //$NON-NLS-1$
		Package p = (Package) nodes[row].getLookup().lookup(Package.class);

		if (null == p)
			return "..."; //$NON-NLS-1$
		if (column != -1 && allowSelection == false) {
			column++;
		}

		switch (column){
			case -1 :
				return nodes[row];
			case 0 :
				return toBeSelectable.contains(p);

			case 1 :
				return p.getName();

			case 2 :
				return p.getShortDescription();

			case 3 :
				String temp;
				if (p.getVersion().toString().startsWith("0:"))
					temp = p.getVersion().toString().substring(2,
							p.getVersion().toString().length());
				else
					return p.getVersion();
				return temp;
			case 4 :
				return unitDescriber(p.getSize(), IN_BYTE);

			case 5 :
				return unitDescriber(p.getInstalledSize(), IN_KBYTE);
			case 6 :
				if (existsaDebFile)
					for (Package pkg : pkgmgr.getDebianFilePackages())
						if (pkg.getName().equalsIgnoreCase(p.getFilename())
								&& pkg.getVersion().equals(p.getVersion()))
							return true;
				return false;

			case 7 :
				if (existsaDebFile) {
					if (true == (Boolean) getValueAt(row, 6)) {
						return (new ImageIcon(IconManager.getInstance(
								DetailViewProvider.class, "icons").getImage(
								"tree." + "greenCheck")));
					} else
						return (new ImageIcon(IconManager.getInstance(
								DetailViewProvider.class, "icons").getImage("tree." + "redX")));
				}
			default :
				return "";
		}
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex == 0 && allowSelection)
			return true;

		return super.isCellEditable(rowIndex, columnIndex);
	}

	@Override
	public void setValueAt(Object aValue, int row, int columnIndex) {
		if (columnIndex == 0) {
			Node[] nodes = pln.getChildren().getNodes();
			if (nodes.length <= row || null == nodes[row])
				return; //$NON-NLS-1$
			Package p = (Package) nodes[row].getLookup().lookup(Package.class);

			if (null == p)
				return;

			if (columnIndex == 0 && ((Boolean) aValue).booleanValue()) {
				toBeSelectable.add(p);
				if (!(((Boolean) getValueAt(row, 6)).booleanValue()))
					toBeSelectableDEB.add(p);
			} else {
				toBeSelectable.remove(p);
				if (!(((Boolean) getValueAt(row, 6)).booleanValue()))
					toBeSelectableDEB.remove(p);
			}
			this.fireTableCellUpdated(row, columnIndex);

		}
	}

	Node getNodeAtRow(int row) {
		Node[] nodes = pln.getChildren().getNodes();
		if (nodes.length <= row)
			return null;
		return nodes[row];
	}

	/*
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	public int getColumnCount() {
		if (existsaDebFile == true)
			// return (7);
			return 8;
		return allowSelection ? 6 : 5;
		// return 5;
	}

	/*
	 * @see javax.swing.table.TableModel#getColumnName(int)
	 */
	public String getColumnName(int column) {
		if (allowSelection == false) {
			column++;
		}

		switch (column){
			case 0 :
				return Messages.getString("node.PackageListNode.getColumnName.tagged");
			case 1 :
				return Messages.getString("node.PackageListNode.getColumnName.name");
			case 2 :
				return Messages
						.getString("node.PackageListNode.getColumnName.description");
			case 3 :
				return Messages.getString("node.PackageListNode.getColumnName.version");
			case 4 :
				return Messages.getString("node.PackageListNode.getColumnName.size");
			case 5 :
				return Messages
						.getString("node.PackageListNode.getColumnName.installedsize");
			case 6 :
				return Messages
						.getString("node.PackageListNode.getColumnName.isDebLocal.DontShow");
			case 7 :
				return Messages
						.getString("node.PackageListNode.getColumnName.isDebLocal");
				// case 7 : return"lol";
			default :
				return Messages.getString("node.unknown");
		}
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex == 0 && allowSelection)
			return Boolean.class;
		else if ((columnIndex == 4) || (columnIndex == 3 && !allowSelection)
				|| (columnIndex == 5 && allowSelection))
			return Integer.class;
		else if ((columnIndex == 3 && allowSelection)
				|| (columnIndex == 2 && !allowSelection))
			return (Version.class);
		else if (columnIndex == 6)
			return Boolean.class;
		else if (columnIndex == 7)
			return ImageIcon.class;
		else
			return super.getColumnClass(columnIndex);

	}

	/*
	 * @see javax.swing.table.TableModel#getRowCount()
	 */
	public int getRowCount() {
		return pln.getChildren().getNodes().length;
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

	public Collection<Package> getSelectedPackages() {
		return (toBeSelectable);
	}

	public Package getPackageAtRow(int row) {
		Node[] nodes = pln.getChildren().getNodes();
		if (nodes.length <= row || null == nodes[row])
			return null; //$NON-NLS-1$
		return (Package) nodes[row].getLookup().lookup(Package.class);
	}

	public String getUsedInstallSpace() {
		if (toBeSelectable == null || toBeSelectable.size() == 0)
			return "0";
		float neededSpace = 0;

		for (Package pkg : toBeSelectable) {
			neededSpace = neededSpace + pkg.getInstalledSize();
		}
		return unitDescriber(neededSpace, IN_KBYTE);
	}

	public String getUsedCacheSpace() {

		if (toBeSelectableDEB == null || toBeSelectableDEB.size() == 0)
			return "0";
		float neededSpace = 0;
		for (Package pkg : toBeSelectableDEB) {
			neededSpace = neededSpace + pkg.getSize();
		}
		return unitDescriber(neededSpace, IN_BYTE);
	}

	public String unitDescriber(float value, int counter) {
		boolean greaterThanOne = true;
		while (greaterThanOne == true) {
			if (counter < 4)
				if (value > 1024f) {
					value = value / 1024f;
					counter++;
				} else
					greaterThanOne = false;
			else
				greaterThanOne = false;
		}
		value *= 10f;
		value = ((float) Math.round(value)) / 10f;
		String ret = "";
		switch (counter){
			case 0 :
				ret = " Byte";
				break;
			case 1 :
				ret = " KB";
				break;
			case 2 :
				ret = " MB";
				break;
			case 3 :
				ret = " GB";
				break;
		}
		return (Float.toString(value) + ret);

	}

}
