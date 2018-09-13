package org.openthinclient.db.conf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.db.DatabaseConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = LocalH2TestConfig.class)
public class DatabaseSetupTest {

    @Autowired
    DataSource dataSource;
    @Value("${application.version}")
    private String projectVersion;

    @Test
    public void testOpenthinclientManagerVersionPresent() throws Exception {

      final Connection connection = dataSource.getConnection();
      ResultSet resultSet = connection.createStatement().executeQuery("SELECT version_upstream FROM otc_package WHERE name='openthinclient-manager-version'");
      assertNotNull(resultSet);
      resultSet.next();
      assertEquals("Expected project-version not found at package-entry", projectVersion, resultSet.getString(1));
    }


    @Test
    public void testSetupAndTablesPresent() throws Exception {

        final Connection connection = dataSource.getConnection();
        connection.createStatement().executeQuery("SELECT * FROM otc_package");
    }
    
    @Test
    public void testAutoincrement() throws Exception {
      
      final Connection connection = dataSource.getConnection();
      ResultSet resultSet = connection.createStatement().executeQuery("SELECT count(1) FROM otc_source");
      assertNotNull(resultSet);
      resultSet.next();
      assertEquals(1, resultSet.getInt(1));
      
      String driverClassName = ((org.apache.tomcat.jdbc.pool.DataSource) dataSource).getDriverClassName();
      
      if (driverClassName.equals(DatabaseConfiguration.DatabaseType.H2.getDriverClassName())) {
        connection.createStatement().executeUpdate("INSERT INTO otc_source (`enabled`,`description`,`url`,`last_updated`,`status`) VALUES ('1','description', 'url', CURRENT_TIMESTAMP(), 'ENABLED')");
      }
      
      if (driverClassName.equals(DatabaseConfiguration.DatabaseType.APACHE_DERBY.getDriverClassName())) {
        connection.createStatement().executeUpdate("INSERT INTO otc_source (enabled,description,url,last_updated,status) VALUES (true,'description', 'url', CURRENT_TIMESTAMP, 'ENABLED')");
      }      
      
      if (driverClassName.equals(DatabaseConfiguration.DatabaseType.MYSQL.getDriverClassName())) {
        connection.createStatement().executeUpdate("INSERT INTO otc_source (`enabled`,`description`,`url`,`last_updated`,`status`) VALUES (b'1','description', 'url', now(), 'ENABLED')");
      }
      
      resultSet = connection.createStatement().executeQuery("SELECT count(1) FROM otc_source");
      assertNotNull(resultSet);
      resultSet.next();
      assertEquals(2, resultSet.getInt(1));
    }

    /**
     * This test ensures that the database is correctly initialized by liquibase. If, for some
     * reason, hibernate decides to do the initialization, description will be of type varchar
     * instead of CLOB
     * NOTE: MySQL does not support CLOB datatype, instead MySQL uses LONGTEXT
     */
    @Test
    public void testDescriptionFieldIsCLOB() throws Exception {

        final Connection connection = dataSource.getConnection();

        final ResultSet columns = connection.getMetaData().getColumns(null, null, "OTC_PACKAGE", "DESCRIPTION");

        assertTrue(columns.next());

        String driverClassName = ((org.apache.tomcat.jdbc.pool.DataSource) dataSource).getDriverClassName();
        
        if (driverClassName.equals(DatabaseConfiguration.DatabaseType.H2.getDriverClassName()) ||
            driverClassName.equals(DatabaseConfiguration.DatabaseType.APACHE_DERBY.getDriverClassName())) {
          
          assertEquals("CLOB", columns.getString("TYPE_NAME"));
        }
        
        if (driverClassName.equals(DatabaseConfiguration.DatabaseType.MYSQL.getDriverClassName())) {
          assertEquals("LONGTEXT", columns.getString("TYPE_NAME"));
        }
    }
}
