package org.openthinclient.service.update;

import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

public class UpdateRunner {

  @Autowired
  private ApplicationContext applicationContext;
  @Autowired
  private ManagerHome managerHome;

  @Value("${otc.application.version.update.process}")
  private String updateProcess;

  private boolean isRunning = false;

  public boolean isRunning() {
    return this.isRunning;
  }

  public void run() {
    isRunning = true;
    new Thread(() -> {
      PackageManagerConfiguration configuration = managerHome.getConfiguration(PackageManagerConfiguration.class);
      RuntimeProcessExecutor.executeManagerUpdateCheck(updateProcess, configuration.getProxyConfiguration(), exitValue -> {
        isRunning = false;
        applicationContext.publishEvent(new UpdateRunnerEvent(this, exitValue));
      });
    }).start();
  }
}
