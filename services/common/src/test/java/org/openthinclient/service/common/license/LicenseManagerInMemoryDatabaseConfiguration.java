package org.openthinclient.service.common.license;

import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.impl.DefaultManagerHome;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Import({LicenseManagerConfiguration.class, //
         HibernateJpaAutoConfiguration.class
})
@EnableAutoConfiguration
public class LicenseManagerInMemoryDatabaseConfiguration {

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create() //
                .driverClassName(org.h2.Driver.class.getName()) //
                .url("jdbc:h2:mem:license-test-" + System.currentTimeMillis() + ";DB_CLOSE_ON_EXIT=FALSE") //
                .build();
    }

}
//@Import({DataSourceAutoConfiguration.class, DataSourceConfiguration.class, LiquibaseAutoConfiguration.class})
//@EnableAutoConfiguration
//public class LicenseManagerInMemoryDatabaseConfiguration {
//
//    @Bean
//    public ManagerHome managerHome() throws Exception {
//
//        final Path testDataDirectory = Paths.get("target", "test-data");
//        Files.createDirectories(testDataDirectory);
//        final Path tempDir = Files.createTempDirectory(testDataDirectory, getClass().getSimpleName());
//
//        final DefaultManagerHome home = new DefaultManagerHome(tempDir.toFile());
//        final DatabaseConfiguration dbConfig = home.getConfiguration(DatabaseConfiguration.class);
//
//        dbConfig.setUsername("sa");
//        dbConfig.setType(DatabaseConfiguration.DatabaseType.H2);
//
//        home.save(DatabaseConfiguration.class);
//
//        return home;
//    }
//
//}