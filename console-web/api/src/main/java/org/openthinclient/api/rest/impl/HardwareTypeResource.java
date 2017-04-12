package org.openthinclient.api.rest.impl;

import org.openthinclient.api.importer.impl.ImportModelMapper;
import org.openthinclient.api.importer.impl.RestModelImporter;
import org.openthinclient.api.importer.model.ImportableHardwareType;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.service.HardwareTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/model/hardware-type")
public class HardwareTypeResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(HardwareTypeResource.class);
  private final RestModelImporter importer;
  private final HardwareTypeService service;
  private final ImportModelMapper mapper;

  public HardwareTypeResource(RestModelImporter importer, HardwareTypeService service, ImportModelMapper mapper) {
    this.importer = importer;
    this.service = service;
    this.mapper = mapper;
  }

  @PostMapping()
  public ResponseEntity<String> createHardwareType(@RequestBody ImportableHardwareType hardwareType) {

    try {
      importer.importHardwareType(hardwareType);
    } catch (Exception e) {
      LOGGER.error("Received illegal hardware type creation request", e);
      return ResponseEntity.badRequest().body(e.getMessage());
    }

    return ResponseEntity.status(HttpStatus.CREATED).body("");

  }

  @GetMapping
  public List<String> getHardwareTypeNames() {
    return service.findAll().stream().map(HardwareType::getName).collect(Collectors.toList());
  }

  @GetMapping("/{name}")
  public ImportableHardwareType getHardwareType(@PathVariable("name") String name) {

    final HardwareType hw = service.findByName(name);
    if (hw == null)
      return null;

    return mapper.toImportable(hw);
  }

}
