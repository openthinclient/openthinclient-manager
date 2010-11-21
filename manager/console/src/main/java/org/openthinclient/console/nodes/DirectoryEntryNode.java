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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.directory.server.tools.ToolCommandListener;
import org.apache.directory.server.tools.commands.exportcmd.ExportCommandExecutor;
import org.apache.directory.server.tools.commands.importcmd.ImportCommandExecutor;
import org.apache.directory.server.tools.util.ListenerParameter;
import org.apache.directory.server.tools.util.Parameter;
import org.apache.log4j.Logger;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.actions.NodeAction;
import org.openide.util.actions.SystemAction;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.DetailView;
import org.openthinclient.console.DetailViewProvider;
import org.openthinclient.console.EditorProvider;
import org.openthinclient.console.LdifActionProgressBar;
import org.openthinclient.console.MainTreeTopComponent;
import org.openthinclient.console.Messages;
import org.openthinclient.console.RefreshAction;
import org.openthinclient.console.Refreshable;
import org.openthinclient.console.nodes.views.DirectoryEntryDetailView;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.DirectoryFacade;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.TypeMapping;
import org.openthinclient.ldap.Util;

import com.levigo.util.swing.SwingWorker;

/** Getting the feed node and wrapping it in a FilterNode */
public class DirectoryEntryNode extends MyAbstractNode
		implements
			DetailViewProvider,
			EditorProvider,
			Refreshable {
	private static final Logger logger = Logger.getLogger(TypeMapping.class);

	// FIXME: somebody please fix all this static crap!
	private static class ExportLDIFAction extends NodeAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/*
		 * @see org.openide.util.actions.CallableSystemAction#asynchronous()
		 */
		@Override
		protected boolean asynchronous() {
			return true;
		}

		/*
		 * @see org.openide.util.actions.SystemAction#getName()
		 */
		@Override
		public String getName() {
			return "Export LDIF";
		}

		/*
		 * @see org.openide.util.actions.SystemAction#getHelpCtx()
		 */
		@Override
		public HelpCtx getHelpCtx() {
			return null;
		}

		/*
		 * @see
		 * org.openide.util.actions.NodeAction#performAction(org.openide.nodes.Node
		 * [])
		 */
		@Override
		protected void performAction(Node[] activatedNodes) {
			final LdifActionProgressBar bar = new LdifActionProgressBar();
			final JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle("Export object tree as LDIF");
			chooser.addChoosableFileFilter(new FileFilter() {
				@Override
				public String getDescription() {
					return "LDIF files";
				}

				@Override
				public boolean accept(File f) {
					return f.isDirectory() || f.getName().endsWith(".ldif")
							|| f.getName().endsWith(".txt");
				}
			});

			final LDAPConnectionDescriptor lcd = (LDAPConnectionDescriptor) activatedNodes[0]
					.getLookup().lookup(LDAPConnectionDescriptor.class);

			final String secViewName = Messages
					.getString("SecondaryDirectoryViewNode.name");

			final boolean isSec = activatedNodes[0].getDisplayName().equals(
					secViewName)
					|| activatedNodes[0].getParentNode().getName().equals(secViewName)
					|| activatedNodes[0].getParentNode().getParentNode().getName()
							.equals(secViewName);

			if (chooser.showDialog(MainTreeTopComponent.getDefault(), "Export") == JFileChooser.APPROVE_OPTION) {
				bar.startProgress();
				final String dn = ((DirectoryEntryNode) activatedNodes[0]).getDn();
				try {
					final NameCallback nc = new NameCallback("Bind DN");
					final PasswordCallback pc = new PasswordCallback("Password", false);

					lcd.getCallbackHandler().handle(new Callback[]{nc, pc});

					final List<Parameter> params = new ArrayList<Parameter>();
					params.add(new Parameter(ExportCommandExecutor.HOST_PARAMETER, lcd
							.getHostname()));
					params.add(new Parameter(ExportCommandExecutor.PORT_PARAMETER,
							(int) lcd.getPortNumber()));

					switch (lcd.getAuthenticationMethod()){
						case SIMPLE :
							params.add(new Parameter(ExportCommandExecutor.AUTH_PARAMETER,
									"simple"));
							params.add(new Parameter(ExportCommandExecutor.USER_PARAMETER, nc
									.getName()));
							params.add(new Parameter(
									ExportCommandExecutor.PASSWORD_PARAMETER, new String(pc
											.getPassword())));
					}

					params.add(new Parameter(ExportCommandExecutor.BASEDN_PARAMETER, lcd
							.getBaseDN()));
					params.add(new Parameter(ExportCommandExecutor.SCOPE_PARAMETER,
							ExportCommandExecutor.SCOPE_SUBTREE));
					params.add(new Parameter(ExportCommandExecutor.EXPORTPOINT_PARAMETER,
							dn));
					String path = chooser.getSelectedFile().getCanonicalPath();
					if (!path.endsWith(".ldif"))
						path = path + ".ldif";

					final File temp = File.createTempFile("openthinclient-export-",
							".ldif");
					params.add(new Parameter(ExportCommandExecutor.FILE_PARAMETER, temp
							.getPath()));
					params
							.add(new Parameter(ExportCommandExecutor.DEBUG_PARAMETER, true));
					params.add(new Parameter(ExportCommandExecutor.VERBOSE_PARAMETER,
							true));

					final ProgressHandle handle = ProgressHandleFactory
							.createHandle("LDIF export");
					final ListenerParameter listeners[] = new ListenerParameter[]{
							new ListenerParameter(
									ExportCommandExecutor.EXCEPTIONLISTENER_PARAMETER,
									new ToolCommandListener() {
										public void notify(Serializable o) {
											ErrorManager.getDefault().annotate((Throwable) o,
													"Exception during LDIF export");
											ErrorManager.getDefault().notify((Throwable) o);
										}
									}),
							new ListenerParameter(
									ExportCommandExecutor.OUTPUTLISTENER_PARAMETER,
									new ToolCommandListener() {
										public void notify(Serializable o) {
											handle.progress(o.toString());
										}
									}),
							new ListenerParameter(
									ExportCommandExecutor.ERRORLISTENER_PARAMETER,
									new ToolCommandListener() {
										public void notify(Serializable o) {
											final IOException e = new IOException(o.toString());
											ErrorManager.getDefault().annotate(e,
													"Error during LDIF export");
											ErrorManager.getDefault().notify((Throwable) o);
										}
									})};

					handle.start();
					try {
						final ExportCommandExecutor ex = new ExportCommandExecutor();

						ex.execute(params.toArray(new Parameter[params.size()]), listeners);
					} finally {
						handle.finish();
						createExportFile(temp, path, lcd.getBaseDN(), bar);
						bar.finished(Messages.getString("LdifExportPanel.name"),
								Messages.getString("LdifExportPanel.text"));
					}
				} catch (final Throwable t) {
					logger.error("Could not export", t);
					bar.finished("LdifExportPanel.name", t.toString());
				}

			}
		}

		/*
		 * @see org.openide.util.actions.NodeAction#enable(org.openide.nodes.Node[])
		 */
		@Override
		protected boolean enable(Node[] activatedNodes) {
			return activatedNodes.length == 1
					&& activatedNodes[0] instanceof DirectoryEntryNode;
		}
	}

	private static class ImportLDIFAction extends NodeAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		LDAPConnectionDescriptor lcd;

		/*
		 * @see org.openide.util.actions.CallableSystemAction#asynchronous()
		 */
		@Override
		protected boolean asynchronous() {
			return true;
		}

		/*
		 * @see org.openide.util.actions.SystemAction#getName()
		 */
		@Override
		public String getName() {
			return "Import LDIF";
		}

		/*
		 * @see org.openide.util.actions.SystemAction#getHelpCtx()
		 */
		@Override
		public HelpCtx getHelpCtx() {
			return null;
		}

		/*
		 * @see
		 * org.openide.util.actions.NodeAction#performAction(org.openide.nodes.Node
		 * [])
		 */
		@Override
		protected void performAction(Node[] activatedNodes) {
			final JFileChooser chooser = new JFileChooser();

			chooser.setDialogTitle("Import object tree from LDIF");
			chooser.addChoosableFileFilter(new FileFilter() {
				@Override
				public String getDescription() {
					return "LDIF files";
				}

				@Override
				public boolean accept(File f) {
					return f.isDirectory() || f.getName().endsWith(".ldif")
							|| f.getName().endsWith(".txt");
				}
			});

			lcd = (LDAPConnectionDescriptor) activatedNodes[0].getLookup().lookup(
					LDAPConnectionDescriptor.class);

			if (chooser.showDialog(MainTreeTopComponent.getDefault(), "Import") == JFileChooser.APPROVE_OPTION) {
				final LdifActionProgressBar bar = new LdifActionProgressBar();
				bar.startProgress();
				final File importFile = chooser.getSelectedFile();
				try {
					importTempFile(importFile, lcd, bar);
				} catch (final Exception e) {
					logger.error("Could not import", e);
					bar.finished(Messages.getString("LdifImportPanel.name"), e.toString());
				}
			}
		}

		/*
		 * @see org.openide.util.actions.NodeAction#enable(org.openide.nodes.Node[])
		 */
		@Override
		protected boolean enable(Node[] activatedNodes) {
			return activatedNodes.length == 1
					&& activatedNodes[0] instanceof DirectoryEntryNode;
		}

	}

	static final class ImportWorker extends SwingWorker {

		private final LDAPConnectionDescriptor lcd;
		private final Set<File> importFiles;
		private final LdifActionProgressBar bar;

		private boolean interrupt = false;

		public ImportWorker(LDAPConnectionDescriptor lcd, Set<File> importFiles,
				LdifActionProgressBar bar) {
			this.lcd = lcd;
			this.importFiles = importFiles;
			this.bar = bar;
		}

		@Override
		public Object construct() {
			try {
				for (final File importFile : this.importFiles)
					if (importFile != null) {

						if (logger.isDebugEnabled())
							logger.debug("import following temporary file: " + importFile);

						final NameCallback nc = new NameCallback("Bind DN");
						final PasswordCallback pc = new PasswordCallback("Password", false);

						lcd.getCallbackHandler().handle(new Callback[]{nc, pc});

						// Preparing the call to the Import Command
						final List<Parameter> params = new ArrayList<Parameter>();
						final ImportCommandExecutor importCommandExecutor = new ImportCommandExecutor();

						params.add(new Parameter(ImportCommandExecutor.HOST_PARAMETER, lcd
								.getHostname()));
						params.add(new Parameter(ImportCommandExecutor.PORT_PARAMETER,
								new Integer(lcd.getPortNumber())));

						switch (lcd.getAuthenticationMethod()){
							case SIMPLE :

								params.add(new Parameter(ImportCommandExecutor.AUTH_PARAMETER,
										"simple"));
								params.add(new Parameter(ImportCommandExecutor.USER_PARAMETER,
										nc.getName()));
								params.add(new Parameter(
										ImportCommandExecutor.PASSWORD_PARAMETER, new String(pc
												.getPassword())));
						}
						params.add(new Parameter(ImportCommandExecutor.FILE_PARAMETER,
								importFile));
						params
								.add(new Parameter(
										ImportCommandExecutor.IGNOREERRORS_PARAMETER, new Boolean(
												true)));
						params.add(new Parameter(ImportCommandExecutor.DEBUG_PARAMETER,
								new Boolean(false)));
						params.add(new Parameter(ImportCommandExecutor.VERBOSE_PARAMETER,
								new Boolean(false)));
						params.add(new Parameter(ImportCommandExecutor.QUIET_PARAMETER,
								new Boolean(false)));

						// Calling the import command
						importCommandExecutor.execute(
								params.toArray(new Parameter[params.size()]),
								new ListenerParameter[0]);
						importFile.delete();
					}
			} catch (final Throwable t) {
				logger.error("Could not import", t);
				ErrorManager.getDefault().annotate(t, "Could not import");
				ErrorManager.getDefault().notify(t);
				return false;
			}
			return true;
		}

		@Override
		public void finished() {
			interrupt = true;
			bar.finished(Messages.getString("LdifImportPanel.name"),
					Messages.getString("LdifImportPanel.text"));
		}

		public boolean getInterrupt() {
			return this.interrupt;
		}
	}

	public static void importHTTPAction(LDAPConnectionDescriptor lcd,
			Set<File> importFiles) {

		final LdifActionProgressBar bar = new LdifActionProgressBar();

		final ImportWorker imp = new ImportWorker(lcd, importFiles, bar);
		imp.start();

		bar.loadDialog();
	}

	public static void importAction(LDAPConnectionDescriptor lcd,
			File importFile, LdifActionProgressBar bar) {

		try {
			if (logger.isDebugEnabled())
				logger.debug("import following temporary file: " + importFile);

			final NameCallback nc = new NameCallback("Bind DN");
			final PasswordCallback pc = new PasswordCallback("Password", false);

			lcd.getCallbackHandler().handle(new Callback[]{nc, pc});

			// Preparing the call to the Import Command
			final List<Parameter> params = new ArrayList<Parameter>();
			final ImportCommandExecutor importCommandExecutor = new ImportCommandExecutor();

			params.add(new Parameter(ImportCommandExecutor.HOST_PARAMETER, lcd
					.getHostname()));
			params.add(new Parameter(ImportCommandExecutor.PORT_PARAMETER,
					new Integer(lcd.getPortNumber())));

			switch (lcd.getAuthenticationMethod()){
				case SIMPLE :

					params.add(new Parameter(ImportCommandExecutor.AUTH_PARAMETER,
							"simple"));
					params.add(new Parameter(ImportCommandExecutor.USER_PARAMETER, nc
							.getName()));
					params.add(new Parameter(ImportCommandExecutor.PASSWORD_PARAMETER,
							new String(pc.getPassword())));
			}
			params
					.add(new Parameter(ImportCommandExecutor.FILE_PARAMETER, importFile));
			params.add(new Parameter(ImportCommandExecutor.IGNOREERRORS_PARAMETER,
					new Boolean(true)));
			params.add(new Parameter(ImportCommandExecutor.DEBUG_PARAMETER,
					new Boolean(false)));
			params.add(new Parameter(ImportCommandExecutor.VERBOSE_PARAMETER,
					new Boolean(false)));
			params.add(new Parameter(ImportCommandExecutor.QUIET_PARAMETER,
					new Boolean(false)));

			// Calling the import command
			importCommandExecutor.execute(
					params.toArray(new Parameter[params.size()]),
					new ListenerParameter[0]);

			// FIXME: implement exception/error Listener
			// see e.g.:
			// http://svn.apache.org/repos/asf/directory/sandbox/pamarcelot/trunks/ldapstudio-importexport-plugin/src/main/java/org/apache/directory/ldapstudio/importexport/controller/actions/ImportAction.java
			bar.finished(Messages.getString("LdifImportPanel.name"),
					Messages.getString("LdifImportPanel.text"));

		} catch (final Throwable t) {
			bar.finished();
			logger.error("Could not import", t);
			ErrorManager.getDefault().annotate(t, "Could not import");
			ErrorManager.getDefault().notify(t);
		}
	}

	private static void importTempFile(File importFile,
			LDAPConnectionDescriptor lcd, LdifActionProgressBar bar) throws Exception {
		final FileInputStream fstream = new FileInputStream(importFile);
		final DataInputStream in = new DataInputStream(fstream);
		final BufferedReader br = new BufferedReader(new InputStreamReader(in));
		final StringBuffer content = new StringBuffer();

		String strLine;
		final String baseDn = lcd.getBaseDN();

		final Pattern toReplace = Pattern.compile(".*#%BASEDN%#$");
		while ((strLine = br.readLine()) != null) {
			final Matcher m = toReplace.matcher(strLine);
			if (m.matches()) {
				final int pos = strLine.lastIndexOf("#%BASEDN%#");
				content.append(strLine.substring(0, pos) + baseDn).append(
						System.getProperty("line.separator"));
			} else
				content.append(strLine).append(System.getProperty("line.separator"));
		}
		in.close();

		final File tempFile = File
				.createTempFile("openthinclient-import-", ".ldif");
		OutputStream os = null;
		try {
			os = new BufferedOutputStream(new FileOutputStream(
					tempFile.getAbsolutePath()));
			os.write(content.toString().getBytes());
		} finally {
			if (null != os) {
				os.flush();
				os.close();
			}
		}

		importAction(lcd, tempFile, bar);
		tempFile.delete();
	}

	private static void createExportFile(File tempFile, String path, String dn,
			LdifActionProgressBar bar) throws Exception {
		final FileInputStream fstream = new FileInputStream(tempFile);
		final DataInputStream in = new DataInputStream(fstream);
		final BufferedReader br = new BufferedReader(new InputStreamReader(in));
		final StringBuffer content = new StringBuffer().append("version: 1")
				.append(System.getProperty("line.separator"));
		String strLine;

		// replace last occurrence of dn with "#%BASEDN%#" on relevant entries
		final Pattern toReplace = Pattern.compile(
				"((^dn:)|(^uniquemember:)|(^l:)) .*" + dn + "$",
				Pattern.CASE_INSENSITIVE);
		while ((strLine = br.readLine()) != null) {
			final Matcher m = toReplace.matcher(strLine);
			if (m.matches()) {
				final int pos = strLine.lastIndexOf(dn);
				content.append(strLine.substring(0, pos) + "#%BASEDN%#").append(
						System.getProperty("line.separator"));
			} else
				content.append(strLine).append(System.getProperty("line.separator"));
		}
		in.close();
		tempFile.delete();
		OutputStream os = null;
		try {
			os = new BufferedOutputStream(new FileOutputStream(path));
			os.write(content.toString().getBytes());
		} finally {
			if (null != os) {
				os.flush();
				os.close();
			}
		}
	}

	private final String rdn;
	private final String dn;
	private static boolean mutable;

	static class ChildEntries extends AbstractAsyncArrayChildren {
		private final String dn;

		public ChildEntries(String dn) {
			this.dn = dn;
		}

		@Override
		protected Collection asyncInitChildren() {
			try {
				final LDAPConnectionDescriptor lcd = ((DirectoryEntryNode) getNode())
						.getConnectionDescriptor();

				if (lcd == null)
					return Collections.EMPTY_LIST;

				final DirContext ctx = lcd.createDirectoryFacade().createDirContext();
				NamingEnumeration<NameClassPair> bindings;
				try {
					bindings = ctx.list(ctx.getNameParser("").parse(dn)); //$NON-NLS-1$

					final List<String> names = new ArrayList<String>();

					if (null != bindings)
						while (bindings.hasMoreElements()) {
							final NameClassPair b = bindings.next();
							final String name = b.isRelative() ? b.getName()
									+ (dn.length() > 0 ? "," + dn : "") : b.getName(); //$NON-NLS-1$ //$NON-NLS-2$
							names.add(name);
						}

					if (mutable)
						return names;
					else
						return Collections.EMPTY_LIST;
				} finally {
					if (null != ctx)
						ctx.close();
				}
			} catch (final Exception e) {
				ErrorManager.getDefault().notify(e);
				add(new Node[]{new ErrorNode(
						Messages.getString("DirectoryEntryNode.cantDisplay"), e)}); //$NON-NLS-1$

				return Collections.EMPTY_LIST;
			}
		}

		@Override
		protected Node[] createNodes(Object key) {
			if (key instanceof Node[])
				return (Node[]) key;

			return new Node[]{new DirectoryEntryNode(getNode(), (String) key)};
		}
	}

	/**
	 * @param node
	 * @param lcd
	 * @param rdn
	 */
	public DirectoryEntryNode(Node node, String dn) {
		super(new ChildEntries(dn), node.getLookup());
		this.dn = dn;
		final int i = dn.indexOf(',');
		this.rdn = i > 0 ? dn.substring(0, i) : dn;
	}

	/**
	 * @param node
	 * @param lcd
	 * @param dn
	 */
	public DirectoryEntryNode(Children c, Node node,
			LDAPConnectionDescriptor lcd, String dn) {
		super(c, new ProxyLookup(new Lookup[]{Lookups.fixed(new Object[]{lcd}),
				node.getLookup()}));
		this.dn = this.rdn = dn;
	}

	/**
	 * @param node
	 * @param lcd
	 * @param dn
	 */
	public DirectoryEntryNode(Node node, LDAPConnectionDescriptor lcd, String dn) {
		this(new ChildEntries(dn), node, lcd, dn);
	}

	public LDAPConnectionDescriptor getConnectionDescriptor() {
		final LDAPConnectionDescriptor lcd = (LDAPConnectionDescriptor) getLookup()
				.lookup(LDAPConnectionDescriptor.class);

		final String secViewName = Messages
				.getString("SecondaryDirectoryViewNode.name");
		String levelOne = "";
		String levelTwo = "";
		String levelThree = "";

		boolean isSec = false;

		try {
			levelOne = this.getDisplayName();
			levelTwo = this.getParentNode().getName();
			levelThree = this.getParentNode().getParentNode().getName();

			isSec = levelOne.equals(secViewName) || levelTwo.equals(secViewName)
					|| levelThree.equals(secViewName);
		} catch (final NullPointerException n) {

		}

		// if(LDAPDirectory.areSettingsModified() == false && isSec) {
		// Realm realm = new Realm();
		// realm.setConnectionDescriptor(lcd);
		// try {
		// LDAPDirectory dir = realm.getDirectory();
		// } catch (DirectoryException e) {
		// e.printStackTrace();
		// }
		// }

		// if(LDAPDirectory.areSettingsModified() && isSec){
		// LDAPConnectionDescriptor lcdNew = LDAPDirectory.getNewLcd();
		//
		// if(lcdNew != null) {
		// mutable = true;
		// return lcdNew;
		// }
		// // }
		// else if(isSec) {
		// mutable = false;
		// return lcd;
		// }
		mutable = true;
		return lcd;
	}

	@Override
	public String getName() {
		return dn;
	}

	/*
	 * @see java.beans.FeatureDescriptor#getDisplayName()
	 */
	@Override
	public String getDisplayName() {
		return rdn;
	}

	@Override
	public Action[] getActions(boolean context) {
		if (isWritable())
			return new Action[]{
					// FIXME: readd when fixed
					// SystemAction.get(EditAction.class)};
					// SystemAction.get(DeleteAction.class)};
					SystemAction.get(ExportLDIFAction.class),
					SystemAction.get(ImportLDIFAction.class), null,
					SystemAction.get(RefreshAction.class)};
		else
			return new Action[]{SystemAction.get(ExportLDIFAction.class), null,
					SystemAction.get(RefreshAction.class)};
	}

	/*
	 * @see org.openide.nodes.FilterNode#canCopy()
	 */
	@Override
	public boolean canCopy() {
		return true;
	}

	/*
	 * @see org.openide.nodes.FilterNode#canDestroy()
	 */
	@Override
	public boolean canDestroy() {
		return true;
	}

	/*
	 * @see org.openide.nodes.Node#destroy()
	 */
	@Override
	public void destroy() throws IOException {
		try {
			try {

				final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
				final Realm realm = new Realm();
				realm.setConnectionDescriptor(lcd);

				final LDAPDirectory dir = realm.getDirectory();

				final DirectoryFacade df = lcd.createDirectoryFacade();
				final DirContext ctx = df.createDirContext();
				try {
					final Name targetName = df.makeRelativeName(this.dn);
					Util.deleteRecursively(ctx, targetName);
				} finally {
					ctx.close();
				}
			} catch (final NamingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (final DirectoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/*
	 * @see org.openide.nodes.FilterNode#canRename()
	 */
	@Override
	public boolean canRename() {
		return false;
	}

	/*
	 * @see org.openide.nodes.AbstractNode#setName(java.lang.String)
	 */
	@Override
	public void setName(String s) {
		final String sEdit = LDAPDirectory.idToUpperCase(s);
		final String rest = LDAPDirectory.idToUpperCase(this.dn).replace(
				LDAPDirectory.idToUpperCase(this.rdn) + ",", "");
		final boolean isRightDN = (sEdit.startsWith("CN=") || sEdit
				.startsWith("L=")) && sEdit.endsWith(rest);

		if (null == s || s.length() == 0 || isRightDN == false) {
			DialogDisplayer.getDefault().notify(
					new NotifyDescriptor(
							Messages.getString("DirectoryEntryNode.nameInvalid", s), //$NON-NLS-1$ //$NON-NLS-2$
							Messages.getString("DirectoryEntryNode.cantChangeName"), //$NON-NLS-1$
							NotifyDescriptor.DEFAULT_OPTION, NotifyDescriptor.ERROR_MESSAGE,
							null, null));
			return;
		}

		try {

			final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
			final DirectoryFacade df = lcd.createDirectoryFacade();
			final DirContext ctx = df.createDirContext();
			try {
				final Name oldName = df.makeRelativeName(this.dn);
				final Name newName = df.makeRelativeName(s);

				ctx.rename(oldName, newName);
			} finally {
				ctx.close();
			}
		} catch (final NamingException e) {
			e.printStackTrace();
		}

	}

	/*
	 * @see org.openthinclient.console.DetailViewProvider#getDetailView()
	 */
	public DetailView getDetailView() {
		return new DirectoryEntryDetailView();
	}

	/*
	 * @see org.openthinclient.console.EditorProvider#getEditor()
	 */
	public DetailView getEditor() {
		return new DirectoryEntryDetailView();
	}

	/*
	 * @see org.openthinclient.console.Refreshable#refresh()
	 */
	public void refresh() {
		((AbstractAsyncArrayChildren) getChildren()).refreshChildren();
	}

	public String getDn() {
		return dn;
	}

	public String getRdn() {
		return rdn;
	}

}
