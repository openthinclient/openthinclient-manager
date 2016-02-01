package org.openthinclient.pkgmgr;

import org.junit.Test;
import org.openthinclient.util.dpkg.DPKGPackageFactory;
import org.openthinclient.util.dpkg.PackageReferenceListParser;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class DPKGPackageFactoryTest {

  @Test
  public void testPackagesParsing() throws Exception {
    final List<org.openthinclient.pkgmgr.db.Package> packageList = new DPKGPackageFactory()
          .getPackage(getClass().getResourceAsStream("/test-repository/Packages"));

    assertEquals(4, packageList.size());

    assertEquals("foo", packageList.get(0).getName());
    assertEquals("zonk", packageList.get(1).getName());
    assertEquals(new PackageReferenceListParser().parse("bar2"), packageList.get(1).getConflicts());


    assertEquals("bar2", packageList.get(2).getName());
    assertEquals(new PackageReferenceListParser().parse("foo"), packageList.get(2).getDepends());
    assertEquals("bar", packageList.get(3).getName());
    assertEquals(new PackageReferenceListParser().parse("foo (>= 0:2.1-1)"), packageList.get(3).getDepends());

  }
}
