package org.openthinclient.util.dpkg;

import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DPKGPackageFactoryTest {

  @Test
  public void testParsePackages() throws Exception {

    final InputStream packagesStream = getClass().getResourceAsStream("/test-repository/Packages");
    assertNotNull(packagesStream);

    final DPKGPackageFactory factory = new DPKGPackageFactory();
    final List<org.openthinclient.pkgmgr.db.Package> packages = factory.getPackage(packagesStream);

    assertEquals(4, packages.size());

    assertEquals("foo", packages.get(0).getName());
    assertEquals("zonk", packages.get(1).getName());
    assertEquals("bar2", packages.get(2).getName());
    assertEquals("bar", packages.get(3).getName());


  }
}
