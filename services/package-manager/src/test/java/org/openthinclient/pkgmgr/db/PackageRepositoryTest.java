package org.openthinclient.pkgmgr.db;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.pkgmgr.PackageManagerInMemoryDatabaseConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.openthinclient.pkgmgr.PackageTestUtils.createInstallation;
import static org.openthinclient.pkgmgr.PackageTestUtils.createPackage;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {
    PackageManagerInMemoryDatabaseConfiguration.class
})
public class PackageRepositoryTest {

  @Autowired
  PackageRepository packageRepository;

  @Autowired
  InstallationRepository installationRepository;
  
  @Autowired
  SourceRepository sourceRepository;
  
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
    // setup a source
    Source source = new Source();
    source.setDescription("description");
    source.setEnabled(true);
    source.setUrl(new URL("http://localhost"));
    sourceRepository.saveAndFlush(source);

    final Package pkg1 = createPackage("pkg1", "1.0-12");
    pkg1.setInstalled(true);
    pkg1.setSource(source);
    packageRepository.saveAndFlush(pkg1);
    Package pkg2 = createPackage("pkg2", "1.0-12");
    pkg2.setSource(source);
    packageRepository.saveAndFlush(pkg2);

    assertEquals(2, packageRepository.count());
    assertEquals(1, packageRepository.findByInstalledTrue().size());

    final List<Package> installablePackages = packageRepository.findInstallablePackages();
    assertEquals(1, installablePackages.size());
    assertEquals("pkg2", installablePackages.get(0).getName());
  }

  @Test
  public void testInstallation() throws Exception {

    assertEquals(0, installationRepository.count());
    
    Installation installation = createInstallation("Comment", LocalDateTime.now(), LocalDateTime.now().plusMinutes(2));
    installationRepository.saveAndFlush(installation);
 
    assertEquals(1, installationRepository.findAll().size());
  }
  
  @Test
  public void testSource() throws Exception {
    
    assertEquals(0, sourceRepository.count());
    
    Source source = new Source();
    source.setDescription("description");
    source.setEnabled(true);
    source.setUrl(new URL("http://localhost"));
    
    sourceRepository.saveAndFlush(source);
    
    assertEquals(1, sourceRepository.findAll().size());
  }
}