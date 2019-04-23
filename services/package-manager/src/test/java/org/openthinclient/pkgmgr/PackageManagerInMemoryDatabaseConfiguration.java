package org.openthinclient.pkgmgr;

import org.openthinclient.pkgmgr.spring.PackageManagerDatabaseConfiguration;
import org.openthinclient.pkgmgr.spring.PackageManagerRepositoryConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
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
@Import({PackageManagerRepositoryConfiguration.class, //
        HibernateJpaAutoConfiguration.class, //
        PackageManagerDatabaseConfiguration.class
})
public class PackageManagerInMemoryDatabaseConfiguration {

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create() //
                .driverClassName(org.h2.Driver.class.getName()) //
                .url("jdbc:h2:mem:pkgmngr-test-" + System.currentTimeMillis() + ";DB_CLOSE_ON_EXIT=FALSE") //
                .build();
    }

    @Bean
    public EntityManagerFactory entityManagerFactory() {

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(true);

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setPackagesToScan("org.openthinclient.pkgmgr.db");
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
