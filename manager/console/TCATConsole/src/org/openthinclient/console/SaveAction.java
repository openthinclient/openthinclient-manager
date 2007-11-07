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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import org.openide.ErrorManager;
import org.openide.nodes.Node;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Realm;
import org.openthinclient.ldap.DirectoryException;

/**
 * @author bohnerne
 */
public class SaveAction extends AbstractAction {
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public SaveAction() {
		super();
	}

	/**
	 * @param name
	 */
	public SaveAction(String name) {
		super(name);
	}

	/**
	 * @param name
	 * @param icon
	 */
	public SaveAction(String name, Icon icon) {
		super(name, icon);
	}

	/*
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		Node[] nodes = MainTreeTopComponent.getDefault().getActivatedNodes();

		for (Node node : nodes) {
			DirectoryObject dirObject = (DirectoryObject) node.getLookup().lookup(
					DirectoryObject.class);
			Realm realm = (Realm) node.getLookup().lookup(Realm.class);
			try {
				realm.getDirectory().save(dirObject);
				if (node instanceof Refreshable) {
					((Refreshable) node).refresh();
				}
			} catch (DirectoryException e1) {
				ErrorManager.getDefault().notify(e1);
			}

		}

	}

}
