package org.openthinclient.service.common.license;

import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.manager.util.http.impl.HttpClientDownloadManager;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.impl.DefaultManagerHome;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class LicenseManagerTestConfiguration {

  @Bean
  public ManagerHome managerHome() throws IOException {

    final Path testDataDirectory = Paths.get("target", "test-data");
    Files.createDirectories(testDataDirectory);
    final Path tempDir = Files.createTempDirectory(testDataDirectory, getClass().getSimpleName());

    final DefaultManagerHome managerHome = new DefaultManagerHome(tempDir.toFile());
    managerHome.getMetadata().setServerID("0815-777-12345");

    return managerHome;

  }

  @Bean
  @Scope(value = "singleton")
  public DownloadManager downloadManager() {
    return new HttpClientDownloadManager(null, "TestUserAgent");
  }

}
