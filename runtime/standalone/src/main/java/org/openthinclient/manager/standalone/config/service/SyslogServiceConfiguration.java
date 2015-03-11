package org.openthinclient.manager.standalone.config.service;

import org.openthinclient.syslogd.SyslogService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by jn on 05.03.15.
 */
@Configuration
public class SyslogServiceConfiguration {

    @Bean
    public SyslogService syslogService() {
        return new SyslogService();
    }
}
