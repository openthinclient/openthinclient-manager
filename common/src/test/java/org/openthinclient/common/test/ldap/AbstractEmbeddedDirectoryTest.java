package org.openthinclient.common.test.ldap;

import java.io.File;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.directory.server.sar.DirectoryService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.ldap.LDAPConnectionDescriptor.ProviderType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AbstractEmbeddedDirectoryTest {

	private static DirectoryService ds;
	private static short ldapPort;
	protected static String baseDN = "dc=test,dc=test";

	@BeforeClass
	public static void setUp() throws Exception {
		ds = new DirectoryService();
		ds.setEmbeddedAccessControlEnabled(false);
		ds.setEmbeddedAnonymousAccess(true);
		ds.setEmbeddedServerEnabled(true);
		ds
				.setContextFactory("org.apache.directory.server.jndi.ServerContextFactory");
		ds.setContextProviderURL("uid=admin,ou=system");
		ds.setContextSecurityAuthentication("simple");
		ds.setContextSecurityCredentials("secret");
		ds.setContextSecurityPrincipal("uid=admin,ou=system");

		ds.setEmbeddedCustomRootPartitionName("dc=test,dc=test");
		ds.setEmbeddedWkdir("unit-test-tmp");

		final DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		final DocumentBuilder b = f.newDocumentBuilder();
		final Document d = b.newDocument();

		final Element wrapper = d.createElement("xml-properties");
		d.appendChild(wrapper);

		final Element e = d.createElement("config-property");
		e.setAttribute("name", "NisSchema");
		e
				.appendChild(d
						.createTextNode("org.apache.directory.server.core.schema.bootstrap.NisSchema"));
		wrapper.appendChild(e);

		ds.setEmbeddedCustomBootstrapSchema(wrapper);

		// ds.setEmbeddedLDIFdir("${jboss.server.data.dir}/apacheds-ldif");
		// <attribute name="EmbeddedCustomBootstrapSchema">
		// <xml-properties>
		// <config-property
		//
		// name="NisSchema">org.apache.directory.server.core.schema.bootstrap.NisSchema</config-property>
		// </xml-properties>
		// </attribute>

		ds.setEmbeddedEnableNtp(false);
		ds.setEmbeddedEnableKerberos(false);
		ds.setEmbeddedEnableChangePassword(false);
		ds.setEmbeddedLDAPNetworkingSupport(true);

		ldapPort = getRandomNumber();

		ds.setEmbeddedLDAPPort(ldapPort);
		// ds.setEmbeddedLDAPSPort(11636);

		ds.start();
	}

	@AfterClass
	public static void cleanUp() {
		if (null != ds)
			ds.stop();

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
}