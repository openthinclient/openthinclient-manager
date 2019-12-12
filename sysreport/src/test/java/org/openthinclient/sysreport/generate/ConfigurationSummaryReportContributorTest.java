package org.openthinclient.sysreport.generate;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.ApplicationGroup;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Device;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.Printer;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.service.*;
import org.openthinclient.pkgmgr.PackageManager;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.ManagerHomeMetadata;
import org.openthinclient.service.common.license.License;
import org.openthinclient.service.common.license.LicenseManager;
import org.openthinclient.sysreport.StatisticsReport;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class ConfigurationSummaryReportContributorTest {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();
  @Mock
  private ManagerHome managerHome;
  @Mock
  private PackageManager packageManager;
  @Mock
  private ClientService clientService;
  @Mock
  private ApplicationService applicationService;
  @Mock
  private ApplicationGroupService applicationGroupService;
  @Mock
  private ClientGroupService clientGroupService;
  @Mock
  private RealmService realmService;
  @Mock
  private UserService userService;
  @Mock
  private UserGroupService userGroupService;
  @Mock
  private DeviceService deviceService;
  @Mock
  private LocationService locationService;
  @Mock
  private PrinterService printerService;
  @Mock
  private HardwareTypeService hardwareTypeService;
  @Mock
  private LicenseManager licenseManager;

  @Test
  public void contribute() {

    ConfigurationSummaryReportContributor csrc = new ConfigurationSummaryReportContributor(managerHome, licenseManager, clientService,
        clientGroupService, applicationService,
        applicationGroupService,
        realmService, userService,
        userGroupService,
        deviceService,
        locationService,
        printerService,
        hardwareTypeService);

    // prepare
    Mockito.when(managerHome.getMetadata()).thenReturn(getManagerHomeMetaData());
    Mockito.when(realmService.getDefaultRealm()).thenReturn(new Realm());
    Mockito.when(licenseManager.getLicense()).thenReturn(new License());

    Location l1 = createLocation("l1");
    Location l2 = createLocation("l2");
    Set<Location> locations = Stream.of(l1, l2).collect(Collectors.toSet());
    Mockito.when(locationService.findAll()).thenReturn(locations);

    HardwareType h1 = createHardwareType("h1");
    HardwareType h2 = createHardwareType("h2");
    Set<HardwareType> hardwareTypes = Stream.of(h1, h2).collect(Collectors.toSet());
    Mockito.when(hardwareTypeService.findAll()).thenReturn(hardwareTypes);

    Printer p1 = createPrinter("p1");
    Printer p2 = createPrinter("p2");
    Set<Printer> printers = Stream.of(p1, p2).collect(Collectors.toSet());
    Mockito.when(printerService.findAll()).thenReturn(printers);

    Application d1 = createProfile(new Application(), "d1", "desktop");
    Application d2 = createProfile(new Application(), "d2", "desktop");
    Application rdp1 = createProfile(new Application(), "rdp1", "RDP");
    Application rdp2 = createProfile(new Application(), "rdp2", "RDP");
    Application rdp3 = createProfile(new Application(), "rdp3", "RDP");
    Set<Application> applications = Stream.of(d1, d2, rdp1, rdp2, rdp3).collect(Collectors.toSet());
    Mockito.when(applicationService.findAll()).thenReturn(applications);

    Device m1 = createProfile(new Device(),"dev1", "monitor");
    Device m2 = createProfile(new Device(),"dev2", "monitor");
    Device k1 = createProfile(new Device(),"kb1", "key");
    Device k2 = createProfile(new Device(),"kb2", "key");
    Device k3 = createProfile(new Device(),"kb3", "key");
    Set<Device> devices = Stream.of(m1, m2, k1, k2, k3).collect(Collectors.toSet());
    Mockito.when(deviceService.findAll()).thenReturn(devices);

    Client c1 = createClient("c1");
    c1.setHardwareType(h1);
    c1.setLocation(l1);
    c1.getPrinters().add(p1);
    c1.getApplications().add(d1);
    c1.getApplications().add(rdp1);
    c1.getDevices().add(m1);
    c1.getDevices().add(k1);
    Client c2 = createClient("c2");
    c2.setHardwareType(h2);
    c2.setLocation(l2);
    c2.getPrinters().add(p2);
    c2.getApplications().add(d2);
    c2.getDevices().add(m2);
    Client c3 = createClient("c3");
    c3.setHardwareType(h2);
    c3.setLocation(l2);
    Set<Client> clients = Stream.of(c1, c2, c3).collect(Collectors.toSet());
    Mockito.when(clientService.findAll()).thenReturn(clients);

    ApplicationGroup desk = createApplicationGroup("desk");
    desk.getApplications().addAll(Stream.of(d1).collect(Collectors.toSet())); // d1 only app of group desk
    desk.getMembers().add(c3);
    ApplicationGroup frdp = createApplicationGroup("frdp");
    frdp.getApplications().addAll(Stream.of(rdp1, rdp2).collect(Collectors.toSet())); // rdp1, rdp2
    frdp.getMembers().add(c3);
    Set<ApplicationGroup> applicationGroups = Stream.of(desk, frdp).collect(Collectors.toSet());
    Mockito.when(applicationGroupService.findAll()).thenReturn(applicationGroups);

    Mockito.when(clientGroupService.findAll()).thenReturn(Collections.emptySet());
    Mockito.when(userService.findAll()).thenReturn(Collections.emptySet());
    Mockito.when(userGroupService.findAll()).thenReturn(Collections.emptySet());

    // run
    StatisticsReport report = new StatisticsReport();
    csrc.contribute(report);
    StatisticsReport.ConfigurationSummary summary = report.getConfiguration();

    // test
    assertEquals(clients.size(), summary.getThinClientCount());

    // application
    Map<String, Long> summaryApplications = summary.getApplications();
    summaryApplications.forEach((s, aLong) -> {
      assertEquals("Counted applications for schema '" + s + "' doesn't match expected size", getSchemaCount(applications, s), aLong);
    });
    Map<String, Long> applicationTypeUsage = summary.getApplicationTypeUsage();
    Map<String, Long> expectedApps = new HashMap<>();
    expectedApps.put("desktop", 3L);
    expectedApps.put("RDP", 2L);
    expectedApps.forEach((schemaName, expectedLong) -> {
      assertEquals("Counted clients for schema '" + schemaName + "' doesn't match expected size", expectedLong, applicationTypeUsage.get(schemaName));
    });

    // devices
    Map<String, Long> summaryDevices = summary.getDevices();
    summaryDevices.forEach((s, aLong) -> {
      assertEquals("Counted devices for schema '" + s + "' doesn't match expected size", getSchemaCount(devices, s), aLong);
    });
    Map<String, Long> deviceTypeUsage = summary.getDeviceTypeUsage();
    Map<String, Long> expectedDevs = new HashMap<>();
    expectedDevs.put("key", 1L);
    expectedDevs.put("monitor", 2L);
    expectedDevs.forEach((schemaName, expectedLong) -> {
      assertEquals("Counted clients for schema '" + schemaName + "' doesn't match expected size", expectedLong, deviceTypeUsage.get(schemaName));
    });

    // location
    assertEquals("Wrong number of locations", 2L, (long) summary.getLocations().get("location"));
    Map<String, Long> locationTypeUsage = summary.getLocationUsage();
    assertEquals("Wrong number of clients attached to location", 1L,  (long) locationTypeUsage.get("l1")); // one configured locations
    assertEquals("Wrong number of clients attached to location", 2L,  (long) locationTypeUsage.get("l2")); // two configured locations

    // HardwareType
    assertEquals("Wrong number of hardwaretypes", 2L, (long) summary.getHardwaretypes().get("HardwareType"));
    Map<String, Long> hardwareTypeUsage = summary.getHardwaretypeUsage();
    assertEquals("Wrong number of clients attached to HardwareType", 1L,  (long) hardwareTypeUsage.get("h1")); // one configured hardwareType
    assertEquals("Wrong number of clients attached to HardwareType", 2L,  (long) hardwareTypeUsage.get("h2")); // two configured hardwareTypes

    // Printers
    assertEquals("Wrong number of printers", 2L, (long) summary.getPrinters().get("printer"));
    Map<String, Long> printerUsage = summary.getPrinterUsage();
    assertEquals("Wrong number of clients attached to printer", 1L,  (long) printerUsage.get("p1")); // one configured hardwareType
    assertEquals("Wrong number of clients attached to printer", 1L,  (long) printerUsage.get("p2")); // two configured hardwareTypes

  }


  protected ApplicationGroup createApplicationGroup(String name) {
    ApplicationGroup group = new ApplicationGroup();
    group.setName(name);
    group.setApplications(new HashSet<>());
    group.setMembers(new HashSet<>());
    return group;
  }

  private <T extends Profile> Long getSchemaCount(Set<T> profiles, String schemaName) {
    return profiles.stream()
        .filter(p -> p.getSchema(p.getRealm()).getKey().equals(schemaName))
        .count();
  }

  private <P extends Profile> P createProfile(P profile, String name, String schemaName) {
    profile.setName(name);
    profile.setDn("cn="+name);
    Schema schema = new Schema();
    schema.setName(schemaName);
    profile.setSchema(schema);
    return  profile;
  }

  private Location createLocation(String name) {
    final Location location = new Location();
    location.setName(name);
    location.setDn("cn="+name);
    Schema schema = new Schema();
    schema.setName("location");
    location.setSchema(schema);
    return  location;
  }

  private Printer createPrinter(String name) {
    final Printer printer = new Printer();
    printer.setName(name);
    printer.setDn("cn="+name);
    Schema schema = new Schema();
    schema.setName("printer");
    printer.setSchema(schema);
    return  printer;
  }

  private HardwareType createHardwareType(String name) {
    final HardwareType hardwareType = new HardwareType();
    hardwareType.setName(name);
    hardwareType.setDn("cn="+name);
    Schema schema = new Schema();
    schema.setName("HardwareType");
    hardwareType.setSchema(schema);
    return  hardwareType;
  }

  private Client createClient(String name) {
    final Client client = new Client();
    client.setName(name);
    client.setDn("cn="+name);
    client.setApplications(new HashSet<>());
    client.setDevices(new HashSet<>());
    client.setPrinters(new HashSet<>());
    return  client;
  }

  protected ManagerHomeMetadata getManagerHomeMetaData() {
    return new ManagerHomeMetadata() {
      @Override
      public String getServerID() {
        return "Junit";
      }

      @Override
      public void setServerID(String id) {

      }

      @Override
      public boolean isUsageStatisticsEnabled() {
        return false;
      }

      @Override
      public void save() {

      }

      @Override
      public int getAcknowledgedPrivacyNoticeVersion() {
        return 0;
      }

      @Override
      public void setAcknowledgedPrivacyNoticeVersion(int version) {

      }
    };
  }
}