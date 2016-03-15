package org.openthinclient.pkgmgr;

import org.junit.Assert;
import org.openthinclient.pkgmgr.spring.PackageManagerExecutionEngineConfiguration;
import org.openthinclient.pkgmgr.spring.PackageManagerFactoryConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

import java.io.File;

@Configuration
@Import({PackageManagerInMemoryDatabaseConfiguration.class, PackageManagerExecutionEngineConfiguration.class, PackageManagerFactoryConfiguration.class})
public class SimpleTargetDirectoryPackageManagerConfiguration {

    @Autowired
    PackageManagerFactory packageManagerFactory;

    @Bean
    @Scope(value = "prototype")
    public File targetDirectory() {
        File targetDirectory = new File("target");

        if (!targetDirectory.exists()) {
            Assert.assertTrue(targetDirectory.mkdirs());
        }

        if (!targetDirectory.isDirectory())
            throw new AssertionError("Expected: " + targetDirectory.getAbsolutePath() + " to be a directory");

        // we're creating an subdirectory per test, to ensure that there will be no other existing files
        targetDirectory = new File(targetDirectory, "pkgmgr-test-" + System.currentTimeMillis());
        Assert.assertTrue(targetDirectory.mkdirs());

        return targetDirectory;
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
    @Scope(value="prototype")
    public PackageManager packageManager() throws Exception {
        return packageManagerFactory.createPackageManager(packageManagerConfiguration());
    }

    private File dir(File targetDirectory, String subPath) {
        final File dir = new File(targetDirectory, subPath);
        Assert.assertTrue(dir.mkdirs());
        return dir;
    }

}
