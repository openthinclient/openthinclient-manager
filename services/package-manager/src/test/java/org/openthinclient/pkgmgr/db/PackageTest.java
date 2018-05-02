package org.openthinclient.pkgmgr.db;

import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

public class PackageTest {

  @Test
  public void testEqualsIgnoresTheEntityId() throws Exception {

    // For some logic to work, the id of the entity will not be part of the equals comparison
    // FIXME that is inconsistent and should be changed to compare all fields in equals
    final Package pkg = new Package();
    setId(pkg, 1);

    final Package pkg2 = new Package();
    setId(pkg2, 12);
    assertEquals(pkg2, pkg);
  }

  @Test
  public void testEqualsIgnoresTheChangeLog() {
    // similar to id, equals ignores the changelog during comparision

    final Package pkg1 = new Package();
    pkg1.setChangeLog("Changelog 1");
    final Package pkg2 = new Package();
    pkg2.setChangeLog("Changelog 1");

    assertEquals(pkg1, pkg2);

  }

  private void setId(Package pkg, long id) throws Exception {

    final Field idField = Package.class.getDeclaredField("id");
    idField.setAccessible(true);
    idField.set(pkg, id);

  }
}