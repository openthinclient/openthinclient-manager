package org.openthinclient.pkgmgr.op;

import org.openthinclient.pkgmgr.db.Package;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

public interface PackageOperationContext {

   OutputStream createFile(Package pkg, Path path) throws IOException;

   void createDirectory(Package pkg, Path path) throws IOException;

   void createSymlink(Package pkg, Path link, Path target) throws IOException;

}
