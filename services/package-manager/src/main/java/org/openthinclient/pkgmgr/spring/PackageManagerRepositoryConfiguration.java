package org.openthinclient.pkgmgr.spring;

import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.db.SourceRepository;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * A general configuration class. This is used to centrally define all repository specific configurations
 * that are required for spring to bootstrap the jpa repositories.
 */
@Configuration
@EnableJpaRepositories(basePackageClasses = SourceRepository.class)
@EntityScan(basePackageClasses = {Source.class, Jsr310JpaConverters.class})
public class PackageManagerRepositoryConfiguration {

}
