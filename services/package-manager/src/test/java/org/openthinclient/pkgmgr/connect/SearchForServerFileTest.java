package org.openthinclient.pkgmgr.connect;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.pkgmgr.DebianTestRepositoryServer;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.SimpleTargetDirectoryPackageManagerConfiguration;
import org.openthinclient.pkgmgr.Source;
import org.openthinclient.pkgmgr.SourcesList;
import org.openthinclient.util.dpkg.UrlAndFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.URL;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SearchForServerFileTest.SearchForServerFileTestConfiguration.class)
public class SearchForServerFileTest {

  @Configuration()
  @Import(SimpleTargetDirectoryPackageManagerConfiguration.class)
  public static class SearchForServerFileTestConfiguration {
    
  }
  
  private static DebianTestRepositoryServer testRepositoryServer;

  @BeforeClass
  public static void startRepositoryServer() throws Exception {
    testRepositoryServer = new DebianTestRepositoryServer();
    testRepositoryServer.start();
  }

  @AfterClass
  public static void stopRepositoryServer() throws Exception {
    testRepositoryServer.stop();
  }

  @Autowired
  PackageManagerConfiguration packageManagerConfiguration;
  
  
  @Test
  public void testCheckForNewUpdatedFiles() throws Exception {

    final SourcesList sourcesList = new SourcesList();
    final Source source = new Source();
    source.setUrl(new URL("http://localhost:9090/"));
    source.setEnabled(true);
    source.setType(Source.Type.PACKAGE);
    sourcesList.getSources().add(source);

    final SearchForServerFile searchForServerFile = new SearchForServerFile(packageManagerConfiguration, sourcesList);

    final List<UrlAndFile> result = searchForServerFile.checkForNewUpdatedFiles(null);

    assertNotNull(result);
    assertEquals(1, result.size());

  }
}