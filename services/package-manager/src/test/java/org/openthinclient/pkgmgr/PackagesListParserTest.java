package org.openthinclient.pkgmgr;

import static org.junit.Assert.assertEquals;
import static org.openthinclient.pkgmgr.PackagesUtil.PACKAGES_SIZE;

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.Version;
import org.openthinclient.util.dpkg.PackageReferenceListParser;
import org.openthinclient.util.dpkg.PackagesListParser;

public class PackagesListParserTest {

  @Test
  public void testPackagesParsing() throws Exception {
    
    final List<org.openthinclient.pkgmgr.db.Package> packageList = new PackagesListParser()
            .parse(getClass().getResourceAsStream("/test-repository/Packages"));

    // TODO JN: fix this
    assertEquals(PACKAGES_SIZE + 1, packageList.size());

    Package zonk = getPackage(packageList, "zonk", "2.0-1");
    assertEquals(new PackageReferenceListParser().parse("bar2"), zonk.getConflicts());

    Package bar2 = getPackage(packageList, "bar2", "2.0-1");
    assertEquals(new PackageReferenceListParser().parse("foo"), bar2.getDepends());
    
    Package bar = getPackage(packageList, "bar", "2.0-1");
    assertEquals(new PackageReferenceListParser().parse("foo (>= 0:2.1-1)"), bar.getDepends());

  }

  private Package getPackage(List<Package> packages, String name, String version) throws Exception {
     Optional<Package> findAny = packages.stream()
                                         .filter(p -> p.getName().equals(name) && p.getVersion().equals(Version.parse(version)))
                                         .findAny();
     if (findAny.isPresent()) {
       return findAny.get();
     } else {
       throw new Exception("Package " + name + " with version " + version + " not found.");
     }
  }
  
}
