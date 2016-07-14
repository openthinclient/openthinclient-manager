package org.openthinclient.api.rest.impl;

import org.openthinclient.api.rest.model.AbstractProfileObject;
import org.openthinclient.api.rest.model.Application;
import org.openthinclient.api.rest.model.Client;
import org.openthinclient.api.rest.model.Configuration;
import org.openthinclient.api.rest.model.Device;
import org.openthinclient.api.rest.model.Location;
import org.openthinclient.api.rest.model.Printer;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.Realm;
import org.openthinclient.common.model.schema.EntryNode;
import org.openthinclient.common.model.schema.Node;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;

import java.util.List;

public class ModelMapper {
    public Application translate(Realm realm, org.openthinclient.common.model.Application source) {

        final Application application = new Application();
        translate(realm, source, application);
        return application;
    }

    public Client translate(Realm realm, org.openthinclient.common.model.Client source) {

        Client client = new Client();
        translate(realm, source, client);
        client.setMacAddress(source.getMacAddress());
        client.setLocation(translate(realm, source.getLocation()));

        return client;
    }

    public Configuration createConfiguration(Realm realm, Profile source) {

        Schema schema = getSchema(realm, source);

        if (schema == null)
            return null;
        final Configuration configuration = new Configuration();
        applyConfiguration(source, configuration, schema.getChildren(), "");
        return configuration;
    }

    public Device translate(Realm realm, org.openthinclient.common.model.Device source) {

        final Device device = new Device();

        translate(realm, source, device);

        return device;
    }

    public Printer translate(Realm realm, org.openthinclient.common.model.Printer source) {

        final Printer printer = new Printer();

        translate(realm, source, printer);

        return printer;
    }

    public void translate(Realm realm, Profile source, AbstractProfileObject target) {
        final Schema schema = getSchema(realm, source);

        target.setSubtype(schema.getName());

        target.setName(source.getName());
        target.setDescription(source.getDescription());

        target.setConfiguration(createConfiguration(realm, source));
    }

    public Location translate(Realm realm, org.openthinclient.common.model.Location source) {
        final Location location = new Location();
        translate(realm, source, location);
        return location;
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
