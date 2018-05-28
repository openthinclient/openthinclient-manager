package org.openthinclient.api.rest.impl;

import org.openthinclient.api.rest.model.AbstractProfileObject;
import org.openthinclient.api.rest.model.Application;
import org.openthinclient.api.rest.model.Client;
import org.openthinclient.api.rest.model.Configuration;
import org.openthinclient.api.rest.model.Device;
import org.openthinclient.api.rest.model.HardwareType;
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
    source.getMembers().forEach(directoryObject -> {
      application.addMember(directoryObject.getDn());
    });
    return application;
  }

  public Client translate(Realm realm, org.openthinclient.common.model.Client source) {

    Client client = new Client();
    translate(realm, source, client);
    client.setMacAddress(source.getMacAddress());
    if (source.getLocation() != null)
      client.setLocation(translate(realm, source.getLocation()));
    if (source.getHardwareType() != null)
      client.setHardwareType(translate(realm, source.getHardwareType()));
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
    source.getMembers().forEach(member -> {
      device.addMember(member.toString());
    });

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

  public HardwareType translate(Realm realm, org.openthinclient.common.model.HardwareType source) {
    final HardwareType target = new HardwareType();
    translate(realm, source, target);
    return target;
  }

  private Schema getSchema(Realm realm, Profile source) {
    if (realm != null) {
      try {
        source.initSchemas(realm);
      } catch (SchemaLoadingException e) {
        throw new RuntimeException(e);
      }
    }
    try {
      return source.getSchema(realm);
    } catch (SchemaLoadingException e) {
      // there should be no way that this exception will actually thrown at any time. The schema
      // is ready after initSchemas.
      throw new RuntimeException(e);
    }
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
