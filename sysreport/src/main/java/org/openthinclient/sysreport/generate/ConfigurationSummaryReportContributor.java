package org.openthinclient.sysreport.generate;

import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.ApplicationGroup;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Device;
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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

public class ConfigurationSummaryReportContributor implements ReportContributor<StatisticsReport> {


  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationSummaryReportContributor.class);

  private final ClientService clientService;
  private final UnrecognizedClientService unrecognizedClientService;
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
                                               UnrecognizedClientService unrecognizedClientService,
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
    this.unrecognizedClientService = unrecognizedClientService;
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

    report.getConfiguration().setThinClientCount(clientService.count());
    report.getConfiguration().setUnrecognizedClientCount(unrecognizedClientService.count());

    // this is very likely to be extremely inefficient. As the code will be executed only once in a
    // while, it should be acceptable
    Set<Client> clients = clientService.findAll();
    report.getConfiguration().setThinClientCount(clients.size());

    Set<ApplicationGroup> applicationGroups = applicationGroupService.findAll();
    report.getConfiguration().setApplicationGroupCount(applicationGroups.size());
    report.getConfiguration().setThinClientGroupCount(clientGroupService.findAll().size());

    // application usage
    Set<Application> applications = applicationService.findAll();
    report.getConfiguration().setApplications(countTypes(applications));

    // application-clients usage with group handling
    ClientMap<Application> appClients = getCollectClientsByProfile(clients, Client::getApplications);
    for (ApplicationGroup applicationGroup : applicationGroups) { // applicationGroup and clients
      for (Application groupedApplication : applicationGroup.getApplications()) {
        Set<Client> applicationGroupClients = (Set<Client>) applicationGroup.getMembers().stream()
            .filter(Objects::nonNull)
            .filter(o -> o instanceof Client)
            .collect(toSet());
        appClients.get(groupedApplication).addAll(applicationGroupClients);
      }
    }
    report.getConfiguration().setApplicationTypeUsage(countSchemaObjects(applications, appClients));

    // hardwaretype usage
    Set<HardwareType> hardwareTypes = hardwareTypeService.findAll();
    report.getConfiguration().setHardwaretypes(countTypes(hardwareTypes));
    ClientMap<HardwareType> hwtClients = getCollectClientsByProfile(clients, Client::getHwTypes);
    report.getConfiguration().setHardwaretypeUsage(countSchemaObjects(hardwareTypes, hwtClients));

    // device usage
    Set<Device> devices = deviceService.findAll();
    report.getConfiguration().setDevices(countTypes(devices));
    ClientMap<Device> deviceClients = getCollectClientsByProfile(clients, Client::getDevices);
    for (HardwareType hardwaretype : hardwareTypes) {
      for (Device device : hardwaretype.getDevices()) {
        deviceClients.get(device).addAll(hwtClients.get(hardwaretype));
      }
    }
    report.getConfiguration().setDeviceTypeUsage(countSchemaObjects(devices, deviceClients));

    // location usage
    Set<Location> locations = locationService.findAll();
    report.getConfiguration().setLocations(countTypes(locations));
    Map<Location, Set<Client>> locationClients = getCollectClientsByProfile(clients, client -> Stream.of(client.getLocation()).collect(Collectors.toSet()));
    report.getConfiguration().setLocationUsage(countSchemaObjects(locations, locationClients));

    // printer usage
    Set<Printer> printers = printerService.findAll();
    report.getConfiguration().setPrinters(countTypes(printers));
    Map<Printer, Set<Client>> printerClients = getCollectClientsByProfile(clients, Client::getPrinters);
    for (Location location: locations) {
      for (Printer printer : location.getPrinters()) {
        if (printerClients.containsKey(printer)) {
          printerClients.get(printer).addAll(locationClients.get(location));
        } else {
          printerClients.put(printer, locationClients.get(location));
        }
      }
    }
    report.getConfiguration().setPrinterUsage(countSchemaObjects(printers, printerClients));

    // secondary ldap
    String secondaryLdapUrl = realmService.getDefaultRealm().getValue("Directory.Secondary.LDAPURLs");
    String version = realmService.getDefaultRealm().getValue("UserGroupSettings.DirectoryVersion");
    boolean secondaryLdapActive = version != null && version.equals("secondary") && secondaryLdapUrl != null && secondaryLdapUrl.length() > 0;
    report.getConfiguration().setSecondaryLdapActive(secondaryLdapActive);
    // user count on primary ldap
    if (!secondaryLdapActive) {
      report.getConfiguration().setPrimaryLdapUserCount(userService.count());
      report.getConfiguration().setPrimaryLdapUserGroupCount(userGroupService.count());
    }

    // licence usage
    report.getConfiguration().setLicenseState(licenseManager.getLicenseState(clients.size()).name());
    License license = licenseManager.getLicense();
    if(license != null) {
      report.getConfiguration().setLicenseCount(license.getCount());
      report.getConfiguration().setLicenseSoftExpiredDate(license.getSoftExpiredDate());
      report.getConfiguration().setLicenseExpiredDate(license.getExpiredDate());
    }

  }

  /**
   * Grouping given members with the given clients
   * @param clients to be grouped
   * @param function to be called to obtain members of client
   * @param <T> Profiles
   * @return Map of members with clients
   */
  protected <T extends Profile> ClientMap<T> getCollectClientsByProfile(Set<Client> clients, Function<Client, Set<T>> function) {
    ClientMap<T> profileClients = new ClientMap<>();
    for (Client client : clients) {
      for (T profile : function.apply(client)) {
        if(profile != null) {
          profileClients.get(profile).add(client);
        }
      }
    }
    return profileClients;
  }

  protected <T extends Profile> Map<String, Long> countSchemaObjects(Set<T> allProfiles, Map<T, Set<Client>> clients) {
    Map<String, Set<Client>> schemaTCcount = new HashMap<>();  // transform profile to schema and a SET of clients
    for (Map.Entry<T, Set<Client>> ac : clients.entrySet()) {
      getProfileWithRealm(allProfiles, ac.getKey()).ifPresent(profile -> {
        String schemaName = "";
        try {
          schemaName = profile.getSchema(profile.getRealm()).getName();
        } catch (Exception e) {
          LOGGER.warn("Cannot create statistics for profile " + profile.getName() + ", reason: " + e.getMessage());
        }
        schemaTCcount.compute(schemaName, (k, v) -> (v == null) ? ac.getValue() : Stream.concat(v.stream(), ac.getValue().stream()).collect(toSet()));
      });
    }
    final Map<String, Long> objectsCounts = new HashMap<>();
    schemaTCcount.forEach((s, c) -> objectsCounts.put(s, (long) c.size()));
    return new TreeMap<>(objectsCounts);
  }

  protected <T extends Profile> Optional<T> getProfileWithRealm(Set<T> allProfiles, T capp) {
    if (allProfiles == null || capp == null || capp.getDn() == null) {
      return Optional.empty();
    }
    return allProfiles.stream()
        .filter(profile -> profile.getDn() != null ? profile.getDn().equals(capp.getDn()) : profile.getName().equals(capp.getName()))
        .findFirst();
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

  /**
   * simple LazyMap from a T to a Set of Clients
   */
  private class ClientMap<T> extends HashMap<T, Set<Client>> {
    @Override
    public Set<Client> get(Object key) {
      Set<Client> clientSet = super.get(key);
      if (clientSet == null) {
        clientSet = new HashSet<Client>();
        this.put((T) key, clientSet);
      }
      return clientSet;
    }
  }

}
