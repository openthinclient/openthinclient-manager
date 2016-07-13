package org.openthinclient.api.rest.impl;

import org.openthinclient.api.rest.model.AbstractProfileObject;
import org.openthinclient.api.rest.model.Application;
import org.openthinclient.api.rest.model.Client;
import org.openthinclient.api.rest.model.Configuration;
import org.openthinclient.api.rest.model.Device;
import org.openthinclient.api.rest.model.Location;
import org.openthinclient.common.model.ApplicationGroup;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.schema.EntryNode;
import org.openthinclient.common.model.schema.Node;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.DefaultLDAPClientService;
import org.openthinclient.common.model.service.DefaultLDAPRealmService;
import org.openthinclient.common.model.service.RealmService;
import org.openthinclient.ldap.DirectoryException;
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

    private final RealmService realmService;
    private final ClientService clientService;

    public ProfileRepository() {
        // FIXME the services should be centrally managed and injected
        realmService = new DefaultLDAPRealmService();
        clientService = new DefaultLDAPClientService();
    }

    @RequestMapping("/clients/{hwAddress}")
    public ResponseEntity<Client> getClient(@PathVariable("hwAddress") String hwAddress) {

        final Optional<Client> client = findClient(hwAddress).map((source) -> translate(source.getRealm(), source));

        if (client.isPresent())
            return ResponseEntity.ok(client.get());
        return notFound();
    }

    private <T> ResponseEntity<T> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    }

    private Optional<org.openthinclient.common.model.Client> findClient(String hwAddress) {
        hwAddress = hwAddress.toLowerCase();

        String finalHwAddress = hwAddress;
        return realmService.findAllRealms().stream().flatMap(realm -> {
            try {
                return clientService.findByHwAddress(realm, finalHwAddress).stream();
            } catch (DirectoryException e) {
                throw new RuntimeException(e);
            }
        }).findFirst();
    }

    @RequestMapping("/clients")
    public ResponseEntity<List<Client>> getClients() {

        final Stream<Client> clients = realmService.findAllRealms().stream().flatMap(realm -> {
            try {
                return clientService.findAll(realm).stream();
            } catch (DirectoryException e) {
                throw new RuntimeException(e);
            }
        }).map((source) -> translate(source.getRealm(), source));

        return ResponseEntity.ok(clients.collect(Collectors.toList()));
    }

    @RequestMapping("/clients/{hwAddress}/devices")
    public ResponseEntity<List<Device>> getDevices(@PathVariable("hwAddress") String hwAddress) {
        final Optional<org.openthinclient.common.model.Client> opt = findClient(hwAddress);

        if (!opt.isPresent()) {
            return notFound();
        }

        final org.openthinclient.common.model.Client client = opt.get();

        final List<Device> res = client.getDevices().stream().map((source) -> translate(client.getRealm(), source)).collect(Collectors.toList());

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
        final List<Application> res = localApplications.map((source) -> translate(realm, source)).collect(Collectors.toList());

        // process all application groups recursively

        for (ApplicationGroup applicationGroup : client.getApplicationGroups()) {
            addApplications(realm, applicationGroup, res);
        }


        return ResponseEntity.ok(res);

    }

    private void addApplications(Realm realm, ApplicationGroup applicationGroup, List<Application> res) {

        for (org.openthinclient.common.model.Application source : applicationGroup.getApplications()) {
            final Application application = translate(realm, source);
            if (application != null) {
                res.add(application);
            }
        }

        for (ApplicationGroup group : applicationGroup.getApplicationGroups()) {
            addApplications(realm, group, res);
        }
    }

    private Location translate(Realm realm, org.openthinclient.common.model.Location source) {
        final Location location = new Location();
        translate(realm, source, location);
        return location;
    }

    private Device translate(Realm realm, org.openthinclient.common.model.Device source) {

        final Device device = new Device();

        translate(realm, source, device);

        return device;
    }

    private void translate(Realm realm, Profile source, AbstractProfileObject target) {
        final Schema schema = getSchema(realm, source);

        target.setSubtype(schema.getName());

        target.setName(source.getName());
        target.setDescription(source.getDescription());

        target.setConfiguration(createConfiguration(realm, source));
    }

    private Application translate(Realm realm, org.openthinclient.common.model.Application source) {

        final Application application = new Application();
        translate(realm, source, application);
        return application;
    }


    private Client translate(Realm realm, org.openthinclient.common.model.Client source) {

        Client client = new Client();
        translate(realm, source, client);
        client.setMacAddress(source.getMacAddress());
        client.setLocation(translate(realm, source.getLocation()));

        return client;
    }

    private Configuration createConfiguration(Realm realm, Profile source) {

        Schema schema = getSchema(realm, source);

        if (schema == null)
            return null;
        final Configuration configuration = new Configuration();
        applyConfiguration(source, configuration, schema.getChildren(), "");
        return configuration;
    }

    private Schema getSchema(Realm realm, Profile source) {
        if (realm != null) {
            try {
                source.initSchemas(realm);
            } catch (SchemaLoadingException e) {
                throw new RuntimeException(e);
            }
        }
        Schema schema = null;
        try {
            schema = source.getSchema(realm);


        } catch (SchemaLoadingException e) {
            e.printStackTrace();
        }
        return schema;
    }

    private void applyConfiguration(Profile profile, Configuration configuration, List<Node> configurationNodes, String prefix) {

        for (Node n : configurationNodes) {
            if (n instanceof EntryNode) {
                final String key = prefix + n.getName();
                final String val = profile.getValue(key);
                configuration.setAdditionalProperty(key, val);
            }

            if (n.getChildren() != null && n.getChildren().size() > 0) {
                applyConfiguration(profile, configuration, n.getChildren(), prefix + n.getName() + ".");
            }
        }

    }


}
