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
import org.openthinclient.common.model.Location;
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

    Application d1 = createApplication("d1", "desktop");
    Application d2 = createApplication("d2", "desktop");
    Application rdp1 = createApplication("rdp1", "RDP");
    Application rdp2 = createApplication("rdp2", "RDP");
    Application rdp3 = createApplication("rdp3", "RDP");
    Set<Application> applications = Stream.of(d1, d2, rdp1, rdp2, rdp3).collect(Collectors.toSet());
    Mockito.when(applicationService.findAll()).thenReturn(applications);

    Client c1 = createClient("c1");
    c1.setLocation(l1);
    c1.getApplications().add(d1);
    c1.getApplications().add(rdp1);
    Client c2 = createClient("c2");
    c2.setLocation(l2);
    c2.getApplications().add(d2);
    Client c3 = createClient("c3");
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
    Mockito.when(deviceService.findAll()).thenReturn(Collections.emptySet());
    Mockito.when(printerService.findAll()).thenReturn(Collections.emptySet());
    Mockito.when(hardwareTypeService.findAll()).thenReturn(Collections.emptySet());

    // run
    StatisticsReport report = new StatisticsReport();
    csrc.contribute(report);
    StatisticsReport.ConfigurationSummary summary = report.getConfiguration();

    // test
    assertEquals(clients.size(), summary.getThinClientCount());
    Map<String, Long> summaryApplications = summary.getApplications();
    summaryApplications.forEach((s, aLong) -> {
      assertEquals("Counted applications for schema '" + s + "' doesn't match expected size", getSchemaApplicationCount(applications, s), aLong);
    });

    // application
    Map<String, Long> applicationTypeUsage = summary.getApplicationTypeUsage();
    Map<String, Long> expected = new HashMap<>();
    expected.put("desktop", 3L);
    expected.put("RDP", 2L);
    expected.forEach((schemaName, expectedLong) -> {
      assertEquals("Counted clients for schema '" + schemaName + "' doesn't match expected size", expectedLong, applicationTypeUsage.get(schemaName));
    });
    // location
    assertEquals("Wrong number of locations", 2, summary.getLocations());
    Map<String, Long> locationTypeUsage = summary.getLocationUsage();
    assertEquals("Wrong number of clients attached to location", 1L,  (long) summary.getLocations().get("l1")); // one configured locations
    assertEquals("Wrong number of clients attached to location", 2L,  (long) summary.getLocations().get("l2")); // two configured locations

  }


  protected ApplicationGroup createApplicationGroup(String name) {
    ApplicationGroup group = new ApplicationGroup();
    group.setName(name);
    group.setApplications(new HashSet<>());
    group.setMembers(new HashSet<>());
    return group;
  }

  private Long getSchemaApplicationCount(Set<Application> applications, String schemaName) {
    return applications.stream()
        .filter(a -> a.getSchema(a.getRealm()).getKey().equals(schemaName))
        .count();
  }

  private Application createApplication(String name, String schemaName) {
    final Application application = new Application();
    application.setName(name);
    application.setDn("cn="+name);
    Schema schema = new Schema();
    schema.setName(schemaName);
    application.setSchema(schema);
    return  application;
  }

  private Location createLocation(String name) {
    final Location location = new Location();
    location.setName(name);
    location.setDn("cn="+name);
    return  location;
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