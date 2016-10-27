package org.openthinclient.db.conf;

import org.openthinclient.db.DatabaseConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.impl.DefaultManagerHome;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Import({DataSourceAutoConfiguration.class, DataSourceConfiguration.class, LiquibaseAutoConfiguration.class})
@EnableAutoConfiguration
public class LocalMySQLTestConfig {

    @Bean
    public ManagerHome managerHome() throws Exception {

        final Path testDataDirectory = Paths.get("target", "test-data");
        Files.createDirectories(testDataDirectory);
        final Path tempDir = Files.createTempDirectory(testDataDirectory, getClass().getSimpleName());

        final DefaultManagerHome home = new DefaultManagerHome(tempDir.toFile());
        final DatabaseConfiguration dbConfig = home.getConfiguration(DatabaseConfiguration.class);
        
        dbConfig.setType(DatabaseConfiguration.DatabaseType.MYSQL);
        dbConfig.setUrl("jdbc:mysql://localhost:3306/otc");
        dbConfig.setUsername("otc");
        dbConfig.setPassword("otc");
        
        home.save(DatabaseConfiguration.class);

        return home;
    }

}
