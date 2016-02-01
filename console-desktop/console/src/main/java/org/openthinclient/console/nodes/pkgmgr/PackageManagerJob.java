package org.openthinclient.console.nodes.pkgmgr;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.nodes.Node;
import org.openide.util.Utilities;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.schema.provider.HTTPSchemaProvider;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;
import org.openthinclient.console.ConsoleFrame;
import org.openthinclient.console.Messages;
import org.openthinclient.console.nodes.DirObjectListNode;
import org.openthinclient.console.nodes.RealmNode;
import org.openthinclient.console.nodes.pkgmgr.PackageManagerJobQueue.ProgressbarWorker;
import org.openthinclient.console.util.DetailViewFormBuilder;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.pkgmgr.PackageManagerException;
import org.openthinclient.pkgmgr.db.Package;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class PackageManagerJob {

	public final static int ONE_SECOND = 200;
	public static final int REFRESH_ALL_PACKAGES = 0;
	public static final int REFRESH_INSTALLED_PACKAGES = 1;
	public static final int REFRESH_INSTALLABLE_PACKAGES = 2;
	public static final int REFRESH_UPDATEABLE_PACKAGES = 3;
	public static final int REFRESH_REMOVED_PACKAGES = 4;
	public static final int REFRESH_DEBIAN_PACKAGES = 5;
	Node node;
	Collection<Package> packageCollection;
	List<Package> packageList;
	PackageManagerDelegation pkgmgr;
	Timer timer;
	ProgressbarWorker pgw;
	private JProgressBar progressBar;
	private JLabel jLInfo;
	private boolean moreInformation = false;
	/**
	 * load a Dialog with one Centered JProgressBar, start it on after it's
	 * visible is set TRUE a new ProgressWorker is started (new
	 * ProgressbarWorker(this, jd).start();) in this case "this" is the actually
	 * job and jd is the actually created JDialog
	 */
	private JDialog progressDialog;

	public PackageManagerJob(Node node, Collection<Package> packageCollection) {
		this.node = node;
		this.packageCollection = packageCollection;
		this.packageList = new ArrayList<Package>(packageCollection);
		pkgmgr = getPackageManagementNode(node).getPackageManagerDelegation();
	}

	public PackageManagerJob(Node node) {
		this.node = node;
		pkgmgr = getPackageManagementNode(node).getPackageManagerDelegation();
	}

	/**
	 *
	 * @return the Node of the Job
	 */
	public Node getNode() {
		return node;
	}

	/**
	 * Create the "real" job which the PackageManager should do and the method
	 * which should be called respectively I have designed it for starting hard
	 * stuff like installing deleting and so on, for methods which need a long
	 * time so that a JProgressBar is useful for the consumer
	 *
	 * @return actually not evaluated
	 * @throws PackageManagerException
	 */
	// FIXME should return an boolean value to check if the "whatever" was
	// successful or not...
	abstract Object doPMJob() throws PackageManagerException;

	public boolean doErrorLoadForApplication(Realm realm,
			List<Application> applicationSet) {

		final DetailViewFormBuilder dfb = new DetailViewFormBuilder(
				new FormLayout("f:p:g"));

		for (final Application appl : applicationSet)
			try {
				dfb.append(new JLabel(Messages.getString(
						"Job.ApplicationAlreadyUsed", appl.getName(),
						appl.getSchema(realm).getName())));
				// dfb.append(new JLabel(Messages
				// + " "
				// + appl.getName()
				// + " "
				// + Messages.getString("Job.ApplicationAlreadyUsed2")
				// + " "
				// + appl.getSchema(realm).getName()));
			} catch (final SchemaLoadingException e1) {
				e1.printStackTrace();
				ErrorManager.getDefault().notify(e1);
			}
		dfb.nextLine();
		dfb.append(new JLabel(Messages
				.getString("Job.ApplicationAlreadyUsedQuestion")));

		final DialogDescriptor descriptor = new DialogDescriptor(dfb.getPanel(),
				getNodeAction(), true, new Object[]{DialogDescriptor.CANCEL_OPTION,
						DialogDescriptor.OK_OPTION}, null, DialogDescriptor.BOTTOM_ALIGN,
				null, new ActionListener() {
					public void actionPerformed(ActionEvent e) {
					}
				});
		descriptor.setClosingOptions(new Object[]{DialogDescriptor.OK_OPTION,
				DialogDescriptor.CANCEL_OPTION});
		final Dialog dialog = DialogDisplayer.getDefault().createDialog(
				descriptor);
		dialog.setIconImage(Utilities.loadImage(
				"org/openthinclient/console/icon.png", true));
		dialog.setVisible(true);

		if (descriptor.getValue() == DialogDescriptor.OK_OPTION) {
			boolean ret = true;
			for (final Application appl : applicationSet)
				try {
					final LDAPDirectory dir = realm.getDirectory();
					if (!dir.delete(appl))
						ret = false;
				} catch (final DirectoryException e1) {
					e1.printStackTrace();
					ret = false;
					ErrorManager.getDefault().notify(e1);
				}
			if (ret) {
				Node tmp = node;
				while (tmp != null) {
					if (tmp instanceof RealmNode)
						for (final Node chilnode : ((RealmNode) tmp).getChildren()
								.getNodes())
							if (chilnode.getName().equalsIgnoreCase(
									Messages.getString("Applications_title")))
								((DirObjectListNode) chilnode).refresh();
					tmp = tmp.getParentNode();
				}
			}
			return ret;
		}
		return false;

	}

	public boolean accepptLicenseDialog(String packageName, String licenseText) {
		final JTextArea jta = new JTextArea(licenseText);
		jta.setFocusable(false);
		jta.setLineWrap(true);
		jta.setWrapStyleWord(true);

		final JScrollPane scrollPane = new JScrollPane(jta);

		final JButton okButton = new JButton("I Accept");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});

		final DialogDescriptor descriptor = new DialogDescriptor(scrollPane,
				"License Agreement for Package " + packageName, true, new Object[]{
						DialogDescriptor.CANCEL_OPTION, okButton}, null,
				DialogDescriptor.BOTTOM_ALIGN, null, new ActionListener() {
					public void actionPerformed(ActionEvent e) {
					}
				});
		descriptor.setClosingOptions(new Object[]{okButton,
				DialogDescriptor.CANCEL_OPTION});

		final Dialog licenseDialog = DialogDisplayer.getDefault().createDialog(
				descriptor);
		licenseDialog.setPreferredSize(new Dimension(640, 480));
		licenseDialog.setMinimumSize(new Dimension(640, 480));
		licenseDialog.pack();

		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		licenseDialog.setLocation(
				(screenSize.width - licenseDialog.getWidth()) / 2,
				(screenSize.height - licenseDialog.getHeight()) / 2);

		licenseDialog.setIconImage(Utilities.loadImage(
				"org/openthinclient/console/icon.png", true));

		licenseDialog.setVisible(true);

		return descriptor.getValue() == okButton;
	}

	public void loadDialog(final PackageManagerDelegation pm) {
		try {
			doLoadDialog(pm);
		} catch (Exception e) {
			ErrorManager.getDefault().notify(e);
		}
	}

	protected void doLoadDialog(final PackageManagerDelegation pm) {
		final CellConstraints cc = new CellConstraints();
		Font f = UIManager.getFont("TitledBorder.font"); //$NON-NLS-1$
		f = f.deriveFont(Font.BOLD, AffineTransform.getScaleInstance(1.5, 1.5));

		progressDialog = new JDialog(ConsoleFrame.getINSTANCE(), true);
		progressDialog.setResizable(false);

		final JPanel cp = new JPanel(new FormLayout("f:p:g", "p,p,p"));
		cp.setPreferredSize(new Dimension(300, 100));
		progressDialog.setContentPane(cp);
		progressDialog.setIconImage(Utilities.loadImage(
				"org/openthinclient/console/icon.png", true));

		cp.setBorder(Borders.DIALOG_BORDER);

		final JLabel jl2 = new JLabel(Messages.getString("pleasewait"));
		jl2.setFont(f);
		jl2.setForeground(new Color(50, 50, 150));
		cp.add(jl2, cc.xy(1, 1));
		final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		cp.setLocation(new Double(dim.getWidth()).intValue() / 2 - 150,
				new Double(dim.getHeight()).intValue() / 2 - 150);
		cp.setSize(new Dimension(300, 100));
		progressBar = new JProgressBar(0, pm.getMaxProgress());
		progressBar.setValue(0);
		progressBar.setStringPainted(true);
		pm.resetValuesForDisplaying();
		jLInfo = new JLabel(" ");
		cp.add(jLInfo, cc.xy(1, 3));

		if (node.getName().equalsIgnoreCase(
				Messages.getString("node.AvailablePackagesNode"))
				|| node.getName().equalsIgnoreCase(
						Messages.getString("node.UpdatablePackagesNode")))
			moreInformation = true;
		timer = new Timer(ONE_SECOND, new ActionListener() {
			public void actionPerformed(ActionEvent evt) {

				while (pm.isDone() == false && !Thread.interrupted()
						&& !PackageManagerJobQueue.errorExisting) {
					progressBar.setValue(pm.getActprogress());
					if (moreInformation && null != pm.getActPackName())
						jLInfo.setText(pm.getActPackName() + ": "
								+ pm.getActMaxFileSize()[0] + "KB / "
								+ pm.getActMaxFileSize()[1] + "KB");
					cp.repaint();
					cp.paint(cp.getGraphics());
					try {
						if (!Thread.interrupted())
							Thread.sleep(300);
						else
							timer.stop();
					} catch (final InterruptedException e) {
						timer.stop();
						ErrorManager.getDefault().notify(e);
					}
				}
				timer.stop();
			}
		});

		cp.add(progressBar, cc.xy(1, 2));
		timer.start();

		progressDialog.setContentPane(cp);
		progressDialog.pack();

		pgw = new ProgressbarWorker(this, progressDialog);
		pgw.start();

		// center dialog box
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		progressDialog.setLocation(
				(screenSize.width - progressDialog.getWidth()) / 2,
				(screenSize.height - progressDialog.getHeight()) / 2);
		progressDialog.getSize();
		progressDialog.setIconImage(Utilities.loadImage(
				"org/openthinclient/console/icon.png", true));

		progressDialog.setVisible(true);
	}

	void stopTimer() {
		timer.stop();
	}

	public void createInformationOptionPane(boolean reloadSchemaProviderOption) {
		if (reloadSchemaProviderOption) {
			final Realm realm = (Realm) node.getLookup().lookup(Realm.class);
			final String hostname = realm.getConnectionDescriptor().getHostname();
			try {
				realm.removeSchemaProvider();
				realm.setNeedsRefresh();
				final HTTPSchemaProvider httpsp = new HTTPSchemaProvider(hostname);
				httpsp.reload();
			} catch (final MalformedURLException e2) {
				e2.printStackTrace();
				ErrorManager.getDefault().notify(e2);
			}
		}
		progressBar.setValue(pkgmgr.getActprogress());
		progressDialog.setVisible(false);
		progressDialog.validate();
		progressDialog.dispose();

		timer.stop();
		String message = "";
		if (node.getName().equalsIgnoreCase(
				Messages.getString("node.AvailablePackagesNode")))
			message = Messages.getString("action.end.allpackagesinstalled");
		// message = Messages.getString("action.end.all1") + " "
		// + Messages.getString("action.end.packages") + " "
		// + Messages.getString("action.end.all2") + " "
		// + Messages.getString("action.end.install");
		else if (node.getName().equalsIgnoreCase(
				Messages.getString("node.InstalledPackagesNode")))
			message = Messages.getString("action.end.allpackagesdeleted");
		// message = Messages.getString("action.end.all1") + " "
		// + Messages.getString("action.end.packages") + " "
		// + Messages.getString("action.end.all2") + " "
		// + Messages.getString("action.end.delete");
		else if (node.getName().equalsIgnoreCase(
				Messages.getString("node.UpdatablePackagesNode")))
			message = Messages.getString("action.end.allpackagesupdated");
		// message = Messages.getString("action.end.all1") + " "
		// + Messages.getString("action.end.packages") + " "
		// + Messages.getString("action.end.all2") + " "
		// + Messages.getString("action.end.update");
		else if (node.getName().equalsIgnoreCase(
				Messages.getString("node.AlreadyDeletedPackagesNode")))
			message = Messages.getString("action.end.allpackagesdeleted");
		// message = Messages.getString("action.end.all1") + " "
		// + Messages.getString("action.end.packages") + " "
		// + Messages.getString("action.end.all2") + " "
		// + Messages.getString("action.end.delete");
		else if (node.getName().equalsIgnoreCase(
				Messages.getString("node.DebianFilePackagesNode")))
			message = Messages.getString("action.end.allfilesdeleted");
		// message = Messages.getString("action.end.all1") + " "
		// + Messages.getString("action.end.files") + " "
		// + Messages.getString("action.end.all2") + " "
		// + Messages.getString("action.end.delete");
		else
			message = Messages.getString("action.end.reloadCacheDB");
		final JLabel what = new JLabel(message);

		while (progressDialog.isVisible() == true)
			try {
				progressDialog.setVisible(false);
				Thread.sleep(200);
			} catch (final InterruptedException e) {
				e.printStackTrace();
				ErrorManager.getDefault().notify(e);
			}

		final DialogDescriptor descriptor = new DialogDescriptor(what,
				getNodeAction(), true, new Object[]{DialogDescriptor.OK_OPTION},
				DialogDescriptor.OK_OPTION, DialogDescriptor.DEFAULT_OPTION, null,
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
					}
				});
		descriptor.setClosingOptions(new Object[]{DialogDescriptor.OK_OPTION});
		final Dialog readyDialog = DialogDisplayer.getDefault().createDialog(
				descriptor);
		readyDialog.setIconImage(Utilities.loadImage(
				"org/openthinclient/console/icon.png", true));
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
	}

	public boolean checkIfApplicationsLinkToPackages() {
		final Realm realm = (Realm) node.getLookup().lookup(Realm.class);
		final List<Application> applSet = new ArrayList<Application>();
		try {
			for (final Application appl : realm.getDirectory().list(
					Application.class))
				for (final Package pkg : packageList)
					if (pkg.getName().equalsIgnoreCase(appl.getSchema(realm).getName()))
						applSet.add(appl);
		} catch (final SchemaLoadingException e) {
			e.printStackTrace();
			ErrorManager.getDefault().notify(e);
		} catch (final DirectoryException e) {
			e.printStackTrace();
			ErrorManager.getDefault().notify(e);
		}
		boolean ret = false;
		if (applSet.size() == 0)
			ret = true;
		else if (doErrorLoadForApplication(realm, applSet))
			ret = true;
		return ret;

	}

	// FIXME: stupid lookup doesn't work
	public PackageManagementNode getPackageManagementNode(Node node) {
		if (node instanceof PackageManagementNode)
			return (PackageManagementNode) node;
		else if (node.getParentNode() instanceof PackageManagementNode)
			return (PackageManagementNode) node.getParentNode();
		return null;
	}
}
