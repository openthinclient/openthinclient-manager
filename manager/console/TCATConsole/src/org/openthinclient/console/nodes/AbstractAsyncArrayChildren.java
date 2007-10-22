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

import java.util.Collection;
import java.util.Collections;

import javax.swing.SwingUtilities;

import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openthinclient.console.Messages;


/** Defining the children of a feed node */
public abstract class AbstractAsyncArrayChildren extends Children.Keys {
	protected void addNotify() {
		refreshChildren();
	}

	/**
	 * 
	 */
	public void refreshChildren() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setKeys(Collections.EMPTY_LIST);
				add(new Node[]{new OperationPendingNode(getPendingMessage())});
			}
		});

		// if there aren't any children yet, let the user know what's going on
		// if (getNodes().length == 0)

		new Thread("Directory operation") { //$NON-NLS-1$
			/*
			 * @see java.lang.Thread#run()
			 */
			@Override
			public void run() {

				//FIXME i maybe have done a big mistake.... we will seee........FT
//				 removeAllChildren();
//				System.out.println("AbstractAsyncArrayChildren/refreshChildren/run");
				final Collection keys = asyncInitChildren();
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						removeAllChildren();
						setKeys(keys);
					}
				});
			}
		}.start();
	}

	/**
	 * @return
	 */
	protected String getPendingMessage() {
		return Messages.getString("AbstractAsyncArrayChildren.loading"); //$NON-NLS-1$
	}

	/**
	 * 
	 */
	abstract protected Collection asyncInitChildren();

	/**
	 * 
	 */
	protected void removeAllChildren() {
		remove(getNodes());
	}
}
