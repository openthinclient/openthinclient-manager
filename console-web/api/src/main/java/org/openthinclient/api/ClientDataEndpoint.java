package org.openthinclient.api;
import java.util.Collections;
import java.util.Map;

import org.openthinclient.service.store.ClientBootData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClientDataEndpoint {
  @GetMapping("/api/v2/client-data/{mac}")
  public Map<String, String> getClientData(@PathVariable String mac) {
    final ClientBootData clientData = ClientBootData.load(mac);
    if (clientData == null) {
      return Collections.emptyMap();
    }
    return clientData.getAll();
  }
}
