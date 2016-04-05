package org.openthinclient.util.dpkg;

import org.junit.Test;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.op.PackageManagerOperation.PackageChange;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.junit.Assert.*;
import static org.openthinclient.pkgmgr.PackageTestUtils.createPackage;
import static org.openthinclient.pkgmgr.PackageTestUtils.createVersion;

public class PackageManagerOperationResolverImplTest {

  @Test
  public void testFindPackageChangesWithNoInstalledPackages() throws Exception {

    final Package pkg1 = createPackage("pkg1", "1.0-2");

    final Stream<PackageChange> stream = resolver().findPackageChanges(
        singleton(pkg1), emptyList());

    assertEquals(0, stream.count());

  }

  private PackageManagerOperationResolverImpl resolver() {
    return new PackageManagerOperationResolverImpl(
        Collections::emptyList, Collections::emptyList);
  }

  @Test
  public void testFindPackageChangesWithNonMatchingInstalledPackage() throws Exception {
    final Package pkg1 = createPackage("pkg1", "1.0-2");
    final Package pkg2 = createPackage("pkg2", "1.0-2");

    final Stream<PackageChange> stream = resolver().findPackageChanges(
        singleton(pkg1), singleton(pkg2));

    assertEquals(0, stream.count());
  }

  @Test
  public void testFindPackageChangesWithSingleInstalledPackageInOdlerVersion() throws Exception {
    final Package pkg_new = createPackage("pkg1", "1.0-2");
    final Package pkg_old = createPackage("pkg1", "1.0-1");

    final Stream<PackageChange> stream = resolver().findPackageChanges(
        singleton(pkg_new), singleton(pkg_old));

    final PackageChange modification = stream.findFirst().get();
    assertEquals(createVersion("1.0", "1"), modification.getInstalled().getVersion());
    assertEquals(createVersion("1.0", "2"), modification.getRequested().getVersion());
  }

  @Test
  public void testFindPackageChangesWithMultipleInstalledPackagesNonMatching() throws Exception {
    final Package pkg_new = createPackage("pkg1", "1.0-2");

    final Stream<PackageChange> stream = resolver().findPackageChanges(
        singleton(pkg_new), Arrays.asList( //
            createPackage("pkg2", "12.0-1"), //
            createPackage("pkg3", "12.0-1"), //
            createPackage("pkg4", "12.0-1"), //
            createPackage("pkg5", "12.0-1") //
        ));

    assertEquals(0, stream.count());
  }

  @Test
  public void testFindPackageChangesWithMultipleInstalledPackagesAndOneInOlderVersion() throws Exception {
    final Package pkg_new = createPackage("pkg1", "1.0-2");

    final Stream<PackageChange> stream = resolver().findPackageChanges(
        singleton(pkg_new), Arrays.asList( //
            createPackage("pkg2", "12.0-1"), //
            createPackage("pkg3", "12.0-1"), //
            createPackage("pkg4", "12.0-1"), //
            createPackage("pkg1", "1.0-1"), //
            createPackage("pkg5", "12.0-1") //
        ));

    final PackageChange modification = stream.findFirst().get();
    assertEquals(createVersion("1.0", "1"), modification.getInstalled().getVersion());
    assertEquals(createVersion("1.0", "2"), modification.getRequested().getVersion());
  }

  @Test
  public void testIsSamePackage() throws Exception {

    final PackageManagerOperationResolverImpl r = resolver();
    assertTrue(r.isSamePackage(createPackage("pkg1", "1.0-1"), createPackage("pkg1", "1.0-1")));
    assertFalse(r.isSamePackage(createPackage("pkg1", "1.0-1"), createPackage("pkg2", "1.0-1")));
    assertFalse(r.isSamePackage(createPackage("pkg1", "1.0-1"), createPackage("pkg1", "1.0-2")));

  }
  
  @Test
  public void testFindPackagesToInstall() {
    PackageManagerOperationResolverImpl res = new PackageManagerOperationResolverImpl(null, null);
    Collection<Package> packagesToInstall = Arrays.asList(createPackage("foo", "2.0-1"));
    List<PackageChange> changes = Arrays.asList();
    Collection<Package> availablePackages = Arrays.asList(createPackage("foo", "2.0-1"));
    List<Package> collect = res.findPackagesToInstall(packagesToInstall, changes, availablePackages).collect(Collectors.toList());
    assertFalse(collect.isEmpty());
    assertEquals(1, collect.size());
  }
}