package org.openthinclient.pkgmgr;

import org.openthinclient.pkgmgr.spring.PackageManagerRepositoryConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;

@Configuration
@Import({ PackageManagerRepositoryConfiguration.class, //
      HibernateJpaAutoConfiguration.class })
public class PackageManagerInMemoryDatabaseConfiguration {

   @Bean
   public DataSource dataSource() {
      return DataSourceBuilder.create() //
            .driverClassName(org.h2.Driver.class.getName()) //
              .url("jdbc:h2:mem:pkgmngr-test-" + System.currentTimeMillis() + ";DB_CLOSE_ON_EXIT=FALSE") //
            .build();
   }

}
