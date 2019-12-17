package org.openthinclient.service.common.license;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

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

    @Bean
    public EntityManagerFactory entityManagerFactory() {

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(true);

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setPackagesToScan("org.openthinclient.service.common.license");
        factory.setDataSource(dataSource());
        factory.afterPropertiesSet();

        return factory.getObject();
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager txManager = new JpaTransactionManager();
        txManager.setEntityManagerFactory(entityManagerFactory());
        return txManager;
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