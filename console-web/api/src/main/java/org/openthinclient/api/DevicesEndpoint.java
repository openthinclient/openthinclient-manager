package org.openthinclient.api;
import java.util.Map;

import org.openthinclient.service.store.Profiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DevicesEndpoint {
  @GetMapping("/api/v2/devices/{mac}")
  public Iterable<Map<String, String>> getDevices(@PathVariable String mac) {
    return Profiles.getDevices(mac);
  }
}
