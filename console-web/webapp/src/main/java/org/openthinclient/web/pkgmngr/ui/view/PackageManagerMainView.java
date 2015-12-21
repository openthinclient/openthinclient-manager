package org.openthinclient.web.pkgmngr.ui.view;

import org.openthinclient.web.pkgmngr.ui.design.PackageManagerMainDesign;

public class PackageManagerMainView extends PackageManagerMainDesign {

  public PackageListMasterDetailsView getAvailablePackagesView() {
    return availablePackages;
  }

  public PackageListMasterDetailsView getInstalledPackagesView() {
    return installedPackages;
  }
}
