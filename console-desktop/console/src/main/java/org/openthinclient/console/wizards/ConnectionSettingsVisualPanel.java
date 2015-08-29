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
package org.openthinclient.console.wizards;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;

import org.openide.WizardDescriptor;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Property;
import org.openthinclient.common.model.Realm;
import org.openthinclient.console.Messages;
import org.openthinclient.console.util.UsernamePasswordCallbackHandler;
import org.openthinclient.console.wizards.initrealm.NewRealmInitWizardIterator;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.LDAPConnectionDescriptor.AuthenticationMethod;
import org.openthinclient.ldap.LDAPConnectionDescriptor.ConnectionMethod;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

@SuppressWarnings("serial")
public final class ConnectionSettingsVisualPanel extends JPanel {

	private javax.swing.JTextField baseDNField;

	private javax.swing.JTextField hostField;

	private javax.swing.JTextField schemaProviderName;

	private javax.swing.JComboBox connectMethodField;

	private javax.swing.JComboBox authMethodField;

	private javax.swing.JPasswordField passwordField;

	private javax.swing.JLabel passwordLabel;

	private javax.swing.JTextField portField;

	private javax.swing.JTextField userDNField;

	private javax.swing.JLabel userLabel;

	private javax.swing.JLabel dnLabel;

	private JLabel schemaProviderLabel;

	private javax.swing.JLabel connectMethodLabel;

	private javax.swing.JLabel authMethodLable;

	private JCheckBox savePasswordCheckbox;

	private LDAPConnectionDescriptor lcd;

	private Set<Property> propertyList;

	private boolean checkEnableForward = true;

	private URL url;

	/**
	 * Creates new form ConnectionSettingsVisualPanel
	 * 
	 * @param panel1
	 */
	// public ConnectionSettingsVisualPanel() {};
	public ConnectionSettingsVisualPanel(final ConnectionSettingsWizardPanel panel) {
		initComponents();

		if (null == panel)
			return;

		final DocumentListener dl = new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				fireChangeEvent(panel);
			}

			public void insertUpdate(DocumentEvent e) {
				fireChangeEvent(panel);
			}

			public void removeUpdate(DocumentEvent e) {
				fireChangeEvent(panel);
			}
		};

		hostField.getDocument().addDocumentListener(dl);
		baseDNField.getDocument().addDocumentListener(dl);
		schemaProviderName.getDocument().addDocumentListener(dl);
		userDNField.getDocument().addDocumentListener(dl);
		passwordField.getDocument().addDocumentListener(dl);
		portField.getDocument().addDocumentListener(dl);

		final ActionListener myActionForwarder = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				methodFieldItemStateChanged();
				fireChangeEvent(panel);
			}
		};
		connectMethodField.addActionListener(myActionForwarder);
		authMethodField.addActionListener(myActionForwarder);
	}

	/**
	 * @param panel1 TODO
	 * @return
	 * 
	 */
	LDAPConnectionDescriptor createLDAPConnectionDescriptor() {
		if (null == lcd) {
			lcd = new LDAPConnectionDescriptor();
			lcd.setAuthenticationMethod((AuthenticationMethod) authMethodField
					.getSelectedItem());
			lcd.setConnectionMethod((ConnectionMethod) connectMethodField
					.getSelectedItem());
			lcd.setBaseDN(baseDNField.getText());
			lcd.setHostname(hostField.getText());
			lcd.setPortNumber(Short.parseShort(portField.getText()));

			lcd.setCallbackHandler(new UsernamePasswordCallbackHandler(lcd
					.getLDAPUrl(), userDNField.getText(), passwordField.getPassword(),
					savePasswordCheckbox.isSelected()));
		}
		return lcd;
	}

	@Override
	public String getName() {
		return Messages.getString("ConnectionSettings.name"); //$NON-NLS-1$
	}

	/**
	 * @param wd
	 * @param name TODO
	 * @param defaultValue TODO
	 * @return
	 */
	private String getProperty(WizardDescriptor wd, String name,
			String defaultValue) {
		final Object value = wd.getProperty(name);
		return value != null ? value.toString() : defaultValue;
	}

	private void initComponents() {
		final DefaultFormBuilder dfb = new DefaultFormBuilder(new FormLayout(
				"r:p,3dlu,f:p:g,3dlu,p,3dlu,p"), Messages.getBundle(), this); //$NON-NLS-1$
		final int DEFAULT_COLSPAN = 5;

		
		
/*
		if (null != System.getProperty("ThinClientManager.server.Codebase"))
			try {
				url = new URL(System.getProperty("ThinClientManager.server.Codebase"));
			} catch (final MalformedURLException e1) {
				e1.printStackTrace();
			}
*/


//		try {
//
//			final BasicService basicService =
//				(BasicService)ServiceManager.
//				lookup("javax.jnlp.BasicService");
//
//			//fragt sich, ob basicService.getCodeBase() das selbe liefert wie System.getProperty("ThinClientManager.server.Codebase")       
//			url = basicService.getCodeBase();
//
//
//		} catch (UnavailableServiceException use) {
//		    use.printStackTrace();
//
//		    //vielleicht etwas zu rabiat
//		    //System.exit(-1);
//		}

		try {
			url = new URL("http://localhost");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}




		// connection line
		hostField = new javax.swing.JTextField();
		if (null != url)
			hostField.setText(url.getHost());
		dfb.appendI15d("ConnectionSettings.connection.host", hostField); //$NON-NLS-1$

		portField = new javax.swing.JTextField();
		portField.setText("10389"); //$NON-NLS-1$
		portField.setColumns(5);
		dfb.appendI15d("ConnectionSettings.connection.port", portField); //$NON-NLS-1$

		dfb.nextLine();

		connectMethodField = new javax.swing.JComboBox(new DefaultComboBoxModel(
				LDAPConnectionDescriptor.ConnectionMethod.values()));
		connectMethodField.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList list, Object value,
					int index, boolean isSelected, boolean cellHasFocus) {
				return super.getListCellRendererComponent(list,
						Messages.getString("ConnectionSettings.connection.method." //$NON-NLS-1$
								+ ((LDAPConnectionDescriptor.ConnectionMethod) value).name()),
						index, isSelected, cellHasFocus);
			}
		});
		connectMethodField.setEnabled(false);
		connectMethodLabel = dfb.appendI15d(
				"ConnectionSettings.connection.method", connectMethodField); //$NON-NLS-1$
		connectMethodLabel.setEnabled(false);

		dfb.nextLine();

		// base dn line
		dfb.appendUnrelatedComponentsGapRow();

		dfb.nextLine();

		dfb.appendI15dSeparator("ConnectionSettings.optional"); //$NON-NLS-1$

		dfb.nextLine();

		baseDNField = new javax.swing.JTextField();
		baseDNField.setText("ou=openthinclient,dc=openthinclient,dc=org");
		dnLabel = dfb.appendI15d(
				"ConnectionSettings.baseDN", baseDNField, DEFAULT_COLSPAN); //$NON-NLS-1$

		dfb.nextLine();

		// disable schemaprovider until possible use by full AD integration
		schemaProviderName = new javax.swing.JTextField();
		schemaProviderName.setEnabled(false);
		// if (null != url)
		// schemaProviderName.setText(url.getHost());
		schemaProviderLabel = dfb.appendI15d(
				"ConnectionSettings.schemaProviderName", schemaProviderName,
				DEFAULT_COLSPAN); //$NON-NLS-1$
		schemaProviderLabel.setEnabled(false);

		dfb.nextLine();

		// authentication settings
		dfb.appendUnrelatedComponentsGapRow();

		dfb.nextLine();

		dfb.appendI15dSeparator("ConnectionSettings.authentication"); //$NON-NLS-1$

		dfb.nextLine();

		authMethodField = new javax.swing.JComboBox(new DefaultComboBoxModel(
				LDAPConnectionDescriptor.AuthenticationMethod.values()));
		authMethodField.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList list, Object value,
					int index, boolean isSelected, boolean cellHasFocus) {
				return super.getListCellRendererComponent(
						list,
						Messages.getString("ConnectionSettings.authentication.method." //$NON-NLS-1$
								+ ((LDAPConnectionDescriptor.AuthenticationMethod) value)
										.name()), index, isSelected, cellHasFocus);
			}
		});
		authMethodField.setEnabled(false);
		authMethodLable = dfb.appendI15d(
				"ConnectionSettings.authentication.method", authMethodField, //$NON-NLS-1$
				DEFAULT_COLSPAN);
		authMethodLable.setEnabled(false);

		dfb.nextLine();

		userDNField = new javax.swing.JTextField();
		userLabel = dfb.appendI15d("ConnectionSettings.user", userDNField, //$NON-NLS-1$
				DEFAULT_COLSPAN);
		userDNField
				.setText("cn=administrator,ou=users,ou=openthinclient,dc=openthinclient,dc=org");

		dfb.nextLine();

		passwordField = new javax.swing.JPasswordField();
		passwordLabel = dfb.appendI15d("ConnectionSettings.password", //$NON-NLS-1$
				passwordField, DEFAULT_COLSPAN);

		dfb.nextLine();

		savePasswordCheckbox = new JCheckBox(
				Messages.getString("ConnectionSettings.savePassword")); //$NON-NLS-1$
		dfb.append(savePasswordCheckbox, DEFAULT_COLSPAN);

		dfb.nextLine();

	}

	/**
	 * @param wd
	 * @param panel1 TODO
	 * @return
	 */
	boolean valid(WizardDescriptor wd) {
		try {
			Integer.parseInt(portField.getText());
		} catch (final NumberFormatException e) {
			wd.putProperty("WizardPanel_errorMessage", Messages //$NON-NLS-1$
					.getString("ConnectionSettings.validation.port")); //$NON-NLS-1$
			return false;
		}

		if (!(baseDNField.getText().length() > 0 && hostField.getText().length() > 0)) {
			wd.putProperty("WizardPanel_errorMessage", Messages //$NON-NLS-1$
					.getString("ConnectionSettings.validation.baseDNOrHost")); //$NON-NLS-1$
			return false;
		}

		if (userDNField.isEnabled() && userDNField.getText().length() == 0) {
			wd.putProperty("WizardPanel_errorMessage", Messages //$NON-NLS-1$
					.getString("ConnectionSettings.validation.notAnonymus")); //$NON-NLS-1$
			return false;
		}

		wd.putProperty("WizardPanel_errorMessage", null); //$NON-NLS-1$
		return true;
	}

	private void methodFieldItemStateChanged() {
		authMethodField.setSelectedItem(AuthenticationMethod.SIMPLE);

		final boolean enabled = authMethodField.getSelectedItem() != null
				&& authMethodField.getSelectedItem() != LDAPConnectionDescriptor.AuthenticationMethod.NONE;
		// userDNField.setEnabled(enabled);
		// userLabel.setEnabled(enabled);
		passwordField.setEnabled(enabled);
		passwordLabel.setEnabled(enabled);
		savePasswordCheckbox.setEnabled(enabled);

		if (!enabled)
			// userDNField.setText(""); //$NON-NLS-1$
			passwordField.setText(""); //$NON-NLS-1$
	}

	/**
	 * @param wd
	 */
	void readSettings(WizardDescriptor wd) {
		String host = "";
		if (null != url)
			host = url.getHost();
		hostField.setText(getProperty(wd, "hostname", host)); //$NON-NLS-1$ //$NON-NLS-2$
		// disable schemaprovider until possible use by full AD integration
		// schemaProviderName.setText(getProperty(wd, "schemaProviderName", host));
		// //$NON-NLS-1$ //$NON-NLS-2$
		portField.setText(getProperty(wd, "port", "10389")); //$NON-NLS-1$ //$NON-NLS-2$
		// baseDNField.setText(getProperty(wd, "baseDN", "")); //$NON-NLS-1$
		// //$NON-NLS-2$

		connectMethodField.setSelectedItem(wd.getProperty("connectMethod") != null //$NON-NLS-1$
				? wd.getProperty("connectMethod") //$NON-NLS-1$
				: LDAPConnectionDescriptor.ConnectionMethod.PLAIN);
		authMethodField.setSelectedItem(wd.getProperty("authMethod") != null //$NON-NLS-1$
				? wd.getProperty("authMethod") //$NON-NLS-1$
				: LDAPConnectionDescriptor.AuthenticationMethod.NONE);
		// userDNField.setText(getProperty(wd, "userDN", "")); //$NON-NLS-1$
		// //$NON-NLS-2$
		savePasswordCheckbox.setSelected(wd.getProperty("savePassword") != null //$NON-NLS-1$
				&& ((Boolean) wd.getProperty("savePassword")).booleanValue()); //$NON-NLS-1$

		valid(wd);
	}

	private void enableForward(LDAPConnectionDescriptor lcd, WizardDescriptor wd) {
		final Object efo = wd.getProperty("enableForward"); //$NON-NLS-1$
		if (efo != null)
			checkEnableForward = ((Boolean) efo).booleanValue();

		if (checkEnableForward == true) {
			final int indexNextObj = NewRealmInitWizardIterator.getIndex();
			final String activePanel = NewRealmInitWizardIterator
					.current(indexNextObj).getClass().getSimpleName();

			if (activePanel.equals("SelectBasePanel")) {
				Set<Realm> realms = new HashSet<Realm>();
				try {
					realms = LDAPDirectory.listRealms(lcd);
				} catch (final DirectoryException e) {
					e.printStackTrace();
					final JLabel jlabel = new JLabel();
					JOptionPane.showMessageDialog(jlabel, e.toString(),
							Messages.getString("NewRealmInitAction.error.name"),
							JOptionPane.ERROR_MESSAGE);
				}
				if (!realms.isEmpty()) {
					String realmURLs = "";
					for (final Iterator iterator = realms.iterator(); iterator.hasNext();) {
						final Realm realm = (Realm) iterator.next();
						realmURLs = realmURLs
								+ realm.getConnectionDescriptor().getLDAPUrl() + "\n";
					}
					final JLabel jlabel = new JLabel();
					JOptionPane.showMessageDialog(jlabel,
							Messages.getString("NewRealmInitAction.error.text") + ": \n"
									+ realmURLs,
							Messages.getString("NewRealmInitAction.error.name"),
							JOptionPane.ERROR_MESSAGE);
					NewRealmInitWizardIterator.setIndex(0);
				}
			}
		}
	}

	/**
	 * @param wd
	 */
	void storeSettings(WizardDescriptor wd) {
		if (valid(wd)) {
			wd.putProperty("hostname", hostField.getText()); //$NON-NLS-1$
			wd.putProperty("port", portField.getText()); //$NON-NLS-1$
			wd.putProperty("baseDN", baseDNField.getText()); //$NON-NLS-1$
			wd.putProperty("connectMethod", connectMethodField.getSelectedItem()); //$NON-NLS-1$
			wd.putProperty("authMethod", authMethodField.getSelectedItem()); //$NON-NLS-1$
			wd.putProperty("userDN", userDNField.getText()); //$NON-NLS-1$
			wd.putProperty("schemaProviderName", schemaProviderName.getText()); //$NON-NLS-1$
			wd.putProperty("savePassword", savePasswordCheckbox.isSelected()); //$NON-NLS-1$
			wd.putProperty("connectionDescriptor", createLDAPConnectionDescriptor()); //$NON-NLS-1$

			enableForward(createLDAPConnectionDescriptor(), wd);
		}
	}

	/**
	 * @param panel
	 */
	private void fireChangeEvent(final ConnectionSettingsWizardPanel panel) {
		panel.fireChangeEvent();
		lcd = null;
	}
}
