package org.openthinclient.api.rest.impl;

import org.openthinclient.api.rest.model.Application;
import org.openthinclient.api.rest.model.Client;
import org.openthinclient.api.rest.model.Device;
import org.openthinclient.api.rest.model.Printer;
import org.openthinclient.common.model.ApplicationGroup;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping(value = "/api/v1/profiles/", method = RequestMethod.GET, produces = "application/json")
public class ProfileRepository {

  private final ClientService clientService;
  private final ModelMapper mapper;

  @Autowired
  public ProfileRepository(ClientService clientService) {
    this.clientService = clientService;

    mapper = new ModelMapper();
  }

  @RequestMapping("/clients/{hwAddress}")
  public ResponseEntity<Client> getClient(@PathVariable("hwAddress") String hwAddress) {

    final Optional<Client> client = findClient(hwAddress).map((source) -> mapper.translate(source.getRealm(), source));

    if (client.isPresent())
      return ResponseEntity.ok(client.get());
    return notFound();
  }

  private <T> ResponseEntity<T> notFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
  }

  private Optional<org.openthinclient.common.model.Client> findClient(String hwAddress) {
    hwAddress = hwAddress.toLowerCase();

    return clientService.findByHwAddress(hwAddress).stream().findFirst();
  }

  @RequestMapping("/clients")
  public ResponseEntity<List<Client>> getClients() {

    final Stream<Client> clients = clientService.findAll().stream() //
            .map((source) -> mapper.translate(source.getRealm(), source));

    return ResponseEntity.ok(clients.collect(Collectors.toList()));
  }

  @RequestMapping("/clients/{hwAddress}/devices")
  public ResponseEntity<List<Device>> getDevices(@PathVariable("hwAddress") String hwAddress) {
    final Optional<org.openthinclient.common.model.Client> opt = findClient(hwAddress);

    if (!opt.isPresent()) {
      return notFound();
    }

    final org.openthinclient.common.model.Client client = opt.get();

    final List<Device> res =
            Stream.concat( //
                    // include the devices directly associated with the client
                    client.getDevices().stream(), //
                    // and the devices assigned to the hardware type
                    client.getHardwareType().getDevices().stream() //
            )
                    .map((source) -> mapper.translate(client.getRealm(), source)).collect(Collectors.toList());

    return ResponseEntity.ok(res);

  }

  @RequestMapping("/clients/{hwAddress}/applications")
  public ResponseEntity<List<Application>> getApplications(@PathVariable("hwAddress") String hwAddress) {
    final Optional<org.openthinclient.common.model.Client> opt = findClient(hwAddress);

    if (!opt.isPresent()) {
      return notFound();
    }

    final org.openthinclient.common.model.Client client = opt.get();

    final Stream<org.openthinclient.common.model.Application> localApplications = client.getApplications().stream();

    final Realm realm = client.getRealm();
    final List<Application> res = localApplications.map((source) -> mapper.translate(realm, source)).collect(Collectors.toList());

    // process all application groups recursively

    for (ApplicationGroup applicationGroup : client.getApplicationGroups()) {
      addApplications(realm, applicationGroup, res);
    }


    return ResponseEntity.ok(res);

  }

  @RequestMapping("/clients/{hwAddress}/printers")
  public ResponseEntity<List<Printer>> getPrinters(@PathVariable("hwAddress") String hwAddress) {
    final Optional<org.openthinclient.common.model.Client> opt = findClient(hwAddress);

    if (!opt.isPresent()) {
      return notFound();
    }

    final org.openthinclient.common.model.Client client = opt.get();

    final Stream<org.openthinclient.common.model.Printer> printers =
            Stream.concat( //
                    client.getPrinters().stream(), //
                    client.getLocation().getPrinters().stream() //
            );

    final Realm realm = client.getRealm();
    final List<Printer> res = printers.map((source) -> mapper.translate(realm, source)).collect(Collectors.toList());

    return ResponseEntity.ok(res);
  }

  private void addApplications(Realm realm, ApplicationGroup applicationGroup, List<Application> res) {

    for (org.openthinclient.common.model.Application source : applicationGroup.getApplications()) {
      final Application application = mapper.translate(realm, source);
      if (application != null) {
        res.add(application);
      }
    }

    for (ApplicationGroup group : applicationGroup.getApplicationGroups()) {
      addApplications(realm, group, res);
    }
  }

}
