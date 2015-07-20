package org.openthinclient.pkgmgr.impl;

import org.openthinclient.util.dpkg.PackageDatabase;

public class SerializationPackageDatabaseTest  extends AbstractPackageDatabaseTestBase {
  public SerializationPackageDatabaseTest() {
    super(new PackageDatabase.SerializationPackageDatabaseFactory());
  }
}
