package org.openthinclient.api.rest.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.openthinclient.common.model.Device;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.service.DeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/model/device")
public class DeviceResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(DeviceResource.class);

  private final DeviceService service;
  private final ModelMapper mapper;

  public DeviceResource(DeviceService service) {
    this.service = service;
    this.mapper = new ModelMapper();
  }

  @GetMapping
  public List<String> getDevices() {
    return service.findAll().stream().map(DirectoryObject::getName).collect(Collectors.toList());
  }

  @GetMapping("/{name}")
  public org.openthinclient.api.rest.model.Device getDevice(@PathVariable("name") String name) {

    final Device device = service.findByName(name);
    if (device == null)
      return null;

    return mapper.translate(device.getRealm(), device);
  }

}
