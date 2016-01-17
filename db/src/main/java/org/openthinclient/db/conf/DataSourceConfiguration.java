package org.openthinclient.db.conf;

import org.openthinclient.db.DatabaseConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfiguration {

   @Autowired
   private ManagerHome managerHome;

   /**
    * Configures the appropriate {@link DataSource} to be used. This bean definition will override the {@link DataSourceAutoConfiguration spring boot autoconfigured data source}.
    *
    * @return the appropriate {@link DataSource}
    */
   @Bean
   @Primary
   public DataSource dataSource() {

      final DatabaseConfiguration conf = managerHome.getConfiguration(DatabaseConfiguration.class);

      DataSourceBuilder factory = DataSourceBuilder //
            .create(getClass().getClassLoader()) //
            .driverClassName(conf.getType().getDriverClassName()) //
            .url(conf.getUrl()) //
            .username(conf.getUsername()) //
            .password(conf.getPassword());
      return factory.build();

   }

}
