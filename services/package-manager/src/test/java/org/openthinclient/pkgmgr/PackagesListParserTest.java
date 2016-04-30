package org.openthinclient.pkgmgr;

import org.junit.Test;
import org.openthinclient.util.dpkg.PackageReferenceListParser;
import org.openthinclient.util.dpkg.PackagesListParser;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PackagesListParserTest {

  @Test
  public void testPackagesParsing() throws Exception {
    final List<org.openthinclient.pkgmgr.db.Package> packageList = new PackagesListParser()
            .parse(getClass().getResourceAsStream("/test-repository/Packages"));

    assertEquals(16, packageList.size());

    assertEquals("foo", packageList.get(0).getName());
    assertEquals("zonk", packageList.get(5).getName());
    assertEquals(new PackageReferenceListParser().parse("bar2"), packageList.get(5).getConflicts());


    assertEquals("bar2", packageList.get(2).getName());
    assertEquals(new PackageReferenceListParser().parse("foo"), packageList.get(2).getDepends());
    assertEquals("bar", packageList.get(3).getName());
    assertEquals(new PackageReferenceListParser().parse("foo (>= 0:2.1-1)"), packageList.get(3).getDepends());

  }

  @Test
  public void testParsePackages() throws Exception {

    final InputStream packagesStream = getClass().getResourceAsStream("/test-repository/Packages");
    assertNotNull(packagesStream);

    final PackagesListParser factory = new PackagesListParser();
    final List<org.openthinclient.pkgmgr.db.Package> packages = factory.parse(packagesStream);

    assertEquals(16, packages.size());

    assertEquals("foo", packages.get(0).getName());
    assertEquals("zonk", packages.get(1).getName());
    assertEquals("bar2", packages.get(2).getName());
    assertEquals("bar", packages.get(3).getName());


  }
}
