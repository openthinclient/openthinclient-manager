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
package org.openthinclient.console.nodes;

import org.openide.ErrorManager;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openthinclient.console.Messages;



/** Getting the root node */
public class RootNode extends MyAbstractNode {
	public static Class ROOT_CHILDREN[] = new Class[]{RealmsNode.class};

	public RootNode() throws DataObjectNotFoundException {
		super(new Children.Array());
		for (Class c : ROOT_CHILDREN) {
			try {
				Node[] nodes = new Node[]{((Class<? extends Node>) c).newInstance()};
				getChildren().add(nodes);	
			} catch (Exception e) {
				ErrorManager.getDefault().notify(e);
				getChildren().add(new Node[]{new ErrorNode(Messages
						.getString("RootNode.cantCreateChild"), e)}); //$NON-NLS-1$
			}
		}		
	}
}
