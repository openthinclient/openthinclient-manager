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
package org.openthinclient.console.nodes.pkgmgr;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.beans.PropertyVetoException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import javax.swing.UIManager;

import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Node;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.schema.provider.HTTPSchemaProvider;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;
import org.openthinclient.console.MainTreeTopComponent;
import org.openthinclient.console.Messages;
import org.openthinclient.console.nodes.DirObjectListNode;
import org.openthinclient.console.nodes.RealmNode;
import org.openthinclient.console.util.DetailViewFormBuilder;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.util.dpkg.Package;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.levigo.util.swing.SwingWorker;

public final class PackageManagerJobQueue {

	public static boolean errorExisting = false;

	private static final Object lock = new Object();
	/** The singleton instance of the TileCache */
	private static PackageManagerJobQueue singletonInstance;

	private JobQueue jobQueue;

	static final class ProgressbarWorker extends SwingWorker {
		Job job;
		final Dialog jd;
		private boolean isAccomblished = true;

		public ProgressbarWorker(Job job, Dialog progressbarDialog) {
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
				Object o = job.doPMJob();
				try {
					((ExplorerManager.Provider) MainTreeTopComponent.getDefault())
							.getExplorerManager().setSelectedNodes(new Node[]{job.node});
				} catch (PropertyVetoException e) {
					e.printStackTrace();
					ErrorManager.getDefault().notify(e);
				}
				jd.setVisible(false);
				return o;
			} catch (PackageManagerException e) {
				e.printStackTrace();
				ErrorManager.getDefault().notify(e);
				getInstance().destroyJobQueue();
				interrupt();
				isAccomblished = false;
				jd.setVisible(false);
				errorExisting = true;
				job.doLoadError(e.toString());
				return false;
			} finally {
				jd.setVisible(false);
				job.stopTimer();
			}
		}

		/**
		 * refresh all children nodes from the given parent Node
		 * 
		 * @param node one chuildren node
		 */
		public void refreshnode(Node node) {
			while (node != null) {
				if (node instanceof PackageManagementNode) {
					((PackageManagementNode) node).refresh();
				}
				node = node.getParentNode();
			}

		}

		/*
		 * 
		 * @see org.openthinclient.util.swing.SwingWorker#finished()
		 */
		@Override
		public void finished() {

			// }
			if (isAccomblished) {
				for (Frame frame : JFrame.getFrames())
					frame.setEnabled(true);
				jd.setVisible(false);
				jd.dispose();
				refreshnode(job.getNode());
			}

		}

		/**
		 * creates a Pane which tell the user that the started action is
		 * accomblished
		 * 
		 */

	}

	public static abstract class Job {
		Node node;
		Collection<Package> packageCollection;
		List<Package> packageList;
		PackageManagerDelegation pm;
		Timer timer;
		public final static int ONE_SECOND = 200;
		private JProgressBar progressBar;
		private JLabel jLInfo;
		private boolean moreInformation = false;
		ProgressbarWorker pgw;
		
		public static final int REFRESH_ALL_PACKAGES=0;
		public static final int REFRESH_INSTALLED_PACKAGES=1;
		public static final int REFRESH_INSTALLABLE_PACKAGES=2;
		public static final int REFRESH_UPDATEABLE_PACKAGES=3;
		public static final int REFRESH_REMOVED_PACKAGES=4;
		public static final int REFRESH_DEBIAN_PACKAGES=5;

		Job(Node node, Collection<Package> packageCollection) {
			this.node = node;
			this.packageCollection = packageCollection;
			this.packageList = new ArrayList<Package>(packageCollection);
			this.pm = (PackageManagerDelegation) (node).getLookup().lookup(
					PackageManagerDelegation.class);

		}

		public Job(Node node) {
			this.node = node;
			this.pm = (PackageManagerDelegation) (node).getLookup().lookup(
					PackageManagerDelegation.class);
		}

		/**
		 * 
		 * @return the Node of the Job
		 */
		public Node getNode() {
			return node;
		}

		/**
		 * Create the "realy" job which the PackageManager should do and the method
		 * which should be called respectively I have designed it for starting realy
		 * hard stuff like inustalling deleting and so on, for methods which need a
		 * long time so that a JProgressBar is usefull for the consumer
		 * 
		 * @return actually not evaluated
		 * @throws PackageManagerException
		 */
		// FIXME should return an bollean value to check if the "whatever" was
		// successfull or not...
		abstract Object doPMJob() throws PackageManagerException;

		public void doLoadError(String errorMessage) {
			final JDialog jd = new JDialog();
			CellConstraints cc = new CellConstraints();
			String tempMsg = errorMessage;
			JPanel contentPane = new JPanel();
			DetailViewFormBuilder dfb = new DetailViewFormBuilder(new FormLayout(
					"f:p:g", "p"));

			while (dialog.isVisible() == true)
				try {
					dialog.setVisible(false);
					dialog.validate();
					timer.stop();
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
					ErrorManager.getDefault().notify(e);
				}

			while (tempMsg.length() > 0) {
				int maxLength;
				if (tempMsg.length() < 80)
					maxLength = tempMsg.length();
				else
					maxLength = 79;
				String lengthsearch = tempMsg.substring(0, maxLength);
				int pointOfLastSpace;
				if (lengthsearch.contains(" ") && lengthsearch.lastIndexOf(" ") > 0)
					pointOfLastSpace = lengthsearch.lastIndexOf(" ");
				else
					pointOfLastSpace = maxLength;
				JLabel jlb = new JLabel(tempMsg.substring(0, pointOfLastSpace));
				dfb.append(jlb);
				tempMsg = tempMsg.substring(pointOfLastSpace);
				tempMsg = tempMsg.trim();
			}

			JButton okButton = new JButton();
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					for (Frame frame : JFrame.getFrames())
						frame.setEnabled(true);
					jd.setVisible(false);
					jd.dispose();
				}
			});
			okButton.setText("OK");
			okButton.setVisible(true);
			contentPane.setSize(500, 300);
			contentPane.setLayout(new FormLayout("500px", "200px,100px"));
			contentPane.add(dfb.getPanel(), cc.xy(1, 1));
			contentPane.add(okButton, cc.xy(1, 2));
			jd.setContentPane(contentPane);
			jd.setTitle(getNodeAction() + " ERROR");
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			jd.setLocation((new Double(dim.getWidth()).intValue() / 2) - 250,
					(new Double(dim.getHeight()).intValue() / 2) - 150);
			jd.setSize(new Dimension(500, 300));

			jd.setVisible(true);
		}

		public boolean doErrorLoadForApplication(Realm realm,
				List<Application> applicationSet) {

			DetailViewFormBuilder dfb = new DetailViewFormBuilder(new FormLayout(
					"f:p:g"));
			
			for (Application appl : applicationSet) {
				try {
					dfb.append(new JLabel(Messages
							.getString("Job.ApplicationAlreadyUsed1")
							+ " "
							+ appl.getName()
							+ " "
							+ Messages.getString("Job.ApplicationAlreadyUsed2")
							+" "
							+ appl.getSchema(appl.getRealm()).getName()));
				} catch (SchemaLoadingException e1) {
					e1.printStackTrace();
					ErrorManager.getDefault().notify(e1);
				}
			}
			dfb.nextLine();
			dfb.append(new JLabel(Messages.getString("Job.ApplicationAlreadyUsedQuestion")));
			DialogDescriptor descriptor = new DialogDescriptor(dfb.getPanel(),
					getNodeAction(), true, new Object[]{DialogDescriptor.CANCEL_OPTION,
							DialogDescriptor.OK_OPTION}, null, DialogDescriptor.BOTTOM_ALIGN,
					null, (new ActionListener() {
						public void actionPerformed(ActionEvent e) {
						}
					}));
			descriptor.setClosingOptions(new Object[]{DialogDescriptor.OK_OPTION,
					DialogDescriptor.CANCEL_OPTION});
			dialog = DialogDisplayer.getDefault().createDialog(descriptor);
			dialog.setVisible(true);
			
			if (descriptor.getValue() == DialogDescriptor.OK_OPTION) {
				boolean ret=true;
				for (Application appl : applicationSet) {
					try {
						LDAPDirectory dir = realm.getDirectory();
						if(!dir.delete(appl))
							ret=false;
					} catch (DirectoryException e1) {
						e1.printStackTrace();
						ret=false;
						ErrorManager.getDefault().notify(e1);
					}

				}
				if(ret){
					Node tmp =node;
					while (tmp != null) {
						if (tmp instanceof RealmNode) {
							for(Node chilnode:((RealmNode) tmp).getChildren().getNodes())
								if(chilnode.getName().equalsIgnoreCase(Messages.getString("Applications_title")))
									((DirObjectListNode) chilnode).refresh();	
						}
						tmp = tmp.getParentNode();
					}
				}
				return ret;
			}
			return false;

		}

		/**
		 * load a Dialog with one Centered JProgressBar, start it on after it's
		 * visible is set TRUE a new ProgressWorker is started (new
		 * ProgressbarWorker(this, jd).start();) in this case "this" is the actually
		 * job and jd is the actually created JDialog
		 * 
		 */
		private final JPanel jd = new JPanel();
		private Dialog dialog;

		public void loadDialog() {
			CellConstraints cc = new CellConstraints();
			Font f = UIManager.getFont("TitledBorder.font"); //$NON-NLS-1$
			f = f.deriveFont(Font.BOLD, AffineTransform.getScaleInstance(1.5, 1.5));
			jd.setSize(400, 100);
			jd.setLayout(new FormLayout("f:p:g", "p,p,20dlu"));
			JLabel jl2 = new JLabel(Messages.getString("pleasewait"));
			jl2.setFont(f);
			jl2.setForeground(new Color(50, 50, 150));
			jd.add(jl2, cc.xy(1, 1));
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			jd.setLocation((new Double(dim.getWidth()).intValue() / 2) - 150,
					(new Double(dim.getHeight()).intValue() / 2) - 150);
			jd.setSize(new Dimension(300, 100));
			progressBar = new JProgressBar(0, pm.getMaxProgress());
			progressBar.setValue(0);
			progressBar.setStringPainted(true);
			pm.resetValuesForDisplaying();
			jLInfo = new JLabel(" ");
			jd.add(jLInfo, cc.xy(1, 3));
			
			if (node.getName().equalsIgnoreCase(
					Messages.getString("node.AvailablePackagesNode"))
					|| node.getName().equalsIgnoreCase(
							Messages.getString("node.UpdatablePackagesNode")))
				moreInformation = true;
			timer = new Timer(ONE_SECOND, new ActionListener() {
				public void actionPerformed(ActionEvent evt) {

					while (pm.isDone() == false && !Thread.interrupted()
							&& !errorExisting) {
						progressBar.setValue(pm.getActprogress());
						if (moreInformation && null != pm.getActPackName()) {
							jLInfo.setText(pm.getActPackName() + ": "
									+ pm.getActMaxFileSize()[0] + "KB / "
									+ pm.getActMaxFileSize()[1] + "KB");
						}
						jd.repaint();
						jd.paint(jd.getGraphics());
						try {
							if (!Thread.interrupted())
								Thread.sleep(300);
							else {
								timer.stop();
							}
						} catch (InterruptedException e) {
							timer.stop();
							ErrorManager.getDefault().notify(e);
							doLoadError(e.toString());
						}

					}
					timer.stop();
				}
			});

			jd.add(progressBar, cc.xy(1, 2));
			timer.start();
			DialogDescriptor descriptor = new DialogDescriptor(jd, getNodeAction(),
					true, new Object[]{}, null, DialogDescriptor.BOTTOM_ALIGN, null,
					(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
						}
					}));
			descriptor.setClosingOptions(new Object[]{progressBar});

			dialog = DialogDisplayer.getDefault().createDialog(descriptor);

			dialog.setSize(300, 100);
			pgw = new ProgressbarWorker(this, dialog);
			pgw.start();
			dialog.setVisible(true);
		}

		private void stopTimer() {
			timer.stop();
		}

		public void createInformationOptionPane(boolean reloadSchemaProviderOption) {
			if (reloadSchemaProviderOption) {
				Realm realm = (Realm) node.getLookup().lookup(Realm.class);
				String hostname = realm.getConnectionDescriptor().getHostname();
				try {
					realm.removeSchemaProvider();
					realm.setNeedsRefresh();
					HTTPSchemaProvider httpsp = new HTTPSchemaProvider(hostname);
					httpsp.reload();
				} catch (MalformedURLException e2) {
					e2.printStackTrace();
					ErrorManager.getDefault().notify(e2);
				}
			}
			progressBar.setValue(pm.getActprogress());
			dialog.setVisible(false);
			dialog.validate();
			dialog.dispose();
			timer.stop();
			String message = "";
			if (node.getName().equalsIgnoreCase(
					Messages.getString("node.AvailablePackagesNode")))
				message = Messages.getString("action.end.all1") + " "
						+ Messages.getString("action.end.packages") + " "
						+ Messages.getString("action.end.all2") + " "
						+ Messages.getString("action.end.install");
			else if (node.getName().equalsIgnoreCase(
					Messages.getString("node.InstalledPackagesNode")))
				message = Messages.getString("action.end.all1") + " "
						+ Messages.getString("action.end.packages") + " "
						+ Messages.getString("action.end.all2") + " "
						+ Messages.getString("action.end.delete");
			else if (node.getName().equalsIgnoreCase(
					Messages.getString("node.UpdatablePackagesNode")))
				message = Messages.getString("action.end.all1") + " "
						+ Messages.getString("action.end.packages") + " "
						+ Messages.getString("action.end.all2") + " "
						+ Messages.getString("action.end.update");
			else if (node.getName().equalsIgnoreCase(
					Messages.getString("node.AlreadyDeletedPackagesNode")))
				message = Messages.getString("action.end.all1") + " "
						+ Messages.getString("action.end.packages") + " "
						+ Messages.getString("action.end.all2") + " "
						+ Messages.getString("action.end.delete");
			else if (node.getName().equalsIgnoreCase(
					Messages.getString("node.DebianFilePackagesNode")))
				message = Messages.getString("action.end.all1") + " "
						+ Messages.getString("action.end.files") + " "
						+ Messages.getString("action.end.all2") + " "
						+ Messages.getString("action.end.delete");
			else
				message = Messages.getString("action.end.reloadCacheDB");
			JLabel what = new JLabel(message);

			while (dialog.isVisible() == true)
				try {
					dialog.setVisible(false);
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
					ErrorManager.getDefault().notify(e);
				}
				
			DialogDescriptor descriptor = new DialogDescriptor(what, getNodeAction(),
					true, new Object[]{DialogDescriptor.OK_OPTION},
					DialogDescriptor.OK_OPTION, DialogDescriptor.DEFAULT_OPTION, null,
					(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
						}
					}));
			descriptor.setClosingOptions(new Object[]{DialogDescriptor.OK_OPTION});
			Dialog readyDialog = DialogDisplayer.getDefault()
					.createDialog(descriptor);
			readyDialog.setVisible(true);
		}

		/**
		 * 
		 * @return String in which the Text of the Title will be returned
		 */
		public String getNodeAction() {
			String ret = "";
			if (node.getName().equalsIgnoreCase(
					Messages.getString("node.AvailablePackagesNode")))
				ret = Messages.getString("installAction.getName");
			else if (node.getName().equalsIgnoreCase(
					Messages.getString("node.InstalledPackagesNode")))
				ret = Messages.getString("deleteAction.getName");
			else if (node.getName().equalsIgnoreCase(
					Messages.getString("node.UpdatablePackagesNode")))
				ret = Messages.getString("updateAction.getName");
			else if (node.getName().equalsIgnoreCase(
					Messages.getString("node.AlreadyDeletedPackagesNode")))
				ret = Messages.getString("realyDeleteAction.getName");
			else if (node.getName().equalsIgnoreCase(
					Messages.getString("node.DebianFilePackagesNode")))
				ret = Messages.getString("deleteAction.getName");
			else
				ret = Messages.getString("reloadAction.getName");
			return ret;

		}

		/**
		 * here you can give a job to the PackageManager it is designed for some
		 * actions like give all packages which depends on the constructor given
		 * 
		 * @throws PackageManagerException
		 */
		abstract void doJob();

		/**
		 * enable all JFrames, refresh all nodes and delete the package list
		 * 
		 */
		public void dontWantToInstall() {
			packageList.removeAll(packageList);
			for (Frame frame : JFrame.getFrames()) {
				frame.setEnabled(true);
			}
		}

		public boolean checkIfApplicationsLinkToPackages() {
			Realm realm = (Realm) node.getLookup().lookup(Realm.class);
			List<Application> applSet = new ArrayList<Application>();
			try {
				for (Application appl : realm.getDirectory().list(Application.class))
					for (Package pkg : packageList)
						if (pkg.getName().equalsIgnoreCase(
								appl.getSchema(appl.getRealm()).getName()))
							applSet.add(appl);
			} catch (SchemaLoadingException e) {
				e.printStackTrace();
				ErrorManager.getDefault().notify(e);
			} catch (DirectoryException e) {
				e.printStackTrace();
				ErrorManager.getDefault().notify(e);
			}
			boolean ret=false;
			if (applSet.size() == 0)
				ret=true;
			else if(doErrorLoadForApplication(realm, applSet))
				ret =true;
				for (Frame frame : Frame.getFrames())
					frame.setEnabled(!ret);
			return ret;

		}
	}

	private final class JobQueue extends Thread {
		private LinkedList<Job> queue = new LinkedList<Job>();

		private JobQueue() {
			super("JobQueue");
		}

		/**
		 * @see java.lang.Runnable#run()
		 */
		public void run() {
			try {
				// Always check of Thread's interrupt state. CK.
				while (!Thread.currentThread().isInterrupted()) {
					Job nextJob = null;
					// wait for new job to arrive
					synchronized (this) {
						while (queue.size() == 0) {
							try {
								wait();
							} catch (InterruptedException e) {
								jobQueue = null;
								ErrorManager.getDefault().notify(e);
								notify();
							}
						}
						nextJob = queue.removeFirst();
					}
					if (nextJob != null) {
						nextJob.doJob();
					}
				}
				jobQueue = null;
				queue = null;
			} catch (ThreadDeath td) {
				ErrorManager.getDefault().notify(td);
				// Be careful if you really want to start a
				// new thread when this thread is stopped,
				// because the ThreadGroup could be in
				// the process of destruction. CK.
				jobQueue = null;
				// Always rethrow ThreadDeath exception if caught it!
				throw td;
			} catch (OutOfMemoryError ooe) {
				ErrorManager.getDefault().notify(ooe);
				jobQueue = null;
				throw ooe;
			}
		}

		/**
		 * Add a job
		 */
		synchronized void addJob(Job job) {

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
			if (null == singletonInstance) {
				singletonInstance = new PackageManagerJobQueue();
			}
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
	public void addPackageManagerJob(Job job) {
		instanceValidation();
		jobQueue.addJob(job);
	}

	private void destroyJobQueue() {
		if (null != jobQueue || jobQueue.isAlive()) {
			jobQueue.interrupt();

		}
	}

}
