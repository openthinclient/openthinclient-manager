package org.openthinclient.sysreport.generate;

import org.openthinclient.common.model.service.ApplicationGroupService;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.ClientGroupService;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.sysreport.StatisticsReport;

public class StatisticsReportGenerator extends AbstractReportGenerator<StatisticsReport> {
  public StatisticsReportGenerator(ManagerHome managerHome, PackageManager packageManager, ClientService clientService,
                                   ApplicationService applicationService, ApplicationGroupService applicationGroupService,
                                   ClientGroupService clientGroupService) {
    super(managerHome);
    contributors.add(new NetworkInterfaceContributor());
    contributors.add(new PackageManagerSummaryReportContributor(packageManager));
    contributors.add(new ConfigurationSummaryReportContributor(clientService, applicationService, applicationGroupService, clientGroupService));
  }

  @Override
  protected StatisticsReport createReportInstance() {
    return new StatisticsReport();
  }
}
