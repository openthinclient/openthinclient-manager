package org.openthinclient.api.importer;

import org.openthinclient.clientmgr.ClientManagerDatabaseConfiguration;
import org.openthinclient.clientmgr.ClientManagerRepositoryConfiguration;
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
@Import({ClientManagerRepositoryConfiguration.class, //
        HibernateJpaAutoConfiguration.class, //
        ClientManagerDatabaseConfiguration.class
})
public class ClientManagerInMemoryDatabaseConfiguration {

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create() //
                .driverClassName(org.h2.Driver.class.getName()) //
                .url("jdbc:h2:mem:clientmgr-test-" + System.currentTimeMillis() + ";DB_CLOSE_ON_EXIT=FALSE") //
                .build();
    }

    @Bean
    public EntityManagerFactory entityManagerFactory() {

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(true);

        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(vendorAdapter);
        factory.setPackagesToScan("org.openthinclient.clientmgr.db");
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
