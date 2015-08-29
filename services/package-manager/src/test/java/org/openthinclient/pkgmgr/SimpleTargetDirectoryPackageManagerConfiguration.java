package org.openthinclient.pkgmgr;

import org.junit.Assert;
import org.openthinclient.service.common.home.ConfigurationDirectory;
import org.openthinclient.service.common.home.ConfigurationFile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileOutputStream;

@Configuration
public class SimpleTargetDirectoryPackageManagerConfiguration {

  @Bean
  public PackageManagerConfiguration packageManagerConfiguration() throws Exception {

    final PackageManagerConfiguration configuration = new PackageManagerConfiguration();

    File targetDirectory = new File("target");

    if (!targetDirectory.exists()) {
      Assert.assertTrue(targetDirectory.mkdirs());
    }

    if (!targetDirectory.isDirectory())
      throw new AssertionError("Expected: " + targetDirectory.getAbsolutePath() + " to be a directory");

    // we're creating an subdirectory per test, to ensure that there will be no other existing files
    targetDirectory = new File(targetDirectory, "pkgmgr-test-"+ System.currentTimeMillis());
    Assert.assertTrue(targetDirectory.mkdirs());

    final File sourcesList = new File(targetDirectory, "sources.list");
    final FileOutputStream out = new FileOutputStream(sourcesList);
    out.write('\n');
    out.close();
    configuration.setSourcesList(sourcesList);

    final String subPath = "install";
    configuration.setInstallDir(dir(targetDirectory, subPath));
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

  private File dir(File targetDirectory, String subPath) {
    final File dir = new File(targetDirectory, subPath);
    Assert.assertTrue(dir.mkdirs());
    return dir;
  }

}
