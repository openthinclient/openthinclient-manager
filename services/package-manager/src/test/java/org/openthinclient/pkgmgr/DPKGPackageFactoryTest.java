package org.openthinclient.pkgmgr;

import org.junit.Test;
import org.openthinclient.util.dpkg.*;

import java.util.List;

import static org.junit.Assert.*;

public class DPKGPackageFactoryTest {

  @Test
  public void testPackagesParsing() throws Exception {
    final List<org.openthinclient.util.dpkg.Package> packageList = new DPKGPackageFactory(null).getPackage(getClass().getResourceAsStream("/test-repository/Packages"));

    assertEquals(5, packageList.size());

    assertEquals("foo", packageList.get(0).getName());
    
    assertEquals("foo", packageList.get(1).getName());
    
    assertEquals("zonk", packageList.get(2).getName());
    assertEquals(new ANDReference("bar2"), packageList.get(2).getConflicts());

    assertEquals("bar2", packageList.get(3).getName());
    assertEquals(new ANDReference("foo"), packageList.get(3).getDepends());
    
    assertEquals("bar", packageList.get(4).getName());
    assertEquals(new ANDReference("foo (>= 0:2.1-1)"), packageList.get(4).getDepends());

  }
}