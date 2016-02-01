package org.openthinclient.db.conf;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.db.DatabaseConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.impl.DefaultManagerHome;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = DatabaseSetupTest.LocalTestConfig.class)
public class DatabaseSetupTest {

   @Autowired
   DataSource dataSource;

   @Test
   public void testSetupAndTablesPresent() throws Exception {

      //

      final Connection connection = dataSource.getConnection();

      connection.createStatement().executeQuery("SELECT * FROM otc_package");

   }

   @Import({ DataSourceAutoConfiguration.class, DataSourceConfiguration.class, LiquibaseAutoConfiguration.class })
   @EnableAutoConfiguration
   public static class LocalTestConfig {

      @Bean
      public ManagerHome managerHome() throws Exception {

         final Path testDataDirectory = Paths.get("target", "test-data");
         Files.createDirectories(testDataDirectory);
         final Path tempDir = Files.createTempDirectory(testDataDirectory, getClass().getSimpleName());

         final DefaultManagerHome home = new DefaultManagerHome(tempDir.toFile());
         final DatabaseConfiguration dbConfig = home.getConfiguration(DatabaseConfiguration.class);
         dbConfig.setUsername("sa");
         dbConfig.setType(DatabaseConfiguration.DatabaseType.H2);
         home.save(DatabaseConfiguration.class);

         return home;
      }

   }

}
