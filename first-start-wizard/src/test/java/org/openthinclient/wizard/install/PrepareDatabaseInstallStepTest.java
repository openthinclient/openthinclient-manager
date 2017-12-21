package org.openthinclient.wizard.install;

import org.junit.Test;
import org.openthinclient.api.context.InstallContext;
import org.openthinclient.db.DatabaseConfiguration;
import org.openthinclient.db.conf.DataSourceConfiguration;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.service.common.home.impl.DefaultManagerHome;
import org.openthinclient.wizard.model.DatabaseModel;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PrepareDatabaseInstallStepTest {

  @Test
  public void testInstallDerby() throws Exception {
    
     final Path targetDir = prepareTestInstallationDirectory();

     final DefaultManagerHome home = new DefaultManagerHome(targetDir.toFile());

     final InstallContext installContext = new InstallContext();
     installContext.setManagerHome(home);

     String derbyDatabaseUrl = DataSourceConfiguration.createApacheDerbyDatabaseUrl(installContext.getManagerHome());
     final DatabaseModel model = new DatabaseModel();

     model.setType(DatabaseConfiguration.DatabaseType.APACHE_DERBY);

     final PrepareDatabaseInstallStep step = new PrepareDatabaseInstallStep(model);
     step.execute(installContext);

     // verify that the database configuration has been written
     assertTrue(Files.isRegularFile(targetDir.resolve("db.xml")));

     DataSource ds = (DataSource) installContext.getContext().getBean("dataSource");
     Connection conn = ds.getConnection();
     System.err.println("connection closed: " + conn.isClosed());
     conn.createStatement().executeQuery("SELECT * FROM otc_source");
     
     Collection<Package> installedPackages = installContext.getPackageManager().getInstalledPackages();
     
//     final SourceRepository sourceRepository = installContext.getPackageManager().getSourceRepository();
//     sourceRepository.findAll();
     installContext.getPackageManager().findAllSources();

     final Connection connection = DriverManager.getConnection(derbyDatabaseUrl, "sa", "");
     // ensure that the otc_source table exists.
     connection.createStatement().executeQuery("SELECT * FROM otc_source");

     connection.close();
  }
  
  
   @Test
   public void testInstallH2() throws Exception {
     
      final Path targetDir = prepareTestInstallationDirectory();

      final DefaultManagerHome home = new DefaultManagerHome(targetDir.toFile());

      final InstallContext installContext = new InstallContext();
      installContext.setManagerHome(home);

      final DatabaseModel model = new DatabaseModel();

      model.setType(DatabaseConfiguration.DatabaseType.H2);

      final PrepareDatabaseInstallStep step = new PrepareDatabaseInstallStep(model);
      step.execute(installContext);

      // verify that the database configuration has been written
      assertTrue(Files.isRegularFile(targetDir.resolve("db.xml")));

//      final SourceRepository sourceRepository = installContext.getPackageManager().getSourceRepository();
//      sourceRepository.findAll();
      installContext.getPackageManager().findAllSources();

      final Connection connection = DriverManager.getConnection(DataSourceConfiguration.createH2DatabaseUrl(installContext.getManagerHome()), "sa", "");
      // ensure that the otc_source table exists.
      connection.createStatement().executeQuery("SELECT * FROM otc_source");

      connection.close();
   }

   private Path prepareTestInstallationDirectory() throws IOException {
      final Path testDataDirectory = Paths.get("target", "test-data");
      Files.createDirectories(testDataDirectory);
      return Files.createTempDirectory(testDataDirectory, getClass().getSimpleName());
   }

   @Test
   public void testCreateH2Configuration() throws Exception {

      final DatabaseModel model = new DatabaseModel();
      model.setType(DatabaseConfiguration.DatabaseType.H2);

      final DatabaseConfiguration target = new DatabaseConfiguration();
     DatabaseModel.apply(model, target);

      assertEquals("sa", target.getUsername());
      assertEquals("", target.getPassword());
      assertEquals(DatabaseConfiguration.DatabaseType.H2, target.getType());
      assertEquals(null, target.getUrl());
   }

   @Test
   public void testCreateApacheDerbyConfiguration() throws Exception {

      final DatabaseModel model = new DatabaseModel();
      model.setType(DatabaseConfiguration.DatabaseType.APACHE_DERBY);

      final DatabaseConfiguration target = new DatabaseConfiguration();
      PrepareDatabaseInstallStep.apply(target, model);

      assertEquals(DatabaseConfiguration.DatabaseType.APACHE_DERBY, target.getType());
      assertEquals(null, target.getUrl());
      assertEquals("sa", target.getUsername());
      assertEquals("", target.getPassword());
   }   
   
   
   @Test
   public void testCreateMySQLConfigurationWithDefaultPort() throws Exception {

      final DatabaseModel model = new DatabaseModel();
      model.setType(DatabaseConfiguration.DatabaseType.MYSQL);
      model.getMySQLConfiguration().setHostname("mysql-simple-host.com");
      // using the default port
      model.getMySQLConfiguration().setDatabase("otc-database");
      model.getMySQLConfiguration().setUsername("some-user");
      model.getMySQLConfiguration().setPassword("secret PAssWoRD");

      final DatabaseConfiguration target = new DatabaseConfiguration();
      DatabaseModel.apply(model, target);

      assertEquals(DatabaseConfiguration.DatabaseType.MYSQL, target.getType());
      assertEquals("some-user", target.getUsername());
      assertEquals("secret PAssWoRD", target.getPassword());
      assertEquals("jdbc:mysql://mysql-simple-host.com:3306/otc-database", target.getUrl());
   }
}
