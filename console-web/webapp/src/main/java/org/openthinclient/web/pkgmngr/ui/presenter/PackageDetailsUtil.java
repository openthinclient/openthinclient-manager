package org.openthinclient.web.pkgmngr.ui.presenter;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.util.dpkg.PackageReference;
import org.openthinclient.util.dpkg.PackageReferenceList;
import org.openthinclient.web.pkgmngr.ui.view.AbstractPackageItem;
import org.openthinclient.web.pkgmngr.ui.view.MissingPackageItem;
import org.openthinclient.web.pkgmngr.ui.view.ResolvedPackageItem;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * Returns a list of references package items. This method check if conflicts/provides/dependes has available packages
     * @param packageReferenceList PackageReferenceList
     * @param availablePackages List<Package>
     * @param usedPackages List<String>
     * @return a List<AbstractPackageItem>
     */
    public static List<AbstractPackageItem> getReferencedPackageItems(PackageReferenceList packageReferenceList, List<Package> availablePackages, List<String> usedPackages) {
        List<AbstractPackageItem> items = new ArrayList<>();
        for (PackageReference pr : packageReferenceList) {
            boolean isReferenced = false;
            for (Package _package : availablePackages) {
                if (pr.matches(_package) && !usedPackages.contains(_package.getName())) {
                    items.add(new ResolvedPackageItem((PackageReference.SingleReference) pr));
                    isReferenced = true;
                    usedPackages.add(_package.getName());
                }
            }
            if (!isReferenced) {
                if (pr instanceof PackageReference.SingleReference) {
                    items.add(PackageDetailsUtil.createMissingPackageItem("", (PackageReference.SingleReference) pr));
                }
            }
        }
        return items;
    }
}
