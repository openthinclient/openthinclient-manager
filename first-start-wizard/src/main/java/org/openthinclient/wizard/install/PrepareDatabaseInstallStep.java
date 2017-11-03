package org.openthinclient.wizard.install;

import org.openthinclient.DownloadManagerFactory;
import org.openthinclient.api.context.InstallContext;
import org.openthinclient.db.DatabaseConfiguration;
import org.openthinclient.db.conf.DataSourceConfiguration;
import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.PackageManagerFactory;
import org.openthinclient.pkgmgr.spring.PackageManagerExecutionEngineConfiguration;
import org.openthinclient.pkgmgr.spring.PackageManagerFactoryConfiguration;
import org.openthinclient.pkgmgr.spring.PackageManagerRepositoryConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.wizard.model.DatabaseModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALL_PREPAREDATABASEINSTALLSTEP_LABEL;

public class PrepareDatabaseInstallStep extends AbstractInstallStep {

    private static final Logger LOG = LoggerFactory.getLogger(PrepareDatabaseInstallStep.class);

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
        } else if (model.getType() == DatabaseConfiguration.DatabaseType.APACHE_DERBY) {
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

        DatabaseModel.apply(this.databaseModel, target);

        managerHome.save(DatabaseConfiguration.class);

        // prepare the spring context and execute the liquibase migration

        LOG.info("Preparing database bootstrap");
        AnnotationConfigApplicationContext context = createDatabaseInitApplicationContext(installContext);
        // this refresh will ensure that the database will be initialized correctly
        context.refresh();

        // we're done with this temporary context. Shutting down the context
        context.close();
        LOG.info("Database bootstrap completed");

        // for further processing we require a more sophisticated application context that will contain a package manager
        LOG.info("Preparing package manager aware application context");
        context = createPackageManagerApplicationContext(installContext);
        context.refresh();

        final PackageManager packageManager = context.getBean(PackageManagerFactory.class)
                .createPackageManager(installContext.getManagerHome().getConfiguration(PackageManagerConfiguration.class));

        installContext.setPackageManager(packageManager);
        installContext.setContext(context);

    }

    private AnnotationConfigApplicationContext createPackageManagerApplicationContext(InstallContext installContext) {
        final AnnotationConfigApplicationContext context = createDatabaseInitApplicationContext(installContext);

        context.register( //
                HibernateJpaAutoConfiguration.class, //
                PackageManagerRepositoryConfiguration.class, //
                PackageManagerExecutionEngineConfiguration.class, //
                PackageManagerFactoryConfiguration.class
        );

        return context;
    }

    protected AnnotationConfigApplicationContext createDatabaseInitApplicationContext(InstallContext installContext) {
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        final ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        beanFactory.registerSingleton(InstallContext.class.getCanonicalName(), installContext);
        context.register( //
                InstallContextBasedConfiguration.class, //
                DataSourceAutoConfiguration.class, //
                LiquibaseAutoConfiguration.class, //
                LiquibaseAutoConfiguration.LiquibaseConfiguration.class, //
                DataSourceConfiguration.class  //
        );
        return context;
    }

    @Override
    public String getName() {
        return mc.getMessage(UI_FIRSTSTART_INSTALL_PREPAREDATABASEINSTALLSTEP_LABEL);
    }

    @Override
    public double getProgress() {
        return 1;
    }

    @Configuration
    public static class InstallContextBasedConfiguration {

        /**
         * Associated {@link InstallContext}. The actual instance will be registered as a singleton
         * pragmatically in {@link #createDatabaseInitApplicationContext(InstallContext)}
         */
        @SuppressWarnings("SpringJavaAutowiringInspection")
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

        @Bean
        @Scope(value = "singleton")
        public DownloadManager downloadManager(ManagerHome managerHome) {
            final PackageManagerConfiguration configuration = managerHome.getConfiguration(PackageManagerConfiguration.class);
            return DownloadManagerFactory.create(managerHome.getMetadata().getServerID(), configuration.getProxyConfiguration());
        }
    }
}
