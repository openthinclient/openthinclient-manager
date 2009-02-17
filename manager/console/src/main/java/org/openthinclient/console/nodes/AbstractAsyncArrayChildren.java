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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.SwingUtilities;

import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openthinclient.console.Messages;

/** Defining the children of a feed node */
public abstract class AbstractAsyncArrayChildren extends Children.Keys {
	ChildRefresherThread crt;

	@Override
	protected void addNotify() {
		refreshChildren();
	}

	public void refreshChildren() {
		if (null != crt && crt.isAlive())
			crt.setStop(true);

		crt = new ChildRefresherThread("Directory operation: "
				+ getNode().getName());
		crt.start();
	}

	private final class ChildRefresherThread extends Thread { //$NON-NLS-1$
		private boolean stop;

		ChildRefresherThread(String name) {
			super(name);
			yield();
			setPriority(Thread.MIN_PRIORITY);
			stop = false;
		}

		public void setStop(boolean stop) {
			this.stop = true;
		}

		/*
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			final Node[] pn = new Node[]{new OperationPendingNode(getPendingMessage())};
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					remove(getNodes());
					setKeys(Collections.EMPTY_LIST);
					setBefore(true);
					add(pn);
				}
			});

			Collection keys = Collections.EMPTY_LIST;
			if (!stop)
				keys = asyncInitChildren();

			final int bufferSize = 4;
			final List<Object> tmpKeys = new CopyOnWriteArrayList<Object>();

			for (final Iterator i = keys.iterator(); i.hasNext();) {
				if (stop)
					break;
				final Object key = i.next();
				tmpKeys.add(key);
				final int modu = keys.size() % bufferSize;
				if (tmpKeys.size() > modu && tmpKeys.size() % bufferSize != modu)
					continue;

				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						setKeys(tmpKeys);
					}
				});
				try {
					Thread.sleep(100);
				} catch (final InterruptedException e) {
					// ignore
				}

			}

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					remove(pn);
				}
			});
		}
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

}
