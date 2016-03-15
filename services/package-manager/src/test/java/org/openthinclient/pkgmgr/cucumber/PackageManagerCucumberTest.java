package org.openthinclient.pkgmgr.cucumber;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.openthinclient.pkgmgr.DebianTestRepositoryServer;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(glue = {"org.openthinclient.pkgmgr.cucumber", "cucumber.api.spring"},
      plugin = {"html:target/cucumber-report.html", "json:target/cucumber-report.json"})
public class PackageManagerCucumberTest {

   @Rule
   public static final DebianTestRepositoryServer TEST_REPOSITORY_SERVER = new DebianTestRepositoryServer();

   @Before
   public void setUp() throws Exception {
      System.out.println("SETUP");

   }

   @After
   public void tearDown() throws Exception {
      System.out.println("FINISHED");

   }
}
