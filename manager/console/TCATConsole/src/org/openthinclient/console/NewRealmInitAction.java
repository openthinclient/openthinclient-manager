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
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CallableSystemAction;
import org.openthinclient.common.directory.ACLUtils;
import org.openthinclient.common.directory.LDAPConnectionDescriptor;
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
import org.openthinclient.ldap.Mapping;
import org.openthinclient.ldap.TypeMapping;

/**
 * @author bohnerne
 */
@SuppressWarnings("serial")
public class NewRealmInitAction extends CallableSystemAction {

	public NewRealmInitAction() {
		super();
	}

	/**
	 * @param dir
	 * @throws DirectoryException
	 */
	@SuppressWarnings("unused")
	private void initHwtype(LDAPDirectory dir, Realm realm)
			throws DirectoryException {
		HardwareType type = new HardwareType();
		type.setName(Messages.getString("NewRealmInitAction.defaultHardware.name")); //$NON-NLS-1$
		type.setDescription(Messages
				.getString("NewRealmInitAction.defaultHardware.description")); //$NON-NLS-1$
		try {
			type.setSchema(realm.getSchemaProvider().getSchema(type.getClass(),
					"hardware type"));
		} catch (SchemaLoadingException e) {
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
		Location location = new Location();
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
	private static void initAdmin(LDAPDirectory dir, Realm realm, String name,
			String baseDN) throws DirectoryException {

		baseDN = "cn=" + name + "," + baseDN;
		User admin = new User();
		admin.setName(name);
		admin.setDescription(Messages
				.getString("NewRealmInitAction.adminUser.description")); //$NON-NLS-1$
		admin.setNewPassword(Messages
				.getString("NewRealmInitAction.adminUser.initialPassword")); //$NON-NLS-1$
		admin.setSn("Admin");
		admin.setGivenName("Joe");
		// admin.setDn(baseDN);
		// admin.setAdmin(true);

		UserGroup administrators = realm.getAdministrators();
		// administrators.setAdminGroup(true);

		dir.save(admin);
		administrators.getMembers().add(admin);
		realm.setAdministrators(administrators);
		TypeMapping.setIsNewAction(false);
		TypeMapping.setCurrentObject(realm);
		TypeMapping.setToMakeNew(admin);
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
			OrganizationalUnit ou = new OrganizationalUnit();
			ou.setName(newFolderName);
			ou.setDescription("openthinclient.org Console"); //$NON-NLS-1$
			directory.save(ou, "");
		} catch (DirectoryException e1) {
			ErrorManager.getDefault().notify(e1);
		}
	}

	public static Realm initRealm(LDAPDirectory dir, String description)
			throws DirectoryException {
		try {
			Realm realm = new Realm();
			realm.setDescription(description);
			final UserGroup admins = new UserGroup();
			admins.setName("administrators"); //$NON-NLS-1$
			// admins.setAdminGroup(true);
			realm.setAdministrators(admins);

			String date = new Date().toString();
			realm.setValue("invisibleObjects.initialized", date); //$NON-NLS-1$

			User roPrincipal = new User();
			roPrincipal.setName("roPrincipal");
			roPrincipal.setSn("Read Only User");
			roPrincipal.setNewPassword("secret");
			// roPrincipal.setAdmin(true);

			realm.setReadOnlyPrincipal(roPrincipal);
			realm.getProperties().setDescription("realm"); // ???

			dir.save(realm, "");

			return realm;
		} catch (Exception e) {
			throw new DirectoryException(Messages
					.getString("NewRealmInitAction.error.cantSetup"), e); //$NON-NLS-1$
		}
	}

	private static void initOUs(DirContext ctx, LDAPDirectory dir)
			throws DirectoryException {
		try {
			Mapping rootMapping = dir.getMapping();

			Collection<TypeMapping> typeMappers = rootMapping.getTypes().values();
			for (TypeMapping mapping : typeMappers) {
				OrganizationalUnit ou = new OrganizationalUnit();
				final String baseDN = mapping.getBaseDN();

				// we create only thos OUs for which we have a base DN
				if (null != baseDN) {
					ou.setName(baseDN.substring(baseDN.indexOf("=") + 1)); //$NON-NLS-1$

					dir.save(ou, ""); //$NON-NLS-1$
				}
			}

		} catch (Exception e) {
			throw new DirectoryException(Messages
					.getString("NewRealmInitAction.error.cantInitOuStructure"), e); //$NON-NLS-1$
		}
	}

	/*
	 * @see org.openide.util.actions.CallableSystemAction#asynchronous()
	 */
	@Override
	protected boolean asynchronous() {
		return true;
	}

	/*
	 * @see org.openide.util.actions.CallableSystemAction#performAction()
	 */
	@Override
	public void performAction() {
		NewRealmInitWizardIterator iterator = new NewRealmInitWizardIterator();
		WizardDescriptor wizardDescriptor = new WizardDescriptor(iterator);
		iterator.setWizardDescriptor(wizardDescriptor);

		wizardDescriptor.setTitleFormat(new MessageFormat("{0} ({1})")); //$NON-NLS-1$
		wizardDescriptor.setTitle(Messages.getString("action." //$NON-NLS-1$
				+ this.getClass().getSimpleName()));

		Dialog dialog = DialogDisplayer.getDefault().createDialog(wizardDescriptor);
		dialog.setSize(830, 600);
		dialog.setVisible(true);
		dialog.toFront();

		if (wizardDescriptor.getValue() == WizardDescriptor.FINISH_OPTION) {
			String baseDN = (String) wizardDescriptor
					.getProperty("oldSelectedBaseDN"); //$NON-NLS-1$    

			LDAPConnectionDescriptor lcd = (LDAPConnectionDescriptor) wizardDescriptor
					.getProperty("connectionDescriptor"); //$NON-NLS-1$

			if (baseDN.equals("")) {
				// do nothing
			} else {
				String newLcdBaseDN = baseDN + "," + lcd.getBaseDN();
				lcd.setBaseDN(newLcdBaseDN);
			}

			Object reg = wizardDescriptor.getProperty("registration");
			boolean registrate = ((Boolean) reg).booleanValue();
			String description = (String) wizardDescriptor.getProperty("description"); //$NON-NLS-1$
			String newFolderName = (String) wizardDescriptor
					.getProperty("newFolderName"); //$NON-NLS-1$
			Object newOU = wizardDescriptor.getProperty("newFolderBox"); //$NON-NLS-1$
			boolean createNewOU = ((Boolean) newOU).booleanValue();

			if (createNewOU == true) {
				try {
					createOU(newFolderName, LDAPDirectory.openEnv(lcd));
				} catch (DirectoryException e1) {
					e1.printStackTrace();
				}
			}
			try {
				if (newFolderName != null)
					lcd.setBaseDN("ou=" + newFolderName + "," + lcd.getBaseDN());

				LdapContext ctx = lcd.createInitialContext();
				LDAPDirectory dir = LDAPDirectory.openEnv(lcd);
				Realm realm = initRealm(dir, description);
				realm.setConnectionDescriptor(lcd);

				// Serversettings
				realm.setValue("Serversettings.Hostname", realm
						.getConnectionDescriptor().getHostname());
				Short s = new Short(realm.getConnectionDescriptor().getPortNumber());
				realm.setValue("Serversettings.Portnumber", s.toString());
				String schemaProviderName = wizardDescriptor.getProperty(
						"schemaProviderName").toString();

				if (schemaProviderName.equals("")) {
					schemaProviderName = realm.getConnectionDescriptor().getHostname();
				}
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
					HTTPLdifImportAction action = new HTTPLdifImportAction(lcd
							.getHostname());
					action.importOneFromURL("locations", realm);
				}

				if (isBooleanOptionSet(wizardDescriptor, "initHwtypeAndDevices")) { //$NON-NLS-1$
					// initHwtype(dir, realm);
					HTTPLdifImportAction action = new HTTPLdifImportAction(lcd
							.getHostname());
					action.importOneFromURL("hwtypes", realm);
					action.importOneFromURL("devices", realm);
				}

				if (isBooleanOptionSet(wizardDescriptor, "createADSACIs")) { //$NON-NLS-1$
					ACLUtils aclUtils = new ACLUtils(ctx);
					aclUtils.makeACSA(""); //$NON-NLS-1$

					if (isBooleanOptionSet(wizardDescriptor, "enableSearchForAll")) //$NON-NLS-1$
						aclUtils.enableSearchForAllUsers(""); //$NON-NLS-1$

					if (isBooleanOptionSet(wizardDescriptor, "enableAdminAccess")) //$NON-NLS-1$
						aclUtils.enableAdminUsers(""); //$NON-NLS-1$
				}

				if (registrate == true) {
					try {
						AddRealmAction add = new AddRealmAction();
						AddRealmAction.setAutomaticRegistration(true);
						AddRealmAction.setRealm(realm);
						add.actionPerformed(null);
					} catch (DataObjectNotFoundException e) {
						e.printStackTrace();
					}
				} else {
					AddRealmAction.setAutomaticRegistration(false);
				}

			} catch (Exception f) {
				final String msg = Messages.getString(
						"NewRealmInit.error.setup_realm_failed_error", f); //$NON-NLS-1$
				ErrorManager.getDefault().annotate(f, msg);
				ErrorManager.getDefault().notify(f);
			}
		}
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getName()
	 */
	@Override
	public String getName() {
		// System.out.println(Messages.getString("action." //$NON-NLS-1$
		// + NewRealmInitAction.class.getSimpleName()));
		return Messages.getString("action." //$NON-NLS-1$
				+ NewRealmInitAction.class.getSimpleName());
	}

	/*
	 * @see org.openide.util.actions.SystemAction#getHelpCtx()
	 */
	@Override
	public HelpCtx getHelpCtx() {
		return null;
	}

	@Override
	protected String iconResource() {
		return "org/openthinclient/console/otc-new.png";
	}

}