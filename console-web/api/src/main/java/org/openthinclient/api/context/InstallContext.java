package org.openthinclient.api.context;

import org.openthinclient.DownloadManagerFactory;
import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
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

  public DownloadManager getDownloadManager() {
    if (managerHome == null)
      return null;

    final PackageManagerConfiguration configuration = managerHome.getConfiguration(PackageManagerConfiguration.class);
    return DownloadManagerFactory.create(managerHome.getMetadata().getServerID(), configuration.getProxyConfiguration());
  }
}
