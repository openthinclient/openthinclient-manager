package org.openthinclient.clientmgr;

import org.openthinclient.clientmgr.db.ItemConfigurationRepository;
import org.openthinclient.clientmgr.db.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientManagerDatabaseConfiguration {

    @Autowired
    ItemRepository itemRepository;

    @Autowired
    ItemConfigurationRepository itemConfigurationRepository;

    @Bean
    public ClientManagerService clientManagerService() {
        return new ClientManagerService(itemRepository, itemConfigurationRepository);
    }

}
