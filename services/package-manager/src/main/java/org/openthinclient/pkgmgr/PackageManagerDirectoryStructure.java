package org.openthinclient.pkgmgr;

import org.openthinclient.pkgmgr.db.Source;

import java.nio.file.Path;

public interface PackageManagerDirectoryStructure {

   Path changelogFileLocation(Source source, org.openthinclient.pkgmgr.db.Package pkg);

}
