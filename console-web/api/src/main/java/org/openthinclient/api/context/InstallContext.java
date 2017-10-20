package org.openthinclient.api.context;

import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.service.common.home.ManagerHome;
import org.springframework.context.ConfigurableApplicationContext;

public class InstallContext {

  private PackageManager packageManager;
  private ConfigurableApplicationContext context;
  private ManagerHome managerHome;

  public ConfigurableApplicationContext getContext() {
    return context;
  }

  public void setContext(ConfigurableApplicationContext context) {
    this.context = context;
  }

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
