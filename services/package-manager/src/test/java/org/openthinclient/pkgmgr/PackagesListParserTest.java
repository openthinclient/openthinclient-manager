package org.openthinclient.pkgmgr;

import org.junit.Ignore;
import org.junit.Test;
import org.openthinclient.util.dpkg.PackageReferenceListParser;
import org.openthinclient.util.dpkg.PackagesListParser;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PackagesListParserTest {

  @Test
  @Ignore("Order has changed")
  public void testPackagesParsing() throws Exception {
    
    final List<org.openthinclient.pkgmgr.db.Package> packageList = new PackagesListParser()
            .parse(getClass().getResourceAsStream("/test-repository/Packages"));

    assertEquals(21, packageList.size());

    assertEquals("foo", packageList.get(0).getName());
    assertEquals("zonk", packageList.get(5).getName());
    assertEquals(new PackageReferenceListParser().parse("bar2"), packageList.get(5).getConflicts());

    assertEquals("bar2", packageList.get(9).getName());
    assertEquals(new PackageReferenceListParser().parse("foo"), packageList.get(9).getDepends());
    assertEquals("bar", packageList.get(8).getName());
    assertEquals(new PackageReferenceListParser().parse("foo (>= 0:2.1-1)"), packageList.get(8).getDepends());

  }

  @Test
  @Ignore("Order has changed")
  public void testParsePackages() throws Exception {

    final InputStream packagesStream = getClass().getResourceAsStream("/test-repository/Packages");
    assertNotNull(packagesStream);

    final PackagesListParser factory = new PackagesListParser();
    final List<org.openthinclient.pkgmgr.db.Package> packages = factory.parse(packagesStream);

    assertEquals(21, packages.size());

    assertEquals("foo", packages.get(0).getName());
    assertEquals("zonk", packages.get(5).getName());
    assertEquals("bar2", packages.get(9).getName());
    assertEquals("bar", packages.get(8).getName());


  }
}
