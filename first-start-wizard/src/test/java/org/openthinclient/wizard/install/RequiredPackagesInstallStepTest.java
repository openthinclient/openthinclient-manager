package org.openthinclient.wizard.install;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.openthinclient.api.distributions.InstallableDistribution;
import org.openthinclient.api.distributions.InstallableDistributions;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.op.DefaultPackageManagerOperation;
import org.openthinclient.pkgmgr.op.InstallPlan;
import org.openthinclient.pkgmgr.op.InstallPlanStep;
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.util.dpkg.LocalPackageList;
import org.openthinclient.util.dpkg.PackageManagerOperationResolverImpl;
import org.openthinclient.util.dpkg.PackagesListParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequiredPackagesInstallStepTest {

  private static final Logger LOG = LoggerFactory.getLogger(RequiredPackagesInstallStepTest.class);

  @Test
  public void testResolvePackages() throws Exception {

    List<org.openthinclient.pkgmgr.db.Package> installable = Arrays.asList(
            createPackage("base", "2.0-12"),
            createPackage("base", "2.0-27"),
            createPackage("base", "2.0-18")
    );

    final List<Optional<Package>> result = new RequiredPackagesInstallStep(null).resolvePackages(installable, Collections.singletonList("base"));

    assertEquals(1, result.size());
    assertTrue(result.get(0).isPresent());
    assertEquals("0:2.0-27", result.get(0).get().getVersion().toString());

  }

  @Test
  public void testResolvePackagesFromDistributionsXML() throws Exception {

    // setup installable packages form Packages.txt
    final LocalPackageList localPackageList = new LocalPackageList(new Source(), new File("target/test-classes/Packages.txt"));
    List<Package> installablePackages = parsePackagesList(localPackageList).collect(Collectors.toList());
    assertEquals(36, installablePackages.size());

    // setup distribution from distribution.xml
    final InstallableDistribution distribution = InstallableDistributions.getDefaultDistributions().getPreferred();
    assertNotNull(distribution);
    assertEquals(12, distribution.getMinimumPackages().size());

    // read packages to install from distribution.xml
    RequiredPackagesInstallStep rpis = new RequiredPackagesInstallStep(distribution);
    final List<Optional<Package>> resolvedPackages = rpis.resolvePackages(installablePackages, distribution.getMinimumPackages());
    assertEquals(12, resolvedPackages.size());

    // resolving installable and dependencies fro an empty system
    LOG.info("Resolving dependencies");
    PackageManagerOperation operation = new DefaultPackageManagerOperation(new PackageManagerOperationResolverImpl(() -> Collections.EMPTY_LIST, () -> installablePackages));
    resolvedPackages.stream().map(Optional::get).forEach(operation::install);
    operation.resolve();

    // test
    InstallPlan installPlan = operation.getInstallPlan();
    List<InstallPlanStep> steps = installPlan.getSteps();
    assertEquals("Wrong number of expected packages to install. Please check the package resolving.", 18, steps.size());

  }

  private Package createPackage(String name, String version) {
    final Package pkg = new Package();
    pkg.setName(name);
    pkg.setVersion(version);
    return pkg;
  }

  private Stream<Package> parsePackagesList(LocalPackageList localPackageList) {
    LOG.info("Processing packages for {}", localPackageList.getSource().getUrl());

    try {
      return new PackagesListParser()
          .parse(Files.newInputStream(localPackageList.getPackagesFile().toPath()))
          .stream()
          .map(p -> {
            p.setSource(localPackageList.getSource());
            return p;
          });
    } catch (IOException e) {
      LOG.error("Failed to parse packages list for " + localPackageList.getSource().getUrl(), e);
      return Stream.empty();
    }
  }

}
