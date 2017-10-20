package org.openthinclient.pkgmgr;

import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.manager.util.http.impl.HttpClientDownloadManager;
import org.openthinclient.pkgmgr.spring.PackageManagerExecutionEngineConfiguration;
import org.openthinclient.pkgmgr.spring.PackageManagerFactoryConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

@Configuration
@Import({PackageManagerInMemoryDatabaseConfiguration.class,
         PackageManagerExecutionEngineConfiguration.class,
         PackageManagerFactoryConfiguration.class})
public class SimpleTargetDirectoryPackageManagerConfiguration {

  @Bean
  @Scope(value = "prototype")
  public File targetDirectory() throws Exception {
    Path targetDirectory = Paths.get("target");

    if (!Files.exists(targetDirectory)) {
      Files.createDirectories(targetDirectory);
    }

    assertTrue("Expected " + targetDirectory + " to be a directory", Files.isDirectory(targetDirectory));
    // we're creating an subdirectory per test, to ensure that there will be no other existing files
    targetDirectory = targetDirectory.resolve("pkgmgr-test-" + System.currentTimeMillis());

    Files.createDirectories(targetDirectory);

    return targetDirectory.toFile();
  }

  @Bean
  @Scope(value = "prototype")
  public PackageManagerConfiguration packageManagerConfiguration() throws Exception {

    File targetDirectory = targetDirectory();
    final PackageManagerConfiguration configuration = new PackageManagerConfiguration();

    configuration.setInstallDir(dir(targetDirectory, "install"));
    configuration.setWorkingDir(dir(targetDirectory, "cache"));
    configuration.setArchivesDir(dir(targetDirectory, "archives"));
    configuration.setTestinstallDir(dir(targetDirectory, "install-test"));
    configuration.setPartialDir(dir(targetDirectory, "archives-partial"));
    configuration.setInstallOldDir(dir(targetDirectory, "install-old"));
    configuration.setListsDir(dir(targetDirectory, "lists"));

    final File dbDir = dir(targetDirectory, "db");

    configuration.setPackageDB(new File(dbDir, "package.db"));
    configuration.setCacheDB(new File(dbDir, "chache.db"));
    configuration.setOldDB(new File(dbDir, "remove.db"));
    configuration.setArchivesDB(new File(dbDir, "archives.db"));

    return configuration;

  }

  @Bean
  @Scope(value = "prototype")
  public PackageManager packageManager(PackageManagerFactory packageManagerFactory) throws Exception {
    return packageManagerFactory.createPackageManager(packageManagerConfiguration());
  }

  private File dir(File targetDirectory, String subPath) throws IOException {
    final Path sub = targetDirectory.toPath().resolve(subPath);
    Files.createDirectories(sub);
    return sub.toFile();
  }

  @Bean
  @Scope(value = "singleton")
  public DownloadManager downloadManager() {
      return new HttpClientDownloadManager(null, "TestUserAgent");
  }

}
