package org.openthinclient.web.pkgmngr.ui.presenter;

import org.openthinclient.util.dpkg.PackageReference;
import org.openthinclient.web.pkgmngr.ui.view.MissingPackageItem;

/**
 * A util/helper class
 */
public class PackageDetailsUtil {

    /**
     * Creates a missing PackageItem
     * @param sr PackageReference
     * @return a MissingPackageItem object
     */
    public static MissingPackageItem createMissingPackageItem(PackageReference.SingleReference sr) {
        return createMissingPackageItem(" (Missing)", sr);
    }

    /**
     * Creates a missing PackageItem
     * @param missingLabel a Label to mark the version
     * @param sr PackageReference
     * @return a MissingPackageItem object
     */
    public static MissingPackageItem createMissingPackageItem(String missingLabel, PackageReference.SingleReference sr) {
        String relation = sr.getRelation() != null ? sr.getRelation().getTextualRepresentation() + " " : "";
        String version  = sr.getVersion() != null ? sr.getVersion().toStringWithoutEpoch() : "";
        return new MissingPackageItem(sr.getName() + missingLabel, relation.concat(version));
    }
}
