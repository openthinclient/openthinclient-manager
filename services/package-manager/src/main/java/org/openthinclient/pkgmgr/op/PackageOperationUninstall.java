package org.openthinclient.pkgmgr.op;

import org.openthinclient.pkgmgr.db.Package;

public class PackageOperationUninstall implements PackageOperation {

    private final Package pkgToUninstall;

    public PackageOperationUninstall(Package pkgToUninstall) {
        this.pkgToUninstall = pkgToUninstall;
    }

    @Override
    public Package getPackage() {
        return pkgToUninstall;
    }

    @Override
    public void execute(PackageOperationContext context) {

    }
}
