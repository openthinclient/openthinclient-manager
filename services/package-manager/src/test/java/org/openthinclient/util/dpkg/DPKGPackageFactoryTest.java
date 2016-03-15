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

    assertEquals(16, packages.size());

    assertEquals("foo", packages.get(0).getName());
    assertEquals(new Version("2.0-1"), packages.get(0).getVersion());
    assertEquals("foo", packages.get(1).getName());
    assertEquals(new Version("2.1-1"), packages.get(1).getVersion());
    assertEquals("foo-fork", packages.get(2).getName());
    assertEquals(new Version("2.0-1"), packages.get(2).getVersion());
    assertEquals("foo2", packages.get(3).getName());
    assertEquals(new Version("2.0-1"), packages.get(3).getVersion());
    assertEquals("zonk", packages.get(4).getName());
    assertEquals(new Version("2.0-1"), packages.get(4).getVersion());
    assertEquals("zonk", packages.get(5).getName());
    assertEquals(new Version("2.1-1"), packages.get(5).getVersion());
    assertEquals("zonk-dev", packages.get(6).getName());
    assertEquals(new Version("2.0-1"), packages.get(6).getVersion());
    assertEquals("zonk2", packages.get(7).getName());
    assertEquals(new Version("2.0-1"), packages.get(7).getVersion());
    assertEquals("bar", packages.get(8).getName());
    assertEquals(new Version("2.0-1"), packages.get(8).getVersion());
    assertEquals("bar2", packages.get(9).getName());
    assertEquals(new Version("2.0-1"), packages.get(9).getVersion());
    assertEquals("bar2", packages.get(10).getName());
    assertEquals(new Version("2.1-1"), packages.get(10).getVersion());
    assertEquals("bar2-dev", packages.get(11).getName());
    assertEquals(new Version("2.0-1"), packages.get(11).getVersion());
    assertEquals("bas", packages.get(12).getName());
    assertEquals(new Version("2.0-1"), packages.get(12).getVersion());
    assertEquals("bas", packages.get(13).getName());
    assertEquals(new Version("2.1-1"), packages.get(13).getVersion());
    assertEquals("bas-dev", packages.get(14).getName());
    assertEquals(new Version("2.0-1"), packages.get(14).getVersion());
    assertEquals("bas2", packages.get(15).getName());
    assertEquals(new Version("2.0-1"), packages.get(15).getVersion());

  }
}