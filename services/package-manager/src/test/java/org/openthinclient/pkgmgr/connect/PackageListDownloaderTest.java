package org.openthinclient.pkgmgr.connect;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.manager.util.http.DownloadManagerFactory;
import org.openthinclient.pkgmgr.DebianTestRepositoryServer;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.SimpleTargetDirectoryPackageManagerConfiguration;
import org.openthinclient.pkgmgr.SourcesList;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.util.dpkg.LocalPackageList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PackageListDownloaderTest.SearchForServerFileTestConfiguration.class)
public class PackageListDownloaderTest {

    @ClassRule
    public static final DebianTestRepositoryServer testRepositoryServer = new DebianTestRepositoryServer();

  @Autowired
  PackageManagerConfiguration packageManagerConfiguration;

  @Test
  public void testCheckForNewUpdatedFiles() throws Exception {

    final SourcesList sourcesList = new SourcesList();
    final Source source = new Source();
    source.setUrl(testRepositoryServer.getServerUrl());
    source.setEnabled(true);
    sourcesList.getSources().add(source);

    final PackageListDownloader packageListDownloader = new PackageListDownloader(packageManagerConfiguration, DownloadManagerFactory.create(packageManagerConfiguration.getProxyConfiguration()));

    final LocalPackageList result = packageListDownloader.download(source);

    assertNotNull(result);
    assertEquals(testRepositoryServer.getServerUrl(), result.getSource().getUrl());
    assertNotNull(result.getPackagesFile());
    assertTrue(result.getPackagesFile().isFile());

  }

  @Configuration()
  @Import(SimpleTargetDirectoryPackageManagerConfiguration.class)
  public static class SearchForServerFileTestConfiguration {

  }
}
