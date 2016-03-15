package org.openthinclient.pkgmgr.cucumber;

import org.openthinclient.pkgmgr.DebianTestRepositoryServer;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.PackageManagerFactory;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.PackageRepository;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.db.SourceRepository;
import org.openthinclient.pkgmgr.it.PackageInstallTest;
import org.openthinclient.pkgmgr.op.PackageListUpdateReport;
import org.openthinclient.pkgmgr.progress.ListenableProgressFuture;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SpringApplicationConfiguration(classes={ PackageInstallTest.PackageManagerConfig.class,
      PackageManagerStepDefinitions.MyConfig.class})
public class PackageManagerStepDefinitions {

   @Autowired
   ObjectFactory<PackageManagerConfiguration> packageManagerConfigurationFactory;
   @Autowired
   SourceRepository repo;

   @Autowired
   PackageRepository packageRepository;
   @Autowired
   PackageManagerFactory packageManagerFactory;

   PackageManagerConfiguration packageManagerConfiguration;
   PackageManager packageManager;

   @Before
   public void createPackageManagerInstance() throws Exception {

      packageManagerConfiguration = packageManagerConfigurationFactory.getObject();

      repo.deleteAll();

      final Source source = new Source();
      source.setEnabled(true);
       // FIXME is there a better way to provide the repository server instance?
       final DebianTestRepositoryServer server = PackageManagerCucumberTest.TEST_REPOSITORY_SERVER;
       source.setUrl(server.getServerUrl());

      repo.save(source);
      final PackageManager packageManager = packageManagerFactory.createPackageManager(packageManagerConfiguration);



      assertNotNull("failed to create package manager instance", packageManager);
      assertNotNull("sources-list could not be loaded", packageManager.getSourcesList());
      assertEquals("number of entries in sources list is not correct", 1,
            packageManager.getSourcesList().getSources().size());
      assertEquals("wrong URL of repository", server.getServerUrl(),
            packageManager.getSourcesList().getSources().get(0).getUrl());

      //assertEquals(0, packageManager.findByInstalledFalse().size());
      //assertEquals(0, packageManager.findByInstalledFalse().size());
      final ListenableProgressFuture<PackageListUpdateReport> updateFuture = packageManager.updateCacheDB();

      assertNotNull("couldn't update cache-DB", updateFuture.get());
      assertEquals("wrong number of installables packages", 4, packageManager.getInstallablePackages().size());

      this.packageManager = packageManager;
   }

   @Given("^empty manager home$")
   public void empty_manager_home() throws Throwable {
      assertTrue(Files.list(packageManagerConfiguration.getInstallDir().toPath()).count() == 0);
   }

   private void print(String msg) {
      System.out.println("["
            + " / PackageManagerStepDefinitions@" + System.identityHashCode(this) + msg);
      System.out.flush();
   }

   @When("^install package ([^\\s]*)$")
   public void install_package(String packageName) throws Throwable {

      final Optional<Package> testPackage = packageManager.getInstallablePackages().stream().filter(
            pkg -> pkg.getName().equals(packageName)).findFirst();

      assertTrue("package " + packageName + " could not be found", testPackage.isPresent());

      final Package pkg = testPackage.get();

      // FIXME
      fail("Missing install implementation");
//      assertTrue(packageManager.install(Arrays.asList(pkg)));

   }

   @Then("^manager home contains file ([^\\s]*)$")
   public void manager_home_contains_file(String path) throws Throwable {
      final Path rootPath = packageManagerConfiguration.getInstallDir().toPath();

      final Path expectedChild = rootPath.resolve(path);

      assertTrue("Expected '" + expectedChild
            + "' to be a file", Files.isRegularFile(expectedChild));

   }

   @Then("^manager home contains file ([^\\s]*) with md5 sum ([^\\s]*)$")
   public void manager_home_contains_file(String path, String md5) throws Throwable {
      print("expect file: " + path + " with md5 " + md5);
   }

   @Configuration
   public static class MyConfig {


   }
}
