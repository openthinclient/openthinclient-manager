package org.openthinclient.pkgmgr;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.PackageManagerDatabase;
import org.openthinclient.pkgmgr.db.PackageRepository;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.db.SourceRepository;
import org.openthinclient.pkgmgr.db.Version;
import org.openthinclient.progress.NoopProgressReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.openthinclient.pkgmgr.PackageTestUtils.createPackage;
import static org.openthinclient.pkgmgr.PackageTestUtils.createSource;
import static org.openthinclient.pkgmgr.PackagesUtil.PACKAGES_SIZE;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = UpdateDatabaseTest.Config.class)
public class UpdateDatabaseTest {
   
  @Rule
  public final DebianTestRepositoryServer testRepositoryServer = new DebianTestRepositoryServer();

  @Autowired
  PackageManagerDatabase db;
  @Autowired
  PackageRepository packageRepository;
  @Autowired
  SourceRepository sourceRepository;
  @Autowired
  PackageManagerConfiguration configuration;
  @Autowired
  DownloadManager downloadManager;

  @Autowired
  PackageManagerFactory packageManagerFactory;

  @Before
  public void configureSource() {
    final Source source = new Source();
    source.setUrl(testRepositoryServer.getServerUrl());
    source.setEnabled(true);
    sourceRepository.saveAndFlush(source);
  }

  @After
  public void cleanup() {
    packageRepository.deleteAll();
    sourceRepository.deleteAll();
  }

  @Test
  public void testUpdatePackages() {

    UpdateDatabase updater = createUpdateDatabase();

    updater.execute(new NoopProgressReceiver());

      // we're expecting amount of PACKAGES_SIZE packages to exist at this point in time
      assertEquals(PACKAGES_SIZE, packageRepository.count());

      // Simple changelog test
      // check if changelog has been added zonk-dev.changelog is currently the only one
      Optional<Package> zonkDev = packageRepository.findAll().stream().filter(p -> p.getName().equals("zonk-dev")).findFirst();
      if (zonkDev.isPresent()) {
         String changeLog = zonkDev.get().getChangeLog();
         assertTrue(StringUtils.isNotBlank(changeLog));
         // check if correct version has been applied
         assertTrue(changeLog.contains("zonk-dev (2.0-1)"));
         assertTrue(changeLog.contains("Fixed something"));
         assertTrue(changeLog.contains("Mon, 28 Nov 2016 12:12:30 +0100"));
         // wrong version entries
         assertFalse(changeLog.contains("zonk-dev (2.0-7)"));
         assertFalse(changeLog.contains("copy requested profile from skel"));
         assertFalse(changeLog.contains("Wed, 18 Jan 2017 02:02:30 +0100"));

        // Test license information
        String license = zonkDev.get().getLicense();
        assertNotNull("Expected a license value.", license);
        assertTrue(license.contains("Grundfunktionen und zahlreiche Anwendungen"));

      } else {
         fail("Expected package 'zonk-dev' not found, cannot test changelog entries.");
      }

      // running another update should not add new packages
      updater = new UpdateDatabase(configuration, getSourcesList(), db, downloadManager);

      // TODO test: changelog update for a package

      updater.execute(new NoopProgressReceiver());
      assertEquals(PACKAGES_SIZE, packageRepository.count());

  }

  @Test
  public void testUpdateWithRepositoryContainingAdditionalVersion() {

    testRepositoryServer.setRepository("test-repository_versioning/repo01");

    UpdateDatabase updater = createUpdateDatabase();
    updater.execute(new NoopProgressReceiver());

    assertEquals(1, packageRepository.count());
    final Package pkgBeforeUpdate = packageRepository.findAll().get(0);
    assertEquals("foo", pkgBeforeUpdate.getName());
    assertEquals(Version.parse("2.0-1"), pkgBeforeUpdate.getVersion());

    // updating with a repository that contains a additional version
    // after the update, there must be two different versions of the same package
    testRepositoryServer.setRepository("test-repository_versioning/repo02");
    updater.execute(new NoopProgressReceiver());

    assertEquals(2, packageRepository.count());

  }

  @Test
  public void testUpdateMultipleTimesWithoutChanges() {

    testRepositoryServer.setRepository("test-repository_versioning/repo01");

    UpdateDatabase updater = createUpdateDatabase();
    updater.execute(new NoopProgressReceiver());

    assertEquals(1, packageRepository.count());
    final Package pkgBeforeUpdate = packageRepository.findAll().get(0);
    assertEquals("foo", pkgBeforeUpdate.getName());
    assertEquals(Version.parse("2.0-1"), pkgBeforeUpdate.getVersion());

    // update a second time. The result has to be the same as before.
    updater.execute(new NoopProgressReceiver());

    assertEquals(1, packageRepository.count());

    // update a thrid time. Again the result has to be the same as before.
    updater.execute(new NoopProgressReceiver());

    assertEquals(1, packageRepository.count());
  }

  @Test
  public void testUpdateWithoutChangesOfThePackageMetadata() {

    testRepositoryServer.setRepository("test-repository_versioning/repo01");

    UpdateDatabase updater = createUpdateDatabase();
    updater.execute(new NoopProgressReceiver());

    assertEquals(1, packageRepository.count());
    final Package pkgBeforeUpdate = packageRepository.findAll().get(0);
    assertEquals("foo", pkgBeforeUpdate.getName());
    assertEquals(Version.parse("2.0-1"), pkgBeforeUpdate.getVersion());
    assertEquals("Lars Behrens <lbehrens@unknown>", pkgBeforeUpdate.getMaintainer());
    assertEquals("<insert up to 60 chars description>", pkgBeforeUpdate.getShortDescription());

    testRepositoryServer.setRepository("test-repository_versioning/repo01.1");
    // update a second time. This time, the update will result in a change of the package metadata
    updater.execute(new NoopProgressReceiver());

    assertEquals(1, packageRepository.count());

    final Package pkgAfterUpdate = packageRepository.findAll().get(0);
    assertEquals("The New Maintainer <someone@else.com>", pkgAfterUpdate.getMaintainer());
    assertEquals("This now has a short description", pkgAfterUpdate.getShortDescription());

    // update a thrid time. Again the result has to be the same as before.
    updater.execute(new NoopProgressReceiver());

    assertEquals(1, packageRepository.count());
  }

  @Test
  public void testUpdateWithVersionChanges() {

    testRepositoryServer.setRepository("test-repository_versioning/repo01");

    UpdateDatabase updater = createUpdateDatabase();
    updater.execute(new NoopProgressReceiver());

    assertEquals(1, packageRepository.count());
    final Package pkgBeforeUpdate = packageRepository.findAll().get(0);
    assertEquals("foo", pkgBeforeUpdate.getName());
    assertEquals(Version.parse("2.0-1"), pkgBeforeUpdate.getVersion());

    // updating with a repository that contains a additional version
    // after the update, there must be two different versions of the same package
    testRepositoryServer.setRepository("test-repository_versioning/repo03");
    updater.execute(new NoopProgressReceiver());

    assertEquals(1, packageRepository.count());

    final Package pkgAfterUpdate = packageRepository.findAll().get(0);

    assertEquals("foo", pkgAfterUpdate.getName());
    assertEquals(Version.parse("2.1-1"), pkgAfterUpdate.getVersion());

  }

  private UpdateDatabase createUpdateDatabase() {
    return new UpdateDatabase(configuration, getSourcesList(), db, downloadManager);
  }

  @Test
  public void testCollectedOutdatedPackages() {

    final Source source = new Source();

    final List<Package> existing = Arrays.asList( //
            createPackage("foo", "2.0-1", false, source), //
            createPackage("foo", "2.1-1", false, source), //
            createPackage("foo", "2.2-1", false, source), //
            createPackage("bar", "3.5-1", false, source) //
    );

    final List<Package> newPackageList = Arrays.asList( //
            createPackage("foo", "2.1-1", false, source), //
            createPackage("foo", "2.2-1", false, source), //
            createPackage("bar", "3.5-1", false, source) //
    );

    final UpdateDatabase updater = createUpdateDatabase();

    final List<Package> outdated = updater.collectOutdatedPackages(newPackageList, existing);

    assertEquals(1, outdated.size());
    assertEquals("foo", outdated.get(0).getName());
    assertEquals(Version.parse("2.0-1"), outdated.get(0).getVersion());
  }

  @Test
  public void testPackageMetadataEquals() {
    final UpdateDatabase updater = createUpdateDatabase();
    final Source s1 = createSource("http://somewhere", "Source 1");
    final Source s2 = createSource("http://somewhere.else", "Source 2");

    assertTrue(updater.packageMetadataEquals( //
            createPackage("foo", "2.1-1", false, s1), //
            createPackage("foo", "2.1-1", true, s1) //
    ));

    assertFalse(updater.packageMetadataEquals( //
            createPackage("foo", "2.0-1", false, s1), //
            createPackage("foo", "2.1-1", false, s1) //
    ));

    assertFalse(updater.packageMetadataEquals( //
            createPackage("bar", "2.1-1", false, s1), //
            createPackage("foo", "2.1-1", false, s1) //
    ));

    assertFalse(updater.packageMetadataEquals( //
            createPackage("foo", "2.1-1", false, s1), //
            createPackage("foo", "2.1-1", false, s2) //
    ));
  }

  public SourcesList getSourcesList() {

      final SourcesList sourcesList = new SourcesList();
      sourcesList.getSources().addAll(sourceRepository.findAll());
      return sourcesList;

  }

  @Configuration()
  @Import({SimpleTargetDirectoryPackageManagerConfiguration.class, PackageManagerInMemoryDatabaseConfiguration.class})
  public static class Config {

  }
}