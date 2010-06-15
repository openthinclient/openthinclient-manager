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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.table.AbstractTableModel;

import org.openide.nodes.Node;
import org.openthinclient.common.model.Client;
import org.openthinclient.console.nodes.DirObjectListNode;

/**
 * Provides the Table-Model for the Startup-Dialog at a specific date
 */
class ClientListTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	private final TreeMap cronIDs = new TreeMap();

	private final DirObjectListNode dol;

	private final Set<Client> toBeSelectable = new HashSet<Client>();

	private final boolean allowSelection;

	public ClientListTableModel(DirObjectListNode dol, boolean allowSelection) {
		this.dol = dol;
		this.allowSelection = allowSelection;

	}

	/*
	 * @see javax.swing.table.TableModel#getValueAt(int, int)
	 */
	public Object getValueAt(int row, int column) {
		final Node[] nodes = dol.getChildren().getNodes();
		if (nodes.length <= row || null == nodes[row])
			return "";
		final Client c = (Client) nodes[row].getLookup().lookup(Client.class);

		if (null == c)
			return "...";
		if (column != -1 && allowSelection == false)
			column++;

		switch (column){
			case -1 :
				return nodes[row];
			case 0 :
				return toBeSelectable.contains(c);
			case 1 :
				return c.getName();
			case 2 :
				return c.getIpHostNumber();
			case 3 :
				return c.getMacAddress();
			case 4 :
				return c.getHardwareType();
			case 5 :
				return c.getLocation();
			case 6 :
				return c.getDescription();
			case 7 :
				return c.getStatus();
			case 8 :
				return cronIDs.get(row);
			default :
				return "";
		}
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex == 0 && allowSelection || columnIndex == 8)
			return true;
		return super.isCellEditable(rowIndex, columnIndex);
	}

	@Override
	public void setValueAt(Object aValue, int row, int columnIndex) {
		if (columnIndex == 0) {
			final Node[] nodes = dol.getChildren().getNodes();
			if (nodes.length <= row || null == nodes[row])
				return;
			final Client c = (Client) nodes[row].getLookup().lookup(Client.class);

			if (null == c)
				return;

			if (columnIndex == 0 && ((Boolean) aValue).booleanValue())
				toBeSelectable.add(c);
			else
				toBeSelectable.remove(c);
			this.fireTableCellUpdated(row, columnIndex);
		}
		if (columnIndex == 8) {
			cronIDs.put(row, aValue);
			this.fireTableCellUpdated(row, columnIndex);
		}
	}

	Node getNodeAtRow(int row) {
		final Node[] nodes = dol.getChildren().getNodes();
		if (nodes.length <= row)
			return null;
		return nodes[row];
	}

	/*
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	public int getColumnCount() {
		return allowSelection ? 9 : 8;
	}

	/*
	 * @see javax.swing.table.TableModel#getColumnName(int)
	 */
	@Override
	public String getColumnName(int column) {
		if (allowSelection == false)
			column++;

		switch (column){
			case 0 :
				return Messages.getString("node.PackageListNode.getColumnName.tagged");
			case 1 :
				return Messages.getString("DirObjectEditor.name");
			case 2 :
				return Messages.getString("Client.ipHostNumber");
			case 3 :
				return Messages.getString("Client.macAddress");
			case 4 :
				return Messages.getString("Client.hardwareType");
			case 5 :
				return Messages.getString("Client.location");
			case 6 :
				return Messages.getString("DirObjectEditor.description");
			case 7 :
				return Messages.getString("Client.status");
			case 8 :
				return Messages.getString("StartAtTime.table.id"); // 
			default :
				return Messages.getString("node.unknown");
		}
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex == 0 && allowSelection)
			return Boolean.class;
		else
			return super.getColumnClass(columnIndex);
	}

	/*
	 * @see javax.swing.table.TableModel#getRowCount()
	 */
	public int getRowCount() {
		return dol.getChildren().getNodes().length;
	}

	public Collection<Client> getSelectedClients() {
		return toBeSelectable;
	}

	public Client getClientsAtRow(int row) {
		final Node[] nodes = dol.getChildren().getNodes();
		if (nodes.length <= row || null == nodes[row])
			return null;
		return (Client) nodes[row].getLookup().lookup(Client.class);
	}
}
