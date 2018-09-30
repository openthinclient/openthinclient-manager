package org.openthinclient.sysreport.generate;

import org.apache.commons.lang3.SystemUtils;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.sysreport.AbstractReport;

public class ServerReportContributor implements ReportContributor<AbstractReport> {
  private final ManagerHome managerHome;

  public ServerReportContributor(ManagerHome managerHome) {
    this.managerHome = managerHome;
  }

  @Override
  public void contribute(AbstractReport report) {

    report.getServer().setServerId(managerHome.getMetadata().getServerID());

    report.getServer().getOS().setArch(SystemUtils.OS_ARCH);
    report.getServer().getOS().setName(SystemUtils.OS_NAME);
    report.getServer().getOS().setVersion(SystemUtils.OS_VERSION);

    report.getServer().setEnvironment(System.getenv());


  }
}
