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
package org.openthinclient.console.nodes.pkgmgr;

import java.awt.Dialog;
import java.beans.PropertyVetoException;
import java.util.LinkedList;

import org.openide.ErrorManager;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Node;
import org.openide.util.Utilities;
import org.openthinclient.console.MainTreeTopComponent;

import com.levigo.util.swing.SwingWorker;

public final class PackageManagerJobQueue {

	public static boolean errorExisting = false;

	private static final Object lock = new Object();
	/** The singleton instance of the TileCache */
	private static PackageManagerJobQueue singletonInstance;

	private JobQueue jobQueue;

	static final class ProgressbarWorker extends SwingWorker {
		PackageManagerJob job;
		final Dialog jd;
		private boolean isAccomplished = true;

		public ProgressbarWorker(PackageManagerJob job, Dialog progressbarDialog) {
			this.job = job;
			this.jd = progressbarDialog;
		}

		/*
		 * 
		 * @see org.openthinclient.util.swing.SwingWorker#construct()
		 */

		@Override
		public Object construct() {
			try {
				final Object o = job.doPMJob();
				try {
					((ExplorerManager.Provider) MainTreeTopComponent.getDefault())
							.getExplorerManager().setSelectedNodes(new Node[]{job.node});
				} catch (final PropertyVetoException e) {
					e.printStackTrace();
					ErrorManager.getDefault().notify(e);
				} finally {
					jd.setVisible(false);
				}
				return o;
			} catch (final Exception e) {
				e.printStackTrace();
				ErrorManager.getDefault().notify(e);
				getInstance().destroyJobQueue();
				interrupt();
				isAccomplished = false;
				errorExisting = true;
				return false;
			} finally {
				jd.setVisible(false);
				jd.setIconImage(Utilities.loadImage(
						"org/openthinclient/console/icon.png", true));
				job.stopTimer();
				for (final String warning : job.pkgmgr.getWarnings())
					ErrorManager.getDefault().notify(new Throwable(warning));
			}
		}

		/**
		 * refresh all children nodes from the given parent Node
		 * 
		 * @param node one children node
		 */
		public void refreshnode(Node node) {
			while (node != null) {
				if (node instanceof PackageManagementNode)
					((PackageManagementNode) node).refresh();
				node = node.getParentNode();
			}
		}

		/*
		 * 
		 * @see org.openthinclient.util.swing.SwingWorker#finished()
		 */
		@Override
		public void finished() {
			if (isAccomplished) {
				jd.setVisible(false);
				jd.dispose();
				refreshnode(job.getNode());
			}
		}

		/**
		 * creates a Pane which tell the user that the started action is
		 * accomplished
		 * 
		 */

	}

	private final class JobQueue extends Thread {
		private LinkedList<PackageManagerJob> queue = new LinkedList<PackageManagerJob>();

		private JobQueue() {
			super("JobQueue");
		}

		/**
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			try {
				// Always check of Thread's interrupt state. CK.
				while (!Thread.currentThread().isInterrupted()) {
					PackageManagerJob nextJob = null;
					// wait for new job to arrive
					synchronized (this) {
						while (queue.size() == 0)
							try {
								wait();
							} catch (final InterruptedException e) {
								jobQueue = null;
								ErrorManager.getDefault().notify(e);
								notify();
							}
						nextJob = queue.removeFirst();
					}
					if (nextJob != null)
						nextJob.doJob();
				}
				jobQueue = null;
				queue = null;
			} catch (final ThreadDeath td) {
				ErrorManager.getDefault().notify(td);
				// Be careful if you really want to start a
				// new thread when this thread is stopped,
				// because the ThreadGroup could be in
				// the process of destruction. CK.
				jobQueue = null;
				// Always rethrow ThreadDeath exception if caught it!
				throw td;
			} catch (final OutOfMemoryError ooe) {
				ErrorManager.getDefault().notify(ooe);
				jobQueue = null;
				throw ooe;
			}
		}

		/**
		 * Add a job
		 */
		synchronized void addJob(PackageManagerJob job) {
			queue.addLast(job);
			notify();
		}
	}

	/**
	 * Returns a PackageManagerJobQueue instance
	 * 
	 * @return PackageManagerJobQueue
	 */
	public static PackageManagerJobQueue getInstance() {
		synchronized (lock) {
			if (null == singletonInstance)
				singletonInstance = new PackageManagerJobQueue();
		}
		singletonInstance.instanceValidation();
		return singletonInstance;
	}

	/**
	 * Constructor
	 */
	private PackageManagerJobQueue() {
		instanceValidation();
	}

	/**
	 * Make sure that everything is (still) up and running. (Applet stops may have
	 * killed the queue etc.)
	 */
	private void instanceValidation() {
		synchronized (lock) {
			if (null == jobQueue || !jobQueue.isAlive())
				createJobQueue();
		}
	}

	/**
	 * Creates internal job queue
	 */
	private void createJobQueue() {
		jobQueue = new JobQueue();
		jobQueue.setPriority(Thread.MIN_PRIORITY);
		jobQueue.setDaemon(true);
		jobQueue.start();
	}

	/**
	 * Add a job to be processed
	 * 
	 * @param job
	 */
	public void addPackageManagerJob(PackageManagerJob job) {
		instanceValidation();
		jobQueue.addJob(job);
	}

	private void destroyJobQueue() {
		if (null != jobQueue || jobQueue.isAlive())
			jobQueue.interrupt();
	}
}
