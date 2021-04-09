package org.openthinclient.manager.standalone.migrate;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;
import org.openthinclient.manager.standalone.config.Migrations;
import org.openthinclient.service.common.home.impl.DefaultManagerHome;
import org.openthinclient.service.common.home.impl.XMLManagerHomeMetadata;
import org.springframework.test.util.ReflectionTestUtils;

public class MigrationsTest {

  @Test
  public void testMigrateInstallationWithoutServerId() throws Exception {
    final Path dir = createTempDirectory();

    final String metadata = "<meta:manager-home-metadata xmlns:meta=\"http://www.openthinclient.org/ns/manager/metadata/1.0\" home-schema-version=\"1\">\n" +
            "    <!-- I'm scared of the server-id and therefore removed it -->\n" +
            "    <!-- server-id>dcbd44fe-919a-4544-a313-2fa8b7e6d352</server-id -->\n" +
            "</meta:manager-home-metadata>";
    Files.write(dir.resolve(XMLManagerHomeMetadata.FILENAME), metadata.getBytes());

    final DefaultManagerHome managerHome = new DefaultManagerHome(dir.toFile());
    assertNull(managerHome.getMetadata().getServerID());

    Migrations migrations = new Migrations();
    ReflectionTestUtils.setField(migrations, "managerHome", managerHome);

    migrations.setServerId();

    assertNotNull(managerHome.getMetadata().getServerID());

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
