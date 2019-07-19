package org.openthinclient.sysreport.generate;

import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.service.ApplicationGroupService;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.ClientGroupService;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.sysreport.StatisticsReport;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ConfigurationSummaryReportContributor implements ReportContributor<StatisticsReport> {
  private final ClientService clientService;
  private final ApplicationService applicationService;
  private final ApplicationGroupService applicationGroupService;
  private final ClientGroupService clientGroupService;

  public ConfigurationSummaryReportContributor(ClientService clientService, ApplicationService applicationService, ApplicationGroupService applicationGroupService, ClientGroupService clientGroupService) {
    this.clientService = clientService;
    this.applicationService = applicationService;
    this.applicationGroupService = applicationGroupService;
    this.clientGroupService = clientGroupService;
  }

  @Override
  public void contribute(StatisticsReport report) {

    // this is very likely to be extremely inefficient. As the code will be executed only once in a
    // while, it should be acceptable
    final int allClients = clientService.count();
    report.getConfiguration().setThinClientCount(allClients);

    report.getConfiguration().setApplicationGroupCount(applicationGroupService.findAll().size());
    report.getConfiguration().setThinClientGroupCount(clientGroupService.findAll().size());

    final Set<Application> applications = applicationService.findAll();

    final Map<String, Long> applicationCounts = applications.stream() //
            .map(application -> application.getSchema(application.getRealm())) //
            .map(Schema::getName) //
            .peek(System.err::println)
            .collect(Collectors.groupingBy(k -> k, Collectors.counting()));

    TreeMap<String, Long> sortedApplicationCounts = new TreeMap<>(applicationCounts);

    report.getConfiguration().setApplications(sortedApplicationCounts);
  }
}
