package org.openthinclient.pkgmgr.impl;

public class MapDBPackageDatabaseTest extends AbstractPackageDatabaseTestBase {
  public MapDBPackageDatabaseTest() {
    super(new MapDBPackageDatabase.MapDBPackageDatabaseFactory());
  }
}
