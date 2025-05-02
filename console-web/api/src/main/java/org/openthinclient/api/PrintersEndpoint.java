package org.openthinclient.api;
import java.util.Map;

import org.openthinclient.service.store.Profiles;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrintersEndpoint {
  /* This Endpoint is available via GET for backwards compatibility and POST to
   * support unlimited parameters (ie. groupDNs) which may be too long for a
   * GET request.
   */

  @GetMapping({"/api/v2/printers", "/api/v2/printers/{mac}"})
  public Iterable<Map<String, String>> getPrinters(
        @PathVariable(required = false) String mac,
        @RequestParam MultiValueMap<String, String> params) {
    return _getPrinters(mac, params);
  }

  @PostMapping({"/api/v2/printers", "/api/v2/printers/{mac}"})
  public Iterable<Map<String, String>> postPrinters(
        @PathVariable(required = false) String mac,
        @RequestParam MultiValueMap<String, String> params) {
    return _getPrinters(mac, params);
  }

  private Iterable<Map<String, String>> _getPrinters(
        String mac,
        MultiValueMap<String, String> params) {
    String userDN = params.getFirst("userDN");
    String[] usergroupDNs = null;
    String withUsergroups = params.getFirst("withUsergroups");
    if (withUsergroups != null && !withUsergroups.isEmpty()
        && params.containsKey("usergroupDN")) {
      usergroupDNs = params.get("usergroupDN").toArray(new String[0]);
    }
    return Profiles.getPrinters(mac, userDN, usergroupDNs);
  }
}
