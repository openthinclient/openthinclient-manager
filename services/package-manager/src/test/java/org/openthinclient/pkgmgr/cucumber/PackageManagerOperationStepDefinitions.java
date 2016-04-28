package org.openthinclient.pkgmgr.cucumber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.PackageManagerFactory;
import org.openthinclient.pkgmgr.db.InstallationLogEntryRepository;
import org.openthinclient.pkgmgr.db.InstallationRepository;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.PackageRepository;
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

  private final List<Package> generatedPackages;
  @Autowired
  ObjectFactory<PackageManagerConfiguration> packageManagerConfigurationFactory;
  @Autowired
  SourceRepository repo;
  @Autowired
  PackageRepository packageRepository;
  @Autowired
  PackageManagerFactory packageManagerFactory;
  @Autowired
  InstallationLogEntryRepository installationLogEntryRepository;
  @Autowired
  InstallationRepository installationRepository;
  //  @Autowired
  private PackageManager packageManager;
  private Package currentPackage;
  private PackageManagerOperation operation;
  
  public PackageManagerOperationStepDefinitions() {
    generatedPackages = new ArrayList<>();
  }

  @Given("^installable package ([-_+A-Za-z0-9]*) in version (\\d+)\\.(\\d+)-(\\d+)$")
  public void installablePackageNoDepsInVersion(String name, int major, int minor, int debianRevision) throws Throwable {

    currentPackage = new Package();
    currentPackage.setName(name);
    currentPackage.setVersion(createVersion(major, minor, debianRevision));
    currentPackage.setInstalled(false);

    generatedPackages.add(currentPackage);
    packageRepository.saveAndFlush(currentPackage);

  }

  @Given("^installed package ([-_+A-Za-z0-9]*) in version (\\d+)\\.(\\d+)-(\\d+)$")
  public void installedPackageNoDepsInVersion(String name, int major, int minor, int debianRevision) throws Throwable {

    Version version = createVersion(major, minor, debianRevision);
    Package  installedPackage = packageRepository.getByName(name).stream().filter(pck ->  
       pck.getVersion().equals(version) && pck.getArchitecture() == null
    ).findFirst().get();
    assertNotNull(installedPackage);
    
    installedPackage.setInstalled(true);
    packageRepository.saveAndFlush(installedPackage);

  }  
  
  @And("^dependency to ([-_+A-Za-z0-9]*) version (\\d+)\\.(\\d+)-(\\d+)$")
  public void dependencyTo(String name, int major, int minor, int debianRevision) throws Throwable {

    final Version version = createVersion(major, minor, debianRevision);
    currentPackage.getDepends().add(new PackageReference.SingleReference(name, PackageReference.Relation.EQUAL, version));
    packageRepository.saveAndFlush(currentPackage);
  }

  @And("^dependency to ([-_+A-Za-z0-9]*)$")
  public void dependencyTo(String name) throws Throwable {
    currentPackage.getDepends().add(new PackageReference.SingleReference(name, null, null));
    packageRepository.saveAndFlush(currentPackage);
  }
  
  @And("^dependency to ([-_+A-Za-z0-9]*) version ([<>=]*) (\\d+)\\.(\\d+)-(\\d+)$")
  public void dependencyTo(String name, String relationStr, int major, int minor, int debianRevision) throws Throwable {
    final Version version = createVersion(major, minor, debianRevision);
    Relation relation = PackageReference.Relation.getByTextualRepresentation(relationStr);
    currentPackage.getDepends().add(new PackageReference.SingleReference(name, relation, version));
    packageRepository.saveAndFlush(currentPackage);
  }    
  
  @And("^conflicts to ([-_+A-Za-z0-9]*) version (\\d+)\\.(\\d+)-(\\d+)$")
  public void conflictsTo(String name, int major, int minor, int debianRevision) throws Throwable {
    final Version version = createVersion(major, minor, debianRevision);
    currentPackage.getConflicts().add(new PackageReference.SingleReference(name, PackageReference.Relation.EQUAL, version));
    packageRepository.saveAndFlush(currentPackage);
  }

  @And("^conflicts to ([-_+A-Za-z0-9]*) version ([<>=]*) (\\d+)\\.(\\d+)-(\\d+)$")
  public void conflictsTo(String name, String relationStr, int major, int minor, int debianRevision) throws Throwable {
    final Version version = createVersion(major, minor, debianRevision);
    Relation relation = PackageReference.Relation.getByTextualRepresentation(relationStr);
    currentPackage.getConflicts().add(new PackageReference.SingleReference(name, relation, version));
    packageRepository.saveAndFlush(currentPackage);
  }  
  
  @And("^conflicts to ([-_+A-Za-z0-9]*)$")
  public void conflictsTo(String name) throws Throwable {
    currentPackage.getConflicts().add(new PackageReference.SingleReference(name, null, null));
    packageRepository.saveAndFlush(currentPackage);
  }  

  @And("^provides ([-_+A-Za-z0-9]*)$")
  public void provides(String name) throws Throwable {
    currentPackage.getProvides().add(new PackageReference.SingleReference(name, null, null));
    packageRepository.saveAndFlush(currentPackage);
  } 
  
  @And("^replaces ([-_+A-Za-z0-9]*)$")
  public void replaces(String name) throws Throwable {
    currentPackage.getReplaces().add(new PackageReference.SingleReference(name, null, null));
    packageRepository.saveAndFlush(currentPackage);
  }      
  
  @And("^suggests ([-_+A-Za-z0-9]*)$")
  public void suggests(String name) throws Throwable {
    currentPackage.getSuggests().add(new PackageReference.SingleReference(name, null, null));
    packageRepository.saveAndFlush(currentPackage);
  }   
  
  private Version createVersion(int major, int minor, int debianRevision) {
    final Version version = new Version();
    version.setUpstreamVersion(major + "." + minor);
    version.setDebianRevision("" + debianRevision);
    return version;
  }

  @When("^install package ([-_+A-Za-z0-9]*) version (\\d+)\\.(\\d+)-(\\d+)$")
  public void installPackageWithVersion(String name, int major, int minor, int debianRevision) throws Throwable {

    final Package pkg = getPackage(name, createVersion(major, minor, debianRevision)).get();

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

  @And("^uninstalling contains ([-_+A-Za-z0-9]*) version (\\d+)\\.(\\d+)-(\\d+)$")
  public void uninstallingContains(String packageName, int major, int minor, int debianRevision) throws Throwable {
      final Package expected = getPackage(packageName, createVersion(major, minor, debianRevision)).get();
      assertNotNull(expected);
  
      assertTrue("The expected uninstall-list is empty.", operation.getInstallPlan().getPackageUninstallSteps().findAny().isPresent());
  
      PackageUninstallStep uninstallStep = operation.getInstallPlan().getPackageUninstallSteps()
                                                    .filter(pus -> hasPackagesSameNameAndVersion(pus.getInstalledPackage(), expected))
                                                    .findAny().get();
      assertNotNull("The expected package version is not identical.", uninstallStep);
      
  }

  @And("^uninstall package ([-_+A-Za-z0-9]*) version (\\d+)\\.(\\d+)-(\\d+)$")
  public void uninstallPackageVersionedVersion(String packageName, int major, int minor, int debianRevision)  throws Throwable {

      final Package pkg = getPackage(packageName, createVersion(major, minor, debianRevision)).get();
      assertNotNull(pkg);
      operation.uninstall(pkg);
  }

  @When("^installation contains ([-_+A-Za-z0-9]*) version (\\d+)\\.(\\d+)-(\\d+)$")
  public void installationContainsPackage(String packageName, int major, int minor, int debianRevision)  throws Throwable {
    
    final Package expected = getPackage(packageName, createVersion(major, minor, debianRevision)).get();
    assertNotNull(expected);
   
    PackageManagerConfiguration packageManagerConfiguration = packageManagerConfigurationFactory.getObject();
    packageManager = packageManagerFactory.createPackageManager(packageManagerConfiguration);
    
    assertTrue("Installation must not be empty.", !operation.getInstallPlan().getSteps().isEmpty());
    Optional<PackageInstallStep> optional = operation.getInstallPlan().getPackageInstallSteps().filter(step -> hasPackagesSameNameAndVersion(step.getPackage(), expected)).findFirst();
    assertTrue("The expected: " + expected.forConflictsToString().replaceAll("\n", ",") + " could not be found.", optional.isPresent());
  }
  
  @When("^installation not contains ([-_+A-Za-z0-9]*) version (\\d+)\\.(\\d+)-(\\d+)$")
  public void installationNotContainsPackage(String packageName, int major, int minor, int debianRevision)  throws Throwable {
    
    final Package notExpected = getPackage(packageName, createVersion(major, minor, debianRevision)).get();
    assertNotNull(notExpected);
   
    PackageManagerConfiguration packageManagerConfiguration = packageManagerConfigurationFactory.getObject();
    packageManager = packageManagerFactory.createPackageManager(packageManagerConfiguration);

    Optional<PackageInstallStep> optional = operation.getInstallPlan().getPackageInstallSteps().filter(step -> hasPackagesSameNameAndVersion(step.getPackage(), notExpected)).findFirst();
    assertFalse("The NOT expected: " + notExpected.forConflictsToString().replaceAll("\n", ",") + " could be found.", optional.isPresent());
  }  
  
  @When("^installation contains (\\d+) packages$")
  public void installationContainsPackageSize(int size)  throws Throwable {
    
    PackageManagerConfiguration packageManagerConfiguration = packageManagerConfigurationFactory.getObject();
    packageManager = packageManagerFactory.createPackageManager(packageManagerConfiguration);

    assertEquals("The expected installation-step-size does not fit:", size, operation.getInstallPlan().getPackageInstallSteps().count());
  }  
  
  @And("^changes contains update of ([-_+A-Za-z0-9]*) from (\\d+)\\.(\\d+)-(\\d+) to (\\d+)\\.(\\d+)-(\\d+)$")
  public void changesContainsUpdate(String packageName, int oldMajor, int oldMinor, int oldDebianRevision, 
                                                        int newMajor, int newMinor, int newDebianRevision) throws Throwable {

    final Package oldPackage = getPackage(packageName, createVersion(oldMajor, oldMinor, oldDebianRevision)).get();
    assertNotNull(oldPackage);

    final Package newPackage = getPackage(packageName, createVersion(newMajor, newMinor, newDebianRevision)).get();
    assertNotNull(newPackage);

    assertTrue("Expected at least one package to change, but packageVersionChangeSteps are empty.", operation.getInstallPlan().getPackageVersionChangeSteps().findAny().isPresent());
    PackageVersionChangeStep changeStep = operation.getInstallPlan().getPackageVersionChangeSteps()
                                                   .filter(ics -> hasPackagesSameNameAndVersion(ics.getInstalledPackage(), oldPackage))
                                                   .filter(ics -> hasPackagesSameNameAndVersion(ics.getTargetPackage(), newPackage))
                                                   .findAny().get();
    assertNotNull(changeStep);
    
  }

  @Then("^conflicts contains ([-_+A-Za-z0-9]*) (\\d+)\\.(\\d+)-(\\d+) to ([-_+A-Za-z0-9]*) (\\d+)\\.(\\d+)-(\\d+)$")
  public void conflictsContainsPackage(String sourcePackageName, int sourceMajor, int sourceMinor, int sourceDebianRevision, 
                                       String conflictingPackageName, int conflictingMajor, int conflictingMinor, int conflictingDebianRevision) throws Throwable {

    final Package sourcePackage = getPackage(sourcePackageName, createVersion(sourceMajor, sourceMinor, sourceDebianRevision)).get();
    assertNotNull(sourcePackage);

    final Package conflictingPackage = getPackage(conflictingPackageName, createVersion(conflictingMajor, conflictingMinor, conflictingDebianRevision)).get();
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
  
  @And("^unresolved is empty$")
  public void unresolvedIsEmpty() throws Throwable {
    assertTrue("Expected empty unresolved, but found: " + operation.getUnresolved(), operation.getUnresolved().isEmpty());
  }
  
  @And("^unresolved contains ([-_+A-Za-z0-9]*)$")
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
}
