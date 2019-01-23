package org.openthinclient.sysreport.generate;

import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.sysreport.StatisticsReport;

import java.util.Set;

public class ConfigurationSummaryReportContributor implements ReportContributor<StatisticsReport> {
  private final ClientService clientService;
  private final ApplicationService applicationService;

  public ConfigurationSummaryReportContributor(ClientService clientService, ApplicationService applicationService) {
    this.clientService = clientService;
    this.applicationService = applicationService;
  }

  @Override
  public void contribute(StatisticsReport report) {

    // this is very likely to be extremely inefficient. As the code will be executed only once in a
    // while, it should be acceptable
    final Set<Client> allClients = clientService.findAll();
    report.getConfiguration().setThinClientCount(allClients.size());

    final Set<Application> allApplications = applicationService.findAll();
    report.getConfiguration().setApplicationCount(allApplications.size());

    // FIXME after merging the WebDevice Management branch, we have to add the group-object statistics
    // report.getConfiguration().setApplicationGroupCount(FIXME);
    // report.getConfiguration().setThinClientGroupCount(FIXME);
  }
}
