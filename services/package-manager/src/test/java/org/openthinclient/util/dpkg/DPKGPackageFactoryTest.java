package org.openthinclient.util.dpkg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.openthinclient.pkgmgr.PackagesUtil.PACKAGES_SIZE;

import java.io.InputStream;
import java.util.List;

import org.junit.Test;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.Version;

public class DPKGPackageFactoryTest {

  @Test
  public void testParsePackages() throws Exception {

    final InputStream packagesStream = getClass().getResourceAsStream("/test-repository/Packages");
    assertNotNull(packagesStream);

    PackagesListParser parser = new PackagesListParser();
    final List<Package> packages = parser.parse(packagesStream);

    assertEquals(PACKAGES_SIZE, packages.size());
    
    assertTrue(packageExists(packages, "foo", "2.0-1"));
    assertTrue(packageExists(packages, "foo", "2.1-1"));
    assertTrue(packageExists(packages, "foo-fork", "2.0-1"));
    assertTrue(packageExists(packages, "foo2", "2.0-1"));
    assertTrue(packageExists(packages, "zonk", "2.0-1"));
    assertTrue(packageExists(packages, "zonk", "2.1-1"));
    assertTrue(packageExists(packages, "zonk-dev", "2.0-1"));
    assertTrue(packageExists(packages, "zonk2", "2.0-1"));
    assertTrue(packageExists(packages, "bar", "2.0-1"));
    assertTrue(packageExists(packages, "bar2", "2.0-1"));
    assertTrue(packageExists(packages, "bar2", "2.1-1"));
    assertTrue(packageExists(packages, "bar2-dev", "2.0-1"));
    assertTrue(packageExists(packages, "bas", "2.0-1"));
    assertTrue(packageExists(packages, "bas", "2.1-1"));
    assertTrue(packageExists(packages, "bas-dev", "2.0-1"));
    assertTrue(packageExists(packages, "bas2", "2.0-1"));
    assertTrue(packageExists(packages, "rec", "2.0-1"));
    assertTrue(packageExists(packages, "rec-fork", "2.0-1"));
    assertTrue(packageExists(packages, "rec-fork2", "2.0-1"));
    assertTrue(packageExists(packages, "rec-fork2", "2.1-1"));

  }

  private boolean packageExists(List<Package> packages, String name, String version) {
    return packages.stream()
                   .filter(p -> p.getName().equals(name) && p.getVersion().equals(Version.parse(version)))
                   .findAny()
                   .isPresent();
    
  }
}