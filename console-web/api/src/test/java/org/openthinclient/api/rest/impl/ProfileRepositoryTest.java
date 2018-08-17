package org.openthinclient.api.rest.impl;

import org.apache.commons.collections.map.HashedMap;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.openthinclient.api.importer.impl.ClasspathSchemaProvider;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.HardwareTypeService;
import org.openthinclient.common.model.service.UserService;
import org.openthinclient.ldap.LDAPConnectionDescriptor;
import org.openthinclient.service.apacheds.DirectoryServiceConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.impl.ApplianceConfiguration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class ProfileRepositoryTest {

  private final ClasspathSchemaProvider schemaProvider = new ClasspathSchemaProvider();
  @Rule
  public MockitoRule rule = MockitoJUnit.rule();
  @Mock
  ClientService clientService;
  @Mock
  UserService userService;
  @Mock
  HardwareTypeService hardwareTypeService;
  @Mock
  ApplianceConfiguration applianceConfiguration;
  @Mock
  ManagerHome managerHome;

  @Test
  public void testClientHasHardwareType() throws Exception {


    final Client client = new Client();

    final HardwareType hwType = new HardwareType();
    hwType.setName("SomeSimpleHardwareType");
    hwType.setValue("Custom.third", "hardware type specific configuration property");
    hwType.setValue("BootOptions.TFTPBootserver", "${myip}");
    hwType.setDevices(new HashSet<>());
    hwType.setSchema(schemaProvider.getSchema(HardwareType.class, null));
    client.setHardwareType(hwType);
    client.setDevices(new HashSet<>());
    client.setSchema(schemaProvider.getSchema(Client.class, null));

    Mockito.when(clientService.findByHwAddress("00:11:22:33:44:55:66:77")).thenReturn(java.util.Collections.singleton(client));

    final ProfileRepository repo = new ProfileRepository(clientService, userService, hardwareTypeService);

    final org.openthinclient.api.rest.model.Client actual = repo.getClient("00:11:22:33:44:55:66:77").getBody();
    assertEquals("hardware type specific configuration property", actual.getConfiguration().getAdditionalProperties().get("Custom.third"));
  }

  @Test
  public void testGetDevices() throws Exception {

    final Client client = new Client();

    final HardwareType hwType = new HardwareType();
    hwType.setName("SomeSimpleHardwareType");
    hwType.setValue("key", "value-1");
    hwType.setDevices(new HashSet<>());
    hwType.getDevices().add(createDevice("device 2"));
    hwType.setSchema(schemaProvider.getSchema(HardwareType.class, null));
    client.setHardwareType(hwType);
    client.setDevices(new HashSet<>());
    client.getDevices().add(createDevice("device 1"));
    client.setSchema(schemaProvider.getSchema(Client.class, null));

    Mockito.when(clientService.findByHwAddress("00:11:22:33:44:55:66:77")).thenReturn(java.util.Collections.singleton(client));

    final ProfileRepository repo = new ProfileRepository(clientService, userService, hardwareTypeService);

    final List<org.openthinclient.api.rest.model.Device> actualDevices = repo.getDevices("00:11:22:33:44:55:66:77").getBody();

    assertThat(actualDevices, contains(profileWithName("device 1"), profileWithName("device 2")));

  }


    @Test
    public void testClientMergeAndResolveConfigurationHardware() throws Exception {


        final Client hwClient = new Client();

        final HardwareType hwType = new HardwareType();
        hwType.setName("SomeSimpleHardwareType");
        hwType.setValue("Custom.third", "hardware type specific configuration property");
        hwType.setValue("BootOptions.TFTPBootserver", "${myip}"); // will be added to client configuration
        hwType.setValue("BootOptions.NFSRootserver", "${myip}");  // will be overridden by client
        hwType.setDevices(new HashSet<>());
        hwType.setSchema(schemaProvider.getSchema(HardwareType.class, null));
        hwClient.setHardwareType(hwType);
        hwClient.setDevices(new HashSet<>());
        hwClient.setSchema(schemaProvider.getSchema(Client.class, null));
        hwClient.setValue("BootOptions.NFSRootserver", "1.1.1.1");

        Mockito.when(clientService.findByHwAddress("00:11:22:33:44:55:66:77")).thenReturn(java.util.Collections.singleton(hwClient));

        ProfileRepository repo = new ProfileRepository(clientService, userService, hardwareTypeService);
        final org.openthinclient.api.rest.model.Client actual = repo.getClient("00:11:22:33:44:55:66:77").getBody();

        assertEquals("hardware type specific configuration property", actual.getConfiguration().getAdditionalProperties().get("Custom.third"));
        assertEquals("${myip}", actual.getConfiguration().getAdditionalProperties().get("BootOptions.TFTPBootserver"));
        assertEquals("1.1.1.1", actual.getConfiguration().getAdditionalProperties().get("BootOptions.NFSRootserver"));
    }

    @Test
    public void testClientMergeAndResolveConfigurationLocation() throws Exception {

        // client with location
        final Client locClient = new Client();
        locClient.setName("otc-client-NA");

        final Location loc = new Location();
        loc.setName("North America (US)");
        loc.setValue("Lang.lang", "en_US.UTF-8");
        loc.setValue("Time.ntpservers", "pool.ntp.org");
        loc.setValue("BootOptions.TFTPBootserver", "${myip}"); // will be added to client configuration
        loc.setValue("BootOptions.NFSRootserver", "${myip}");  // will be added to client configuration
        loc.setSchema(schemaProvider.getSchema(Location.class, null));

        locClient.setLocation(loc);
        locClient.setDevices(new HashSet<>());
        locClient.setSchema(schemaProvider.getSchema(Client.class, null));

        Mockito.when(clientService.findByHwAddress("00:11:22:33:44:55:66:78")).thenReturn(java.util.Collections.singleton(locClient));
        ProfileRepository repo = new ProfileRepository(clientService, userService, hardwareTypeService);

        final org.openthinclient.api.rest.model.Client locc = repo.getClient("00:11:22:33:44:55:66:78").getBody();
        assertEquals("${myip}", locc.getConfiguration().getAdditionalProperties().get("BootOptions.TFTPBootserver"));
        assertEquals("${myip}", locc.getConfiguration().getAdditionalProperties().get("BootOptions.NFSRootserver"));

    }


    @Test
    public void testClientMergeAndResolveConfigurationByRealm() throws Exception {

        // client with location and realm
        Realm realm = new Realm();
        realm.setSchema(schemaProvider.getSchema(Realm.class, null));
        realm.setValue("BootOptions.TFTPBootserver", "${myip}"); // will be added to client configuration
        realm.setValue("BootOptions.NFSRootserver", "${myip}");  // will be added to client configuration
        // TODO: der Port kommt über das Schema, kann aber manuell beim Setup konfiguriert werden - passt nicht
        realm.setValue("Directory.Primary.LDAPURLs", "ldap://${myip}:10389/${urlencoded:basedn}");  // will be added to client configuration

        final Client locClient = new Client();
        locClient.setName("otc-client-NA");
        locClient.setRealm(realm);

        final Location loc = new Location();
        loc.setName("North America (US)");
        loc.setValue("Lang.lang", "en_US.UTF-8");
        loc.setValue("Time.ntpservers", "pool.ntp.org");
        loc.setSchema(schemaProvider.getSchema(Location.class, null));

        locClient.setLocation(loc);
        locClient.setDevices(new HashSet<>());
        locClient.setSchema(schemaProvider.getSchema(Client.class, null));

        // this is for testing
        LDAPConnectionDescriptor lcd = new LDAPConnectionDescriptor();
        lcd.setBaseDN("dc=openthinclient,dc=org");
        lcd.setHostname("10.10.10.10");
        realm.setConnectionDescriptor(lcd);

        Mockito.when(clientService.findByHwAddress("00:11:22:33:44:55:66:78")).thenReturn(java.util.Collections.singleton(locClient));
        ProfileRepository repo = new ProfileRepository(clientService, userService, hardwareTypeService);

        final org.openthinclient.api.rest.model.Client locc = repo.getClient("00:11:22:33:44:55:66:78").getBody();
        assertEquals("10.10.10.10", locc.getConfiguration().getAdditionalProperties().get("BootOptions.TFTPBootserver"));
        assertEquals("10.10.10.10", locc.getConfiguration().getAdditionalProperties().get("BootOptions.NFSRootserver"));
        assertEquals("ldap://10.10.10.10:10389/dc=openthinclient,dc=org", locc.getConfiguration().getAdditionalProperties().get("Directory.Primary.LDAPURLs"));

    }

  private BaseMatcher<org.openthinclient.api.rest.model.AbstractProfileObject> profileWithName(final String profileName) {
    return new BaseMatcher<org.openthinclient.api.rest.model.AbstractProfileObject>() {

      @Override
      public void describeTo(Description description) {
        description.appendText("profile name matches " + profileName);
      }

      @Override
      public boolean matches(Object item) {
        return ((org.openthinclient.api.rest.model.AbstractProfileObject) item).getName().equals(profileName);
      }
    };
  }

  private Device createDevice(String name) {
    final Device device = new Device();
    device.setName(name);
    device.setMembers(new HashSet());
    device.setSchema(schemaProvider.getSchema(Device.class, "display"));
    return device;
  }

}