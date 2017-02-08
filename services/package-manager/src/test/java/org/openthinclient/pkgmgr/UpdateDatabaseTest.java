package org.openthinclient.pkgmgr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.openthinclient.pkgmgr.PackagesUtil.PACKAGES_SIZE;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.pkgmgr.db.PackageManagerDatabase;
import org.openthinclient.pkgmgr.db.PackageRepository;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.db.SourceRepository;
import org.openthinclient.pkgmgr.progress.NoopProgressReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = UpdateDatabaseTest.Config.class)
public class UpdateDatabaseTest {
   
    @ClassRule
    public static final DebianTestRepositoryServer testRepositoryServer = new DebianTestRepositoryServer();

    @Autowired
    PackageManagerDatabase db;
    @Autowired
    PackageRepository packageRepository;
    @Autowired
    SourceRepository sourceRepository;
    @Autowired
    PackageManagerConfiguration configuration;
    
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
    public void cleanup() throws IOException {
      // delete all test-added sources, but keep initial source (with id=1)
      sourceRepository.delete(sourceRepository.findAll().stream().filter(s -> s.getId() > 1).collect(Collectors.toList()));
      sourceRepository.flush();
    }
    
    
    @Test
    public void testUpdatePackages() throws Exception {
       
        UpdateDatabase updater = new UpdateDatabase(configuration, getSourcesList(), db);

        updater.execute(new NoopProgressReceiver());

        // we're expecting amount of PACKAGES_SIZE packages to exist at this point in time
        assertEquals(PACKAGES_SIZE, packageRepository.count());

        // Simple changelog test
        // check if changelog has been added zonk-dev.changelog is currently the only one
        Optional<Package> zonkDev = packageRepository.findAll().stream().filter(p -> p.getName().equals("zonk-dev")).findFirst();
        if (zonkDev.isPresent()) {
           String changeLog = zonkDev.get().getChangeLog();
           assertTrue(StringUtils.isNotBlank(changeLog));
           assertTrue(changeLog.contains("zonk-dev"));
           assertTrue(changeLog.contains("Fixed something"));
           assertTrue(changeLog.contains("Klaus Kinsky"));
        } else {
           fail("Expected package 'zonk-dev' not found, cannot test changelog entries.");
        }
        
        // running another update should not add new packages
        updater = new UpdateDatabase(configuration, getSourcesList(), db);

        // TODO test: changelog update for a package 
        
        updater.execute(new NoopProgressReceiver());
        assertEquals(PACKAGES_SIZE, packageRepository.count());

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