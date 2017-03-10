package org.openthinclient.api.importer;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.api.importer.config.ImporterConfiguration;
import org.openthinclient.api.importer.impl.ClasspathSchemaProvider;
import org.openthinclient.api.importer.impl.ImportModelMapper;
import org.openthinclient.api.importer.impl.RestModelImporter;
import org.openthinclient.api.importer.model.ImportableHardwareType;
import org.openthinclient.common.config.LDAPServicesConfiguration;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.HardwareTypeService;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.Mapping;
import org.openthinclient.ldap.TypeMapping;
import org.openthinclient.ldap.auth.UsernamePasswordHandler;
import org.openthinclient.service.apacheds.DirectoryService;
import org.openthinclient.service.apacheds.DirectoryServiceConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Import test:
 * 1. Nimm default Schemas die vorhanden sind (oder andere aus konfiguriertem Verzeichnis)
 * 2. stecke Schemas in LDAP
 * 3. Importiere Profile
 * 4. Test: Syntax mit Profil/Schema, Semantik, Abhängikeiten
 */
@RunWith(SpringRunner.class)
@Import({ImporterConfiguration.class, org.openthinclient.api.importer.impl.RestModelImporterTest.ClasspathSchemaProviderConfiguration.class,
        LDAPServicesConfiguration.class})
public class SchemaProfileTest {

    private static final short ldapPort = getRandomNumber();

    @Configuration
    public static class ClasspathSchemaProviderConfiguration {

        @Bean
        public SchemaProvider schemaProvider() {
            return new ClasspathSchemaProvider();
        }

        @Bean
        public LDAPConnectionDescriptor ldapConnectionDescriptor() {
            return createLdapConnectionDescriptor(getDirectoryServiceConfiguration());
        }
    }

    @Autowired
    HardwareTypeService hardwareTypeService;
    @Autowired
    ClasspathSchemaProvider schemaProvider;
    @Autowired
    ImportModelMapper mapper;
    @Autowired
    public LDAPConnectionDescriptor connectionDescriptor;

    private RestModelImporter importer;

    protected static String baseDN = "dc=test,dc=test";
    protected static String envDN = "ou=test," + baseDN;
    private static DirectoryService ds;

    @BeforeClass
    public static void setUpClass() throws Exception {
        ds = new DirectoryService();
        ds.setConfiguration(getDirectoryServiceConfiguration());
        ds.startService();
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        if (null != ds) {
            ds.stopService();
        }
        deleteRecursively(new File("unit-test-tmp"));
    }

    @Before
    public void setUp() throws Exception {
        importer = new RestModelImporter(mapper, hardwareTypeService, null, null, null, null, null);

        // bootstrap directory (taken from BootstrapLDAPInstallStep.java)
        final LDAPDirectory ldapDirectory = LDAPDirectory.openEnv(connectionDescriptor);

        final OrganizationalUnit primaryOU = setupRootOU(ldapDirectory, getDirectoryServiceConfiguration());

        // load from schema: not working because imported Schema is not applicable to LDAP-objects for import
        // Schema realm = schemaProvider.getSchema(Realm.class, "realm");
        // ldapDirectory.save(realm, primaryOU.getDn());

        // setup default realm and ou's
        setupRealm(ldapDirectory, primaryOU);
        setupDefaultOUs(ldapDirectory, primaryOU);

    }

    @Test
    public void testImportSimpleHardwareType() throws Exception {

        final ImportableHardwareType hw = new ImportableHardwareType();
        String name = "Simple Hardware Type";
        hw.setName(name);

        final HardwareType hardwareType = importer.importHardwareType(hw);

        assertNotNull(hardwareType);

        Set<HardwareType> all = hardwareTypeService.findAll();
        assertNotNull(all);
        assertEquals("cn=" + name + ",ou=hwtypes," + envDN, all.stream().findFirst().get().getDn());

//        final LDAPDirectory ldapDirectory = LDAPDirectory.openEnv(connectionDescriptor);
//        Set<HardwareType> list = ldapDirectory.list(HardwareType.class);
//        assertNotNull(list);
    }

    private static DirectoryServiceConfiguration getDirectoryServiceConfiguration() {
        final DirectoryServiceConfiguration configuration = new DirectoryServiceConfiguration();

        configuration.setAccessControlEnabled(false);
        configuration.setEmbeddedAnonymousAccess(true);
        configuration.setEmbeddedServerEnabled(true);
        configuration.setContextProviderURL("uid=admin,ou=system");
        configuration.setContextSecurityAuthentication("simple");
        configuration.setContextSecurityCredentials("secret");
        configuration.setContextSecurityPrincipal("uid=admin,ou=system");

        configuration.setEmbeddedCustomRootPartitionName("dc=test,dc=test");
        configuration.setEmbeddedWkDir(new File("unit-test-tmp"));

        configuration.setEnableNtp(false);
        configuration.setEnableKerberos(false);
        configuration.setEnableChangePassword(false);

        configuration.setEmbeddedLdapPort(ldapPort);
        return configuration;
    }

    private OrganizationalUnit setupRootOU(LDAPDirectory ldapDirectory, DirectoryServiceConfiguration directoryServiceConfiguration) throws DirectoryException {
        final OrganizationalUnit primaryOU = new OrganizationalUnit();
        primaryOU.setName("test"); // envDN = "ou=test," + baseDN;
        ldapDirectory.save(primaryOU, directoryServiceConfiguration.getEmbeddedCustomRootPartitionName());
        return primaryOU;
    }

    private static void setupDefaultOUs(LDAPDirectory dir, OrganizationalUnit primaryOU) throws DirectoryException {
        final Mapping rootMapping = dir.getMapping();

        final Collection<TypeMapping> typeMappers = rootMapping.getTypes().values();
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

    private static LDAPConnectionDescriptor createLdapConnectionDescriptor(DirectoryServiceConfiguration directoryServiceConfiguration) {
        final LDAPConnectionDescriptor lcd = new LDAPConnectionDescriptor();
        lcd.setConnectionMethod(LDAPConnectionDescriptor.ConnectionMethod.PLAIN);
        lcd.setProviderType(LDAPConnectionDescriptor.ProviderType.SUN);
        lcd.setHostname("localhost");
        lcd.setAuthenticationMethod(LDAPConnectionDescriptor.AuthenticationMethod.SIMPLE);
        lcd.setPortNumber((short) directoryServiceConfiguration.getEmbeddedLdapPort());
        lcd.setCallbackHandler(new UsernamePasswordHandler(directoryServiceConfiguration.getContextSecurityPrincipal(), directoryServiceConfiguration.getContextSecurityCredentials()));
        return lcd;
    }

    private static short getRandomNumber() {
        final Random ran = new Random();
        return (short) (11000 + ran.nextInt(999));
    }

    static void deleteRecursively(File file) {
        if (!file.exists())
            return;

        if (file.isDirectory())
            for (final File f : file.listFiles())
                if (f.isDirectory())
                    deleteRecursively(f);
                else
                    f.delete();

        file.delete();
    }

}
