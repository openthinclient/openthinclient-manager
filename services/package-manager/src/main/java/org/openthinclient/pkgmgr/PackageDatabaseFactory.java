package org.openthinclient.pkgmgr;

import java.io.IOException;
import java.nio.file.Path;

public interface PackageDatabaseFactory {

  PackageDatabase create(Path targetPath) throws IOException;

}
