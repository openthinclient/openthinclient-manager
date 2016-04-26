package org.openthinclient.wizard.install;

import org.openthinclient.db.DatabaseConfiguration;
import org.openthinclient.db.conf.DataSourceConfiguration;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.PackageManagerFactory;
import org.openthinclient.pkgmgr.spring.PackageManagerExecutionEngineConfiguration;
import org.openthinclient.pkgmgr.spring.PackageManagerFactoryConfiguration;
import org.openthinclient.pkgmgr.spring.PackageManagerRepositoryConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.wizard.model.DatabaseModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

public class PrepareDatabaseInstallStep extends AbstractInstallStep {

   private final DatabaseModel databaseModel;

   public PrepareDatabaseInstallStep(DatabaseModel databaseModel) {
      this.databaseModel = databaseModel;
   }

   public static void apply(DatabaseConfiguration target, DatabaseModel model) {
      target.setType(model.getType());
      if (model.getType() == DatabaseConfiguration.DatabaseType.MYSQL) {

         final DatabaseModel.MySQLConfiguration mySQLConfiguration = model.getMySQLConfiguration();
         target.setUrl("jdbc:mysql://" + mySQLConfiguration.getHostname() + ":" + mySQLConfiguration.getPort() + "/" + mySQLConfiguration.getDatabase());
         target.setUsername(mySQLConfiguration.getUsername());
         target.setPassword(mySQLConfiguration.getPassword());
      } else if (model.getType() == DatabaseConfiguration.DatabaseType.H2) {
         target.setUrl(null);
         target.setUsername("sa");
         target.setPassword("");
      } else {
         throw new IllegalArgumentException("Unsupported type of database " + model.getType());
      }
   }

   @Override
   protected void doExecute(InstallContext installContext) throws Exception {

      final ManagerHome managerHome = installContext.getManagerHome();

      // save the database configuration
      final DatabaseConfiguration target = managerHome.getConfiguration(DatabaseConfiguration.class);

      apply(target, this.databaseModel);

      managerHome.save(DatabaseConfiguration.class);

      // prepare the spring context and execute the liquibase migration

      final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

      final ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
      beanFactory.registerSingleton(InstallContext.class.getCanonicalName(), installContext);
      context.register( //
            InstallContextBasedConfiguration.class, //
            //            LiquibaseAutoConfiguration.LiquibaseConfiguration.class, //
            DataSourceConfiguration.class,  //
            HibernateJpaAutoConfiguration.class, //
            //            JpaRepositoriesAutoConfiguration.class, //
            PackageManagerRepositoryConfiguration.class, //
              PackageManagerExecutionEngineConfiguration.class, //
              PackageManagerFactoryConfiguration.class
      );
      context.refresh();

      final PackageManager packageManager = context.getBean(PackageManagerFactory.class)
            .createPackageManager(installContext.getManagerHome().getConfiguration(PackageManagerConfiguration.class));

      installContext.setPackageManager(packageManager);

   }

   @Override
   public String getName() {
      return "Prepare Database";
   }

   @Configuration
   public static class InstallContextBasedConfiguration {

      @Autowired
      InstallContext installContext;

      @Bean
      @Scope("prototype")
      public ManagerHome managerHome() {
         return installContext.getManagerHome();
      }

      @Bean
      @Scope("prototype")
      public PackageManager packageManager() {
         return installContext.getPackageManager();
      }
   }

}
