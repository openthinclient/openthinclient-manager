package org.openthinclient.util.dpkg;

import org.junit.Test;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.op.InstallPlan;
import org.openthinclient.pkgmgr.op.InstallPlanStep;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.openthinclient.pkgmgr.PackageTestUtils.createPackage;
import static org.openthinclient.pkgmgr.PackageTestUtils.createVersion;

public class PackageManagerOperationResolverImplTest {

  @Test
  public void testFindPackageChangesWithNoInstalledPackages() throws Exception {

    final Package pkg1 = createPackage("pkg1", "1.0-2");

    final Stream<InstallPlanStep> stream = resolver().findPackageChanges(
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

    final Stream<InstallPlanStep> stream = resolver().findPackageChanges(
        singleton(pkg1), singleton(pkg2));

    assertEquals(0, stream.count());
  }

  @Test
  public void testFindPackageChangesWithSingleInstalledPackageInOdlerVersion() throws Exception {
    final Package pkg_new = createPackage("pkg1", "1.0-2");
    final Package pkg_old = createPackage("pkg1", "1.0-1");

    final Stream<InstallPlanStep> stream = resolver().findPackageChanges(
        singleton(pkg_new), singleton(pkg_old));

    final InstallPlanStep step = stream.findFirst().get();
    assertThat(step, instanceOf(InstallPlanStep.PackageVersionChangeStep.class));

    final InstallPlanStep.PackageVersionChangeStep modification = (InstallPlanStep.PackageVersionChangeStep) step;
    assertEquals(createVersion("1.0", "1"), modification.getInstalledPackage().getVersion());
    assertEquals(createVersion("1.0", "2"), modification.getTargetPackage().getVersion());
  }

  @Test
  public void testFindPackageChangesWithMultipleInstalledPackagesNonMatching() throws Exception {
    final Package pkg_new = createPackage("pkg1", "1.0-2");

    final Stream<InstallPlanStep> stream = resolver().findPackageChanges(
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

    final Stream<InstallPlanStep> stream = resolver().findPackageChanges(
        singleton(pkg_new), Arrays.asList( //
            createPackage("pkg2", "12.0-1"), //
            createPackage("pkg3", "12.0-1"), //
            createPackage("pkg4", "12.0-1"), //
            createPackage("pkg1", "1.0-1"), //
            createPackage("pkg5", "12.0-1") //
        ));

    final InstallPlanStep step = stream.findFirst().get();
    assertThat(step, instanceOf(InstallPlanStep.PackageVersionChangeStep.class));

    final InstallPlanStep.PackageVersionChangeStep modification = (InstallPlanStep.PackageVersionChangeStep) step;
    assertEquals(createVersion("1.0", "1"), modification.getInstalledPackage().getVersion());
    assertEquals(createVersion("1.0", "2"), modification.getTargetPackage().getVersion());
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
    List<InstallPlanStep> collect = res.findPackagesToInstall(packagesToInstall, new InstallPlan()).collect(Collectors.toList());
    assertFalse(collect.isEmpty());
    assertEquals(1, collect.size());
  }

  @Test
  public void testFindPackagesToInstallWithOnePackageAlreadyPartOfInstall() {
    PackageManagerOperationResolverImpl res = new PackageManagerOperationResolverImpl(null, null);
    Collection<Package> packagesToInstall = Arrays.asList(createPackage("foo", "2.0-1"), createPackage("brazn", "3.0-1"));


    final InstallPlan installPlan = new InstallPlan();
    installPlan.getSteps().add(new InstallPlanStep.PackageInstallStep(createPackage("foo", "2.0-1")));
    installPlan.getSteps().add(new InstallPlanStep.PackageInstallStep(createPackage("bar", "2.1-4")));
    List<InstallPlanStep> collect = res.findPackagesToInstall(packagesToInstall, installPlan).collect(Collectors.toList());
    assertFalse(collect.isEmpty());
    assertEquals(1, collect.size());

    assertEquals("brazn", ((InstallPlanStep.PackageInstallStep) collect.get(0)).getPackage().getName());
  }

  @Test
  public void testIsPartOfInstallPlan() throws Exception {

    final PackageManagerOperationResolverImpl res = new PackageManagerOperationResolverImpl(null, null);

    final InstallPlan installPlan = new InstallPlan();
    installPlan.getSteps().add(new InstallPlanStep.PackageInstallStep(createPackage("foo", "2.0-1")));
    installPlan.getSteps().add(new InstallPlanStep.PackageInstallStep(createPackage("bar", "2.1-4")));

    assertTrue(res.isPartOfInstallPlan(createPackage("foo", "2.0-1"), installPlan));
    assertFalse(res.isPartOfInstallPlan(createPackage("brazn", "2.0-1"), installPlan));
    assertFalse(res.isPartOfInstallPlan(createPackage("foo", "2.1-1"), installPlan));

  }
}