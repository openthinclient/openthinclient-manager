package org.openthinclient.manager.standalone.config;

import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.manager.util.http.impl.HttpClientDownloadManager;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

import java.io.InputStream;
import java.util.Properties;

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

        InputStream inputStream = getClass().getResourceAsStream("/META-INF/maven/org.openthinclient/manager-runtime-standalone/pom.properties");
        java.util.Properties p = new Properties();
        String version = null;
        try {
            p.load(inputStream);
            version = p.getProperty("version");
        } catch (Exception e) {
            System.err.println("Cannot read version from pom.properties.");
        }

        String serverID = managerHome.getMetadata().getServerID();
        String userAgent = version == null ? serverID : serverID + "-" + version;

        return new HttpClientDownloadManager(configuration.getProxyConfiguration(), userAgent);
    }

}
