package org.openthinclient.wizard.install;

import org.junit.Test;
import org.openthinclient.db.DatabaseConfiguration;
import org.openthinclient.db.conf.DataSourceConfiguration;
import org.openthinclient.pkgmgr.db.SourceRepository;
import org.openthinclient.service.common.home.impl.DefaultManagerHome;
import org.openthinclient.wizard.model.DatabaseModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.Assert.assertTrue;

//@RunWith(SpringJUnit4ClassRunner.class)
//@SpringApplicationConfiguration(classes = PrepareDatabaseInstallStepTest.RootTestContext.class)
public class PrepareDatabaseInstallStepTest {

   //   @Autowired
   //   ApplicationContext applicationContext;

   @Test
   public void testInstall() throws Exception {

      final Path targetDir = prepareTestInstallationDirectory();

      final DefaultManagerHome home = new DefaultManagerHome(targetDir.toFile());

      final InstallContext installContext = new InstallContext();
      installContext.setManagerHome(home);

      final DatabaseModel model = new DatabaseModel();

      model.getDatabaseConfiguration().setType(DatabaseConfiguration.DatabaseType.H2);

      final PrepareDatabaseInstallStep step = new PrepareDatabaseInstallStep(model);

      step.execute(installContext);

      // verify that the database configuration has been written
      assertTrue(Files.isRegularFile(targetDir.resolve("db.xml")));

      final SourceRepository sourceRepository = installContext.getPackageManager().getSourceRepository();

      sourceRepository.findAll();

      final Connection connection = DriverManager.getConnection(DataSourceConfiguration.createH2DatabaseUrl(installContext.getManagerHome()));
      // ensure that the otc_source table exists.
      connection.createStatement().executeQuery("SELECT * FROM otc_source");

      connection.close();
   }

   private Path prepareTestInstallationDirectory() throws IOException {
      final Path testDataDirectory = Paths.get("target", "test-data");
      Files.createDirectories(testDataDirectory);
      return Files.createTempDirectory(testDataDirectory, getClass().getSimpleName());
   }

}
