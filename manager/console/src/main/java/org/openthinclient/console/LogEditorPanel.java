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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.nodes.Node;
import org.openide.windows.TopComponent;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.nodes.DirObjectListNode;
import org.openthinclient.console.nodes.DirObjectNode;
import org.openthinclient.console.nodes.RealmNode;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

@SuppressWarnings("serial")
public class LogEditorPanel extends JPanel

{
	private static LogEditorPanel logeditorPanel;

	public static LogEditorPanel getInstance() {
		logeditorPanel = new LogEditorPanel();
		return logeditorPanel;
	}

	private LogDetailView logdetailview;
	private JScrollPane jsp = new JScrollPane();

	public void init(Collection collection, TopComponent tc) {
		logdetailview = LogDetailView.getInstance();

		logdetailview.init(
				(Node[]) collection.toArray(new Node[collection.size()]), tc);
	}

	public void initForToolbar(Node[] selection, TopComponent tc, int who) {
		logdetailview = LogDetailView.getInstance();
		logdetailview.initForToolbar(selection, tc, who);
	}

	public void doEdit() {
		setSize(new Dimension(640, 480));
		setLayout(new FormLayout("f:m:g", "50dlu,f:min(200dlu;pref):g"));
		final CellConstraints cc = new CellConstraints();
		
		jsp = new JScrollPane();
		jsp.add(logdetailview.getMainComponent());
		jsp.setMinimumSize(new Dimension(500, 400));
		add(logdetailview.getHeaderComponent(), cc.xy(1, 1));
		add(jsp, cc.xy(1, 2));
		refreshMain();
		DialogDisplayer.getDefault().createDialog(
				new DialogDescriptor(this, getName(), true,
						new Object[]{DialogDescriptor.OK_OPTION}, this,
						DialogDescriptor.DEFAULT_ALIGN, null, new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								logdetailview.bsearch = false;
							}
						})).setVisible(true);

	}

	public void refreshMain() {
		remove(jsp);
		jsp = new JScrollPane(logdetailview.getMainComponent());
		add(jsp, new CellConstraints().xy(1, 2));
		repaint();
		validate();
	}

	static class LogDetailView extends AbstractDetailView {
		private static LogDetailView detailView;
		private static boolean isClient;
		private static String fileName;
		private String macAdress;
		private JTextField queryField;
		private static JComponent mainComponent;
		private boolean bsearch;
		private String searchValue;
		private static List<String> logFile;
		// private int fileChooser;
		public static final int SYS_LOG_FILE = 1;
		public static final int SERVER_LOG_FILE = 2;

		public static LogDetailView getInstance() {
			if (detailView == null)
				detailView = new LogDetailView();
			return detailView;

		}

		public void initForToolbar(Node[] selection, TopComponent tc, int who) {
			mainComponent = null;
			if (who == SYS_LOG_FILE)
				fileName = "/openthinclient/files/var/log/syslog.log";
			if (who == SERVER_LOG_FILE)
				fileName = "/openthinclient/files/var/log/server.log";
			isClient = false;
			logFile = new ArrayList<String>(getLogFile());
		}

		@Override
		public JComponent getHeaderComponent() {

			
			getFooterComponent();

			final DefaultFormBuilder dfb = new DefaultFormBuilder(new FormLayout(
					"p,  5dlu,100dlu,10dlu,p,10dlu,p,10dlu,p,10dlu,p", "f:p")); //$NON-NLS-1$
			dfb.setDefaultDialogBorder();
			
//			dfb.
			queryField = new JTextField();
			final JButton searchButton = new JButton(Messages.getString("Search")); //$NON-NLS-1$
			searchButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					searchValue = queryField.getText();
					bsearch = true;
					logeditorPanel.refreshMain();
				}
			});
			final JButton clearSearchButton = new JButton(Messages
					.getString("ClearSearch")); //$NON-NLS-1$
			clearSearchButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					bsearch = false;
					searchValue = "";
					queryField.setText("");
					logeditorPanel.refreshMain();
				}
			});
			final JButton updateButton = new JButton(Messages.getString("update")); //$NON-NLS-1$
			updateButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					bsearch = false;
					searchValue = "";
					queryField.setText("");
					logFile = new ArrayList<String>(getLogFile());
					logeditorPanel.refreshMain();
				}
			});
			final JButton saveButton = new JButton(Messages.getString("save")); //$NON-NLS-1$
			saveButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					saveParsedLogFile();
				}
			});
			dfb
					.append(
							Messages.getString("DirObjectListNode.filter"), queryField, searchButton, clearSearchButton, updateButton); //$NON-NLS-1$
			dfb.append(saveButton);
			dfb.nextLine();
			return dfb.getPanel();
		}

		private Node node;

		public JComponent getMainComponent() {
			final JTextArea ta = new JTextArea();
			ta.setEditable(false);
			for (final String str : parseList())
				ta.append(str + "\n");
			mainComponent = ta;// new JScrollPane(ta);
			mainComponent.setBackground(UIManager.getColor("TextField.background"));
			
			return mainComponent;
		}

		public void init(Node[] selection, TopComponent tc) {
			mainComponent = null;
			for (final Node node : selection)
				if (node instanceof RealmNode) {
					isClient = false;
					this.node = node;
					fileName = "/openthinclient/files/var/log/server.log";
					break;
				} else if (node instanceof DirObjectListNode) {
					isClient = false;
					
					this.node = node;
					fileName = "/openthinclient/files/var/log/syslog.log";
					break;
				} else if (node instanceof DirObjectNode) {
					macAdress = ((Client) (DirectoryObject) node.getLookup().lookup(
							DirectoryObject.class)).getMacAddress();
					isClient = true;
					this.node = node;
					fileName = "/openthinclient/files/var/log/syslog.log";
					break;
				}
			logFile = new ArrayList<String>(getLogFile());
		}

		@SuppressWarnings("unchecked")
		private List<String> getLogFile() {
			String homeServer = "";
			Realm realm = null;
			if (null == node) {
				if (null != System.getProperty("ThinClientManager.server.Codebase"))
					try {
						homeServer = new URL(System
								.getProperty("ThinClientManager.server.Codebase")).getHost();
					} catch (final MalformedURLException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				// else
				// throw new IllegalStateException(Messages
				// .getString("LogDetailView.getLogFile.NoRealm"));
			} else {
				realm = (Realm) node.getLookup().lookup(Realm.class);
				if (null != realm.getSchemaProviderName())
					homeServer = realm.getSchemaProviderName();
				else if (null != realm.getConnectionDescriptor().getHostname())
					homeServer = realm.getConnectionDescriptor().getHostname();

			}
			if (homeServer.length() == 0)
				homeServer = "localhost";
			try {
				final URL url = new URL("http", homeServer, 8080, fileName);
				final BufferedReader br = new BufferedReader(new InputStreamReader(url
						.openStream()));
				final ArrayList<String> lines = new ArrayList<String>();
				String line;
				if (isClient) {
					while ((line = br.readLine()) != null)
						if (line.contains(macAdress))
							lines.add(line);
					if (lines.size() == 0)
						lines.add(Messages.getString(
								"LogDetailView.getLogFile.NoEntrysForTC", macAdress));
				} else
					while ((line = br.readLine()) != null)
						lines.add(line);
				br.close();
				if (lines.size() == 0)
					lines.add(Messages.getString("LogDetailView.getLogFile.NoEntrys"));
				return lines;
			} catch (final MalformedURLException e) {
				e.printStackTrace();
				ErrorManager.getDefault().notify(e);
			} catch (final IOException e) {
				e.printStackTrace();
				ErrorManager.getDefault().notify(e);
			}
			return Collections.EMPTY_LIST;
		}

		private List<String> parseList() {
			if (bsearch) {
				final List<String> tmp = new ArrayList<String>();
				for (final String str : logFile)
					if (str.contains(searchValue))
						tmp.add(str);
				return tmp;
			} else
				return logFile;
		}

		public void saveParsedLogFile() {
			final JFileChooser chooser = new JFileChooser();

			chooser.setDialogTitle("LogFile");
			chooser.addChoosableFileFilter(new FileFilter() {
				@Override
				public String getDescription() {
					return "Log files";
				}

				@Override
				public boolean accept(File f) {
					return f.isDirectory() || f.getName().endsWith(".log");
				}
			});
			String path;
			try {
				final int returnVal = chooser.showSaveDialog(chooser.getParent());
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					path = chooser.getSelectedFile().getCanonicalPath();
					if (!path.endsWith(".log"))
						path = path + ".log";

					save(path);

				}
			} catch (final IOException e) {
				e.printStackTrace();
			}

		}

		public void save(String path) {
			try {
				List<String> tmp=new ArrayList<String>(parseList());
				StringBuffer sb =new StringBuffer();
				for (final String str : tmp) {
					sb.append(str);
				}
				final BufferedWriter writer = new BufferedWriter(new FileWriter(
						new File(path)));
				writer.write(sb.toString());
				writer.close();

			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}
}
