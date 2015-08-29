package org.openthinclient.wizard.model;

import org.openthinclient.pkgmgr.Source;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.openthinclient.wizard.install.InstallSystemTask;
import org.openthinclient.wizard.install.InstallableDistribution;
import org.springframework.core.task.AsyncListenableTaskExecutor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class InstallModel {

  private final List<InstallableDistribution> installableDistributions;
  private final AsyncListenableTaskExecutor taskExecutor;
  private final DirectoryModel directoryModel;
  private volatile InstallSystemTask installSystemTask;

  public InstallModel(AsyncListenableTaskExecutor taskExecutor, DirectoryModel directoryModel) {
    this.taskExecutor = taskExecutor;
    this.directoryModel = directoryModel;
    installableDistributions = new ArrayList<>();

    final InstallableDistribution distribution = new InstallableDistribution("openthinclient consus", "Version 2 of the openthinclient operating system");

    distribution.getMinimumPackages().add("base");
    final Source source = new Source();
    source.setType(Source.Type.PACKAGE);
    source.setDescription("Rolling");
    source.setEnabled(true);
    try {
      source.setUrl(new URL("http://archive.openthinclient.org/openthinclient/v2/manager-rolling"));
    } catch (MalformedURLException e) {
      // this exception should not happen, as the url is specified above
      throw new RuntimeException("Failed to create URL instance");
    }
    source.setDistribution("./");
    distribution.getSourcesList().getSources().add(source);

    installableDistributions.add(distribution);

  }

  public List<InstallableDistribution> getInstallableDistributions() {
    return installableDistributions;
  }

  public boolean isInstallInProgress() {
    return installSystemTask != null;
  }

  public InstallSystemTask installSystem(InstallableDistribution installableDistribution) {
    installSystemTask = new InstallSystemTask(new ManagerHomeFactory(), installableDistribution, directoryModel);
    taskExecutor.submitListenable(installSystemTask);

    return installSystemTask;
  }

  public InstallSystemTask getInstallSystemTask() {
    return installSystemTask;
  }
}
