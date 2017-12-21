package org.openthinclient.api.rest.impl;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.openthinclient.api.importer.impl.ClasspathSchemaProvider;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Device;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.service.ClientService;

import java.util.HashSet;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class ProfileRepositoryTest {

  private final ClasspathSchemaProvider schemaProvider = new ClasspathSchemaProvider();
  @Rule
  public MockitoRule rule = MockitoJUnit.rule();
  @Mock
  ClientService clientService;

  @Test
  public void testClientHasHardwareType() throws Exception {


    final Client client = new Client();

    final HardwareType hwType = new HardwareType();
    hwType.setName("SomeSimpleHardwareType");
    hwType.setValue("Custom.third", "hardware type specific configuration property");
    hwType.setDevices(new HashSet<>());
    hwType.setSchema(schemaProvider.getSchema(HardwareType.class, null));
    client.setHardwareType(hwType);
    client.setDevices(new HashSet<>());
    client.setSchema(schemaProvider.getSchema(Client.class, null));

    Mockito.when(clientService.findByHwAddress("00:11:22:33:44:55:66:77")).thenReturn(java.util.Collections.singleton(client));

    final ProfileRepository repo = new ProfileRepository(clientService);

    final org.openthinclient.api.rest.model.Client actual = repo.getClient("00:11:22:33:44:55:66:77").getBody();

    assertEquals("SomeSimpleHardwareType", actual.getHardwareType().getName());
//    assertEquals("value-1", actual.getHardwareType().getConfiguration().getAdditionalProperties().get("key"));
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

    final ProfileRepository repo = new ProfileRepository(clientService);

    final List<org.openthinclient.api.rest.model.Device> actualDevices = repo.getDevices("00:11:22:33:44:55:66:77").getBody();

    assertThat(actualDevices, contains(profileWithName("device 1"), profileWithName("device 2")));

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