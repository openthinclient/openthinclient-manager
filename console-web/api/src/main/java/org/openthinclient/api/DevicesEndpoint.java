package org.openthinclient.api;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.openthinclient.service.store.Profiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DevicesEndpoint {
  @GetMapping("/api/v2/devices/{mac}")
  public Iterable<Map<String, String>> getDevices(@PathVariable String mac) {
    Collection<Map<String, String>> devices = Profiles.getDevices(mac);
    Optional<Map<String, String>> firstLoginDevice = devices.stream()
        .filter(dev -> "device/login".equals(dev.get("type")))
        .findFirst();
    boolean hasAutoLogin = (
      !firstLoginDevice.isPresent()
      || "autologin".equals(firstLoginDevice.get().get("login.type"))
    );
    if (hasAutoLogin) {
      /* Add a synthetic old-style autologin device for any localboot clients
       * that would otherwise hang at the login and not run their updater.
       * Current and future version of the OS/tcos-libs will ignore this device.
       */
      Map<String, String> autologin = new HashMap<>();
      autologin.put("type", "device/autologin");
      devices.add(autologin);
    }
    return devices;
  }
}
