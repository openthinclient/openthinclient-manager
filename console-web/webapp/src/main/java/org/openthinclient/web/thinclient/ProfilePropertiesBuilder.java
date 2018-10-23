package org.openthinclient.web.thinclient;

import java.util.*;
import java.util.stream.Collectors;

import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.ChoiceNode;
import org.openthinclient.common.model.schema.ChoiceNode.Option;
import org.openthinclient.common.model.schema.EntryNode;
import org.openthinclient.common.model.schema.GroupNode;
import org.openthinclient.common.model.schema.Node;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.SectionNode;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.model.SelectOption;
import org.openthinclient.web.thinclient.property.OtcBooleanProperty;
import org.openthinclient.web.thinclient.property.OtcOptionProperty;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.openthinclient.web.thinclient.property.OtcTextProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Build OtcProperty and PropertyGroup-structure form schema-tree
 */
public class ProfilePropertiesBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProfilePropertiesBuilder.class);

  public List<OtcPropertyGroup> getOtcPropertyGroups(String[] schemaNames, Profile profile) throws BuildProfileException {

    // build structure from schema
    List<OtcPropertyGroup> propertyGroups = createPropertyStructure(profile);

    // apply model to configuration-structure
    bindModel2Properties(profile, propertyGroups);

    // add profile metadata-group
    propertyGroups.add(0, createProfileMetaDataGroup(schemaNames, profile));

    return propertyGroups;
  }


  private void bindModel2Properties(Profile profile, List<OtcPropertyGroup> propertyGroups) {
    propertyGroups.forEach(otcPropertyGroup -> {
      otcPropertyGroup.getOtcProperties().forEach(otcProperty -> {
        // Object o = profile.getConfiguration().getAdditionalProperties().get(otcProperty.getKey()); // json
        String profileValue = profile.getValue(otcProperty.getKey());
        ItemConfiguration ic = new ItemConfiguration(otcProperty.getKey(), profileValue);
        otcProperty.setConfiguration(ic);
      });
      bindModel2Properties(profile, otcPropertyGroup.getGroups());
    });
  }

  /**
   * @return
   * @param profile
   */
  private List<OtcPropertyGroup> createPropertyStructure(Profile profile) throws BuildProfileException {

    List<OtcPropertyGroup> properties = new ArrayList<>();
    try {
      Schema schema = profile.getSchema(profile.getRealm());
      OtcPropertyGroup group = new OtcPropertyGroup(null);
      schema.getChildren().forEach(node -> extractChildren(node, group));
      properties.add(group);
    } catch (SchemaLoadingException e) {
      throw new BuildProfileException("Schema kann nicht geladen werden für Profil: " + profile.getName(), e);
    } catch (Exception e) {
      throw new BuildProfileException("Unerwateter Fehler: " + e.getMessage(), e);
    }

    return properties;
  }

  private void extractChildren(Node node, OtcPropertyGroup group) {

    if (node instanceof ChoiceNode) {
        List<Option> options = ((ChoiceNode) node).getOptions();

        if (isProbablyBooleanProperty(options)) {
          group.addProperty(new OtcBooleanProperty(node.getLabel(), prepareTip(node.getTip()),
                  node.getKey(),
                  ((ChoiceNode) node).getValue(),
                  options.get(0).getValue(), options.get(1).getValue()));
        } else {
          group.addProperty(new OtcOptionProperty(
                  node.getLabel(),
                  prepareTip(node.getTip()),
                  node.getKey(),
                  ((ChoiceNode) node).getValue(),
                  options.stream().map(o -> new SelectOption(o.getLabel(), o.getValue())).collect(Collectors.toList())) //
          ); //
        }
      } else if (node instanceof EntryNode) {
        group.addProperty(new OtcTextProperty(node.getLabel(),  prepareTip(node.getTip()), node.getKey(), ((EntryNode) node).getValue()));

      } else if (node instanceof GroupNode || node instanceof SectionNode) {
        OtcPropertyGroup group1 = new OtcPropertyGroup(node.getLabel());
        node.getChildren().forEach(n -> extractChildren(n, group1));
        group.addGroup(group1);
      }

  }

  public OtcPropertyGroup createProfileMetaDataGroup(String[] schemaNames, Profile profile) {

    OtcPropertyGroup group = new OtcPropertyGroup(null);
    group.setCollapseOnDisplay(false);
    group.setDisplayHeaderLabel(false);
    group.addProperty(new OtcTextProperty("Name",  null, "name", profile.getName(),""));
    group.addProperty(new OtcTextProperty("Beschreibung",  null, "description", profile.getDescription(),""));
    OtcOptionProperty optionProperty = new OtcOptionProperty(
            "Typ",
            "Typ festlegen",
            "type",
            null,
             Arrays.stream(schemaNames).map(o -> new SelectOption(o, o)).collect(Collectors.toList()));
    String schemaName = null;
    if (profile.getRealm() != null) {
      schemaName = profile.getSchema(profile.getRealm()).getName();
    }
    optionProperty.setConfiguration(new ItemConfiguration(profile.getClass().getSimpleName().toLowerCase(), schemaName));
    group.addProperty(optionProperty);
    return group;
  }

  /**
   * remove HTML-Tags in tip text
   * @param tip String
   * @return String or null
   */
  private String prepareTip(String tip) {
    if (tip != null) {
      return tip.replaceAll("<html>|</html>|<br>|<b>|</b>", "");
    }
    return null;
  }

  /**
   * how to handle ugly schema values
   */
  private boolean isProbablyBooleanProperty(List<Option> options) {
    if (options.size() == 2) {
      String regex = "yes|no|ja|nein|on|off|true|false";
      return options.get(0).getValue().toLowerCase().matches(regex) && options.get(1).getValue().toLowerCase().matches(regex);
    }
    return false;
  }

  /**
   * Create Item list from DirectoryObjects
   * @param directoryObjects
   * @return
   */
  public List<Item> createItems(Set<? extends DirectoryObject>... directoryObjects) {
    return Arrays.asList(directoryObjects).stream()
                                          .flatMap(ts -> ts.stream())
                                          .map(t -> new Item(t.getName(), getType(t.getClass())))
                                          .collect(Collectors.toList());
  }

  /**
   * Filter directory-object-set by given class-type and return a list of {@link Item}-objects
   * @param members to be filtered
   * @param clazz the filter predicate
   * @return list of {@link Item}
   */
  public List<Item> createFilteredItemsFromDO(Set<? extends DirectoryObject> members, Class<?>... clazz) {
    List<Class<?>> classList = Arrays.asList(clazz);
    return members.stream()
            .filter(member -> classList.contains(member.getClass()))
            .map(member -> new Item(member.getName(), getType(member.getClass())))
            .collect(Collectors.toList());
  }


  private Item.Type getType(Class clazz) {

    Item.Type itemType;
    if (clazz.equals(Client.class)) {
        itemType = Item.Type.CLIENT;
    } else if (clazz.equals(Application.class)) {
      itemType = Item.Type.APPLICATION;
    } else if (clazz.equals(HardwareType.class)) {
      itemType = Item.Type.HARDWARE;
    } else if (clazz.equals(Location.class)) {
      itemType = Item.Type.LOCATION;
    } else if (clazz.equals(Device.class)) {
      itemType = Item.Type.DEVICE;
    } else if (clazz.equals(ApplicationGroup.class)) {
      itemType = Item.Type.APPLICATION_GROUP;
    } else if (clazz.equals(UserGroup.class)) {
      itemType = Item.Type.USER_GROUP;
    } else if (clazz.equals(ClientGroup.class)) {
      itemType = Item.Type.CLIENT_GROUP;
    } else if (clazz.equals(User.class)) {
      itemType = Item.Type.USER;
    } else if (clazz.equals(Printer.class)) {
      itemType = Item.Type.PRINTER;
    } else {
      throw new RuntimeException("ProfileObject class not mapped to item.Type: " + clazz);
    }
    return itemType;
  }

}
