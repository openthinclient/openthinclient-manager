package org.openthinclient.pkgmgr.db;

import org.junit.Test;
import static org.junit.Assert.*;

public class VersionTest {

  @Test
  public void testParsing() throws Exception {
    Version version;

    version = Version.parse("0");
    assertEquals(version.getEpoch(), 0);
    assertEquals(version.getUpstreamVersion(), "0");
    assertEquals(version.getDebianRevision(), null);

    version = Version.parse("1:2-3");
    assertEquals(version.getEpoch(), 1);
    assertEquals(version.getUpstreamVersion(), "2");
    assertEquals(version.getDebianRevision(), "3");

    version = Version.parse("1:2.3~four-5-1.5~alpha");
    assertEquals(version.getEpoch(), 1);
    assertEquals(version.getUpstreamVersion(), "2.3~four-5");
    assertEquals(version.getDebianRevision(), "1.5~alpha");

  }


  @Test
  public void testComparision() throws Exception {

    assertTrue(0 > Version.parse("0"    ).compareTo(Version.parse("1"   )));
    assertTrue(0 > Version.parse("0:9"  ).compareTo(Version.parse("1:0" )));
    assertTrue(0 > Version.parse("1"    ).compareTo(Version.parse("1.1" )));

    assertTrue(0 > Version.parse("1"    ).compareTo(Version.parse("1a"  )));
    assertTrue(0 > Version.parse("1~"   ).compareTo(Version.parse("1"   )));
    assertTrue(0 > Version.parse("1~~a" ).compareTo(Version.parse("1~"  )));
    assertTrue(0 > Version.parse("1~~"  ).compareTo(Version.parse("1~~a")));

    assertTrue(0 > Version.parse("1"    ).compareTo(Version.parse("1-1" )));
    assertTrue(0 > Version.parse("1"    ).compareTo(Version.parse("1.0" )));

    assertEquals(0, Version.parse("0:1" ).compareTo(Version.parse("1"   )));
    assertEquals(0, Version.parse("1.0" ).compareTo(Version.parse("1."  )));

  }

  @Test
  public void testComplexVersionComparisionRegression() throws Exception {

    assertTrue(0 < Version.parse("2020.2.1-401-release").compareTo(Version.parse("2020.2")));
    assertTrue(0 > Version.parse("2020.2").compareTo(Version.parse("2020.2.1-401-release")));

    assertTrue(0 > Version.parse("2020.2-512-develop").compareTo(Version.parse("2020.2.1")));
    assertTrue(0 < Version.parse("2020.2.1").compareTo(Version.parse("2020.2-512-develop")));

  }
}
