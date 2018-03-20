package org.openthinclient.manager.standalone.migrate.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.openthinclient.manager.standalone.migrate.EarlyMigration;
import org.openthinclient.manager.util.http.config.NetworkConfiguration.ProxyConfiguration;
import org.openthinclient.manager.util.http.config.PasswordUtil;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.service.common.home.ConfigurationFile;
import org.openthinclient.service.common.home.ManagerHome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encrypt proxy-user-password if set
 */
public class EncryptProxyUserPasswordMigration implements EarlyMigration {

  private static final Logger LOGGER = LoggerFactory.getLogger(EncryptProxyUserPasswordMigration.class);

  @Override
  public void migrate(ManagerHome managerHome) {

    PackageManagerConfiguration configuration = managerHome.getConfiguration(PackageManagerConfiguration.class);
    ProxyConfiguration proxyConfiguration = configuration.getProxyConfiguration();

    String proxyConfigurationUserPassword = readConfigurationProxyValue(managerHome);
    if (proxyConfiguration == null || StringUtils.isBlank(proxyConfigurationUserPassword)) {
      LOGGER.debug("Nothing todo, found blank proxy-user-configuration");
      return;
    }

    String decryptedPassword = PasswordUtil.decryptDES(proxyConfigurationUserPassword);
    if (decryptedPassword == null) {
      LOGGER.info("Encrypt proxy-user password.");
      proxyConfiguration.setPassword(proxyConfigurationUserPassword);
      managerHome.save(PackageManagerConfiguration.class);
    } else {
      LOGGER.debug("Nothing todo, current proxy-password is already encrypted.");
    }

  }

  /**
   * Read configuration value direct from file
   * @param managerHome home location
   * @return value for password-field or null
   */
  private String readConfigurationProxyValue(ManagerHome managerHome) {

    ConfigurationFile configurationFile = PackageManagerConfiguration.class.getAnnotation(ConfigurationFile.class);
    Path path = Paths.get(managerHome.getLocation().getPath(), configurationFile.value());

    Optional<String> any;
    try (Stream<String> stream = Files.lines(path)) {
      any = stream
          .filter(line -> line.trim().startsWith("<password>"))
          .map(s -> s.substring(s.indexOf(">") + 1, s.indexOf("</")))
          .findAny();
    } catch (IOException e) {
      LOGGER.warn("Cannot read plain config file for migration " + path.toAbsolutePath());
      return null;
    }

    return any.isPresent() ? any.get() : null;
  }

}
