package org.openthinclient.pkgmgr;

import org.junit.Test;
import org.openthinclient.pkgmgr.db.Package;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.openthinclient.pkgmgr.PackageTestUtils.createPackage;

/**
 * Test class for {@link PackageManagerUtils}
 */
public class PackageManagerUtilsTest {

  @Test
  public void testReduceToLatestVersion() {
    
    List<Package> packages = new ArrayList<>();
    packages.add(createPackage("pkg1", "1.0-12"));
    Package p0 = createPackage("pkg2", "1.0-12");
    packages.add(p0);
    Package p1 = createPackage("pkg1", "1.0-15");
    packages.add(p1);
    
    PackageManagerUtils pmu = new PackageManagerUtils();
    List<Package> latestVersion = pmu.reduceToLatestVersion(packages);
    
    assertNotNull(latestVersion);
    assertEquals(2, latestVersion.size());
    assertTrue(latestVersion.contains(p0));
    assertTrue(latestVersion.contains(p1));
  }


  @Test
  public void testParsePackages() {
    assertEquals(createPackage("pkg1", null), PackageManagerUtils.parse("pkg1"));
    assertEquals(createPackage("pkg1", "1.0-12"), PackageManagerUtils.parse("pkg1_1.0-12"));
    assertEquals(createPackage("pkg1-rev2", "1.0-12"), PackageManagerUtils.parse("pkg1-rev2_1.0-12"));
    assertEquals(createPackage("pkg1-rev2-v3", "1.0-12"), PackageManagerUtils.parse("pkg1-rev2-v3_1.0-12"));
  }
}
