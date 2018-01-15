package org.openthinclient.service.common.home.impl;

import org.openthinclient.service.common.home.ManagerHome;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * This is a specialized type of configuration data, only relevant to the openthinclient appliance.
 */
public class ApplianceConfiguration {

  private static final ApplianceConfiguration DISABLED = new ApplianceConfiguration();

  private final boolean enabled;
  private final Properties properties;

  public ApplianceConfiguration() {
    // default state is disabled.
    enabled = false;
    properties = new Properties();
  }

  public ApplianceConfiguration(Properties properties) {
    this.properties = properties;
    enabled = true;
  }

  public static ApplianceConfiguration get(ManagerHome managerHome) throws IOException {

    final Path applianceConfigurationFile = managerHome.getLocation()
            .toPath().resolve(".appliance.properties");

    if (Files.exists(applianceConfigurationFile) && Files.isRegularFile(applianceConfigurationFile)) {

      final Properties properties = new Properties();
      try (final InputStream in = Files.newInputStream(applianceConfigurationFile)) {
        properties.load(in);
      }

      return new ApplianceConfiguration(properties);

    } else {
      return DISABLED;
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getNoVNCConsoleHostname() {
    return properties.getProperty("novnc.server.host");
  }

  public Integer getNoVNCConsolePort() {
    return Integer.valueOf(properties.getProperty("novnc.server.port", "5900"));
  }

  public boolean isNoVNCConsoleEncrypted() {
    return Boolean.valueOf(properties.getProperty("novnc.server.encrypt", "false"));
  }

  public boolean isNoVNCConsoleAutoconnect() {
    return Boolean.valueOf(properties.getProperty("novnc.server.autoconnect", "true"));
  }

  public boolean isNoVNCConsoleAllowfullscreen() {
    return Boolean.valueOf(properties.getProperty("novnc.server.allowfullscreen", "true"));
  }
}
