package org.openthinclient.sysreport.generate;

import org.openthinclient.common.model.service.*;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.license.LicenseManager;
import org.openthinclient.sysreport.StatisticsReport;

public class StatisticsReportGenerator extends AbstractReportGenerator<StatisticsReport> {
  public StatisticsReportGenerator(ManagerHome managerHome, PackageManager packageManager, ClientService clientService,
                                   ApplicationService applicationService, ApplicationGroupService applicationGroupService,
                                   ClientGroupService clientGroupService, RealmService realmService,
                                   UserService userService,
                                   UserGroupService userGroupService,
                                   DeviceService deviceService,
                                   LocationService locationService,
                                   PrinterService printerService,
                                   HardwareTypeService hardwareTypeService,
                                   LicenseManager licenseManager) {
    super(managerHome);
    contributors.add(new NetworkInterfaceContributor());
    contributors.add(new PackageManagerSummaryReportContributor(packageManager));
    contributors.add(new ConfigurationSummaryReportContributor(managerHome, licenseManager, clientService,
        clientGroupService, applicationService,
        applicationGroupService,
        realmService, userService,
        userGroupService,
        deviceService,
        locationService,
        printerService,
        hardwareTypeService));
  }

  @Override
  protected StatisticsReport createReportInstance() {
    return new StatisticsReport();
  }
}
