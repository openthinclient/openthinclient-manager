package org.openthinclient.pkgmgr.spring;

import org.openthinclient.pkgmgr.db.PackageInstalledContentRepository;
import org.openthinclient.pkgmgr.db.PackageManagerDatabase;
import org.openthinclient.pkgmgr.db.PackageRepository;
import org.openthinclient.pkgmgr.db.PackageUninstalledContentRepository;
import org.openthinclient.pkgmgr.db.SourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PackageManagerDatabaseConfiguration {
    @Autowired
    SourceRepository sourceRepository;

    @Autowired
    PackageRepository packageRepository;

    @Autowired
    PackageInstalledContentRepository installedContentRepository;

    @Autowired
    PackageUninstalledContentRepository uninstalledContentRepository;

    @Bean
    public PackageManagerDatabase db() {
        return new PackageManagerDatabase(sourceRepository, packageRepository, installedContentRepository, uninstalledContentRepository);
    }

}
