package org.openthinclient.wizard.install;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.service.common.home.ManagerHome;

public class InstallContext {

  private PackageManager packageManager;

  private ManagerHome managerHome;

  public ManagerHome getManagerHome() {
    return managerHome;
  }

  public void setManagerHome(ManagerHome managerHome) {
    this.managerHome = managerHome;
  }

  public PackageManager getPackageManager() {
    return packageManager;
  }

  public void setPackageManager(PackageManager packageManager) {
    this.packageManager = packageManager;
  }
}
