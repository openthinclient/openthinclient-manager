package org.openthinclient.web.thinclient.model;

import java.util.Arrays;
import java.util.List;
import org.openthinclient.api.importer.model.ImportableHardwareType;
import org.openthinclient.api.rest.model.AbstractProfileObject;
import org.openthinclient.api.rest.model.Application;
import org.openthinclient.api.rest.model.Device;
import org.openthinclient.web.thinclient.model.Item.Type;
import org.springframework.web.client.RestTemplate;

/**
 *
 */
public class RepoDummy {

  public static ImportableHardwareType getHardwareType(String name) {
    RestTemplate restTemplate = new RestTemplate();
    ImportableHardwareType iht = restTemplate.getForObject("http://localhost:8080/api/v1/model/hardware-type/" + name, ImportableHardwareType.class);
    return iht;
  }

  public static Application getApplication(String name) {
    RestTemplate restTemplate = new RestTemplate();
    Application app = restTemplate.getForObject("http://localhost:8080/api/v1/model/application/" + name, Application.class);
    return app;
  }

  public static void saveProfile(AbstractProfileObject profile) {
    // TODO: rest save
    System.out.println("profile = [" + profile + "]");
  }

  public static Item findSingleDevice() {

    Item display = new Item("Display: Dualview 1280x1024 (model 1780)", Type.DEVICE);
    display.setDescription("for openthinclient TC 1780 model");
    display.addConfig(new ItemConfiguration("secondscreen.connect", "DisplayPort-0"));
    display.addConfig(new ItemConfiguration("secondscreen.rotation", "normal"));
    display.addConfig(new ItemConfiguration("firstscreen.resolution", "1280x1024"));
    display.addConfig(new ItemConfiguration("secondscreen.positioning", "--right-of"));
    display.addConfig(new ItemConfiguration("firstscreen.connect", "DVI-0"));
    display.addConfig(new ItemConfiguration("secondscreen.resolution", "1280x1024"));
    display.addConfig(new ItemConfiguration("firstscreen.rotation", "normal"));

    return display;
  }

  public static void save(Item config, ItemConfiguration p) {

  }

  public static Device getDevice(String name) {
    RestTemplate restTemplate = new RestTemplate();
    Device dev = restTemplate.getForObject("http://localhost:8080/api/v1/model/device/" + name, Device.class);
    return dev;
  }

  public static List<String> getDevices() {
    RestTemplate restTemplate = new RestTemplate();
    String[] devs = restTemplate.getForObject("http://localhost:8080/api/v1/model/device/", String[].class);
    return Arrays.asList(devs);
  }

  public static List<String> getApplication() {
    RestTemplate restTemplate = new RestTemplate();
    String[] apps = restTemplate.getForObject("http://localhost:8080/api/v1/model/application/", String[].class);
    return Arrays.asList(apps);
  }


  public static List<String> getHardware() {
    RestTemplate restTemplate = new RestTemplate();
    String[] apps = restTemplate.getForObject("http://localhost:8080/api/v1/model/hardware-type/", String[].class);
    return Arrays.asList(apps);
  }
}
