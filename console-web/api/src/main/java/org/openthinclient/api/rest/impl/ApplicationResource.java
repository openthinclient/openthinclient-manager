package org.openthinclient.api.rest.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.openthinclient.api.importer.impl.ImportModelMapper;
import org.openthinclient.api.importer.impl.RestModelImporter;
import org.openthinclient.api.importer.model.ImportableHardwareType;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.HardwareType;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.service.ApplicationService;
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

@RestController
@RequestMapping("/api/v1/model/application")
public class ApplicationResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationResource.class);

  private final ApplicationService service;
  private final ModelMapper mapper;

  public ApplicationResource(ApplicationService service) {
    this.service = service;
    this.mapper = new ModelMapper();;
  }

  @GetMapping
  public List<String> getApplications() {
    return service.findAll().stream().map(DirectoryObject::getName).collect(Collectors.toList());
  }

  @GetMapping("/{name}")
  public org.openthinclient.api.rest.model.Application getApplication(@PathVariable("name") String name) {

    final Application application = service.findByName(name);
    if (application == null)
      return null;

    return mapper.translate(application.getRealm(), application);
  }

}
