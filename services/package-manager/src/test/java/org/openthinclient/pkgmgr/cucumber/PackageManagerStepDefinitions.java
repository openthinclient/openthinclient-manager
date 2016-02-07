package org.openthinclient.pkgmgr.cucumber;

import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.junit.Assert;
import org.openthinclient.pkgmgr.DebianTestRepositoryServer;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.PackageManagerFactory;
import org.openthinclient.pkgmgr.db.*;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.it.PackageInstallTest;
import org.openthinclient.util.dpkg.DPKGPackageManager;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SpringApplicationConfiguration(classes={ PackageInstallTest.PackageManagerConfig.class,
      PackageManagerStepDefinitions.MyConfig.class})
public class PackageManagerStepDefinitions {

   @Configuration
   public static class MyConfig {


      @Bean(destroyMethod = "stop")
      public DebianTestRepositoryServer startRepoServer() {
         DebianTestRepositoryServer testRepositoryServer = new DebianTestRepositoryServer();
         testRepositoryServer.start();
         return testRepositoryServer;
      }

      private void configureSources(SourceRepository repository, DebianTestRepositoryServer testRepositoryServer) throws Exception {

         repository.deleteAll();

         final Source source = new Source();
         source.setEnabled(true);
         source.setUrl(testRepositoryServer.getServerUrl());

         repository.save(source);
      }

   }

   @Autowired
   ObjectFactory<PackageManagerConfiguration> packageManagerConfigurationFactory;
   @Autowired
   SourceRepository repo;
   @Autowired
   DebianTestRepositoryServer server;

   PackageManagerConfiguration packageManagerConfiguration;
   PackageManager packageManager;

   @Before
   public void createPackageManagerInstance() throws Exception {

      packageManagerConfiguration = packageManagerConfigurationFactory.getObject();

      repo.deleteAll();

      final Source source = new Source();
      source.setEnabled(true);
      source.setUrl(server.getServerUrl());

      repo.save(source);
      final DPKGPackageManager packageManager = PackageManagerFactory.createPackageManager(packageManagerConfiguration, repo);



      assertNotNull("failed to create package manager instance", packageManager);
      assertNotNull("sources-list could not be loaded", packageManager.getSourcesList());
      assertEquals("number of entries in sources list is not correct", 1,
            packageManager.getSourcesList().getSources().size());
      assertEquals("wrong URL of repository", server.getServerUrl(),
            packageManager.getSourcesList().getSources().get(0).getUrl());

      //assertEquals(0, packageManager.getInstallablePackages().size());
      assertTrue("couldn't update cache-DB", packageManager.updateCacheDB());
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

      assertTrue(packageManager.install(Arrays.asList(pkg)));

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
}
