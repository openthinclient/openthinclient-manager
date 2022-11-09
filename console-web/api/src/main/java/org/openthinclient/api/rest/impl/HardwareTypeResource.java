package org.openthinclient.api.rest.impl;

import org.openthinclient.api.importer.impl.ImportModelMapper;
import org.openthinclient.api.importer.model.ImportableHardwareType;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.service.HardwareTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/model/hardware-type")
public class HardwareTypeResource {
  private final HardwareTypeService service;
  private final ImportModelMapper mapper;

  public HardwareTypeResource(HardwareTypeService service, ImportModelMapper mapper) {
    this.service = service;
    this.mapper = mapper;
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
