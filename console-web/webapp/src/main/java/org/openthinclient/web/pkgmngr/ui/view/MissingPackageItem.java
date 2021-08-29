package org.openthinclient.web.pkgmngr.ui.view;

/**
 * A MissingPackageItem is a declared package (by some other package) which is not available for installation/deinstallation
 */
public class MissingPackageItem extends AbstractPackageItem {

   private final String name;
   private final String displayVersion;

    public MissingPackageItem(String name, String displayVersion) {
      this.name = name;
      this.displayVersion = displayVersion;
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
