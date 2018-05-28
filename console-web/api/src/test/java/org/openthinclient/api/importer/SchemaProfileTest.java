package org.openthinclient.api.importer;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.api.context.InstallContext;
import org.openthinclient.api.distributions.ImportItem;
import org.openthinclient.api.distributions.ImportableProfileProvider;
import org.openthinclient.api.distributions.InstallableDistribution;
import org.openthinclient.api.distributions.InstallableDistributions;
import org.openthinclient.api.importer.config.ImporterConfiguration;
import org.openthinclient.api.importer.impl.ClasspathSchemaProvider;
import org.openthinclient.api.importer.impl.ImportModelMapper;
import org.openthinclient.api.importer.impl.RestModelImporter;
import org.openthinclient.api.importer.model.ImportableClient;
import org.openthinclient.api.importer.model.ImportableHardwareType;
import org.openthinclient.api.importer.model.ImportableLocation;
import org.openthinclient.api.rest.model.AbstractProfileObject;
import org.openthinclient.api.rest.model.Application;
import org.openthinclient.api.rest.model.Printer;
import org.openthinclient.common.config.LDAPServicesConfiguration;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Device;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.OrganizationalUnit;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.User;
import org.openthinclient.common.model.UserGroup;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.DeviceService;
import org.openthinclient.common.model.service.HardwareTypeService;
import org.openthinclient.common.model.service.LocationService;
import org.openthinclient.common.model.service.PrinterService;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.Mapping;
import org.openthinclient.ldap.TypeMapping;
import org.openthinclient.ldap.auth.UsernamePasswordHandler;
import org.openthinclient.progress.NoopProgressReceiver;
import org.openthinclient.service.apacheds.DirectoryService;
import org.openthinclient.service.apacheds.DirectoryServiceConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
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
 *
 * FIXME: Two tests will lead to 'Bind exception: address alraedy in use' on Build-machine. That's why testImportSimpleHardwareType() is ignored
 */
@RunWith(SpringRunner.class)
@Import({ImporterConfiguration.class, org.openthinclient.api.importer.impl.RestModelImporterTest.ClasspathSchemaProviderConfiguration.class,
        LDAPServicesConfiguration.class})
@DirtiesContext
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
    LocationService locationService;
    @Autowired
    DeviceService deviceService;
    @Autowired
    ApplicationService applicationService;
    @Autowired
    ClientService clientService;
    @Autowired
    PrinterService printerService;


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

    @Before
    public void setUp() throws Exception {

        ds = new DirectoryService();
        ds.setConfiguration(getDirectoryServiceConfiguration());
        ds.startService();

        importer = new RestModelImporter(mapper, hardwareTypeService, applicationService, clientService, deviceService, locationService, printerService);

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

    @After
    public void cleanUp() throws Exception {
        if (null != ds) {
            ds.stopService();
        }
        deleteRecursively(new File("unit-test-tmp"));
    }

    @Ignore
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

    @Test
    public void testSetProfilesFromDistributionsXML() throws Exception {

        final InstallableDistributions testDistributions = InstallableDistributions.load(SchemaProfileTest.class.getResource("/test-distributions.xml"));
        assertNotNull(testDistributions);
        assertEquals(1, testDistributions.getInstallableDistributions().size());

        final InstallableDistribution distribution = testDistributions.getPreferred();

        ImportableProfileProvider provider = new ImportableProfileProvider(SchemaProfileTest.class.getResource("/").toURI());
        for (ImportItem item : distribution.getImportItems()) {
            AbstractProfileObject profileObject = provider.access(new InstallContext(), item, new NoopProgressReceiver());
            switch (profileObject.getType()) {
                case HARDWARETYPE:
                    importer.importHardwareType((ImportableHardwareType) profileObject);
                    break;
                case LOCATION:
                    importer.importLocation((ImportableLocation) profileObject);
                    break;
                case DEVICE:
                    importer.importDevice((org.openthinclient.api.rest.model.Device) profileObject);
                    break;
                case APPLICATION:
                    importer.importApplication((Application) profileObject);
                    break;
                case CLIENT:
                    importer.importClient((ImportableClient) profileObject);
                    break;
                case PRINTER:
                    importer.importPrinter((Printer) profileObject);
                    break;
            }
        }

        Set<Location> locations = locationService.findAll();
        assertNotNull(locations);
        assertEquals(3, locations.size());

        Set<HardwareType> allHWTypes = hardwareTypeService.findAll();
        assertNotNull(allHWTypes);
        assertEquals(2, allHWTypes.size());

        Set<Device> devices = deviceService.findAll();
        assertNotNull(devices);
        assertEquals(2, devices.size());

        Device display = devices.stream().filter(device -> device.getName().contains("Display: 1280x1024 VGA")).findFirst().get();
        assertEquals("1280x1024", display.getValue("firstscreen.resolution"));
        assertEquals("normal", display.getValue("firstscreen.rotation"));
        assertEquals("VGA", display.getValue("firstscreen.connect"));
        // FIXME: JN this should work
        // DirectoryException: One-to-many associaction contains a member of type class java.lang.String for which I don't have a mapping.
        // assertEquals(1, display.getMembers().size());
        // assertEquals("DC=dummy", display.getMembers().iterator().next().toString());

        Set<org.openthinclient.common.model.Printer> printers = printerService.findAll();
        assertNotNull(printers);
        assertEquals(2, printers.size());

        Set<org.openthinclient.common.model.Application> applications = applicationService.findAll();
        assertNotNull(applications);
        assertEquals(2, applications.size());

        Set<Client> clients = clientService.findAll();
        assertNotNull(clients);
        assertEquals(2, clients.size());
        Client autologonClient = clients.stream().filter(client -> client.getName().contains("Autologon-Client-DE")).findFirst().get();
        assertEquals(2, autologonClient.getApplications().size());
        assertEquals(2, autologonClient.getDevices().size());
        assertEquals(2, autologonClient.getPrinters().size());
        assertEquals("Client (EN-UK) Autologon home-in-RAM",autologonClient.getHardwareType().getName());

    }

    // Methods for setting up Directory

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
