package org.openthinclient.pkgmgr.cucumber;

import cucumber.api.PendingException;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.Version;
import org.openthinclient.pkgmgr.op.PackageManagerOperation;
import org.openthinclient.util.dpkg.PackageReference;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PackageManagerOperationStepDefinitions {

  private final List<Package> generatedPackages;
  @Autowired
  PackageManager packageManager;
  private Package currentPackage;
  private PackageManagerOperation operation;

  public PackageManagerOperationStepDefinitions() {
    generatedPackages = new ArrayList<>();
  }

  @Given("^package ([-_+A-Za-z0-9]*) in version (\\d+)\\.(\\d+)-(\\d+)$")
  public void packageNoDepsInVersion(String name, int major, int minor, int debianRevision) throws Throwable {

    currentPackage = new Package();
    currentPackage.setName(name);
    currentPackage.setVersion(createVersion(major, minor, debianRevision));

    generatedPackages.add(currentPackage);
  }

  @And("^dependency to ([-_+A-Za-z0-9]*) version (\\d+)\\.(\\d+)-(\\d+)$")
  public void dependencyTo(String name, int major, int minor, int debianRevision) throws Throwable {

    final Version version = createVersion(major, minor, debianRevision);
    currentPackage.getDepends().add(new PackageReference.SingleReference(name, PackageReference.Relation.EQUAL,
        version));
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
    assertTrue(operation.getDependencies().isEmpty());
  }

  @And("^suggested is empty$")
  public void suggestedIsEmpty() throws Throwable {
    assertTrue(operation.getSuggested().isEmpty());
  }

  @When("^start new operation$")
  public void startNewOperation() throws Throwable {
    this.operation = packageManager.createOperation();
  }

  @And("^changes is empty$")
  public void changesIsEmpty() throws Throwable {
    fail();
  }

  @And("^uninstalling contains ([-_+A-Za-z0-9]*) version (\\d+)\\.(\\d+)-(\\d+)$")
  public void uninstallingContains(String packageName, int major, int minor, int debianRevision) throws Throwable {
    // Write code here that turns the phrase above into concrete actions
    throw new PendingException();
  }

  @And("^uninstall package ([-_+A-Za-z0-9]*) version (\\d+)\\.(\\d+)-(\\d+)$")
  public void uninstallPackageVersionedVersion(String packageName, int major, int minor, int debianRevision)
      throws Throwable {
    // Write code here that turns the phrase above into concrete actions
    throw new PendingException();
  }

  @When("^installation contains ([-_+A-Za-z0-9]*) version (\\d+)\\.(\\d+)-(\\d+)$")
  public void installationContainsPackage(String packageName, int major, int minor, int debianRevision)
      throws Throwable {
    // Write code here that turns the phrase above into concrete actions
    throw new PendingException();
  }

  @And("^changes contains update of ([-_+A-Za-z0-9]*) from (\\d+)\\.(\\d+)-(\\d+) to (\\d+)\\.(\\d+)-(\\d+)$")
  public void changesContainsUpdate(String packageName, int oldMajor, int oldMinor, int oldDebianRevision, int newMajor,
      int newMinor, int newDebianRevision)
      throws Throwable {
    // Write code here that turns the phrase above into concrete actions
    throw new PendingException();
  }

  @Then("^dependencies contains no-deps version (\\d+)\\.(\\d+)-(\\d+)$")
  public void dependenciesContains(String packageName, int oldMajor, int oldMinor, int oldDebianRevision, int newMajor,
      int newMinor, int newDebianRevision) throws Throwable {
    // Write code here that turns the phrase above into concrete actions
    throw new PendingException();
  }
}
