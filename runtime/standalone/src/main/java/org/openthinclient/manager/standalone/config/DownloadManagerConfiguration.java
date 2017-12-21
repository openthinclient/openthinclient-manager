package org.openthinclient.manager.standalone.config;

import org.openthinclient.DownloadManagerFactory;
import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

/**
 * DownloadManagerConfiguration
 */
@Configuration
@Import({ManagerStandaloneServerConfiguration.class})
public class DownloadManagerConfiguration {

    @Bean
    @Scope(value = "singleton")
    public DownloadManager downloadManager(ManagerHome managerHome) {
        final PackageManagerConfiguration configuration = managerHome.getConfiguration(PackageManagerConfiguration.class);
        return DownloadManagerFactory.create(managerHome.getMetadata().getServerID(), configuration.getProxyConfiguration());
    }

}
