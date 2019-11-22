package org.openthinclient.sysreport.generate;

import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Device;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.Printer;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.service.*;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.license.License;
import org.openthinclient.service.common.license.LicenseManager;
import org.openthinclient.sysreport.StatisticsReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ConfigurationSummaryReportContributor implements ReportContributor<StatisticsReport> {


  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationSummaryReportContributor.class);

  private final ClientService clientService;
  private final ApplicationService applicationService;
  private final ApplicationGroupService applicationGroupService;
  private final ClientGroupService clientGroupService;
  private final RealmService realmService;
  private final UserService userService;
  private final UserGroupService userGroupService;
  private final DeviceService deviceService;
  private final LocationService locationService;
  private final PrinterService printerService;
  private final HardwareTypeService hardwareTypeService;
  private final LicenseManager licenseManager;
  private final ManagerHome managerHome;

  public ConfigurationSummaryReportContributor(ManagerHome managerHome,
                                               LicenseManager licenseManager,
                                               ClientService clientService,
                                               ClientGroupService clientGroupService,
                                               ApplicationService applicationService,
                                               ApplicationGroupService applicationGroupService,
                                               RealmService realmService,
                                               UserService userService,
                                               UserGroupService userGroupService,
                                               DeviceService deviceService,
                                               LocationService locationService,
                                               PrinterService printerService,
                                               HardwareTypeService hardwareTypeService) {
    this.clientService = clientService;
    this.applicationService = applicationService;
    this.applicationGroupService = applicationGroupService;
    this.clientGroupService = clientGroupService;
    this.realmService = realmService;
    this.userService = userService;
    this.userGroupService = userGroupService;
    this.deviceService = deviceService;
    this.locationService = locationService;
    this.printerService = printerService;
    this.hardwareTypeService = hardwareTypeService;
    this.licenseManager = licenseManager;
    this.managerHome = managerHome;
  }

  @Override
  public void contribute(StatisticsReport report) {

    // this is very likely to be extremely inefficient. As the code will be executed only once in a
    // while, it should be acceptable
    Set<Client> clients = clientService.findAll();
    report.getConfiguration().setThinClientCount(clients.size());

    report.getConfiguration().setApplicationGroupCount(applicationGroupService.findAll().size());
    report.getConfiguration().setThinClientGroupCount(clientGroupService.findAll().size());

    // application usage
    Set<Application> applications = applicationService.findAll();
    report.getConfiguration().setApplications(countTypes(applications));
    Set<String> allAppsAttachedToClients = clients.stream()
        .flatMap(client -> client.getApplications().stream())
        .filter(Objects::nonNull)
        .map(DirectoryObject::getDn)
        .collect(Collectors.toSet());
    report.getConfiguration().setApplicationUsage(countObjects(applications, allAppsAttachedToClients));

    // device usage
    Set<Device> devices = deviceService.findAll();
    report.getConfiguration().setDevices(countTypes(devices));
    Set<String> allDevicesAttachedToClients = clients.stream()
        .flatMap(client -> client.getDevices().stream())
        .filter(Objects::nonNull)
        .map(DirectoryObject::getDn)
        .collect(Collectors.toSet());
    report.getConfiguration().setDeviceUsage(countObjects(devices, allDevicesAttachedToClients));

    // printer usage
    Set<Printer> printers = printerService.findAll();
    report.getConfiguration().setPrinters(countTypes(printers));
    Set<String> allPrintersAttachedToClients = clients.stream()
        .flatMap(client -> client.getPrinters().stream())
        .filter(Objects::nonNull)
        .map(DirectoryObject::getDn)
        .collect(Collectors.toSet());
    report.getConfiguration().setPrinterUsage(countObjects(printers, allPrintersAttachedToClients));

    // location usage
    Set<Location> locations = locationService.findAll();
    report.getConfiguration().setLocations(countTypes(locations));
    Set<String> allLocationAttachedToClients = clients.stream()
        .map(Client::getLocation)
        .filter(Objects::nonNull)
        .map(DirectoryObject::getDn)
        .collect(Collectors.toSet());
    report.getConfiguration().setLocationUsage(countObjects(locations, allLocationAttachedToClients));

    // hardwaretype usage
    Set<HardwareType> hardwareTypes = hardwareTypeService.findAll();
    report.getConfiguration().setHardwaretypes(countTypes(hardwareTypes));
    Set<String> allHardwaretypesAttachedToClients = clients.stream()
        .map(Client::getHardwareType)
        .filter(Objects::nonNull)
        .map(DirectoryObject::getDn)
        .collect(Collectors.toSet());
    report.getConfiguration().setHardwaretypeUsage(countObjects(hardwareTypes, allHardwaretypesAttachedToClients));

    // secondary ldap
    String secondaryLdapUrl = realmService.getDefaultRealm().getValue("Directory.Secondary.LDAPURLs");
    boolean secondaryLdapActive = secondaryLdapUrl != null && secondaryLdapUrl.length() > 0;
    report.getConfiguration().setSecondaryLdapActive(secondaryLdapActive);
    // user count on primary ldap
    if (!secondaryLdapActive) {
      report.getConfiguration().setPrimaryLdapUserCount(userService.count());
      report.getConfiguration().setPrimaryLdapUserGroupCount(userGroupService.count());
    }

    // licence usage
    License license = licenseManager.getLicense();
    report.getConfiguration().setLicenseCount(license.getCount());
    report.getConfiguration().setLicenseSoftExpiredDate(license.getSoftExpiredDate());
    report.getConfiguration().setLicenseExpiredDate(license.getExpiredDate());
    report.getConfiguration().setLicenseState(license.getState(managerHome.getMetadata().getServerID(), clients.size()).name());


  }

  private <T extends Profile> Map<String, Long> countTypes(Set<T> objects) {
    final Map<String, Long> typeCounts = objects.stream() //
        .map(profile -> {
          try {
            return profile.getSchema(profile.getRealm());
          } catch (Exception e) {
            LOGGER.warn("Cannot create statistics for profile " + profile.getName() + ", reason: " + e.getMessage());
            return null;
          }
          })
        .filter(Objects::nonNull)
        .map(Schema::getName) //
        .collect(Collectors.groupingBy(k -> k, Collectors.counting()));
    return new TreeMap<>(typeCounts);
  }

  private <T extends Profile> Map<String, Long> countObjects(Set<T> availableObjects, Set<String> objectsAttachedToClient) {
    final Map<String, Long> objectsCounts = new HashMap<>();
    availableObjects.forEach(profile -> {
      if (objectsAttachedToClient.contains(profile.getDn())) {
        try {
          Schema schema = profile.getSchema(profile.getRealm());
          objectsCounts.compute(schema.getKey(), (k, v) ->  (v==null) ? 1 : v+1);
        } catch (Exception e) {
          LOGGER.warn("Cannot create statistics for profile " + profile.getName() + ", reason: " + e.getMessage());
          objectsCounts.compute(profile.getName(), (k, v) ->  (v==null) ? 1 : v+1);
        }
      }
    });
    return new TreeMap<>(objectsCounts);
  }
}
