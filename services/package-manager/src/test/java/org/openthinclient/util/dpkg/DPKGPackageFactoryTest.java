package org.openthinclient.util.dpkg;

import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;

public class DPKGPackageFactoryTest {

  @Test
  public void testParsePackages() throws Exception {

    final InputStream packagesStream = getClass().getResourceAsStream("/test-repository/Packages");
    assertNotNull(packagesStream);

    final DPKGPackageFactory factory = new DPKGPackageFactory(null);
    final List<Package> packages = factory.getPackage(packagesStream);

    assertEquals(5, packages.size());

    assertEquals("foo", packages.get(0).getName());
    assertEquals("foo", packages.get(1).getName());
    assertEquals("zonk", packages.get(2).getName());
    assertEquals("bar2", packages.get(3).getName());
    assertEquals("bar", packages.get(4).getName());


  }
}