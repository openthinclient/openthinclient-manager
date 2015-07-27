package org.openthinclient.common.test.ldap;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.apache.directory.server.core.schema.bootstrap.BootstrapSchema;
import org.apache.directory.server.core.schema.bootstrap.NisSchema;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openthinclient.common.model.OrganizationalUnit;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.DirectoryFacade;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.LDAPConnectionDescriptor.ProviderType;
import org.openthinclient.ldap.Mapping;
import org.openthinclient.ldap.Util;
import org.openthinclient.service.apacheds.DirectoryService;
import org.openthinclient.service.apacheds.DirectoryServiceConfiguration;

public abstract class AbstractEmbeddedDirectoryTest {

	private static DirectoryService ds;
	private static short ldapPort;
	protected static String baseDN = "dc=test,dc=test";
	protected static String envDN = "ou=test," + baseDN;

	@BeforeClass
	public static void setUp() throws Exception {

		ds = new DirectoryService();

		final DirectoryServiceConfiguration configuration = new DirectoryServiceConfiguration();

    	configuration.setAccessControlEnabled(false);
		configuration.setEmbeddedAnonymousAccess(true);
		configuration.setEmbeddedServerEnabled(true);
		configuration.setContextFactory("org.apache.directory.server.jndi.ServerContextFactory");
		configuration.setContextProviderURL("uid=admin,ou=system");
		configuration.setContextSecurityAuthentication("simple");
		configuration.setContextSecurityCredentials("secret");
		configuration.setContextSecurityPrincipal("uid=admin,ou=system");

		configuration.setEmbeddedCustomRootPartitionName("dc=test,dc=test");
		configuration.setEmbeddedWkDir(new File("unit-test-tmp"));

		final List<Class<? extends BootstrapSchema>> customSchema = Arrays.asList(NisSchema.class);
		configuration.setCustomSchema(customSchema);

		// ds.setEmbeddedLDIFdir("${jboss.server.data.dir}/apacheds-ldif");
		// <attribute name="EmbeddedCustomBootstrapSchema">
		// <xml-properties>
		// <config-property
		//
		// name="NisSchema">org.apache.directory.server.core.schema.bootstrap.NisSchema</config-property>
		// </xml-properties>
		// </attribute>

		configuration.setEnableNtp(false);
		configuration.setEnableKerberos(false);
		configuration.setEnableChangePassword(false);
		configuration.setEmbeddedLdapNetworkingSupport(true);

		ldapPort = getRandomNumber();

		configuration.setEmbeddedLdapPort(ldapPort);
		// ds.setEmbeddedLDAPSPort(10636);

    ds.setConfiguration(configuration);

		ds.startService();
	}

	@AfterClass
	public static void cleanUp() throws Exception {
		if (null != ds)
			ds.stopService();

		deleteRecursively(new File("unit-test-tmp"));
	}

	protected static LDAPConnectionDescriptor getConnectionDescriptor() {
		final LDAPConnectionDescriptor lcd = new LDAPConnectionDescriptor();

		lcd.setPortNumber(ldapPort);

		lcd.setProviderType(ProviderType.SUN);
		// lcd.setProviderType(ProviderType.APACHE_DS_EMBEDDED);
		return lcd;
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

	private static short getRandomNumber() {
		final Random ran = new Random();
		return (short) (11000 + ran.nextInt(999));
	}

	protected Mapping mapping;
	protected LDAPConnectionDescriptor connectionDescriptor;

	@Before
	public void initEnv() throws Exception {
		Mapping.disableCache = true;

		connectionDescriptor = getConnectionDescriptor();
		connectionDescriptor.setBaseDN(baseDN);

		final DirectoryFacade facade = connectionDescriptor.createDirectoryFacade();
		final DirContext ctx = facade.createDirContext();
		try {
			Util.deleteRecursively(ctx, facade.makeRelativeName(envDN));
		} catch (final NameNotFoundException e) {
			// ignore
		} finally {
			ctx.close();
		}

		mapping = Mapping.load(getClass().getResourceAsStream(
				"/org/openthinclient/common/directory/APACHE_DS.xml"));
		mapping.initialize();

		mapping.setConnectionDescriptor(connectionDescriptor);

		final OrganizationalUnit ou = new OrganizationalUnit();
		ou.setName("test");
		ou.setDescription("openthinclient.org Console"); //$NON-NLS-1$
		mapping.save(ou, "");

		connectionDescriptor.setBaseDN(envDN);

		// re-set mapping to the env DN
		mapping.setConnectionDescriptor(connectionDescriptor);

		final OrganizationalUnit clients = new OrganizationalUnit();
		clients.setName("clients");
		mapping.save(clients, "");
		
		// creating the new oranizational unit "clientgroups"
		final OrganizationalUnit clientgroups = new OrganizationalUnit();
		clientgroups.setName("clientgroups");
		mapping.save(clientgroups, "");

		final OrganizationalUnit users = new OrganizationalUnit();
		users.setName("users");
		mapping.save(users, "");

		final OrganizationalUnit usergroups = new OrganizationalUnit();
		usergroups.setName("usergroups");
		mapping.save(usergroups, "");

		final OrganizationalUnit apps = new OrganizationalUnit();
		apps.setName("apps");
		mapping.save(apps, "");

		final OrganizationalUnit appgroups = new OrganizationalUnit();
		appgroups.setName("appgroups");
		mapping.save(appgroups, "");

		final OrganizationalUnit devices = new OrganizationalUnit();
		devices.setName("devices");
		mapping.save(devices, "");

		final OrganizationalUnit locations = new OrganizationalUnit();
		locations.setName("locations");
		mapping.save(locations, "");

		final OrganizationalUnit hwtypes = new OrganizationalUnit();
		hwtypes.setName("hwtypes");
		mapping.save(hwtypes, "");

		final OrganizationalUnit printers = new OrganizationalUnit();
		printers.setName("printers");
		mapping.save(printers, "");

		final OrganizationalUnit unrecognizedClients = new OrganizationalUnit();
		unrecognizedClients.setName("unrecognized-clients");
		mapping.save(unrecognizedClients, "");
	}

	@After
	public void destroyEnv() throws IOException, DirectoryException,
			NamingException {
		final LDAPConnectionDescriptor lcd = getConnectionDescriptor();
		lcd.setBaseDN(baseDN);

		final DirectoryFacade facade = lcd.createDirectoryFacade();
		final DirContext ctx = facade.createDirContext();
		try {
			Util.deleteRecursively(ctx, facade.makeRelativeName(envDN));
		} catch (final NameNotFoundException e) {
			// ignore!
		} finally {
			ctx.close();
			Runtime.getRuntime().gc();
		}
	}
}