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

  @Autowired
  PackageStateRepository packageStateRepository;

  @After
  public void clearRepositories() {
    packageStateRepository.deleteAll();
    packageRepository.deleteAll();
  }

  @Test
  public void testAddPackage() throws Exception {

    packageRepository.saveAndFlush(createPackage("pkg1", "1.0-12"));
    assertEquals(1, packageRepository.count());
  }

  @Test
  public void testInstalledPackages() throws Exception {

    assertEquals(0, packageRepository.count());

    final Package pkg1 = packageRepository.saveAndFlush(createPackage("pkg1", "1.0-12"));
    final Package pkg2 = packageRepository.saveAndFlush(createPackage("pkg2", "1.0-12"));
    packageRepository.saveAndFlush(createPackage("pkg3", "1.0-12"));

    assertEquals(3, packageRepository.count());
    assertEquals(0, packageRepository.getInstalledPackages().size());

    packageStateRepository.save(createState(pkg1, PackageState.State.INSTALLED));
    packageStateRepository.save(createState(pkg2, PackageState.State.UNINSTALLED));

    final List<Package> installedPackages = packageRepository.getInstalledPackages();
    assertEquals(1, installedPackages.size());
    assertEquals("pkg1", installedPackages.get(0).getName());
  }

  private PackageState createState(Package pkg1, PackageState.State s) {
    final PackageState state = new PackageState();
    state.setState(s);
    state.setPkg(pkg1);
    return state;
  }
}