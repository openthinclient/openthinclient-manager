package org.openthinclient.web.pkgmngr.ui.view;

import org.openthinclient.pkgmgr.db.Package;

/**
 * A resolved package is available for installation/deinstallation
 */
public class ResolvedPackageItem extends AbstractPackageItem {

   private final Package _package;
   
   public ResolvedPackageItem(Package _package) {
      this._package = _package;
   }
   
   public Package getPackage() {
      return _package;
   }

   @Override
   public String getName() {
      return _package.getName();
   }

   @Override
   public String getDisplayVersion() {
      return _package.getDisplayVersion();
   }
   
}
