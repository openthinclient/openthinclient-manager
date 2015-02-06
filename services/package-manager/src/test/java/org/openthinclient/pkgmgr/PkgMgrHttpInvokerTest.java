package org.openthinclient.pkgmgr;

import static org.junit.Assert.assertNotNull;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.pkgmgr.impl.PackageManagerImpl;
import org.openthinclient.util.dpkg.Package;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:PkgMgrHttpInvokerTest.xml"})
public class PkgMgrHttpInvokerTest {

	private static final Logger logger = Logger.getLogger(PkgMgrHttpInvokerTest.class);
	
    @Autowired
    private PackageManager packageManagerService;

    @Test
    public void testFreeDiskSpace() throws PackageManagerException {
    	long diskSpace = packageManagerService.getFreeDiskSpace();
        assertNotNull(diskSpace);
        logger.debug("FreeDiskSpace: " + diskSpace);
    }

    @Test
    public void testInstalledPackages() throws PackageManagerException {
    	Collection<Package> packages = packageManagerService.getInstalledPackages();
        assertNotNull(packages);
        System.out.println("InstalledPackages: " + packages);
    }    
    
}
