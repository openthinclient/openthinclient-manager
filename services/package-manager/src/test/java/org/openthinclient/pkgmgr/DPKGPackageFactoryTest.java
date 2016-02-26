package org.openthinclient.pkgmgr;

import org.junit.Test;
import org.openthinclient.util.dpkg.*;

import java.util.List;

import static org.junit.Assert.*;

public class DPKGPackageFactoryTest {

  @Test
  public void testPackagesParsing() throws Exception {
    final List<org.openthinclient.util.dpkg.Package> packageList = new DPKGPackageFactory(null).getPackage(getClass().getResourceAsStream("/test-repository/Packages"));

    assertEquals(16, packageList.size());

    assertEquals("foo", packageList.get(0).getName());
    assertEquals(new ANDReference("foo2"), packageList.get(0).getConflicts());
    assertEquals(new ANDReference("foo"), packageList.get(0).getProvides());

    assertEquals("foo", packageList.get(1).getName());
    assertEquals(new ANDReference("foo2"), packageList.get(1).getConflicts());
    
    assertEquals("foo-fork", packageList.get(2).getName());
    assertEquals(new ANDReference("zonk"), packageList.get(2).getConflicts());
    assertEquals(new ANDReference("foo"), packageList.get(2).getProvides());

    assertEquals("foo2", packageList.get(3).getName());
    assertEquals(new ANDReference("foo"), packageList.get(3).getConflicts());

    assertEquals("zonk", packageList.get(4).getName());
    assertEquals(new ANDReference("bar2"), packageList.get(4).getConflicts());

    assertEquals("zonk", packageList.get(5).getName());
    assertEquals(new ANDReference("foo"), packageList.get(5).getDepends());
    assertEquals(new ANDReference("bar2"), packageList.get(5).getConflicts());
    
    assertEquals("zonk-dev", packageList.get(6).getName());
    assertEquals(new ANDReference("foo"), packageList.get(6).getConflicts());

    assertEquals("zonk2", packageList.get(7).getName());
    assertEquals(new ANDReference("bas2"), packageList.get(7).getDepends());

    assertEquals("bar", packageList.get(8).getName());
    assertEquals(new ANDReference("foo (>= 2.1-1)"), packageList.get(8).getDepends());
    assertEquals(new ANDReference("zonk (<= 2.0-1)"), packageList.get(8).getConflicts());
    
    assertEquals("bar2", packageList.get(9).getName());
    assertEquals(new ANDReference("foo"), packageList.get(9).getDepends());
    
    assertEquals("bar2", packageList.get(10).getName());
    assertEquals(new ANDReference("foo"), packageList.get(10).getConflicts());
    
    assertEquals("bar2-dev", packageList.get(11).getName());
    assertEquals(new ANDReference("bar2"), packageList.get(11).getDepends());
    assertEquals(new ANDReference("foo (<= 2.0-1)"), packageList.get(11).getConflicts());
    
    assertEquals("bas", packageList.get(12).getName());
    assertEquals(new ANDReference("foo (>= 2.0-1)"), packageList.get(12).getDepends());
    
    assertEquals("bas", packageList.get(13).getName());
    assertEquals(new ANDReference("foo (>= 2.1-1)"), packageList.get(13).getDepends());
    
    assertEquals("bas-dev", packageList.get(14).getName());
    assertEquals(new ANDReference("foo"), packageList.get(14).getDepends());
    assertEquals(new ANDReference("foo"), packageList.get(14).getConflicts());
    
    assertEquals("bas2", packageList.get(15).getName());
    assertEquals(new ANDReference("zonk2"), packageList.get(15).getDepends());
    
  }
}