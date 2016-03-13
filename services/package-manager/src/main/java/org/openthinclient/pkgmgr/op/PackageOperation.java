package org.openthinclient.pkgmgr.op;

import java.io.IOException;

public interface PackageOperation {

    void execute(PackageOperationContext context) throws IOException;
}
