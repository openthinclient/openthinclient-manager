package org.openthinclient.runtime.control.cmd;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.kohsuke.args4j.Option;
import org.openthinclient.api.distributions.InstallableDistribution;
import org.openthinclient.api.distributions.InstallableDistributions;
import org.openthinclient.db.DatabaseConfiguration;
import org.openthinclient.db.conf.DataSourceConfiguration;
import org.openthinclient.runtime.control.util.DistributionsUtil;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.openthinclient.wizard.install.InstallSystemTask;
import org.openthinclient.wizard.model.DatabaseModel;
import org.openthinclient.wizard.model.DirectoryModel;
import org.openthinclient.wizard.model.NetworkConfigurationModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

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

    final InstallableDistribution distribution = DistributionsUtil.getInstallableDistribution(options.distributionSource,
                                                                                              options.distribution,
                                                                                              options.proxyHost,
                                                                                              options.proxyPort);
    if (distribution == null) {
      LOG.error("Distribution could not be found: " + options.distribution);
    }

    DirectoryModel directoryModel = new DirectoryModel();
    NetworkConfigurationModel networkConfigurationModel = new NetworkConfigurationModel();
    if (options.proxyHost != null && options.proxyPort != null) {
      networkConfigurationModel.getProxyConfiguration().setHost(options.proxyHost);
      networkConfigurationModel.getProxyConfiguration().setPort(options.proxyPort);
      networkConfigurationModel.enableProxyConnectionProperty();
    } else {
      networkConfigurationModel.enableDirectConnectionProperty();
    }
    DatabaseModel databaseModel = new DatabaseModel();
    validateDatabaseConnection(databaseModel);

    directoryModel.getAdministratorUser().setNewPassword(options.adminPassword);

    final ManagerHomeFactory managerHomeFactory = new ManagerHomeFactory();
    managerHomeFactory.setManagerHomeDirectory(options.homePath.toFile());

    ClassPathResource resource = new ClassPathResource("application.properties");
    Properties properties = PropertiesLoaderUtils.loadProperties(resource);
    String version = properties.getProperty("application.packages-update-version");
    String homeVersion = properties.getProperty("application.version");

    final InstallSystemTask task = new InstallSystemTask(managerHomeFactory, distribution, directoryModel, networkConfigurationModel, databaseModel, options.isPreview, version, homeVersion);

    task.call();
  }

  private void validateDatabaseConnection(DatabaseModel databaseModel) throws SQLException {

    final DatabaseConfiguration configuration = new DatabaseConfiguration();
    DatabaseModel.apply(databaseModel, configuration);

    final DataSource dataSource = DataSourceConfiguration.createDataSource(configuration, configuration.getUrl());
  }

  public static class Options {
    @Option(name = "--home", required = true, metaVar = "DIR", usage = "The target manager home directory")
    public Path homePath;

    @Option(name = "--dist-source", required = false, metaVar = "NAME", usage = "The source of distribution.xml, i.e. http://archive.openthinclient.org/openthinclient/distributions.xml, the default value is " + InstallableDistributions.LOCAL_DISTRIBUTIONS_XML)
    public String distributionSource;
    @Option(name = "--dist", required = false, metaVar = "NAME", usage = "The name of the distribution to be installed. When not specified, the preferred (commonly the most recent version) will be installed. Use the command ls-distributions for a list of available distributions")
    public String distribution;
    @Option(name = "--isPreview", required = false, metaVar = "PREVIEW", usage = "Indicates that preview packages should be installed even if older stable versions exist.")
    public boolean isPreview;

    @Option(name = "--admin-password", required = true, metaVar = "PASSWORD", usage = "The initial Administrator password.")
    public String adminPassword;

    @Option(name = "--proxyHost", required = false, metaVar = "PROXYHOST", usage = "The networkproxy host")
    public String proxyHost;
    @Option(name = "--proxyPort", required = false, metaVar = "PROXYPORT", usage = "The networkproxy port")
    public Integer proxyPort;
  }

}
