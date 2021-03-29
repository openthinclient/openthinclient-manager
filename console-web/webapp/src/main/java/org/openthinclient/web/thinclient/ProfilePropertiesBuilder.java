package org.openthinclient.web.thinclient;

import java.text.Collator;
import java.util.*;
import java.util.stream.Collectors;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.ui.UI;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.*;
import org.openthinclient.common.model.schema.provider.SchemaLoadingException;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.model.SelectOption;
import org.openthinclient.web.thinclient.property.*;
import org.openthinclient.web.thinclient.util.ContextInfoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

/**
 * Build OtcProperty and PropertyGroup-structure form schema-tree
 */
public class ProfilePropertiesBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProfilePropertiesBuilder.class);

  IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());

  /**
   *
   * @param schemaNames SchemaNames for profile
   * @param profile the profile
   * @return list of OtcPropertyGroups
   * @throws BuildProfileException
   */
  public List<OtcPropertyGroup> getOtcPropertyGroups(Map<String, String> schemaNames, Profile profile) throws BuildProfileException {

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
        String profileValue = profile.getValueLocal(otcProperty.getKey());
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
      OtcPropertyGroup group = new OtcPropertyGroup();
      schema.getChildren().forEach(node -> extractChildren(node, group, profile));
      properties.add(group);
    } catch (SchemaLoadingException e) {
      throw new BuildProfileException(mc.getMessage(UI_THINCLIENTS_SCHEMA_NOT_LOADED, profile.getName()), e);
    } catch (Exception e) {
      if (schema == null) {
        throw new BuildProfileException(mc.getMessage(UI_THINCLIENTS_SCHEMA_NOT_LOADED, profile.getName()), e);
      } else {
        throw new BuildProfileException(mc.getMessage(UI_THINCLIENTS_UNEXPECTED_ERROR, e.getMessage()), e);
      }
    }

    return properties;
  }

  private void extractChildren(Node node, OtcPropertyGroup group, Profile profile) {

    if (node instanceof SectionNode && "invisibleObjects".equals(node.getName())) {
      return;
    }

    String currentValue = profile.getValueLocal(node.getKey());

    if (node instanceof ChoiceNode) {
        group.addProperty(new OtcOptionProperty(
                node.getLabel(),
                ContextInfoUtil.prepareTip(node.getTip(), node.getKBArticle()),
                node.getKey(),
                currentValue,
                ((ChoiceNode) node).getValue(),
                ((ChoiceNode) node).getOptions().stream()
                        .map(o -> new SelectOption(o.getLabel(), o.getValue()))
                        .collect(Collectors.toList())
        ));
      } else if (node instanceof PasswordNode) {
         group.addProperty(new OtcPasswordProperty(node.getLabel(), ContextInfoUtil.prepareTip(node.getTip(), node.getKBArticle()), node.getKey(),
                                              ((EntryNode) node).getValue()));
      } else if (node instanceof EntryNode) {
        group.addProperty(new OtcTextProperty(node.getLabel(), ContextInfoUtil.prepareTip(node.getTip(), node.getKBArticle()), node.getKey(),
                                              currentValue,
                                              ((EntryNode) node).getValue()));

      } else if (node instanceof GroupNode || node instanceof SectionNode) {
        OtcPropertyGroup group1 = new OtcPropertyGroup(node.getLabelOrNull(), ContextInfoUtil.prepareTip(node.getTip(), node.getKBArticle()));
        node.getChildren().forEach(n -> extractChildren(n, group1, profile));
        group.addGroup(group1);
      }

  }

  public OtcPropertyGroup createDirectoryObjectMetaDataGroup(DirectoryObject directoryObject) {

    OtcPropertyGroup group = new OtcPropertyGroup();

    OtcTextProperty property = new OtcTextProperty(mc.getMessage(UI_COMMON_NAME_LABEL), null, "name", directoryObject.getName(), directoryObject.getName(), null);
    property.getConfiguration().addValidator(new StringLengthValidator(mc.getMessage(UI_PROFILE_NAME_VALIDATOR), 1, null));
    property.getConfiguration().addValidator(new RegexpValidator(mc.getMessage(UI_PROFILE_NAME_REGEXP), "[^ #].*"));
    property.getConfiguration().addValidator(new RegexpValidator(mc.getMessage(UI_PROFILE_NAME_REGEXP), "[a-zA-Z0-9 {}\\[\\]/()#.:*&`'~|?@$\\^%_-]+"));
    property.getConfiguration().addValidator(new RegexpValidator(mc.getMessage(UI_PROFILE_NAME_REGEXP), ".*[^ #]"));
    group.addProperty(property);

    group.addProperty(new OtcTextProperty(mc.getMessage(UI_COMMON_DESCRIPTION_LABEL),  null, "description", directoryObject.getDescription(), directoryObject.getDescription(), null));

    return group;
  }

  public OtcPropertyGroup createProfileMetaDataGroup(Map<String, String> schemaNames, Profile profile) {

    OtcPropertyGroup group = createDirectoryObjectMetaDataGroup(profile);

    String schemaName = null;
    if (profile.getRealm() != null) {
      schemaName = profile.getSchema(profile.getRealm()).getName();
    }
    Collator collator = Collator.getInstance();
    List<SelectOption> selectOptions = schemaNames.entrySet().stream()
                                          .sorted(Comparator.comparing(Map.Entry::getValue, collator))
                                          .map((entry) -> new SelectOption(entry.getValue(), entry.getKey()))
                                          .collect(Collectors.toList());
    OtcOptionProperty optionProperty = new OtcOptionProperty(
             mc.getMessage(UI_COMMON_TYPE_LABEL),
             null,
            "type",
             schemaName != null ? schemaName : selectOptions.size() == 1 ? selectOptions.get(0).getValue() : null,
             null,
             selectOptions);
    ItemConfiguration itemConfiguration = new ItemConfiguration(profile.getClass().getSimpleName().toLowerCase(), schemaName);
    itemConfiguration.disable();
    optionProperty.setConfiguration(itemConfiguration);
    group.addProperty(optionProperty);

    return group;
  }

  /**
   * Create Item list from DirectoryObjects
   * @param directoryObjects
   * @return
   */
  public static List<Item> createItems(Collection<? extends DirectoryObject> directoryObjects) {
    return directoryObjects.stream()
            .map(directoryObject -> new Item(directoryObject.getName(), getType(directoryObject.getClass())))
            .collect(Collectors.toList());
  }

  /**
   * Filter directory-object-set by given class-type and return a list of {@link Item}-objects
   * @param members to be filtered
   * @param clazz the filter predicate
   * @return list of {@link Item}
   */
  public static List<Item> createFilteredItemsFromDO(Collection<? extends DirectoryObject> members, Class<?> clazz) {
    if (members == null) {
      return new ArrayList<>();
    }
    return members.stream()
            .filter(member -> member.getClass() == clazz)
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
          LOGGER.warn("Profile-list-grouping broken: cannot load schema for " + profile, e);
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
      entry.getValue().sort(Comparator.comparing(DirectoryObject::getName, String::compareToIgnoreCase));
      clusteredList.addAll(entry.getValue());

    }

    // remove head if only one kind of entries
    if (map.size() == 1) {
      clusteredList.remove(0);
    }

    return clusteredList;
  }


  private static Item.Type getType(Class<? extends DirectoryObject> clazz) {

    Item.Type itemType;
    if (clazz.equals(Client.class) || clazz.equals(ClientMetaData.class)) {
        itemType = Item.Type.CLIENT;
    } else if (clazz.equals(Application.class)) {
      itemType = Item.Type.APPLICATION;
    } else if (clazz.equals(HardwareType.class)) {
      itemType = Item.Type.HARDWARETYPE;
    } else if (clazz.equals(Location.class)) {
      itemType = Item.Type.LOCATION;
    } else if (clazz.equals(Device.class)) {
      itemType = Item.Type.DEVICE;
    } else if (clazz.equals(ApplicationGroup.class)) {
      itemType = Item.Type.APPLICATIONGROUP;
    } else if (clazz.equals(UserGroup.class)) {
      itemType = Item.Type.USERGROUP;
    } else if (clazz.equals(ClientGroup.class)) {
      itemType = Item.Type.CLIENTGROUP;
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
  public static class MenuGroupProfile extends Profile {
    public MenuGroupProfile(String name) {
      setName(name);
    }
  }
}
