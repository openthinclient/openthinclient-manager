package org.openthinclient.api;
import java.util.Map;
import java.util.stream.Collectors;

import org.openthinclient.service.store.SchemaStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DefaultsEndpoint {
  @GetMapping("/api/v2/defaults")
  public Map<String, Map<String, String>> getDefaults() {
    return SchemaStore.getSchemas().entrySet().stream().collect(
          Collectors.toMap(Map.Entry::getKey, e -> e.getValue().defaults()));
  }
}
