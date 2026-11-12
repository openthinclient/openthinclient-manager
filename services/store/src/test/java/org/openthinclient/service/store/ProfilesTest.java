package org.openthinclient.service.store;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openthinclient.service.apacheds.DirectoryService;
import org.openthinclient.service.apacheds.DirectoryServiceConfiguration;
import org.openthinclient.service.common.home.impl.DefaultManagerHome;

import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;

import javax.naming.Context;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProfilesTest {
  @ClassRule
  public static final TemporaryFolder TEMP = new TemporaryFolder();

  private static final String BASE_DN = "ou=test,dc=test,dc=test";
  private static final String CLIENT_DN = "cn=client,ou=clients," + BASE_DN;
  private static final String NO_LOCATION_CLIENT_DN = "cn=nolocation,ou=clients," + BASE_DN;
  private static final String NO_GROUP_CLIENT_DN = "cn=nogroup,ou=clients," + BASE_DN;
  private static final String DEFAULT_CLIENT_DN = "cn=default,ou=clients," + BASE_DN;
  private static final String GROUP_DN = "cn=group,ou=clientgroups," + BASE_DN;
  private static final String DEFAULT_GROUP_DN = "cn=defaultgroup,ou=clientgroups," + BASE_DN;
  private static final String HWTYPE_DN = "cn=hardware,ou=hwtypes," + BASE_DN;
  private static final String LOCATION_DN = "l=office,ou=locations," + BASE_DN;
  private static final String USER_DN = "cn=user,ou=users," + BASE_DN;
  private static final String USER_GROUP_DN = "cn=usergroup,ou=usergroups," + BASE_DN;
  private static final String USER_APPS_DN = "cn=userapps,ou=appgroups," + BASE_DN;
  private static final String USER_GROUP_APPS_DN = "cn=usergroupapps,ou=appgroups," + BASE_DN;
  private static final String CLIENT_APPS_DN = "cn=clientapps,ou=appgroups," + BASE_DN;
  private static final String CLIENT_GROUP_APPS_DN = "cn=clientgroupapps,ou=appgroups," + BASE_DN;
  private static final String CLIENT_MAC = "02:00:00:00:00:01";
  private static final String NO_LOCATION_MAC = "02:00:00:00:00:02";
  private static final String NO_GROUP_MAC = "02:00:00:00:00:03";
  private static final String UNKNOWN_MAC = "02:00:00:00:00:04";

  private static String previousManagerHome;
  private static DirectoryService directoryService;
  private static ThreadGroup schemaThreads;

  @BeforeClass
  public static void setUpDirectory() throws Exception {
    previousManagerHome = System.getProperty("manager.home");
    DefaultManagerHome home = new DefaultManagerHome(TEMP.newFolder("manager-home"));
    System.setProperty("manager.home", home.getLocation().getAbsolutePath());
    initializeSchemas(home.getLocation().toPath());

    DirectoryServiceConfiguration configuration =
        home.getConfiguration(DirectoryServiceConfiguration.class);
    configuration.setEmbeddedCustomRootPartitionName("dc=test,dc=test");
    configuration.setPrimaryOU("test");
    configuration.setContextSecurityCredentials("secret");
    configuration.setEmbeddedAnonymousAccess(true);
    try (ServerSocket socket = new ServerSocket(0)) {
      configuration.setEmbeddedLdapPort(socket.getLocalPort());
    }
    home.save(DirectoryServiceConfiguration.class);

    directoryService = new DirectoryService();
    directoryService.setConfiguration(configuration);
    directoryService.startService();

    Hashtable<String, String> env = new Hashtable<>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.PROVIDER_URL, "ldap://localhost:" + configuration.getEmbeddedLdapPort());
    env.put(Context.SECURITY_AUTHENTICATION, "simple");
    env.put(Context.SECURITY_PRINCIPAL, "uid=admin,ou=system");
    env.put(Context.SECURITY_CREDENTIALS, "secret");
    DirContext context = new InitialDirContext(env);
    try {
      createOu(context, "ou=test,dc=test,dc=test", "test");
      for (String name : new String[]{
          "clients", "clientgroups", "locations", "printers", "hwtypes",
          "devices", "apps", "appgroups", "usergroups"}) {
        createOu(context, "ou=" + name + "," + BASE_DN, name);
      }

      createClient(context, CLIENT_DN, "client", CLIENT_MAC, LOCATION_DN);
      createClient(context, NO_LOCATION_CLIENT_DN, "nolocation", NO_LOCATION_MAC, null);
      createClient(context, NO_GROUP_CLIENT_DN, "nogroup", NO_GROUP_MAC, LOCATION_DN);
      createClient(context, DEFAULT_CLIENT_DN, "default", "00:00:00:00:00:00", null);

      createGroup(context, GROUP_DN, "group", CLIENT_DN, NO_LOCATION_CLIENT_DN);
      createGroup(context, DEFAULT_GROUP_DN, "defaultgroup", DEFAULT_CLIENT_DN);
      createGroup(context, HWTYPE_DN, "hardware", CLIENT_DN);
      createGroup(context, USER_GROUP_DN, "usergroup", USER_DN);
      createGroup(context, USER_APPS_DN, "userapps", USER_DN);
      createGroup(context, USER_GROUP_APPS_DN, "usergroupapps", USER_GROUP_DN);
      createGroup(context, CLIENT_APPS_DN, "clientapps", CLIENT_DN);
      createGroup(context, CLIENT_GROUP_APPS_DN, "clientgroupapps", GROUP_DN);

      String realmDN = "ou=RealmConfiguration," + BASE_DN;
      createOu(context, realmDN, "RealmConfiguration");
      context.bind("nisMapName=profile," + realmDN, null,
          entry("nisMap", "nisMapName", "profile"));
      createProperty(context, realmDN, "BootOptions.PXEServicePolicy", "AnyClient");

      createProfile(context, "printers", "user", USER_DN);
      createProfile(context, "printers", "usergroup", USER_GROUP_DN);
      createProfile(context, "printers", "client", CLIENT_DN);
      createProfile(context, "printers", "noGroupClient", NO_GROUP_CLIENT_DN);
      createProfile(context, "printers", "group", GROUP_DN);
      createProfile(context, "printers", "location", LOCATION_DN);
      createProfile(context, "printers", "sharedGroupLocation", GROUP_DN, LOCATION_DN);
      createProfile(context, "printers", "sharedClientGroup", CLIENT_DN, GROUP_DN);
      createProfile(context, "printers", "sharedUserGroup", USER_DN, GROUP_DN);

      createProfile(context, "devices", "deviceClient", CLIENT_DN);
      createProfile(context, "devices", "deviceHardware", HWTYPE_DN);
      createProfile(context, "devices", "deviceGroup", GROUP_DN);
      createProfile(context, "devices", "deviceSharedClient", CLIENT_DN, HWTYPE_DN, GROUP_DN);
      createProfile(context, "devices", "deviceSharedHardware", HWTYPE_DN, GROUP_DN);
      createProfile(context, "devices", "deviceNoGroup", NO_GROUP_CLIENT_DN);
      createProfile(context, "devices", "deviceDefault", DEFAULT_CLIENT_DN);
      createProfile(context, "devices", "deviceDefaultGroup", DEFAULT_GROUP_DN);
      createProperty(context, "cn=deviceClient,ou=devices," + BASE_DN, "Mode", "invalid");
      createProperty(context, "cn=deviceClient,ou=devices," + BASE_DN, "Custom", "device-value");

      createProfile(context, "apps", "appUser", USER_DN);
      createProfile(context, "apps", "appUserAppGroup", USER_APPS_DN);
      createProfile(context, "apps", "appUserGroup", USER_GROUP_DN);
      createProfile(context, "apps", "appUserGroupAppGroup", USER_GROUP_APPS_DN);
      createProfile(context, "apps", "appClient", CLIENT_DN);
      createProfile(context, "apps", "appClientAppGroup", CLIENT_APPS_DN);
      createProfile(context, "apps", "appClientGroup", GROUP_DN);
      createProfile(context, "apps", "appClientGroupAppGroup", CLIENT_GROUP_APPS_DN);
      createProfile(context, "apps", "appSharedUser", USER_DN, GROUP_DN);
      createProfile(context, "apps", "appSharedUserGroup", USER_GROUP_DN, CLIENT_DN);
      createProfile(context, "apps", "appSharedClient", CLIENT_DN, GROUP_DN);
      createProfile(context, "apps", "appSharedNested", USER_GROUP_APPS_DN, CLIENT_APPS_DN);
      createProfile(context, "apps", "appDefault", DEFAULT_CLIENT_DN);
      createProfile(context, "apps", "appDefaultGroup", DEFAULT_GROUP_DN);
      createProperty(context, "cn=appUser,ou=apps," + BASE_DN, "Mode", "disabled");
      createProperty(context, "cn=appUser,ou=apps," + BASE_DN, "Custom", "app-value");
    } finally {
      context.close();
    }
  }

  @AfterClass
  public static void stopDirectory() throws Exception {
    try {
      if (schemaThreads != null) {
        schemaThreads.interrupt();
      }
      if (directoryService != null) {
        directoryService.stopService();
      }
    } finally {
      if (previousManagerHome == null) {
        System.clearProperty("manager.home");
      } else {
        System.setProperty("manager.home", previousManagerHome);
      }
    }
  }

  @Test
  public void devicesFollowPrecedenceAndUseFirstRelationForDuplicates() {
    Collection<Map<String, String>> devices = Profiles.getDevices(CLIENT_MAC);
    Map<String, String> relations = relationsByName(devices);

    assertEquals(5, relations.size());
    assertEquals("client", relations.get("deviceClient"));
    assertEquals("hwtype", relations.get("deviceHardware"));
    assertEquals("clientgroup", relations.get("deviceGroup"));
    assertEquals("client", relations.get("deviceSharedClient"));
    assertEquals("hwtype", relations.get("deviceSharedHardware"));
    assertPrecedence(devices, "client", "hwtype", "clientgroup");

    Map<String, String> device = profileNamed(devices, "deviceClient");
    assertEquals("device/test", device.get("type"));
    assertEquals("device-value", device.get("Custom"));
    assertEquals("enabled", device.get("Mode"));
    assertEquals("schema-default", device.get("DefaultSetting"));
  }

  @Test
  public void devicesResolveWithoutHardwareTypeOrClientGroups() {
    Map<String, String> groupRelations =
        relationsByName(Profiles.getDevices(NO_LOCATION_MAC));
    assertEquals(3, groupRelations.size());
    assertEquals("clientgroup", groupRelations.get("deviceGroup"));
    assertEquals("clientgroup", groupRelations.get("deviceSharedClient"));
    assertEquals("clientgroup", groupRelations.get("deviceSharedHardware"));

    Map<String, String> directRelations =
        relationsByName(Profiles.getDevices(NO_GROUP_MAC));
    assertEquals(1, directRelations.size());
    assertEquals("client", directRelations.get("deviceNoGroup"));
  }

  @Test
  public void applicationsFollowPrecedenceAndUseFirstRelationForDuplicates() {
    List<Map<String, String>> apps =
        Profiles.getApps(CLIENT_MAC, USER_DN, new String[]{USER_GROUP_DN});
    Map<String, String> relations = relationsByName(apps);

    assertEquals(12, relations.size());
    assertEquals("user", relations.get("appUser"));
    assertEquals("user-appgroup", relations.get("appUserAppGroup"));
    assertEquals("usergroup", relations.get("appUserGroup"));
    assertEquals("usergroup-appgroup", relations.get("appUserGroupAppGroup"));
    assertEquals("client", relations.get("appClient"));
    assertEquals("client-appgroup", relations.get("appClientAppGroup"));
    assertEquals("clientgroup", relations.get("appClientGroup"));
    assertEquals("clientgroup-appgroup", relations.get("appClientGroupAppGroup"));
    assertEquals("user", relations.get("appSharedUser"));
    assertEquals("usergroup", relations.get("appSharedUserGroup"));
    assertEquals("client", relations.get("appSharedClient"));
    assertEquals("usergroup-appgroup", relations.get("appSharedNested"));
    assertPrecedence(apps, "user", "user-appgroup", "usergroup", "usergroup-appgroup",
        "client", "client-appgroup", "clientgroup", "clientgroup-appgroup");

    Map<String, String> app = profileNamed(apps, "appUser");
    assertEquals("application/test", app.get("type"));
    assertEquals("app-value", app.get("Custom"));
    assertEquals("disabled", app.get("Mode"));
    assertEquals("schema-default", app.get("DefaultSetting"));
  }

  @Test
  public void applicationsDiscoverUserGroupsOnlyWhenNotSupplied() {
    List<Map<String, String>> discovered = Profiles.getApps(CLIENT_MAC, USER_DN, null);
    List<Map<String, String>> supplied =
        Profiles.getApps(CLIENT_MAC, USER_DN, new String[]{USER_GROUP_DN});
    assertEquals(relationsByName(supplied), relationsByName(discovered));

    Map<String, String> withoutGroups =
        relationsByName(Profiles.getApps(null, USER_DN, new String[0]));
    assertEquals(3, withoutGroups.size());
    assertEquals("user", withoutGroups.get("appUser"));
    assertEquals("user", withoutGroups.get("appSharedUser"));
    assertEquals("user-appgroup", withoutGroups.get("appUserAppGroup"));
  }

  @Test
  public void applicationsResolveClientAssignmentsWithoutAUser() {
    List<Map<String, String>> apps = Profiles.getApps(CLIENT_MAC, null, null);
    Map<String, String> relations = relationsByName(apps);

    assertEquals(8, relations.size());
    assertEquals("client", relations.get("appClient"));
    assertEquals("client", relations.get("appSharedUserGroup"));
    assertEquals("client-appgroup", relations.get("appSharedNested"));
    assertEquals("clientgroup", relations.get("appSharedUser"));
    assertEquals("clientgroup-appgroup", relations.get("appClientGroupAppGroup"));
    assertPrecedence(apps, "client", "client-appgroup", "clientgroup", "clientgroup-appgroup");
    assertTrue(Profiles.getApps(null, null, null).isEmpty());
  }

  @Test
  public void devicesAndApplicationsUseTheDefaultClientForUnknownMac() {
    Map<String, String> devices = relationsByName(Profiles.getDevices(UNKNOWN_MAC));
    assertEquals(2, devices.size());
    assertEquals("client", devices.get("deviceDefault"));
    assertEquals("clientgroup", devices.get("deviceDefaultGroup"));

    Map<String, String> apps = relationsByName(Profiles.getApps(UNKNOWN_MAC, null, null));
    assertEquals(2, apps.size());
    assertEquals("client", apps.get("appDefault"));
    assertEquals("clientgroup", apps.get("appDefaultGroup"));
  }

  @Test
  public void printersFollowPrecedenceAndUseFirstRelationForDuplicates() {
    List<Map<String, String>> printers =
        Profiles.getPrinters(CLIENT_MAC, USER_DN, new String[]{USER_GROUP_DN});

    Map<String, String> relations = relationsByName(printers);
    assertEquals(8, relations.size());
    assertEquals("user", relations.get("user"));
    assertEquals("usergroup", relations.get("usergroup"));
    assertEquals("client", relations.get("client"));
    assertEquals("clientgroup", relations.get("group"));
    assertEquals("location", relations.get("location"));
    assertEquals("location", relations.get("sharedGroupLocation"));
    assertEquals("client", relations.get("sharedClientGroup"));
    assertEquals("user", relations.get("sharedUserGroup"));

    Map<String, String> userPrinter = profileNamed(printers, "user");
    assertEquals("printer/test", userPrinter.get("type"));
    assertEquals("enabled", userPrinter.get("Mode"));
    assertEquals("schema-default", userPrinter.get("DefaultSetting"));

    Map<String, Integer> precedence = new HashMap<>();
    precedence.put("user", 0);
    precedence.put("usergroup", 1);
    precedence.put("client", 2);
    precedence.put("location", 3);
    precedence.put("clientgroup", 4);
    int last = -1;
    for (Map<String, String> printer : printers) {
      int current = precedence.get(printer.get("relation"));
      assertTrue("Unexpected printer precedence: " + printers, current >= last);
      last = current;
    }
  }

  @Test
  public void clientGroupPrintersDoNotRequireLocation() {
    Map<String, String> relations =
        relationsByName(Profiles.getPrinters(NO_LOCATION_MAC, null, null));

    assertEquals(4, relations.size());
    assertEquals("clientgroup", relations.get("group"));
    assertEquals("clientgroup", relations.get("sharedGroupLocation"));
    assertEquals("clientgroup", relations.get("sharedClientGroup"));
    assertEquals("clientgroup", relations.get("sharedUserGroup"));
  }

  @Test
  public void clientsWithoutGroupsKeepDirectAndLocationPrinters() {
    List<Map<String, String>> printers = Profiles.getPrinters(NO_GROUP_MAC, null, null);
    Map<String, String> relations = relationsByName(printers);

    assertEquals(3, relations.size());
    assertEquals("client", relations.get("noGroupClient"));
    assertEquals("location", relations.get("location"));
    assertEquals("location", relations.get("sharedGroupLocation"));
    assertEquals("client", printers.get(0).get("relation"));
  }

  private static Map<String, String> relationsByName(Collection<Map<String, String>> profiles) {
    Map<String, String> relations = new HashMap<>();
    for (Map<String, String> profile : profiles) {
      assertNull("Duplicate profile " + profile.get("name"),
          relations.put(profile.get("name"), profile.get("relation")));
    }
    return relations;
  }

  private static Map<String, String> profileNamed(
      Collection<Map<String, String>> profiles, String name) {
    return profiles.stream().filter(profile -> name.equals(profile.get("name")))
        .findFirst().orElseThrow(() -> new AssertionError("Profile not found: " + name));
  }

  private static void assertPrecedence(
      Collection<Map<String, String>> profiles, String... relations) {
    List<String> order = Arrays.asList(relations);
    int last = -1;
    for (Map<String, String> profile : profiles) {
      int current = order.indexOf(profile.get("relation"));
      assertTrue("Unexpected profile precedence: " + profiles, current >= 0 && current >= last);
      last = current;
    }
  }

  private static void initializeSchemas(Path home) throws Exception {
    String schema = "<schema name=\"test\">"
        + "<entry name=\"DefaultSetting\" value=\"schema-default\"/>"
        + "<choice name=\"Mode\" value=\"enabled\">"
        + "<option value=\"enabled\"/><option value=\"disabled\"/>"
        + "</choice></schema>";
    for (String type : new String[]{"device", "application", "printer"}) {
      Path directory = Files.createDirectories(home.resolve("nfs/root/schema").resolve(type));
      Files.write(directory.resolve("test.xml"), schema.getBytes(StandardCharsets.UTF_8));
    }

    // The schema watcher inherits this group so it can be stopped after the tests.
    schemaThreads = new ThreadGroup("ProfilesTest schemas");
    FutureTask<Void> initialize = new FutureTask<>(() -> {
      SchemaStore.getSchemas();
      return null;
    });
    Thread thread = new Thread(schemaThreads, initialize, "ProfilesTest schema initialization");
    thread.setDaemon(true);
    thread.start();
    initialize.get();
    thread.join();
  }

  private static Attributes entry(String objectClass, String nameAttribute, String name) {
    Attributes attributes = new BasicAttributes(true);
    Attribute classes = new BasicAttribute("objectClass");
    classes.add("top");
    classes.add(objectClass);
    attributes.put(classes);
    attributes.put(nameAttribute, name);
    return attributes;
  }

  private static void createOu(DirContext context, String dn, String name) throws Exception {
    context.bind(dn, null, entry("organizationalUnit", "ou", name));
  }

  private static void createClient(DirContext context, String dn, String name,
                                   String mac, String location) throws Exception {
    Attributes client = entry("device", "cn", name);
    client.get("objectClass").add("ieee802Device");
    client.put("macAddress", mac);
    if (location != null) {
      client.put("l", location);
    }
    context.bind(dn, null, client);
  }

  private static void createGroup(DirContext context, String dn, String name, String... memberDNs)
      throws Exception {
    Attributes group = entry("groupOfUniqueNames", "cn", name);
    Attribute members = new BasicAttribute("uniqueMember");
    for (String memberDN : memberDNs) {
      members.add(memberDN);
    }
    group.put(members);
    context.bind(dn, null, group);
  }

  private static void createProfile(
      DirContext context, String ou, String name, String... memberDNs) throws Exception {
    String dn = "cn=" + name + ",ou=" + ou + "," + BASE_DN;
    createGroup(context, dn, name, memberDNs);

    Attributes profile = entry("nisMap", "nisMapName", "profile");
    profile.put("description", "test");
    context.bind("nisMapName=profile," + dn, null, profile);
  }

  private static void createProperty(DirContext context, String dn, String key, String value)
      throws Exception {
    Attributes property = entry("nisObject", "cn", key);
    property.put("nisMapName", "profile");
    property.put("nisMapEntry", value);
    context.bind("cn=" + key + ",nisMapName=profile," + dn, null, property);
  }
}
