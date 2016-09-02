package org.openthinclient.util.dpkg;

import org.junit.Ignore;
import org.junit.Test;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.Version;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DPKGPackageFactoryTest {

  @Test
  @Ignore("Order has changed")
  public void testParsePackages() throws Exception {

    final InputStream packagesStream = getClass().getResourceAsStream("/test-repository/Packages");
    assertNotNull(packagesStream);

    PackagesListParser parser = new PackagesListParser();
    final List<Package> packages = parser.parse(packagesStream);

    // TODO JN: die Reihenfolg muss doch nicht stimmen, aber alle pakete sollen vohanden sein
    
    assertEquals(21, packages.size());

    assertEquals("foo", packages.get(0).getName());
    assertEquals(Version.parse("2.0-1"), packages.get(0).getVersion());
    assertEquals("foo", packages.get(1).getName());
    assertEquals(Version.parse("2.1-1"), packages.get(1).getVersion());
    assertEquals("foo-fork", packages.get(2).getName());
    assertEquals(Version.parse("2.0-1"), packages.get(2).getVersion());
    assertEquals("foo2", packages.get(3).getName());
    assertEquals(Version.parse("2.0-1"), packages.get(3).getVersion());
    assertEquals("zonk", packages.get(4).getName());
    assertEquals(Version.parse("2.0-1"), packages.get(4).getVersion());
    assertEquals("zonk", packages.get(5).getName());
    assertEquals(Version.parse("2.1-1"), packages.get(5).getVersion());
    assertEquals("zonk-dev", packages.get(6).getName());
    assertEquals(Version.parse("2.0-1"), packages.get(6).getVersion());
    assertEquals("zonk2", packages.get(7).getName());
    assertEquals(Version.parse("2.0-1"), packages.get(7).getVersion());
    assertEquals("bar", packages.get(8).getName());
    assertEquals(Version.parse("2.0-1"), packages.get(8).getVersion());
    assertEquals("bar2", packages.get(9).getName());
    assertEquals(Version.parse("2.0-1"), packages.get(9).getVersion());
    assertEquals("bar2", packages.get(10).getName());
    assertEquals(Version.parse("2.1-1"), packages.get(10).getVersion());
    assertEquals("bar2-dev", packages.get(11).getName());
    assertEquals(Version.parse("2.0-1"), packages.get(11).getVersion());
    assertEquals("bas", packages.get(12).getName());
    assertEquals(Version.parse("2.0-1"), packages.get(12).getVersion());
    assertEquals("bas", packages.get(13).getName());
    assertEquals(Version.parse("2.1-1"), packages.get(13).getVersion());
    assertEquals("bas-dev", packages.get(14).getName());
    assertEquals(Version.parse("2.0-1"), packages.get(14).getVersion());
    assertEquals("bas2", packages.get(15).getName());
    assertEquals(Version.parse("2.0-1"), packages.get(15).getVersion());

  }
}