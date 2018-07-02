package org.openthinclient.api.rest.impl;

import org.openthinclient.api.rest.model.*;
import org.openthinclient.common.model.ApplicationGroup;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.User;
import org.openthinclient.common.model.UserGroup;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.HardwareTypeService;
import org.openthinclient.common.model.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping(value = "/api/v1/profiles/", method = RequestMethod.GET, produces = "application/json")
public class ProfileRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProfileRepository.class);

  private final ClientService clientService;
  private final UserService userService;
  private final HardwareTypeService hardwareTypeService;
  private final ModelMapper mapper;

  @Autowired
  public ProfileRepository(ClientService clientService, UserService userService, HardwareTypeService hardwareTypeService) {
    this.clientService = clientService;
    this.userService = userService;
    this.hardwareTypeService = hardwareTypeService;
    mapper = new ModelMapper();
  }

  @RequestMapping("/clients/{hwAddress}")
  public ResponseEntity<Client> getClient(@PathVariable("hwAddress") String hwAddress) {

    Optional<org.openthinclient.common.model.Client> optional = findClient(hwAddress);

    if (optional.isPresent()) {
        org.openthinclient.common.model.Client source = optional.get();
        Client client = mapper.translate(source.getRealm(), source);
        return ResponseEntity.ok(resolveConfiguration(source.getRealm(), client));
    }
    return notFound();
  }

  /**
   *
   * "BootOptions.NFSRootserver": "${myip}" - kann an Client, Standort oder Realm konfiguriert werden
   * "BootOptions.TFTPBootserver": "${myip}" - kann an Client, Standort oder Realm konfiguriert werden
   *
   * Cient-Konfiguration überschreibt Standort-Konfiguration überschreibt Realm-Konfiguration,
   * wenn keine Werte konfiguriert sind, wird die IP des Servers verwendet aber ${myip} wird durch die IP des Servers ersetzt.
   *
   *
   * @param realm
   * @param profileObject extends AbstractProfileObject
   * @return AbstractProfileObject with merged and resolved configuration
   */
  private <T extends AbstractProfileObject> T resolveConfiguration(Realm realm, T profileObject) {

      String hostname = realm != null ? realm.getConnectionDescriptor().getHostname() : null;
      String baseDN   = realm != null ? realm.getConnectionDescriptor().getBaseDN()   : null;

      if (hostname == null || hostname.length() == 0) {
          LOGGER.warn("Hostname not found, this leads to inproper client-configuration.");
      }
      if (baseDN == null || baseDN.length() == 0) {
          LOGGER.warn("BaseDN not found, this leads to inproper client-configuration.");
      }

      // merge configuration into client-configuration
      if (profileObject instanceof Client) {
        Client client = (Client) profileObject;
        if (client.getHardwareType() != null) {
          mergeConfiguration(client, client.getHardwareType().getConfiguration());
          client.setHardwareType(null);
        }
        if (client.getLocation() != null) {
          mergeConfiguration(client, client.getLocation().getConfiguration());
          client.setLocation(null);
        }
      }

      // resolve ${myip}
      // resolve ${urlencoded:basedn}
      Map<String, Object> additionalProperties = profileObject.getConfiguration().getAdditionalProperties();
      Set<Map.Entry<String, Object>> entries = additionalProperties.entrySet();
      entries.forEach(entry -> {
            if (entry.getValue() != null && entry.getValue().toString().contains("${myip}") && hostname != null) {
                entry.setValue(entry.getValue().toString().replaceAll("\\$\\{myip\\}", hostname));
            }

            if (entry.getValue() != null && entry.getValue().toString().contains("${urlencoded:basedn}") && baseDN != null) {
                entry.setValue(entry.getValue().toString().replaceAll("\\$\\{urlencoded\\:basedn\\}", baseDN));
            }
      });

    return profileObject;
  }

    /**
     * Merge configuration into client confguration
     * @param client Client
     * @param conf Configuration
     */
    private void mergeConfiguration(Client client, Configuration conf) {
        Map<String, Object> clientProperties = client.getConfiguration().getAdditionalProperties();
        conf.getAdditionalProperties().forEach((key, value) -> {
            if (!clientProperties.containsKey(key)) {
                clientProperties.put(key, value);
            }
        });
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

  @RequestMapping("/users/{sAMAccountName}/applications")
  public ResponseEntity<List<Application>> getApplicationByUser(@PathVariable("sAMAccountName") String sAMAccountName) {
    final Optional<User> opt = userService.findBySAMAccountName(sAMAccountName);

    if (!opt.isPresent()) {
      return notFound();
    }

    final User user = opt.get();

    final Realm realm = user.getRealm();
    final List<Application> res = Stream.concat(
              user.getApplications().stream(),
              Stream.concat(
                user.getUserGroups().stream().map(UserGroup::getApplications).flatMap(Collection::stream),
                      Stream.concat(
                              user.getApplicationGroups().stream().map(this::getApplications).flatMap(Collection::stream),
                              user.getUserGroups().stream()
                                      .map(UserGroup::getApplicationGroups).flatMap(Collection::stream)
                                      .map(this::getApplications).flatMap(Collection::stream)
                      )
              )
            )
            .map((source) -> mapper.translate(realm, source))
            .collect(Collectors.toList());
    return ResponseEntity.ok(res);
  }

  @RequestMapping("/users/{sAMAccountName}/printers")
  public ResponseEntity<List<Printer>> getPrinterByUser(@PathVariable("sAMAccountName") String sAMAccountName) {
    final Optional<User> opt = userService.findBySAMAccountName(sAMAccountName);

    if (!opt.isPresent()) {
      return notFound();
    }

    final User user = opt.get();

    final Realm realm = user.getRealm();
    final List<Printer> res = Stream.concat(
                user.getPrinters().stream(),
                user.getUserGroups().stream().map(UserGroup::getPrinters).flatMap(Collection::stream)
            )
            .map((source) -> mapper.translate(realm, source))
            .collect(Collectors.toList());
    return ResponseEntity.ok(res);
  }

  @GetMapping("/hardware-type/{name}")
  public ResponseEntity<org.openthinclient.api.rest.model.HardwareType> getHardwareType(@PathVariable("name") String name) {

    final org.openthinclient.common.model.HardwareType hw = hardwareTypeService.findByName(name);
    if (hw == null)
      return notFound();

    hw.initSchemas(hw.getRealm());

    HardwareType hardwareType = mapper.translate(hw.getRealm(), hw);
    return ResponseEntity.ok(resolveConfiguration(hw.getRealm(), hardwareType));

  }


  private Set<org.openthinclient.common.model.Application> getApplications(ApplicationGroup applicationGroup) {
    Set<org.openthinclient.common.model.Application> applications = applicationGroup.getApplications();
    applicationGroup.getApplicationGroups().forEach(ag -> applications.addAll(getApplications(ag)));
    return applications;
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
