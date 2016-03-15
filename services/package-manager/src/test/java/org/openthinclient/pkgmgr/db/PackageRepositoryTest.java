package org.openthinclient.pkgmgr.db;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.pkgmgr.PackageManagerInMemoryDatabaseConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.openthinclient.pkgmgr.PackageTestUtils.createPackage;

@RunWith(
    SpringJUnit4ClassRunner.class
)
@SpringApplicationConfiguration(classes = {
    PackageManagerInMemoryDatabaseConfiguration.class
})
public class PackageRepositoryTest {

  @Autowired
  PackageRepository packageRepository;

  @After
  public void clearRepositories() {
    packageRepository.deleteAll();
    packageRepository.flush();
  }

  @Test
  public void testAddPackage() throws Exception {

    packageRepository.saveAndFlush(createPackage("pkg1", "1.0-12"));
    assertEquals(1, packageRepository.count());
  }

  @Test
  public void testInstalledPackages() throws Exception {

    assertEquals(0, packageRepository.count());

    final Package pkg1 = createPackage("pkg1", "1.0-12");
    pkg1.setInstalled(true);
    packageRepository.saveAndFlush(pkg1);
    packageRepository.saveAndFlush(createPackage("pkg2", "1.0-12"));
    packageRepository.saveAndFlush(createPackage("pkg3", "1.0-12"));

    assertEquals(3, packageRepository.count());
    assertEquals(1, packageRepository.findByInstalledTrue().size());

    final List<Package> installedPackages = packageRepository.findByInstalledTrue();
    assertEquals(1, installedPackages.size());
    assertEquals("pkg1", installedPackages.get(0).getName());
  }

  @Test
  public void testInstallablePackages() throws Exception {

    assertEquals(0, packageRepository.count());

    final Package pkg1 = createPackage("pkg1", "1.0-12");
    pkg1.setInstalled(true);
    packageRepository.saveAndFlush(pkg1);
    packageRepository.saveAndFlush(createPackage("pkg2", "1.0-12"));

    assertEquals(2, packageRepository.count());
    assertEquals(1, packageRepository.findByInstalledTrue().size());

    final List<Package> installablePackages = packageRepository.findByInstalledFalse();
    assertEquals(1, installablePackages.size());
    assertEquals("pkg2", installablePackages.get(0).getName());
  }

}