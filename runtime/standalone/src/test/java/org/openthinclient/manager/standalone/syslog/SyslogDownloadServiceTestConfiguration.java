package org.openthinclient.manager.standalone.syslog;

import org.openthinclient.api.logs.LogDownloadService;
import org.openthinclient.api.logs.LogMvcConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Paths;

/**
 *
 */
@Configuration
@ComponentScan(basePackageClasses = {SyslogDownloadServiceTestConfiguration.class, LogDownloadService.class, LogMvcConfiguration.class})
@ActiveProfiles("test, unsecure")
public class SyslogDownloadServiceTestConfiguration {

    @Bean
    public ManagerHome managerHome() {
        final ManagerHomeFactory factory = new ManagerHomeFactory();
        factory.setManagerHomeDirectory(Paths.get("target").toFile());
        return factory.create();
    }
}
