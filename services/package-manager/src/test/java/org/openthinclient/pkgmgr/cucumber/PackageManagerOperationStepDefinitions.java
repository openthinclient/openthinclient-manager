package org.openthinclient.pkgmgr.cucumber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.pkgmgr.op.PackageManagerOperation.PackageChange;
import org.openthinclient.util.dpkg.PackageReference;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import cucumber.api.PendingException;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class PackageManagerOperationStepDefinitions {

  private final List<Package> generatedPackages;
//  @Autowired
  private PackageManager packageManager;
  private Package currentPackage;
  private PackageManagerOperation operation;

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

    Package installedPackage = new Package();
    installedPackage.setName(name);
    installedPackage.setVersion(createVersion(major, minor, debianRevision));
    installedPackage.setInstalled(true);

    generatedPackages.add(installedPackage);
    packageRepository.saveAndFlush(installedPackage);

  }  
  
  @And("^dependency to ([-_+A-Za-z0-9]*) version (\\d+)\\.(\\d+)-(\\d+)$")
  public void dependencyTo(String name, int major, int minor, int debianRevision) throws Throwable {

    final Version version = createVersion(major, minor, debianRevision);
    currentPackage.getDepends().add(new PackageReference.SingleReference(name, PackageReference.Relation.EQUAL, version));
  }

  @And("^dependency to ([-_+A-Za-z0-9]*)$")
  public void dependencyTo(String name) throws Throwable {
    final Version version = new Version();
    currentPackage.getDepends().add(new PackageReference.SingleReference(name, PackageReference.Relation.EQUAL, version));
  }
  
  @And("^conflicts to ([-_+A-Za-z0-9]*) version (\\d+)\\.(\\d+)-(\\d+)$")
  public void conflictsTo(String name, int major, int minor, int debianRevision) throws Throwable {
    final Version version = createVersion(major, minor, debianRevision);
    currentPackage.getConflicts().add(new PackageReference.SingleReference(name, PackageReference.Relation.EQUAL, version));
  }

  
  @And("^conflicts to ([-_+A-Za-z0-9]*)$")
  public void conflictsTo(String name) throws Throwable {
    final Version version = new Version();
    currentPackage.getConflicts().add(new PackageReference.SingleReference(name, PackageReference.Relation.EQUAL, version));
  }  

  @And("^provides ([-_+A-Za-z0-9]*)$")
  public void provides(String name) throws Throwable {
    final Version version = new Version();
    currentPackage.getProvides().add(new PackageReference.SingleReference(name, PackageReference.Relation.EQUAL, version));
  } 
  
  @And("^replaces ([-_+A-Za-z0-9]*)$")
  public void replaces(String name) throws Throwable {
    final Version version = new Version();
    currentPackage.getProvides().add(new PackageReference.SingleReference(name, PackageReference.Relation.EQUAL, version));
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

  @Then("^dependencies is empty$")
  public void dependenciesIsEmpty() throws Throwable {
    assertTrue("Expected no dependencies, but found " + operation.getDependencies(), operation.getDependencies().isEmpty());
  }

  @And("^suggested is empty$")
  public void suggestedIsEmpty() throws Throwable {
    assertTrue(operation.getSuggested().isEmpty());
  }
  
  @And("^conflicts is empty$")
  public void conflictsIsEmpty() throws Throwable {
    assertTrue(operation.getConflicts().isEmpty());
  }  

  @When("^start new operation$")
  public void startNewOperation() throws Throwable {
    PackageManagerConfiguration packageManagerConfiguration = packageManagerConfigurationFactory.getObject();
    packageManager = packageManagerFactory.createPackageManager(packageManagerConfiguration);

    this.operation = packageManager.createOperation();
  }

  @And("^changes is empty$")
  public void changesIsEmpty() throws Throwable {
      assertTrue("Expected no changes, but found " + operation.getResolveState().getChanges(), operation.getResolveState().getChanges().isEmpty());
  }

  @And("^uninstalling contains ([-_+A-Za-z0-9]*) version (\\d+)\\.(\\d+)-(\\d+)$")
  public void uninstallingContains(String packageName, int major, int minor, int debianRevision) throws Throwable {

    final Package expected = getPackage(packageName, createVersion(major, minor, debianRevision)).get();
    assertNotNull(expected);
  
    assertTrue("The expected uninstall-list is empty, but excpected at least one package: " + expected.forConflictsToString(), !operation.getResolveState().getUninstalling().isEmpty());
    
    Package uninstallPackage = operation.getResolveState().getUninstalling().stream().findFirst().get();
    assertNotNull(uninstallPackage);
    
    assertTrue("The expected package version is not identical.", hasPackagesSameNameAndVersion(uninstallPackage, expected));
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
    
    assertTrue("Installation must not be empty.", !packageManager.getInstalledPackages().isEmpty());
    Optional<Package> optional = packageManager.getInstalledPackages().stream().filter(p1 -> hasPackagesSameNameAndVersion(p1, expected)).findFirst();
    assertTrue("The expected package " + expected.forConflictsToString() + " could not be found.", optional.isPresent());
  }

  @And("^changes contains update of ([-_+A-Za-z0-9]*) from (\\d+)\\.(\\d+)-(\\d+) to (\\d+)\\.(\\d+)-(\\d+)$")
  public void changesContainsUpdate(String packageName, int oldMajor, int oldMinor, int oldDebianRevision, 
                                                        int newMajor, int newMinor, int newDebianRevision) throws Throwable {
    
    final Package oldPackage = getPackage(packageName, createVersion(oldMajor, oldMinor, oldDebianRevision)).get();
    assertNotNull(oldPackage);
    
    final Package newPackage = getPackage(packageName, createVersion(newMajor, newMinor, newDebianRevision)).get();
    assertNotNull(newPackage);    
      
    assertTrue("Expected at least one change-package, but it's empty.", !operation.getResolveState().getChanges().isEmpty());
    PackageChange packageChange = operation.getResolveState().getChanges().stream().findFirst().get();
    assertNotNull(packageChange);
    assertTrue(hasPackagesSameNameAndVersion(packageChange.getInstalled(), oldPackage));
    assertTrue(hasPackagesSameNameAndVersion(packageChange.getRequested(), newPackage));
    
  }

  @Then("^dependencies contains ([-_+A-Za-z0-9]*) version (\\d+)\\.(\\d+)-(\\d+)$")
  public void dependenciesContains(String packageName, int major, int minor, int debianRevision) throws Throwable {
    
    final Package expected = getPackage(packageName, createVersion(major, minor, debianRevision)).get();
    assertNotNull(expected);
    
    assertTrue(!this.operation.getDependencies().isEmpty());
    assertEquals("Expect only one package, but found more.", 1, this.operation.getDependencies().size());
    
    Package depPackage = this.operation.getDependencies().stream().findFirst().get();   
    assertTrue(hasPackagesSameNameAndVersion(depPackage, expected));
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
