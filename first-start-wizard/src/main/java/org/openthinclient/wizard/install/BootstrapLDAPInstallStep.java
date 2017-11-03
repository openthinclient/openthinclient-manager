package org.openthinclient.wizard.install;

import org.openthinclient.api.context.InstallContext;
import org.openthinclient.api.distributions.ImportItem;
import org.openthinclient.api.distributions.ImportableProfileProvider;
import org.openthinclient.api.distributions.InstallableDistribution;
import org.openthinclient.api.importer.config.ImporterConfiguration;
import org.openthinclient.api.importer.impl.RestModelImporter;
import org.openthinclient.api.rest.model.AbstractProfileObject;
import org.openthinclient.common.config.LDAPServicesConfiguration;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.OrganizationalUnit;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.User;
import org.openthinclient.common.model.UserGroup;
import org.openthinclient.common.model.schema.provider.AbstractSchemaProvider;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.schema.provider.ServerLocalSchemaProvider;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.Mapping;
import org.openthinclient.ldap.TypeMapping;
import org.openthinclient.ldap.auth.UsernamePasswordHandler;
import org.openthinclient.progress.LoggingProgressReceiver;
import org.openthinclient.progress.NoopProgressReceiver;
import org.openthinclient.service.apacheds.DirectoryService;
import org.openthinclient.service.apacheds.DirectoryServiceConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.wizard.model.DirectoryModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.File;
import java.util.Collection;
import java.util.Date;

import javax.naming.ldap.LdapName;

import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALL_BOOTSTRAPLDAPINSTALLSTEP_LABEL;

public class BootstrapLDAPInstallStep extends AbstractInstallStep {

  @Configuration
  @Import({LDAPServicesConfiguration.class, ImporterConfiguration.class})
  public static class BootstrapConfiguration {

    // FIXME these contents are essentially the same as in org.openthinclient.manager.standalone.config.DirectoryServicesConfiguration.
    // due to the current project layout, duplicating this is the only viable option at this point in time
    @Autowired
    ManagerHome managerHome;

    @Bean
    public LDAPConnectionDescriptor ldapConnectionDescriptor() {
      LDAPConnectionDescriptor lcd = new LDAPConnectionDescriptor();
      lcd.setProviderType(LDAPConnectionDescriptor.ProviderType.SUN);
      lcd.setAuthenticationMethod(LDAPConnectionDescriptor.AuthenticationMethod.SIMPLE);

      final DirectoryServiceConfiguration configuration = managerHome.getConfiguration(DirectoryServiceConfiguration.class);

      lcd.setCallbackHandler(new UsernamePasswordHandler(configuration.getContextSecurityPrincipal(),
              configuration.getContextSecurityCredentials().toCharArray()));

      return lcd;
    }

    @Bean
    public SchemaProvider schemaProvider() {
      final File homeDirectory = managerHome.getLocation();

      return new ServerLocalSchemaProvider(
              homeDirectory.toPath().resolve("nfs").resolve("root").resolve(AbstractSchemaProvider.SCHEMA_PATH)
      );

    }

  }

  private final DirectoryModel directoryModel;
  private final InstallableDistribution distribution;
  private final ImportableProfileProvider profileProvider;

  public BootstrapLDAPInstallStep(DirectoryModel directoryModel, InstallableDistribution distribution, ImportableProfileProvider profileProvider) {
    this.directoryModel = directoryModel;
    this.distribution = distribution;
    this.profileProvider = profileProvider;
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
    directoryServiceConfiguration.setPrimaryOU(directoryModel.getPrimaryOU().getName());
    
    log.info("Saving the default ldap configuration to the manager home");
    managerHome.save(DirectoryServiceConfiguration.class);


    log.info("Starting the embedded LDAP server and bootstrapping the configuration");
    final DirectoryService directoryService = new DirectoryService();
    directoryService.setConfiguration(directoryServiceConfiguration);
    directoryService.startService();

    bootstrapDirectory(directoryServiceConfiguration);

    directoryService.flushEmbeddedServerData();

    log.info("Loading and configuring base profiles");

    try(final AnnotationConfigApplicationContext importAppContext = new AnnotationConfigApplicationContext()) {
      importAppContext.getBeanFactory().registerSingleton("managerHome", installContext.getManagerHome());
      importAppContext.register(BootstrapConfiguration.class);
      importAppContext.refresh();

      final RestModelImporter importer = importAppContext.getBean(RestModelImporter.class);

      for (ImportItem importItem : distribution.getImportItems()) {
        log.info("Loading profile from " + importItem.getPath());

        // TODO add ProgressReceiver
        final AbstractProfileObject profileObject = profileProvider.access(installContext, importItem, new LoggingProgressReceiver());

        importer.importProfileObject(profileObject);

      }
    }
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
    return mc.getMessage(UI_FIRSTSTART_INSTALL_BOOTSTRAPLDAPINSTALLSTEP_LABEL);
  }

  @Override
  public double getProgress() {
    return 1;
  }
}
