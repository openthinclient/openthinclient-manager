package org.openthinclient.api;
import java.util.Map;

import org.openthinclient.service.store.Profiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApplicationsEndpoint {
  @GetMapping({"/api/v2/applications", "/api/v2/applications/{mac}"})
  public Iterable<Map<String, String>> getApplications(
        @PathVariable(required = false) String mac,
        @RequestParam(required = false) String userDN) {
    return Profiles.getApps(mac, userDN);
  }
}
