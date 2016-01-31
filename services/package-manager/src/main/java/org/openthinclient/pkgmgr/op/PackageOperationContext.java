package org.openthinclient.pkgmgr.op;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

public interface PackageOperationContext {

   OutputStream createFile(Path path) throws IOException;

   void createDirectory(Path path) throws IOException;

   void createSymlink(Path link, Path target) throws IOException;

}
