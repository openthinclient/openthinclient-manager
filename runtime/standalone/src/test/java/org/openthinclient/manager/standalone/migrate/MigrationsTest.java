package org.openthinclient.manager.standalone.migrate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.Test;
import org.openthinclient.manager.util.http.config.PasswordUtil;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.service.common.home.ConfigurationFile;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.ManagerHomeMetadata;
import org.openthinclient.service.common.home.impl.DefaultManagerHome;
import org.openthinclient.service.common.home.impl.XMLManagerHomeMetadata;

public class MigrationsTest {

  @Test
  public void testMigrateInstallationWithoutMetadata() throws Exception {

    final Path dir = createTempDirectory();

    final DefaultManagerHome home = new DefaultManagerHome(dir.toFile());

    final ManagerHomeMetadata metadata = home.getMetadata();

    assertNotNull(metadata);
    assertNull(metadata.getServerID());


    Migrations.runEarlyMigrations(home);

    assertNotNull(home.getMetadata().getServerID());
  }

  @Test
  public void testMigrateInstallationWithManuallyRemovedMetadata() throws Exception {
    final Path dir = createTempDirectory();

    final String metadata = "<meta:manager-home-metadata xmlns:meta=\"http://www.openthinclient.org/ns/manager/metadata/1.0\" home-schema-version=\"1\">\n" +
            "    <!-- I'm scared of the server-id and therefore removed it -->\n" +
            "    <!-- server-id>dcbd44fe-919a-4544-a313-2fa8b7e6d352</server-id -->\n" +
            "</meta:manager-home-metadata>";
    Files.write(dir.resolve(XMLManagerHomeMetadata.FILENAME), metadata.getBytes());

    final DefaultManagerHome home = new DefaultManagerHome(dir.toFile());

    assertNull(home.getMetadata().getServerID());
    Migrations.runEarlyMigrations(home);

    assertNotNull(home.getMetadata().getServerID());

  }

  @Test
  public void testMigrateProxyUserPassword() throws Exception {

    final Path dir = createTempDirectory();
    final Path packageManagerConfigPath = Paths.get("target", "test-classes",  "package-manager.xml");
    Files.copy(packageManagerConfigPath, Paths.get(dir.toString(), "package-manager.xml"));

    final DefaultManagerHome home = new DefaultManagerHome(dir.toFile());

    PackageManagerConfiguration configuration = home.getConfiguration(PackageManagerConfiguration.class);
    String proxyValue = readConfigurationProxyValue(home);
    assertNotNull(configuration);
    assertNotNull(configuration.getProxyConfiguration());
    assertEquals("test", proxyValue);

    Migrations.runEarlyMigrations(home);

    configuration = home.getConfiguration(PackageManagerConfiguration.class);
    assertNotNull(configuration);
    assertNotNull(configuration.getProxyConfiguration());
    assertEquals("test", configuration.getProxyConfiguration().getPassword());
  }

  @Test
  public void testSkipMigratingProxyUserPassword() throws Exception {

    final Path dir = createTempDirectory();
    final Path packageManagerConfigPath = Paths.get("target", "test-classes",  "package-manager.xml");

    // Copy to working dir and set password-filed with encrypted value
    try (Stream<String> input = Files.lines(packageManagerConfigPath);
        PrintWriter output = new PrintWriter(Paths.get(dir.toString(), "package-manager.xml").toString(), "UTF-8"))
    {
      input.map(line -> line.trim().startsWith("<password>") ?
                        line.replace(line.substring(line.indexOf(">") + 1, // replace plain test-password with encrypted one
                                     line.indexOf("</")), PasswordUtil.encryptDES(line.substring(line.indexOf(">") + 1, line.indexOf("</")))
                        ):
                        line)
          .forEachOrdered(output::println);
    }

    final DefaultManagerHome home = new DefaultManagerHome(dir.toFile());

    // password is already encrypted
    PackageManagerConfiguration configuration = home.getConfiguration(PackageManagerConfiguration.class);
    assertNotNull(configuration);
    assertNotNull(configuration.getProxyConfiguration());
    assertEquals("test", configuration.getProxyConfiguration().getPassword());

    Migrations.runEarlyMigrations(home);

    // after migration password is still encrypted
    configuration = home.getConfiguration(PackageManagerConfiguration.class);
    assertNotNull(configuration);
    assertNotNull(configuration.getProxyConfiguration());
    assertEquals("test", configuration.getProxyConfiguration().getPassword());
  }

  private Path createTempDirectory() throws IOException {

    final StackTraceElement[] st = Thread.currentThread().getStackTrace();

    final String className = st[2].getClassName().substring(st[2].getClassName().lastIndexOf('.') + 1);
    final String methodName = st[2].getMethodName();

    final Path unitTestDirectory = Paths.get("target", "unit-test", className, methodName, "" + System.currentTimeMillis());
    Files.createDirectories(unitTestDirectory);

    System.out.println("Created Unit Test Data Directory " + unitTestDirectory);

    return unitTestDirectory;
  }

  private String readConfigurationProxyValue(ManagerHome managerHome) {

    ConfigurationFile configurationFile = PackageManagerConfiguration.class.getAnnotation(ConfigurationFile.class);
    Path path = Paths.get(managerHome.getLocation().getPath(), configurationFile.value());

    Optional<String> any;
    try (Stream<String> stream = Files.lines(path)) {
      any = stream
          .filter(line -> line.trim().startsWith("<password>"))
          .map(s -> s.substring(s.indexOf(">") + 1, s.indexOf("</")))
          .findFirst();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }

    return any.isPresent() ? any.get() : null;
  }


}