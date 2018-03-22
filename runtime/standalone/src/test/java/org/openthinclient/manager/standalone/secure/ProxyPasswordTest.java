package org.openthinclient.manager.standalone.secure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.Test;
import org.openthinclient.manager.util.http.config.PasswordUtil;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.service.common.home.impl.DefaultManagerHome;

/**
 * ProxyPasswordTest
 */
public class ProxyPasswordTest {

  @Test
  public void testPlainProxyUserPassword() throws Exception {

    final Path dir = createTempDirectory();
    final Path packageManagerConfigPath = Paths.get("target", "test-classes",  "package-manager.xml");
    Files.copy(packageManagerConfigPath, Paths.get(dir.toString(), "package-manager.xml"));

    final DefaultManagerHome home = new DefaultManagerHome(dir.toFile());

    // password is plain
    PackageManagerConfiguration configuration = home.getConfiguration(PackageManagerConfiguration.class);
    assertNotNull(configuration);
    assertNotNull(configuration.getProxyConfiguration());
    assertEquals("test", configuration.getProxyConfiguration().getPassword());
  }

  @Test
  public void testEncryptedProxyUserPassword() throws Exception {

    final Path dir = createTempDirectory();
    final Path packageManagerConfigPath = Paths.get("target", "test-classes",  "package-manager.xml");

    // Copy to working dir and set password-filed with encrypted value
    try (Stream<String> input = Files.lines(packageManagerConfigPath);
        PrintWriter output = new PrintWriter(Paths.get(dir.toString(), "package-manager.xml").toString(), "UTF-8"))
    {
      input.map(line -> line.trim().startsWith("<password>") ?
              line.replace(line.substring(line.indexOf(">") + 1, // replace plain test-password with encrypted one
              line.indexOf("</")), "%%ENC:" + PasswordUtil.encryptDES(line.substring(line.indexOf(">") + 1, line.indexOf("</")))
          ):
          line)
          .forEachOrdered(output::println);
    }

    final DefaultManagerHome home = new DefaultManagerHome(dir.toFile());

    // password is encrypted
    PackageManagerConfiguration configuration = home.getConfiguration(PackageManagerConfiguration.class);
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

}
