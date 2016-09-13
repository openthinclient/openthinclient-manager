package org.openthinclient.web.pkgmngr.ui.view;

import org.openthinclient.web.pkgmngr.ui.design.PackageManagerMainDesign;

public class PackageManagerMainView extends PackageManagerMainDesign {

  /** serialVersionUID */
  private static final long serialVersionUID = 9193433664185414165L;

  public PackageListMasterDetailsView getAvailablePackagesView() {
    return availablePackages;
  }

  public PackageListMasterDetailsView getInstalledPackagesView() {
    return installedPackages;
  }
}
