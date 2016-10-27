package org.openthinclient.runtime.control.cmd;

import com.google.common.base.Strings;

import org.kohsuke.args4j.Option;
import org.openthinclient.db.DatabaseConfiguration;
import org.openthinclient.db.conf.DataSourceConfiguration;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.openthinclient.wizard.install.InstallSystemTask;
import org.openthinclient.wizard.install.InstallableDistribution;
import org.openthinclient.wizard.install.InstallableDistributions;
import org.openthinclient.wizard.model.DatabaseModel;
import org.openthinclient.wizard.model.DirectoryModel;
import org.openthinclient.wizard.model.InstallModel;
import org.openthinclient.wizard.model.NetworkConfigurationModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.SQLException;

import javax.sql.DataSource;

public class PrepareHomeCommand extends AbstractCommand<PrepareHomeCommand.Options> {

  private static final Logger LOG = LoggerFactory.getLogger(PrepareHomeCommand.class);

  public PrepareHomeCommand() {
    super("prepare-home");
  }

  @Override
  public Options createOptionsObject() {
    return new Options();
  }

  @Override
  public void execute(Options options) throws Exception {

    LOG.info("Starting preinstall to " + options.homePath.toAbsolutePath());

    final InstallableDistribution distribution;
    if (Strings.isNullOrEmpty(options.distribution))
      distribution = InstallModel.DEFAULT_DISTRIBUTION;
    else {
      distribution = InstallableDistributions.getDefaultDistributions()
              .getInstallableDistributions().stream()
              .filter(dist -> options.distribution.equals(dist.getName()))
              .findFirst().orElse(null);

      if (distribution == null) {
        LOG.error("Distribution could not be found: " + options.distribution);
      }
    }

    DirectoryModel directoryModel = new DirectoryModel();
    NetworkConfigurationModel networkConfigurationModel = new NetworkConfigurationModel();
    DatabaseModel databaseModel = new DatabaseModel();

    networkConfigurationModel.getDirectConnectionProperty().setValue(true);

    databaseModel.setType(options.dbType);

    if (options.dbType == DatabaseConfiguration.DatabaseType.MYSQL) {
      databaseModel.getMySQLConfiguration().setDatabase(options.dbDatabase);
      databaseModel.getMySQLConfiguration().setUsername(options.dbUsername);
      databaseModel.getMySQLConfiguration().setPassword(options.dbPassword);
      databaseModel.getMySQLConfiguration().setHostname(options.dbHostname);
      databaseModel.getMySQLConfiguration().setPort(options.dbMySQLport);
    }
    validateDatabaseConnection(databaseModel);

    directoryModel.getAdministratorUser().setNewPassword(options.adminPassword);

    final ManagerHomeFactory managerHomeFactory = new ManagerHomeFactory();
    managerHomeFactory.setManagerHomeDirectory(options.homePath.toFile());
    final InstallSystemTask task = new InstallSystemTask(managerHomeFactory, distribution, directoryModel, networkConfigurationModel, databaseModel);

    task.call();
  }

  private void validateDatabaseConnection(DatabaseModel databaseModel) throws SQLException {

    final DatabaseConfiguration configuration = new DatabaseConfiguration();
    DatabaseModel.apply(databaseModel, configuration);

    final DataSource dataSource = DataSourceConfiguration.createDataSource(configuration, configuration.getUrl());

    // do not validate the embedded databases as their URL will be constructed once the final manager home directory has been identified.
    if (!databaseModel.getType().isEmbedded())
      DataSourceConfiguration.validateDataSource(dataSource);
  }

  public static class Options {
    @Option(name = "--home", required = true, metaVar = "DIR", usage = "The target manager home directory")
    public Path homePath;


    @Option(name = "--db", required = false, metaVar = "TYPE", usage = "Type of the database that shall be used.")
    public DatabaseConfiguration.DatabaseType dbType = DatabaseConfiguration.DatabaseType.H2;

    @Option(name = "--db-host", required = false, metaVar = "HOST", usage = "Hostname to be used for the database connection. Only required for MySQL database connections")
    public String dbHostname = "localhost";
    @Option(name = "--db-user", required = false, metaVar = "USER", usage = "Username to be used for the database connection. Only required for MySQL database connections")
    public String dbUsername = "root";
    @Option(name = "--db-password", required = false, metaVar = "PASS", usage = "Password to be used for the database connection. Only required for MySQL database connections")
    public String dbPassword;
    @Option(name = "--db-name", required = false, metaVar = "DB", usage = "Name of the Database to be used. Only required for MySQL database connections")
    public String dbDatabase = "openthinclient";
    @Option(name = "--db-mysql-port", required = false, metaVar = "PORT", usage = "The port of the MySQL database. Defaults to 3306")
    public int dbMySQLport = 3306;

    @Option(name = "--dist", required = false, metaVar = "NAME", usage = "The name of the distribution to be installed. When not specified, the preferred (commonly the most recent version) will be installed. Use the command ls-distributions for a list of available distributions")
    public String distribution;

    @Option(name = "--admin-password", required = true, metaVar = "PASSWORD", usage = "The initial Administrator password.")
    public String adminPassword;

  }

}
