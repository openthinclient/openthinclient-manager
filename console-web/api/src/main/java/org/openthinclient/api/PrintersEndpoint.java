package org.openthinclient.api;
import java.util.Map;

import org.openthinclient.service.store.Profiles;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrintersEndpoint {
  @GetMapping({"/api/v2/printers", "/api/v2/printers/{mac}"})
  public Iterable<Map<String, String>> getPrinters(
        @PathVariable(required = false) String mac,
        @RequestParam MultiValueMap<String, String> params) {
    String userDN = params.getFirst("userDN");
    String[] usergroupDNs = null;
    String withUsergroups = params.getFirst("withUsergroups");
    if (withUsergroups != null && !withUsergroups.isEmpty()) {
      usergroupDNs = params.get("usergroupDN").toArray(new String[0]);
    }
    return Profiles.getPrinters(mac, userDN, usergroupDNs);
  }
}
