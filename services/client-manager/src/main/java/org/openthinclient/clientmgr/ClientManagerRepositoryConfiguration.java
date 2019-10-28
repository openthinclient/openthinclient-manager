package org.openthinclient.clientmgr;

import org.openthinclient.clientmgr.db.Item;
import org.openthinclient.clientmgr.db.ItemRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * A general configuration class. This is used to centrally define all repository specific configurations
 * that are required for spring to bootstrap the jpa repositories.
 */
@Configuration
@EnableJpaRepositories(basePackageClasses = ItemRepository.class)
@EntityScan(basePackageClasses = {Item.class, Jsr310JpaConverters.class})
public class ClientManagerRepositoryConfiguration {

}
