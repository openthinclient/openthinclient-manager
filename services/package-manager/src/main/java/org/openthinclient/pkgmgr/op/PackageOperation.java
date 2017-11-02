package org.openthinclient.pkgmgr.op;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.progress.ProgressReceiver;

import java.io.IOException;

public interface PackageOperation {

    /**
     * The {@link Package} that this operation pertains to.
     *
     * @return the {@link Package} this operation pertains to.
     */
    Package getPackage();

    void execute(PackageOperationContext context, ProgressReceiver progressReceiver) throws IOException;
}
