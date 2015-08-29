package org.openthinclient.manager.standalone.config.service;

import org.openthinclient.tftp.TFTPService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by jn on 05.03.15.
 */
@Configuration
public class TftpServiceConfiguration {

    @Bean
    public TFTPService tftpService() {
        return new TFTPService();
    }
}
