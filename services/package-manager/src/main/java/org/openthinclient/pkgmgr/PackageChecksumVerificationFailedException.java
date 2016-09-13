package org.openthinclient.pkgmgr;

import org.openthinclient.pkgmgr.db.Package;

public class PackageChecksumVerificationFailedException extends PackageManagerException {

    /** serialVersionUID */
    private static final long serialVersionUID = 2179768247437683202L;
    
    private final Package pkg;
    private final String md5sum;

    public PackageChecksumVerificationFailedException(String message, Package pkg, String md5sum) {
        super(message);
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
