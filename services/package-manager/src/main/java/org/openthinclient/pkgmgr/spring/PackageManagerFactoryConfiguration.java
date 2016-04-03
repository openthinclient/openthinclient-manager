package org.openthinclient.pkgmgr.spring;

import org.openthinclient.pkgmgr.PackageManagerFactory;
import org.openthinclient.pkgmgr.db.InstallationLogEntryRepository;
import org.openthinclient.pkgmgr.db.InstallationRepository;
import org.openthinclient.pkgmgr.db.PackageInstalledContentRepository;
import org.openthinclient.pkgmgr.db.PackageRepository;
import org.openthinclient.pkgmgr.db.SourceRepository;
import org.openthinclient.pkgmgr.progress.PackageManagerExecutionEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PackageManagerFactoryConfiguration {

    @Autowired
    SourceRepository sourceRepository;

    @Autowired
    PackageRepository packageRepository;

    @Autowired
    InstallationRepository installationRepository;

    @Autowired
    InstallationLogEntryRepository installationLogEntryRepository;

    @Autowired
    PackageManagerExecutionEngine executionEngine;

    @Autowired
    PackageInstalledContentRepository installedContentRepository;

    @Bean
    public PackageManagerFactory packageManagerFactory() {
        return new PackageManagerFactory(sourceRepository, packageRepository, installationRepository, installationLogEntryRepository, installedContentRepository, executionEngine);
    }

}
