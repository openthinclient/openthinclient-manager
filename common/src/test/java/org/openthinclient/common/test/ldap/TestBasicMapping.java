package org.openthinclient.common.test.ldap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.sar.DirectoryService;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openthinclient.common.directory.ACLUtils;
import org.openthinclient.common.directory.LDAPConnectionDescriptor;
import org.openthinclient.common.directory.LDAPDirectory;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.ApplicationGroup;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Device;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Group;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.OrganizationalUnit;
import org.openthinclient.common.model.Printer;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.User;
import org.openthinclient.common.model.UserGroup;
import org.openthinclient.console.HTTPLdifImportAction;
import org.openthinclient.console.NewRealmInitCommand;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.ldap.Util;

public class TestBasicMapping {
	private static DirectoryService ds;

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
	
	 // ds.setEmbeddedLDIFdir("${jboss.server.data.dir}/apacheds-ldif");
	 // <attribute name="EmbeddedCustomBootstrapSchema">
	 // <xml-properties>
	 // <config-property
	 //
//	 name="NisSchema">org.apache.directory.server.core.schema.bootstrap.NisSchema</config-property>
	 // </xml-properties>
	 // </attribute>
	
	 ds.setEmbeddedEnableNtp(false);
	 ds.setEmbeddedEnableKerberos(false);
	 ds.setEmbeddedEnableChangePassword(false);
	 ds.setEmbeddedLDAPNetworkingSupport(true);
	 ds.setEmbeddedLDAPPort(11389);
	 ds.setEmbeddedLDAPSPort(11636);
	
	 ds.start();
	 }

	@AfterClass
	public static void cleanUp() {
		if (null != ds)
			ds.stop();

		deleteRecursively(new File("unit-test-tmp"));
	}

	private static void deleteRecursively(File file) {
		if (!file.exists())
			return;

		if (file.isDirectory())
			for (File f : file.listFiles()) {
				if (f.isDirectory())
					deleteRecursively(f);
				else
					f.delete();
			}

		file.delete();
	}

	@Test
	public void testSomething() {
		System.setProperty("foo", "bar");

		Assert.assertEquals("System property correct", "bar", System
				.getProperty("foo"));
	}

	@Test
	public void testSomethingElse() {
		Assert.assertNotNull("System property: os.name", System
				.getProperty("os.name"));
	}

	@Test
	public void testYetSomething() {
		try {
			new FileInputStream("c:/doesntexist");
			Assert.fail("Expected exception not thrown");
		} catch (FileNotFoundException e) {
			// expected
		}
	}

}
