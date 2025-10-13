package org.openthinclient.pkgmgr.cucumber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.PackageManagerFactory;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.PackageRepository;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.db.SourceRepository;
import org.openthinclient.pkgmgr.db.Version;
import org.openthinclient.pkgmgr.op.InstallPlanStep.PackageInstallStep;
import org.openthinclient.pkgmgr.op.InstallPlanStep.PackageUninstallStep;
import org.openthinclient.pkgmgr.op.InstallPlanStep.PackageVersionChangeStep;
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.pkgmgr.op.PackageManagerOperation.PackageConflict;
import org.openthinclient.pkgmgr.op.PackageManagerOperation.UnresolvedDependency;
import org.openthinclient.util.dpkg.PackageReference;
import org.openthinclient.util.dpkg.PackageReference.Relation;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import cucumber.api.PendingException;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class PackageManagerOperationStepDefinitions {

  @Autowired
  ObjectFactory<PackageManagerConfiguration> packageManagerConfigurationFactory;
  @Autowired
  SourceRepository sourceRepository;
  @Autowired
  PackageRepository packageRepository;
  @Autowired
  PackageManagerFactory packageManagerFactory;

  private final List<Package> generatedPackages;

  private PackageManager packageManager;
  private Package currentPackage;
  private PackageManagerOperation operation;

  public PackageManagerOperationStepDefinitions() {
    generatedPackages = new ArrayList<>();

  }

  // pattern for package name
  final String PACKAGE = "\\p{Alpha}[-_+\\p{Alnum}]*";

  // pattern for package version
  final String VERSION = "\\d+(?:\\.\\d+(?:-\\d+)?)?";

  @Given("empty repository")
  public void emptyRepository() {
    packageRepository.deleteAll();
  }

  @Given("^installable package ("+PACKAGE+") in version ("+VERSION+")$")
  public void installablePackageNoDepsInVersion(String name, String version) throws Throwable {

    Source source = createSource();

    currentPackage = new Package();
    currentPackage.setName(name);
    currentPackage.setVersion(Version.parse(version));
    currentPackage.setInstalled(false);
    currentPackage.setSource(source);

    generatedPackages.add(currentPackage);
    packageRepository.saveAndFlush(currentPackage);

  }

  @Given("^installed package ("+PACKAGE+") in version ("+VERSION+")$")
  public void installedPackageNoDepsInVersion(String name, String version) throws Throwable {

    Package  installedPackage = packageRepository.getByName(name).stream().filter(pck ->
       pck.getVersion().equals(Version.parse(version)) && pck.getArchitecture() == null
    ).findFirst().orElse(null);
    assertNotNull("Package not in repository: "+name+" "+version, installedPackage);

    installedPackage.setInstalled(true);
    packageRepository.saveAndFlush(installedPackage);

  }

  @And("^dependency to ("+PACKAGE+") version ("+VERSION+")$")
  public void dependencyTo(String name, String versionString) throws Throwable {
    final Version version = Version.parse(versionString);
    currentPackage.getDepends().add(new PackageReference.SingleReference(name, PackageReference.Relation.EQUAL, version));
    packageRepository.saveAndFlush(currentPackage);
  }

  @And("^dependency to ("+PACKAGE+")$")
  public void dependencyTo(String name) throws Throwable {
    currentPackage.getDepends().add(new PackageReference.SingleReference(name, null, null));
    packageRepository.saveAndFlush(currentPackage);
  }

  @And("^dependency to ("+PACKAGE+") version ([<>=]*) ("+VERSION+")$")
  public void dependencyTo(String name, String relationStr, String versionString) throws Throwable {
    final Version version = Version.parse(versionString);
    Relation relation = PackageReference.Relation.getByTextualRepresentation(relationStr);
    currentPackage.getDepends().add(new PackageReference.SingleReference(name, relation, version));
    packageRepository.saveAndFlush(currentPackage);
  }

  @And("^conflicts to ("+PACKAGE+") version ("+VERSION+")$")
  public void conflictsTo(String name, String versionString) throws Throwable {
    final Version version = Version.parse(versionString);
    currentPackage.getConflicts().add(new PackageReference.SingleReference(name, PackageReference.Relation.EQUAL, version));
    packageRepository.saveAndFlush(currentPackage);
  }

  @And("^conflicts to ("+PACKAGE+") version ([<>=]*) ("+VERSION+")$")
  public void conflictsTo(String name, String relationStr, String versionString) throws Throwable {
    final Version version = Version.parse(versionString);
    Relation relation = PackageReference.Relation.getByTextualRepresentation(relationStr);
    currentPackage.getConflicts().add(new PackageReference.SingleReference(name, relation, version));
    packageRepository.saveAndFlush(currentPackage);
  }

  @And("^conflicts to ("+PACKAGE+")$")
  public void conflictsTo(String name) throws Throwable {
    currentPackage.getConflicts().add(new PackageReference.SingleReference(name, null, null));
    packageRepository.saveAndFlush(currentPackage);
  }

  @And("^provides ("+PACKAGE+")$")
  public void provides(String name) throws Throwable {
    currentPackage.getProvides().add(new PackageReference.SingleReference(name, null, null));
    packageRepository.saveAndFlush(currentPackage);
  }

  @And("^replaces ("+PACKAGE+")$")
  public void replaces(String name) throws Throwable {
    currentPackage.getReplaces().add(new PackageReference.SingleReference(name, null, null));
    packageRepository.saveAndFlush(currentPackage);
  }

  @When("^install package ("+PACKAGE+") version ("+VERSION+")$")
  public void installPackageWithVersion(String name, String version) throws Throwable {
    final Package pkg = getPackage(name, Version.parse(version)).get();
    operation.install(pkg);
  }

  private Optional<Package> getPackage(String name, Version version) {
    return generatedPackages.stream().filter(
        p -> p.getName().equals(name) && p.getVersion().equals(version)).findFirst();
  }

  @And("^resolve operation$")
  public void resolveOperation() throws Throwable {
    operation.resolve();
  }

  @Deprecated
  @Then("^dependencies is empty$")
  public void dependenciesIsEmpty() throws Throwable {
    throw new PendingException("Deprecated");
//    assertTrue("Expected no dependencies, but found " + operation.getDependencies(), operation.getDependencies().isEmpty());
  }

  @And("^suggested is empty$")
  public void suggestedIsEmpty() throws Throwable {
    assertTrue(operation.getSuggested().isEmpty());
  }

  @And("^conflicts is empty$")
  public void conflictsIsEmpty() throws Throwable {
    assertTrue("Expected empty conflicts, but found: " + operation.getConflicts(), operation.getConflicts().isEmpty());
  }

  @When("^start new operation$")
  public void startNewOperation() throws Throwable {
    PackageManagerConfiguration packageManagerConfiguration = packageManagerConfigurationFactory.getObject();
    packageManager = packageManagerFactory.createPackageManager(packageManagerConfiguration);

    this.operation = packageManager.createOperation();
  }

  @And("^changes is empty$")
  public void changesIsEmpty() throws Throwable {
      assertTrue("Expected no changes, but found " + operation.getInstallPlan().getPackageVersionChangeSteps(),
                                                    !operation.getInstallPlan().getPackageVersionChangeSteps().findAny().isPresent());
  }

  @And("^uninstalling contains ("+PACKAGE+") version ("+VERSION+")$")
  public void uninstallingContains(String packageName, String version) throws Throwable {
      final Package expected = getPackage(packageName, Version.parse(version)).get();
      assertNotNull(expected);

      assertTrue("The expected uninstall-list is empty.", operation.getInstallPlan().getPackageUninstallSteps().findAny().isPresent());

      PackageUninstallStep uninstallStep = operation.getInstallPlan().getPackageUninstallSteps()
                                                    .filter(pus -> hasPackagesSameNameAndVersion(pus.getInstalledPackage(), expected))
                                                    .findAny().get();
      assertNotNull("The expected package version is not identical.", uninstallStep);

  }

  @And("^uninstall package ("+PACKAGE+") version ("+VERSION+")$")
  public void uninstallPackageVersionedVersion(String packageName, String version)  throws Throwable {

      final Package pkg = getPackage(packageName, Version.parse(version)).get();
      assertNotNull(pkg);
      operation.uninstall(pkg);
  }

  @When("^installation contains ("+PACKAGE+") version ("+VERSION+")$")
  public void installationContainsPackage(String packageName, String version)  throws Throwable {

    final Package expected = getPackage(packageName, Version.parse(version)).get();
    assertNotNull(expected);

    PackageManagerConfiguration packageManagerConfiguration = packageManagerConfigurationFactory.getObject();
    packageManager = packageManagerFactory.createPackageManager(packageManagerConfiguration);

    assertTrue("Installation must not be empty.", !operation.getInstallPlan().getSteps().isEmpty());
    Optional<PackageInstallStep> optional = operation.getInstallPlan().getPackageInstallSteps().filter(step -> hasPackagesSameNameAndVersion(step.getPackage(), expected)).findFirst();
    assertTrue("The expected: " + expected.forConflictsToString().replaceAll("\n", ",") + " could not be found.", optional.isPresent());
  }

  @When("^installation not contains ("+PACKAGE+") version ("+VERSION+")$")
  public void installationNotContainsPackage(String packageName, String version)  throws Throwable {

    final Package notExpected = getPackage(packageName, Version.parse(version)).get();
    assertNotNull(notExpected);

    PackageManagerConfiguration packageManagerConfiguration = packageManagerConfigurationFactory.getObject();
    packageManager = packageManagerFactory.createPackageManager(packageManagerConfiguration);

    Optional<PackageInstallStep> optional = operation.getInstallPlan().getPackageInstallSteps().filter(step -> hasPackagesSameNameAndVersion(step.getPackage(), notExpected)).findFirst();
    assertFalse("The NOT expected: " + notExpected.forConflictsToString().replaceAll("\n", ",") + " could be found.", optional.isPresent());
  }

  @When("^installation contains (\\d+) packages?$")
  public void installationContainsPackageSize(int size)  throws Throwable {

    PackageManagerConfiguration packageManagerConfiguration = packageManagerConfigurationFactory.getObject();
    packageManager = packageManagerFactory.createPackageManager(packageManagerConfiguration);

    assertEquals("The expected installation-step-size does not fit:", size, operation.getInstallPlan().getPackageInstallSteps().count());
  }

  @When("^changes contains (\\d+) packages?$")
  public void changesContainsPackageSize(int size) throws Throwable {

    PackageManagerConfiguration packageManagerConfiguration = packageManagerConfigurationFactory.getObject();
    packageManager = packageManagerFactory.createPackageManager(packageManagerConfiguration);

    assertEquals("The expected changes-step-size does not fit:", size, operation.getInstallPlan().getPackageVersionChangeSteps().count());
  }

  @And("^changes contains update of ("+PACKAGE+") from ("+VERSION+") to ("+VERSION+")$")
  public void changesContainsUpdate(String packageName, String oldVersion, String newVersion) throws Throwable {

    final Package oldPackage = getPackage(packageName, Version.parse(oldVersion)).get();
    assertNotNull(oldPackage);

    final Package newPackage = getPackage(packageName, Version.parse(newVersion)).get();
    assertNotNull(newPackage);

    assertTrue("Expected at least one package to change, but packageVersionChangeSteps are empty.", operation.getInstallPlan().getPackageVersionChangeSteps().findAny().isPresent());
    PackageVersionChangeStep changeStep = operation.getInstallPlan().getPackageVersionChangeSteps()
                                                   .filter(ics -> hasPackagesSameNameAndVersion(ics.getInstalledPackage(), oldPackage))
                                                   .filter(ics -> hasPackagesSameNameAndVersion(ics.getTargetPackage(), newPackage))
                                                   .findAny().orElse(null);
    assertNotNull("missing update of " + packageName + " from " + Version.parse(oldVersion) + " to " + Version.parse(newVersion),
                  changeStep);
  }

  @Then("^conflicts contains ("+PACKAGE+") ("+VERSION+") to ("+PACKAGE+") ("+VERSION+")$")
  public void conflictsContainsPackage(String sourcePackageName, String sourceVersion,
                                       String conflictingPackageName, String conflictingVersion) throws Throwable {

    final Package sourcePackage = getPackage(sourcePackageName, Version.parse(sourceVersion)).get();
    assertNotNull(sourcePackage);

    final Package conflictingPackage = getPackage(conflictingPackageName, Version.parse(conflictingVersion)).get();
    assertNotNull(conflictingPackage);

    assertTrue("Expected at least one package that conflicts.", !operation.getConflicts().isEmpty());
    PackageConflict conflict = operation.getConflicts().stream().filter(c -> hasPackagesSameNameAndVersion(sourcePackage, c.getSource())).findFirst().get();
    assertNotNull(conflict);
    assertTrue(hasPackagesSameNameAndVersion(conflict.getConflicting(), conflictingPackage));

  }

  @Then("^installation is empty$")
  public void installationIsEmpty() throws Throwable {
    assertTrue("Expected empty installation, but found: " + operation.getInstallPlan().getPackageInstallSteps(), !operation.getInstallPlan().getPackageInstallSteps().findAny().isPresent());
  }

  @Then("^uninstalling is empty$")
  public void ininstallingIsEmpty() throws Throwable {
    assertTrue("Expected empty uninstallation, but found: " + operation.getInstallPlan().getPackageUninstallSteps(), !operation.getInstallPlan().getPackageUninstallSteps().findAny().isPresent());
  }

  @And("^unresolved is empty$")
  public void unresolvedIsEmpty() throws Throwable {
    assertTrue("Expected empty unresolved, but found: " + operation.getUnresolved(), operation.getUnresolved().isEmpty());
  }

  @And("^unresolved contains ("+PACKAGE+")$")
  public void unresolvedContains(String packageName) throws Throwable {
    PackageReference.SingleReference expected = new PackageReference.SingleReference(packageName, null, null);
    Package package1 = new Package();
    package1.setName(packageName);
    package1.setVersion((Version) null);
    assertNotNull(expected);

    assertTrue(!this.operation.getUnresolved().isEmpty());
    assertEquals("Expect only one package, but found more.", 1, this.operation.getUnresolved().size());

    UnresolvedDependency unresolvedDependency = this.operation.getUnresolved().stream().findFirst().get();
    assertTrue(unresolvedDependency.getMissing().matches(package1));
  }

  @Then("^unresolved contains ("+PACKAGE+") ("+VERSION+") misses ("+PACKAGE+") ("+VERSION+")$")
  public void unresolvedContains(String sourcePackageName, String sourceVersion,
                                 String missingPackageName, String missingVersion) throws Throwable {

    final Package sourcePackage = getPackage(sourcePackageName, Version.parse(sourceVersion)).get();
    assertNotNull(sourcePackage);

    final Package missingPackage = getPackage(missingPackageName, Version.parse(missingVersion)).get();
    assertNotNull(missingPackage);

    assertTrue("Expected at least one package that has unresolved dependencies.", !operation.getUnresolved().isEmpty());
    UnresolvedDependency unresolvedDependency = operation.getUnresolved().stream().filter(c -> hasPackagesSameNameAndVersion(sourcePackage, c.getSource())).findFirst().get();
    assertNotNull(unresolvedDependency);
    assertTrue(unresolvedDependency.getMissing().matches(missingPackage));
  }

  /**
   * Return true is packages has same name and version
   * @param p1 Package
   * @param p2 Package
   * @return true if packages has same name and version
   */
  private boolean hasPackagesSameNameAndVersion(Package p1, Package p2) {

    if (p1 != null && p2 != null &&
        p1.getName().equals(p2.getName()) &&
        p1.getVersion().equals(p2.getVersion())) {
        return true;
    }
    return false;
  }

  /**
   * Creates a Source for packages
   * @return the source
   */
   private Source createSource() {
       Source source = new Source();
       source.setDescription("description");
       source.setEnabled(true);
       try {
         source.setUrl(new URL("http://localhost"));
       } catch (MalformedURLException exception) {
          exception.printStackTrace();
       }
       sourceRepository.saveAndFlush(source);
       return source;
   }
}
