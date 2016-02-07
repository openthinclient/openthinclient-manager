package org.openthinclient.pkgmgr.cucumber;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(glue = {"org.openthinclient.pkgmgr.cucumber", "cucumber.api.spring"},
      plugin = {"html:target/cucumber-report.html", "json:target/cucumber-report.json"})
public class PackageManagerCucumberTest {

   @Before
   public void setUp() throws Exception {
      System.out.println("SETUP");

   }

   @After
   public void tearDown() throws Exception {
      System.out.println("FINISHED");

   }
}
