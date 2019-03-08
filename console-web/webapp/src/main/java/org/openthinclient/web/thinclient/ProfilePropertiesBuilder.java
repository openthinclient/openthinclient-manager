package org.openthinclient.web.thinclient;

import java.util.*;
import java.util.stream.Collectors;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.ui.UI;
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

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

/**
 * Build OtcProperty and PropertyGroup-structure form schema-tree
 */
public class ProfilePropertiesBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProfilePropertiesBuilder.class);

  /**
   *
   * @param schemaNames SchemaNames for profile
   * @param profile the profile
   * @return list of OtcPropertyGroups
   * @throws BuildProfileException
   */
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
   * @param profile to display
   * @return a list of OtcPropertyGroup containing settings for profile
   */
  private List<OtcPropertyGroup> createPropertyStructure(Profile profile) throws BuildProfileException {

    List<OtcPropertyGroup> properties = new ArrayList<>();
    Schema schema = null;
    try {
      schema = profile.getSchema(profile.getRealm());
      OtcPropertyGroup group = new OtcPropertyGroup(null);
      schema.getChildren().forEach(node -> extractChildren(node, group, profile));
      properties.add(group);
    } catch (SchemaLoadingException e) {
      IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
      throw new BuildProfileException(mc.getMessage(UI_THINCLIENTS_SCHEMA_NOT_LOADED, profile.getName()), e);
    } catch (Exception e) {
      IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
      if (schema == null) {
        throw new BuildProfileException(mc.getMessage(UI_THINCLIENTS_SCHEMA_NOT_LOADED, profile.getName()), e);
      } else {
        throw new BuildProfileException(mc.getMessage(UI_THINCLIENTS_UNEXPECTED_ERROR, e.getMessage()), e);
      }
    }

    return properties;
  }

  private void extractChildren(Node node, OtcPropertyGroup group, Profile profile) {

    String value = profile.getValue(node.getKey());

    if (node instanceof ChoiceNode) {
        List<Option> options = ((ChoiceNode) node).getOptions();

        if (isProbablyBooleanProperty(options)) {
          group.addProperty(new OtcBooleanProperty(node.getLabel(), prepareTip(node.getTip()),
                  node.getKey(),
                  value != null ? value : ((ChoiceNode) node).getValue(),
                  options.get(0).getValue(), options.get(1).getValue()));
        } else {
          group.addProperty(new OtcOptionProperty(
                  node.getLabel(),
                  prepareTip(node.getTip()),
                  node.getKey(),
                  value != null ? value : ((ChoiceNode) node).getValue(),
                  options.stream().map(o -> new SelectOption(o.getLabel(), o.getValue())).collect(Collectors.toList())) //
          ); //
        }
      } else if (node instanceof EntryNode) {
        group.addProperty(new OtcTextProperty(node.getLabel(), prepareTip(node.getTip()), node.getKey(),
                                              value != null ? value : ((EntryNode) node).getValue()));

      } else if (node instanceof GroupNode || node instanceof SectionNode) {
        OtcPropertyGroup group1 = new OtcPropertyGroup(node.getLabel());
        node.getChildren().forEach(n -> extractChildren(n, group1, profile));
        group.addGroup(group1);
      }

  }

  public OtcPropertyGroup createProfileMetaDataGroup(String[] schemaNames, Profile profile) {

    IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());

    OtcPropertyGroup group = new OtcPropertyGroup(null);
    group.setCollapseOnDisplay(false);
    group.setDisplayHeaderLabel(false);

    OtcTextProperty property = new OtcTextProperty(mc.getMessage(UI_COMMON_NAME_LABEL), null, "name", profile.getName(), profile.getName());
    property.getConfiguration().addValidator(new StringLengthValidator(mc.getMessage(UI_PROFILE_NAME_VALIDATOR), 3, 255));
    property.getConfiguration().addValidator(new RegexpValidator(mc.getMessage(UI_PROFILE_NAME_REGEXP), "[a-zA-Z0-9\\s-_\\p{Sc}]+"));
    group.addProperty(property);

    group.addProperty(new OtcTextProperty(mc.getMessage(UI_COMMON_DESCRIPTION_LABEL),  null, "description", profile.getDescription(), profile.getDescription()));

    String schemaName = null;
    if (profile.getRealm() != null) {
      schemaName = profile.getSchema(profile.getRealm()).getName();
    }
    List<SelectOption> selectOptions = Arrays.stream(schemaNames).map(o -> new SelectOption(o, o)).collect(Collectors.toList());
    OtcOptionProperty optionProperty = new OtcOptionProperty(
             mc.getMessage(UI_COMMON_TYPE_LABEL),
             mc.getMessage(UI_COMMON_TYPE_TIP),
            "type",
             schemaName != null ? schemaName : selectOptions.size() == 1 ? selectOptions.get(0).getValue() : null,
             selectOptions);
    ItemConfiguration itemConfiguration = new ItemConfiguration(profile.getClass().getSimpleName().toLowerCase(), schemaName);
    itemConfiguration.disable();
    optionProperty.setConfiguration(itemConfiguration);
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
  public static List<Item> createItems(Set<? extends DirectoryObject>... directoryObjects) {
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
  public static List<Item> createFilteredItemsFromDO(Set<? extends DirectoryObject> members, Class<?>... clazz) {
    List<Class<?>> classList = Arrays.asList(clazz);
    return members.stream()
            .filter(member -> classList.contains(member.getClass()))
            .map(member -> new Item(member.getName(), getType(member.getClass())))
            .collect(Collectors.toList());
  }

  /**
   * Creates a clustered, sorted list of Items from DirectoryObject-list
   * @param directoryObjects the list of directoryObjects
   * @return a clustered, sorted list of Items from given list
   */
  public static List<? extends DirectoryObject> createGroupedItems(Set<? extends DirectoryObject> directoryObjects) {

    HashMap<String, List<DirectoryObject>> map = new HashMap<>();
    for (DirectoryObject o : directoryObjects) {
      String schemaName = "Misc";
      if (o instanceof Profile) {
        Profile profile = (Profile) o;
        try {
          Schema schema = profile.getSchema(profile.getRealm());
          schemaName=  schema.getLabel();
        } catch (Exception e) {
          LOGGER.warn("Profile-list-grouping broken: cannot load schema for " + profile);
        }
      }
      if (!map.containsKey(schemaName)) {
        map.put(schemaName, new ArrayList<>());
      }
      map.get(schemaName).add(o);
    }

    // sort by cluster-name
    Map<String, List<DirectoryObject>> result = map.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
            (oldValue, newValue) -> oldValue, LinkedHashMap::new));

    // put to result-list and sort values
    List<DirectoryObject> clusteredList = new ArrayList<>();
    for (Map.Entry<String, List<DirectoryObject>> entry : result.entrySet()) {

      clusteredList.add(new MenuGroupProfile(entry.getKey())); // cluster-headline
      entry.getValue().sort(Comparator.comparing(DirectoryObject::getName));
      clusteredList.addAll(entry.getValue());

    }

    // remove head if only one kind of entries
    if (map.size() == 1) {
      clusteredList.remove(0);
    }

    return clusteredList;
  }


  private static Item.Type getType(Class clazz) {

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
      // TODO: Checked excpetion
      throw new RuntimeException("ProfileObject class not mapped to item.Type: " + clazz);
    }
    return itemType;
  }

  /**
   * This is a dummy profile
   */
  static class MenuGroupProfile extends Profile {
    public MenuGroupProfile(String name) {
      setName(name);
    }
  }
}
