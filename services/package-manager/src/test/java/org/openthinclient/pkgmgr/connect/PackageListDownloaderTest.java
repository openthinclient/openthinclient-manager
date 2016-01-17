package org.openthinclient.pkgmgr.connect;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.pkgmgr.*;
import org.openthinclient.util.dpkg.LocalPackageList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PackageListDownloaderTest.SearchForServerFileTestConfiguration.class)
public class PackageListDownloaderTest {

  private static DebianTestRepositoryServer testRepositoryServer;
  @Autowired
  PackageManagerConfiguration packageManagerConfiguration;

  @BeforeClass
  public static void startRepositoryServer() throws Exception {
    testRepositoryServer = new DebianTestRepositoryServer();
    testRepositoryServer.start();
  }

  @AfterClass
  public static void stopRepositoryServer() throws Exception {
    testRepositoryServer.stop();
  }

  @Test
  public void testCheckForNewUpdatedFiles() throws Exception {

    final SourcesList sourcesList = new SourcesList();
    final Source source = new Source();
    source.setUrl(testRepositoryServer.getServerUrl());
    source.setEnabled(true);
    sourcesList.getSources().add(source);

    final PackageListDownloader packageListDownloader = new PackageListDownloader(packageManagerConfiguration, sourcesList);

    final List<LocalPackageList> result = packageListDownloader.checkForNewUpdatedFiles(null);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(testRepositoryServer.getServerUrl(), result.get(0).getSource().getUrl());
    assertNotNull(result.get(0).getPackagesFile());
    assertTrue(result.get(0).getPackagesFile().isFile());

  }

  @Configuration()
  @Import(SimpleTargetDirectoryPackageManagerConfiguration.class)
  public static class SearchForServerFileTestConfiguration {

  }
}
