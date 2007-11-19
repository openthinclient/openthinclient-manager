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

import java.awt.Dialog;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;

import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;

import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.WizardDescriptor;
import org.openthinclient.common.directory.ACLUtils;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.OrganizationalUnit;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.User;
import org.openthinclient.common.model.UserGroup;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;
import org.openthinclient.console.wizards.initrealm.NewRealmInitWizardIterator;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.Mapping;
import org.openthinclient.ldap.TypeMapping;

import com.levigo.util.swing.action.AbstractCommand;

/**
 * @author bohnerne
 */
@SuppressWarnings("serial")
public class NewRealmInitCommand extends AbstractCommand {
	/**
	 * @param dir
	 * @throws DirectoryException
	 */
	@SuppressWarnings("unused")
	private void initHwtype(LDAPDirectory dir, Realm realm)
			throws DirectoryException {
		final HardwareType type = new HardwareType();
		type.setName(Messages.getString("NewRealmInitAction.defaultHardware.name")); //$NON-NLS-1$
		type.setDescription(Messages
				.getString("NewRealmInitAction.defaultHardware.description")); //$NON-NLS-1$
		try {
			type.setSchema(realm.getSchemaProvider().getSchema(type.getClass(),
					"hardware type"));
		} catch (final SchemaLoadingException e) {
			e.printStackTrace();
		}
		dir.save(type);
	}

	/**
	 * @param dir
	 * @throws DirectoryException
	 */
	@SuppressWarnings("unused")
	private void initLocation(LDAPDirectory dir) throws DirectoryException {
		final Location location = new Location();
		location.setName(Messages
				.getString("NewRealmInitAction.defaultLocation.name")); //$NON-NLS-1$
		location.setDescription(Messages
				.getString("NewRealmInitAction.defaultLocation.description")); //$NON-NLS-1$
		dir.save(location);
	}

	/**
	 * @param dir
	 * @param property
	 * @throws DirectoryException
	 */
	public static void initAdmin(LDAPDirectory dir, Realm realm, String name,
			String baseDN) throws DirectoryException {

		baseDN = "cn=" + name + "," + baseDN;
		final User admin = new User();
		admin.setName(name);
		admin.setDescription(Messages
				.getString("NewRealmInitAction.adminUser.description")); //$NON-NLS-1$
		admin.setNewPassword(Messages
				.getString("NewRealmInitAction.adminUser.initialPassword")); //$NON-NLS-1$
		admin.setSn("Admin");
		admin.setGivenName("Joe");
		// admin.setDn(baseDN);
		// admin.setAdmin(true);

		final UserGroup administrators = realm.getAdministrators();
		// administrators.setAdminGroup(true);

		dir.save(admin);
		administrators.getMembers().add(admin);
		realm.setAdministrators(administrators);

		dir.save(administrators);
		dir.save(realm);
	}

	/**
	 * @param wizardDescriptor
	 * @return
	 */
	private boolean isBooleanOptionSet(WizardDescriptor wizardDescriptor,
			String name) {
		final Object property = wizardDescriptor.getProperty(name);
		return property != null && ((Boolean) property).booleanValue();
	}

	public void createOU(String newFolderName, LDAPDirectory directory) {
		try {
			final OrganizationalUnit ou = new OrganizationalUnit();
			ou.setName(newFolderName);
			ou.setDescription("openthinclient.org Console"); //$NON-NLS-1$
			directory.save(ou, "");
		} catch (final DirectoryException e1) {
			ErrorManager.getDefault().notify(e1);
		}
	}

	public static Realm initRealm(LDAPDirectory dir, String description)
			throws DirectoryException {
		try {
			final Realm realm = new Realm();
			realm.setDescription(description);
			final UserGroup admins = new UserGroup();
			admins.setName("administrators"); //$NON-NLS-1$
			// admins.setAdminGroup(true);
			realm.setAdministrators(admins);

			final String date = new Date().toString();
			realm.setValue("invisibleObjects.initialized", date); //$NON-NLS-1$

			final User roPrincipal = new User();
			roPrincipal.setName("roPrincipal");
			roPrincipal.setSn("Read Only User");
			roPrincipal.setNewPassword("secret");
			// roPrincipal.setAdmin(true);

			realm.setReadOnlyPrincipal(roPrincipal);
			// realm.getProperties().setDescription("realm"); // ???

			realm.setDescription("realm");

			dir.save(realm, "");

			return realm;
		} catch (final Exception e) {
			throw new DirectoryException(Messages
					.getString("NewRealmInitAction.error.cantSetup"), e); //$NON-NLS-1$
		}
	}

	public static void initOUs(DirContext ctx, LDAPDirectory dir)
			throws DirectoryException {
		try {
			final Mapping rootMapping = dir.getMapping();

			final Collection<TypeMapping> typeMappers = rootMapping.getTypes()
					.values();
			for (final TypeMapping mapping : typeMappers) {
				final OrganizationalUnit ou = new OrganizationalUnit();
				final String baseDN = mapping.getBaseRDN();

				// we create only those OUs for which we have a base DN
				if (null != baseDN) {
					ou.setName(baseDN.substring(baseDN.indexOf("=") + 1)); //$NON-NLS-1$

					dir.save(ou, ""); //$NON-NLS-1$
				}
			}

		} catch (final Exception e) {
			throw new DirectoryException(Messages
					.getString("NewRealmInitAction.error.cantInitOuStructure"), e); //$NON-NLS-1$
		}
	}

	@Override
	protected void doExecute(Collection args) {
		final NewRealmInitWizardIterator iterator = new NewRealmInitWizardIterator();
		final WizardDescriptor wizardDescriptor = new WizardDescriptor(iterator);
		iterator.setWizardDescriptor(wizardDescriptor);

		wizardDescriptor.setTitleFormat(new MessageFormat("{0} ({1})")); //$NON-NLS-1$
		wizardDescriptor.setTitle(Messages.getString("action." //$NON-NLS-1$
				+ this.getClass().getSimpleName()));

		final Dialog dialog = DialogDisplayer.getDefault().createDialog(
				wizardDescriptor);
		dialog.setSize(830, 600);
		dialog.setVisible(true);
		dialog.toFront();

		if (wizardDescriptor.getValue() == WizardDescriptor.FINISH_OPTION) {
			final String baseDN = (String) wizardDescriptor
					.getProperty("oldSelectedBaseDN"); //$NON-NLS-1$    

			final LDAPConnectionDescriptor lcd = (LDAPConnectionDescriptor) wizardDescriptor
					.getProperty("connectionDescriptor"); //$NON-NLS-1$

			if (baseDN.equals("")) {
				// do nothing
			} else {
				final String newLcdBaseDN = baseDN + "," + lcd.getBaseDN();
				lcd.setBaseDN(newLcdBaseDN);
			}

			final Object reg = wizardDescriptor.getProperty("registration");
			final boolean register = ((Boolean) reg).booleanValue();
			final String description = (String) wizardDescriptor
					.getProperty("description"); //$NON-NLS-1$
			final String newFolderName = (String) wizardDescriptor
					.getProperty("newFolderName"); //$NON-NLS-1$
			final Object newOU = wizardDescriptor.getProperty("newFolderBox"); //$NON-NLS-1$
			final boolean createNewOU = ((Boolean) newOU).booleanValue();

			if (createNewOU == true)
				try {
					createOU(newFolderName, LDAPDirectory.openEnv(lcd));
				} catch (final DirectoryException e1) {
					e1.printStackTrace();
				}
			try {
				if (newFolderName != null)
					lcd.setBaseDN("ou=" + newFolderName + "," + lcd.getBaseDN());

				final LdapContext ctx = lcd.createDirContext();
				LDAPDirectory dir = LDAPDirectory.openEnv(lcd);
				final Realm realm = initRealm(dir, description);
				realm.setConnectionDescriptor(lcd);

				// Serversettings
				realm.setValue("Serversettings.Hostname", realm
						.getConnectionDescriptor().getHostname());
				final Short s = new Short(realm.getConnectionDescriptor()
						.getPortNumber());
				realm.setValue("Serversettings.Portnumber", s.toString());
				String schemaProviderName = wizardDescriptor.getProperty(
						"schemaProviderName").toString();

				if (schemaProviderName.equals(""))
					schemaProviderName = realm.getConnectionDescriptor().getHostname();
				realm.setValue("Serversettings.SchemaProviderName", schemaProviderName);

				dir = LDAPDirectory.openRealm(realm);
				if (isBooleanOptionSet(wizardDescriptor, "initOUs")) { //$NON-NLS-1$
					initOUs(ctx, dir);

					if (isBooleanOptionSet(wizardDescriptor, "initAdmin")) //$NON-NLS-1$
						initAdmin(dir, realm, (String) wizardDescriptor
								.getProperty("adminName"), wizardDescriptor.getProperty(
								"adminBaseDN").toString()); //$NON-NLS-1$
				}

				HTTPLdifImportAction.setEnableAsk(false);
				if (isBooleanOptionSet(wizardDescriptor, "initLocation")) { //$NON-NLS-1$
					// initLocation(dir);
					final HTTPLdifImportAction action = new HTTPLdifImportAction(lcd
							.getHostname());
					action.importOneFromURL("locations", realm);
				}

				if (isBooleanOptionSet(wizardDescriptor, "initHwtypeAndDevices")) { //$NON-NLS-1$
					// initHwtype(dir, realm);
					final HTTPLdifImportAction action = new HTTPLdifImportAction(lcd
							.getHostname());
					action.importOneFromURL("hwtypes", realm);
					action.importOneFromURL("devices", realm);
				}

				if (isBooleanOptionSet(wizardDescriptor, "createADSACIs")) { //$NON-NLS-1$
					final ACLUtils aclUtils = new ACLUtils(ctx);
					aclUtils.makeACSA(""); //$NON-NLS-1$

					if (isBooleanOptionSet(wizardDescriptor, "enableSearchForAll")) //$NON-NLS-1$
						aclUtils.enableSearchForAllUsers(""); //$NON-NLS-1$

					if (isBooleanOptionSet(wizardDescriptor, "enableAdminAccess")) //$NON-NLS-1$
						aclUtils.enableAdminUsers(""); //$NON-NLS-1$
				}

				if (register == true)
					RealmManager.registerRealm(realm);
			} catch (final Exception f) {
				final String msg = Messages.getString(
						"NewRealmInit.error.setup_realm_failed_error", f); //$NON-NLS-1$
				ErrorManager.getDefault().annotate(f, msg);
				ErrorManager.getDefault().notify(f);
			}
		}
	}

	@Override
	public boolean checkDeeply(Collection args) {
		return true;
	}
}