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

// import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

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

public class LogDetailView extends AbstractDetailView {
	private static LogDetailView detailView;
	private boolean isClient;
	private static String fileName;
	private String macAdress;
	private JTextField queryField;
	private static JComponent mainComponent;

	public static LogDetailView getInstance() {
		if (detailView == null)
			detailView = new LogDetailView();
		return detailView;

	}

	@Override
	public JComponent getHeaderComponent() {
		getFooterComponent();

		final DefaultFormBuilder dfb = new DefaultFormBuilder(new FormLayout(
				"p,100dlu,f:p:g", "f:p")); //$NON-NLS-1$
		queryField = new JTextField();
		final JButton searchButton = new JButton(Messages.getString("Search")); //$NON-NLS-1$
		searchButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				macAdress = queryField.getText();
				isClient = true;
				mainComponent = getFooterComponent();
				mainComponent.repaint();
			}
		});
		dfb
				.append(
						new JLabel(Messages.getString("DirObjectListNode.filter")), queryField, searchButton); //$NON-NLS-1$
		return dfb.getPanel();
	}

	private Node node;

	public JComponent getMainComponent() {
		final JPanel jpl = new JPanel();
		final CellConstraints cc = new CellConstraints();
		jpl.setLayout(new FormLayout("f:p:g", "p,f:p:g"));
		jpl.add(detailView.getHeaderComponent(), cc.xy(1, 1));
		jpl.add(mainComponent, cc.xy(1, 2));
		return jpl;
	}

	@Override
	public JComponent getFooterComponent() {
		final JTextArea ta = new JTextArea();
		ta.setEditable(false);
		for (final String str : getLogFile())
			ta.append(str + "\n");
		mainComponent = new JScrollPane(ta);
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
	}

	@SuppressWarnings("unchecked")
	private List<String> getLogFile() {
		String homeServer = "";
		if (null == node) {
			if (null != System.getProperty("ThinClientManager.server.Codebase"))
				try {
					homeServer = new URL(System
							.getProperty("ThinClientManager.server.Codebase")).getHost();
				} catch (final MalformedURLException e1) {
					e1.printStackTrace();
				}
			else
				throw new IllegalStateException(Messages
						.getString("LogDetailView.getLogFile.NoRealm"));
		} else {
			final Realm realm = (Realm) node.getLookup().lookup(Realm.class);
			if (null == realm)
				throw new IllegalStateException(Messages
						.getString("LogDetailView.getLogFile.NoRealm"));
			if (null != realm.getSchemaProviderName())
				homeServer = realm.getSchemaProviderName();
			else if (null != realm.getConnectionDescriptor().getHostname())
				homeServer = realm.getConnectionDescriptor().getHostname();
			if (homeServer.length() == 0)
				homeServer = "localhost";
		}
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

}
