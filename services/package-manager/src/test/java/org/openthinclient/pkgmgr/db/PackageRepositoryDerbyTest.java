package org.openthinclient.pkgmgr.db;

import org.junit.runner.RunWith;
import org.openthinclient.pkgmgr.PackageManagerApacheDerbyDatabaseConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Runs all {@link PackageRepositoryTest} but with Apache Derby inMemory Database configuration
 */
@RunWith(
    SpringJUnit4ClassRunner.class
)
@SpringBootTest(classes = {
    PackageManagerApacheDerbyDatabaseConfiguration.class
})
public class PackageRepositoryDerbyTest extends PackageRepositoryTest {

}
