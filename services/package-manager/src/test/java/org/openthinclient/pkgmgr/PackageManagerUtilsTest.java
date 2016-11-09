package org.openthinclient.pkgmgr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.openthinclient.pkgmgr.PackageTestUtils.createPackage;

import java.util.ArrayList;
import java.util.List;
import org.openthinclient.pkgmgr.db.Package;

import org.junit.Test;

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

}
