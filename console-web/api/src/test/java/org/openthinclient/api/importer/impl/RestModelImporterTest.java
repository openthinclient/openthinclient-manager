package org.openthinclient.api.importer.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openthinclient.api.importer.config.ImporterConfiguration;
import org.openthinclient.api.importer.model.ImportableClient;
import org.openthinclient.api.importer.model.ImportableHardwareType;
import org.openthinclient.api.importer.model.ProfileReference;
import org.openthinclient.api.importer.model.ProfileType;
import org.openthinclient.common.model.Client;
import org.openthinclient.common.model.Device;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;


@RunWith(SpringRunner.class)
@Import({ImporterConfiguration.class, RestModelImporterTest.ClasspathSchemaProviderConfiguration.class})
public class RestModelImporterTest {

  @Configuration
  public static class ClasspathSchemaProviderConfiguration {
    @Bean
    public SchemaProvider schemaProvider() {
      return new ClasspathSchemaProvider();
    }
  }

  private RestModelImporter importer;

  @MockBean
  HardwareTypeService hardwareTypeService;
  @MockBean
  ApplicationService applicationService;
  @MockBean
  ClientService clientService;
  @MockBean
  DeviceService deviceService;
  @MockBean
  LocationService locationService;
  @MockBean
  PrinterService printerService;

  @Autowired
  ImportModelMapper mapper;

  @Before
  public void setUp() throws Exception {

    importer = new RestModelImporter(mapper, hardwareTypeService, applicationService, clientService, deviceService, locationService, printerService);
  }

  @Test
  public void testImportSimpleHardwareType() throws Exception {

    final ImportableHardwareType hw = new ImportableHardwareType();
    hw.setName("Simple Hardware Type");

    final HardwareType hardwareType = importer.importHardwareType(hw);

    assertNotNull(hardwareType);

    then(hardwareTypeService).should().save(any());

  }

  @Test(expected = MissingReferencedObjectException.class)
  public void testImportHardwareTypeWithMissingDevice() throws Exception {
    final ImportableHardwareType hw = new ImportableHardwareType();
    hw.setName("Incomplete Hardware Type");
    hw.getDevices().add(new ProfileReference(ProfileType.DEVICE, "Missing Device"));

    importer.importHardwareType(hw);

  }

  @Test
  public void testImportHardwareTypeWithReferencedDevice() throws Exception {

    final Device existingDevice = new Device();
    existingDevice.setName("RequiredDevice");
    given(deviceService.findByName("Required Device")).willReturn(existingDevice);

    final ImportableHardwareType hw = new ImportableHardwareType();
    hw.setName("Complete Hardware Type");
    hw.getDevices().add(new ProfileReference(ProfileType.DEVICE, "Required Device"));

    final HardwareType result = importer.importHardwareType(hw);

    assertNotNull(result);
    assertSame(existingDevice, result.getDevices().iterator().next());

    then(hardwareTypeService).should().save(any());

  }

  @Test
  public void testImportDevice() throws Exception {

    org.openthinclient.api.rest.model.Device importable = new org.openthinclient.api.rest.model.Device();
    importable.setName("A Simple Display");
    importable.setSubtype("display");
    importable.getConfiguration().setAdditionalProperty("firstscreen.connect", "CRT1");

    final Device device = importer.importDevice(importable);

    assertEquals("A Simple Display", device.getName());

    assertEquals("display", device.getSchema(null).getName());

    assertEquals("CRT1", device.getValue("firstscreen.connect"));

    then(deviceService).should().save(any());

  }

  @Test
  public void testImportClient() throws Exception {

    final HardwareType hardwareType = new HardwareType();
    hardwareType.setName("Some Type");
    given(hardwareTypeService.findByName("Some Type")).willReturn(hardwareType);

    final Location location = new Location();
    location.setName("location:UK");
    given(locationService.findAll()).willReturn(Collections.singleton(location));

    final ImportableClient importableClient = new ImportableClient();
    importableClient.setMacAddress("00:80:41:ae:fd:7e");
    importableClient.setHardwareType(new ProfileReference(ProfileType.HARDWARETYPE, "Some Type"));
//    importableClient.getApplications().add(new ProfileReference(ProfileType.APPLICATION, "ugga"));
    importableClient.setLocation(new ProfileReference(ProfileType.LOCATION, "location:UK"));

    final Client client = importer.importClient(importableClient);

    then(clientService).should().save(any());

    assertSame(hardwareType, client.getHardwareType());
    assertEquals("00:80:41:ae:fd:7e", client.getMacAddress());
    assertEquals("location:UK", client.getLocation().getName());

  }
}