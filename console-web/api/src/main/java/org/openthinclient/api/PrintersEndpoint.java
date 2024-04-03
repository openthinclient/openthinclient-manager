package org.openthinclient.api;
import java.util.Map;

import org.openthinclient.service.store.Profiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrintersEndpoint {
  @GetMapping({"/api/v2/printers", "/api/v2/printers/{mac}"})
  public Iterable<Map<String, String>> getPrinters(
        @PathVariable(required = false) String mac,
        @RequestParam(required = false) String userDN) {
    return Profiles.getPrinters(mac, userDN);
  }
}
