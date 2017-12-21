package org.openthinclient.web.pkgmngr.ui.view;

import org.openthinclient.pkgmgr.db.Package;
import org.openthinclient.util.dpkg.PackageReference;

/**
 * A resolved package is available for installation/deinstallation
 */
public class ResolvedPackageItem extends AbstractPackageItem {

   private final Package _package;
   private final String name;
   private final String displayVersion;
   
   public ResolvedPackageItem(Package _package) {
      this._package = _package;
      this.name = _package.getName();
      this.displayVersion = _package.getDisplayVersion();
   }

    public ResolvedPackageItem(PackageReference.SingleReference reference) {
        this._package = null;
        this.name = reference.getName();
        if (reference.getVersion() != null) {
            this.displayVersion = reference.getRelation() + " " + reference.getVersion().toStringWithoutEpoch();
        } else {
            this.displayVersion = "";
        }
    }

    public Package getPackage() {
      return _package;
   }

   @Override
   public String getName() {
      return name;
   }

   @Override
   public String getDisplayVersion() {
      return displayVersion;
   }
   
}
