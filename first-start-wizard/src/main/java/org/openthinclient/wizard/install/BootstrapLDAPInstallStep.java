package org.openthinclient.wizard.install;

import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.OrganizationalUnit;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.User;
import org.openthinclient.common.model.UserGroup;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.Mapping;
import org.openthinclient.ldap.TypeMapping;
import org.openthinclient.ldap.auth.UsernamePasswordHandler;
import org.openthinclient.service.apacheds.DirectoryService;
import org.openthinclient.service.apacheds.DirectoryServiceConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.openthinclient.wizard.model.DirectoryModel;

import javax.naming.ldap.LdapName;
import java.util.Collection;
import java.util.Date;

public class BootstrapLDAPInstallStep extends AbstractInstallStep {

  private final DirectoryModel directoryModel;

  public BootstrapLDAPInstallStep(DirectoryModel directoryModel) {
    this.directoryModel = directoryModel;
  }

  public static void main(String[] args) throws Exception {
//    final InstallContext installContext = new InstallContext();
//    installContext.setManagerHome(new ManagerHomeFactory().create());
    new BootstrapLDAPInstallStep(new DirectoryModel()).bootstrapDirectory(new ManagerHomeFactory().create().getConfiguration(DirectoryServiceConfiguration.class));
  }

  public static void setupDefaultOUs(LDAPDirectory dir, OrganizationalUnit primaryOU)
          throws DirectoryException {
    final Mapping rootMapping = dir.getMapping();

    final Collection<TypeMapping> typeMappers = rootMapping.getTypes()
            .values();
    for (final TypeMapping mapping : typeMappers) {
      final OrganizationalUnit ou = new OrganizationalUnit();
      final String baseDN = mapping.getBaseRDN();

      // we create only those OUs for which we have a base DN
      if (null != baseDN) {
        ou.setName(baseDN.substring(baseDN.indexOf("=") + 1)); //$NON-NLS-1$

        dir.save(ou, primaryOU.getDn()); //$NON-NLS-1$
      }
    }

  }

  @Override
  protected void doExecute(InstallContext installContext) throws Exception {

    final ManagerHome managerHome = installContext.getManagerHome();
    final DirectoryServiceConfiguration directoryServiceConfiguration = managerHome.getConfiguration(DirectoryServiceConfiguration.class);

    // are we ok with the defaults? I.e. host (localhost), primaryOU (ou=openthinclient)
    directoryServiceConfiguration.setPrimaryOU(directoryModel.getPrimaryOU().getDn());

    log.info("Saving the default ldap configuration to the manager home");
    managerHome.save(DirectoryServiceConfiguration.class);


    log.info("Starting the embedded LDAP server and bootstrapping the configuration");
    final DirectoryService directoryService = new DirectoryService();
    directoryService.setConfiguration(directoryServiceConfiguration);
    directoryService.startService();

    bootstrapDirectory(directoryServiceConfiguration);

    directoryService.flushEmbeddedServerData();

    log.info("Stopping the embedded LDAP server.");
    directoryService.stopService();
  }

  private void bootstrapDirectory(DirectoryServiceConfiguration directoryServiceConfiguration) throws Exception {
    final LDAPConnectionDescriptor lcd = createLdapConnectionDescriptor(directoryServiceConfiguration);
    final LDAPDirectory ldapDirectory = LDAPDirectory.openEnv(lcd);

    final OrganizationalUnit primaryOU = setupRootOU(ldapDirectory, directoryServiceConfiguration);

    final Realm realm = setupRealm(ldapDirectory, primaryOU);

    setupDefaultOUs(ldapDirectory, primaryOU);

    setupAdminUser(ldapDirectory, primaryOU, realm);
  }

  private Realm setupRealm(LDAPDirectory ldapDirectory, OrganizationalUnit primaryOU) throws Exception {

    final Realm realm = new Realm();
    realm.setDescription(primaryOU.getDescription());
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

    ldapDirectory.save(realm, primaryOU.getDn());

    return realm;
  }

  private void setupAdminUser(LDAPDirectory ldapDirectory, OrganizationalUnit primaryOU, Realm realm) throws Exception {

    final User admin = directoryModel.getAdministratorUser();

    ldapDirectory.save(admin, new LdapName(primaryOU.getDn()).add("ou=users").toString());

    final UserGroup administrators = realm.getAdministrators();

    administrators.getMembers().add(admin);

    ldapDirectory.save(administrators);
    ldapDirectory.save(realm);
  }

  private OrganizationalUnit setupRootOU(LDAPDirectory ldapDirectory, DirectoryServiceConfiguration directoryServiceConfiguration) throws DirectoryException {
    final OrganizationalUnit primaryOU = directoryModel.getPrimaryOU();
    ldapDirectory.save(primaryOU, directoryServiceConfiguration.getEmbeddedCustomRootPartitionName());
    return primaryOU;
  }

  private LDAPConnectionDescriptor createLdapConnectionDescriptor(DirectoryServiceConfiguration directoryServiceConfiguration) {
    final LDAPConnectionDescriptor lcd = new LDAPConnectionDescriptor();
    lcd.setConnectionMethod(LDAPConnectionDescriptor.ConnectionMethod.PLAIN);
    lcd.setProviderType(LDAPConnectionDescriptor.ProviderType.SUN);
    lcd.setHostname("localhost");
    lcd.setAuthenticationMethod(LDAPConnectionDescriptor.AuthenticationMethod.SIMPLE);
    lcd.setPortNumber((short) directoryServiceConfiguration.getEmbeddedLdapPort());
    lcd.setCallbackHandler(new UsernamePasswordHandler(directoryServiceConfiguration.getContextSecurityPrincipal(), directoryServiceConfiguration.getContextSecurityCredentials()));
    return lcd;
  }

  @Override
  public String getName() {
    return "Initial LDAP configuration";
  }
}
