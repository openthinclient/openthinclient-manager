package org.openthinclient.pkgmgr;

import org.openthinclient.pkgmgr.db.Package;

public class PackageChecksumVerificationFailedException extends PackageManagerException {

    private final Package pkg;
    private final String md5sum;

    public PackageChecksumVerificationFailedException(Package pkg, String md5sum) {
        this.pkg = pkg;
        this.md5sum = md5sum;
    }

    public Package getPackage() {
        return pkg;
    }

    public String getMD5sum() {
        return md5sum;
    }
}
