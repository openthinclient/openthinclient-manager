package org.openthinclient.db.conf;

import java.nio.file.Paths;
import org.openthinclient.db.DatabaseConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

@Configuration
@PropertySource("classpath:/org/openthinclient/db/conf/database-application.properties")
public class DataSourceConfiguration {

    @Autowired
    private ManagerHome managerHome;

    /**
     * Creates a H2 database url with local persistence. The generated H2 database url will point to
     * a local directory within the manager home.
     *
     * @param managerHome the {@link ManagerHome} to be used as a reference
     * @return a fully constructed JDBC connection url for H2.
     */
    public static String createH2DatabaseUrl(ManagerHome managerHome) {
        return "jdbc:h2:" + managerHome.getLocation().toPath().resolve("db").resolve("manager").toUri().toString() + ";DB_CLOSE_ON_EXIT=FALSE";
    }

    /**
     * Creates a Apache Derby database url with local persistence. The generated Apache Derby database url will point to
     * a local directory within the manager home.
     *
     * @param managerHome the {@link ManagerHome} to be used as a reference
     * @return a fully constructed JDBC connection url for Apache Derby.
     */
    public static String createApacheDerbyDatabaseUrl(ManagerHome managerHome) {
        return "jdbc:derby:" +  managerHome.getLocation().toPath().resolve("db").resolve("manager").toString() + ";create=true";
    }
    
    
    /**
     * Create a {@link DataSource} based on the given {@link DatabaseConfiguration} using the
     * provided url. <b>NOTE</b>: This method will not configure the {@link DataSource} for the
     * local H2 database.
     *
     * @param conf the {@link DatabaseConfiguration} to be used
     * @param url  the JDBC url to be used.
     * @return an appropriate {@link DataSource}
     */
    public static DataSource createDataSource(final DatabaseConfiguration conf, final String url) {
        DataSourceBuilder factory = DataSourceBuilder //
                .create(DataSourceConfiguration.class.getClassLoader()) //
                .driverClassName(conf.getType().getDriverClassName()) //
                .url(url) //
                .username(conf.getUsername()) //
                .password(conf.getPassword());
        return factory.build();
    }

    /**
     * Validates the given {@link DataSource} by executing a query. In case of an error, a {@link
     * SQLException} will be thrown.
     *
     * @param source the source to validate
     * @throws SQLException in case of an error when trying to connect to the database.
     */
    public static void validateDataSource(DataSource source) throws SQLException {
        try (final Connection connection = source.getConnection()) {
            connection.createStatement().execute("select 1");
        }
    }

    /**
     * Configures the appropriate {@link DataSource} to be used. This bean definition will override
     * the {@link DataSourceAutoConfiguration spring boot autoconfigured data source}.
     *
     * @return the appropriate {@link DataSource}
     */
    @Bean
    @Primary
    public DataSource dataSource() {

        final DatabaseConfiguration conf = managerHome.getConfiguration(DatabaseConfiguration.class);

        final DatabaseConfiguration.DatabaseType type = conf.getType();
        final String url;

        if (type == DatabaseConfiguration.DatabaseType.H2 && conf.getUrl() == null) {
            // when H2 is used and no jdbc URL has been provided, create a url pointing to a local directory in manager home
            url = createH2DatabaseUrl(managerHome);
        } else if (type == DatabaseConfiguration.DatabaseType.APACHE_DERBY) {
            url = createApacheDerbyDatabaseUrl(managerHome);
        } else {
            // in case of MySQL we're adding the autoReconnect=true property to ensure that connections will be reestablished when required.
            url = conf.getUrl() + "?autoReconnect=true";
        }

        DataSource dataSource = createDataSource(conf, url);
        
        /**
         * Liquibase is closing the DB connection after migration,
         * (the problem is: https://github.com/spring-projects/spring-boot/issues/2917)
         * we need to do a 'test connection' before retrieving from pool
         * 
         * (to put these properties into database-application.properties doesn't work
         * spring.datasource.test-on-borrow: true
         * spring.datasource.validation-query: values 1
         * spring.datasource.validation-interval: 0)
         * 
         * ValidationInterval may cause performance problems, 0 means 'test on each request'?
         * This configuration is only required by ApacheDerby.
         */
        if (type == DatabaseConfiguration.DatabaseType.APACHE_DERBY) { 
          ((org.apache.tomcat.jdbc.pool.DataSource) dataSource).setTestOnBorrow(true);
          ((org.apache.tomcat.jdbc.pool.DataSource) dataSource).setValidationQuery("values 1");
          ((org.apache.tomcat.jdbc.pool.DataSource) dataSource).setValidationInterval(0);

          /**
           * Set SystemProperty for ApacheDerby to managerHome log-location, Liquibase creates a derby.log file
           * even if ApacheDerby is not set as database
           */
          String derbyLogPath = Paths.get(managerHome.getLocation().getPath(), "logs/derby.log").toAbsolutePath().toString();
          System.setProperty("derby.stream.error.file", derbyLogPath);

        } else {

          /**
           * If ApacheDerby is not selected: stop Liquibase from creating derby.log
           */
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                System.setProperty("derby.stream.error.file", "NUL");
            } else {
                System.setProperty("derby.stream.error.file", "/dev/null");
            }
        }


        return dataSource;

    }

}
