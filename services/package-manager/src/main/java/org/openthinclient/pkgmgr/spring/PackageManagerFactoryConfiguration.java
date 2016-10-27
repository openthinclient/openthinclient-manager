package org.openthinclient.pkgmgr.spring;

import org.openthinclient.pkgmgr.PackageManagerFactory;
import org.openthinclient.pkgmgr.db.PackageManagerDatabase;
import org.openthinclient.pkgmgr.progress.PackageManagerExecutionEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(PackageManagerDatabaseConfiguration.class)
public class PackageManagerFactoryConfiguration {

    @Autowired
    PackageManagerExecutionEngine executionEngine;

    @Autowired
    PackageManagerDatabase db;

    @Bean
    public PackageManagerFactory packageManagerFactory() {
        return new PackageManagerFactory(db, executionEngine);
    }

}
