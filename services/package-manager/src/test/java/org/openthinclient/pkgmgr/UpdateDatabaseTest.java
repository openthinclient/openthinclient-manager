package org.openthinclient.pkgmgr;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.pkgmgr.db.PackageManagerDatabase;
import org.openthinclient.pkgmgr.db.PackageRepository;
import org.openthinclient.pkgmgr.db.Source;
import org.openthinclient.pkgmgr.db.SourceRepository;
import org.openthinclient.pkgmgr.progress.NoopProgressReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = UpdateDatabaseTest.Config.class)
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

    @Test
    public void testUpdatePackages() throws Exception {

        UpdateDatabase updater = new UpdateDatabase(configuration, getSourcesList(), db);

        updater.execute(new NoopProgressReceiver());

        // we're expecting four packages to exist at this point in time
        assertEquals(19, packageRepository.count());

        // running another update should not add new packages
        updater = new UpdateDatabase(configuration, getSourcesList(), db);

        updater.execute(new NoopProgressReceiver());
        assertEquals(19, packageRepository.count());

    }

    @Before
    public void configureSource() {
        final Source source = new Source();
        source.setUrl(testRepositoryServer.getServerUrl());
        source.setEnabled(true);
        sourceRepository.saveAndFlush(source);
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