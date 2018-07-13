package org.openthinclient.web.thinclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.ChoiceNode;
import org.openthinclient.common.model.schema.ChoiceNode.Option;
import org.openthinclient.common.model.schema.EntryNode;
import org.openthinclient.common.model.schema.GroupNode;
import org.openthinclient.common.model.schema.Node;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.SectionNode;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.model.SelectOption;
import org.openthinclient.web.thinclient.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Build OtcProperty and PropertyGroup-structure form schema-tree
 */
public class ProfilePropertiesBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProfilePropertiesBuilder.class);

  public List<OtcPropertyGroup> getOtcPropertyGroups(Profile profile) {

    // build structure from schema
    List<OtcPropertyGroup> propertyGroups = createPropertyStructure(profile);

    // apply model to configuration-structure
    bindModel2Properties(profile, propertyGroups);

    return propertyGroups;
  }


  @Deprecated
  public ProfileFormLayout getContent(Profile profile) {

    // build structure from schema
    List<OtcPropertyGroup> propertyGroups = createPropertyStructure(profile);

    // apply model to configuration-structure
    bindModel2Properties(profile, propertyGroups);

    // build layout
    ProfileFormLayout layout = new ProfileFormLayout(profile.getName(), profile.getClass());
    propertyGroups.forEach(layout::addProperty);
    layout.onBeanValuesWritten(() -> {
        // get back values and put them to profile-configuration
        List<OtcProperty> otcPropertyList = propertyGroups.stream()
            .flatMap(otcPropertyGroup -> otcPropertyGroup.getAllOtcProperties().stream())
            .collect(Collectors.toList());
        otcPropertyList.forEach(otcProperty -> {
          ItemConfiguration bean = otcProperty.getConfiguration();
          String org = profile.getValue(bean.getKey());
          String current = bean.getValue();
          if (current != null && !StringUtils.equals(org, current)) {
            LOGGER.info("Apply value for " + bean.getKey() + "=" + org + " with new value '" + current + "'");
            profile.setValue(bean.getKey(), bean.getValue());
          }
        });
        layout.valuesSaved();
    });

    return layout;
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
  private List<OtcPropertyGroup> createPropertyStructure(Profile profile) {

    List<OtcPropertyGroup> properties = new ArrayList<>();
    try {
      Schema schema = profile.getSchema(profile.getRealm());
      OtcPropertyGroup group = new OtcPropertyGroup(null);
      schema.getChildren().forEach(node -> extractChildren(node, group));
      properties.add(group);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return properties;
  }

  private void extractChildren(Node node, OtcPropertyGroup group) {

    if (node instanceof ChoiceNode) {
        List<Option> options = ((ChoiceNode) node).getOptions();

        if (isProbablyBooleanProperty(options)) {
          group.addProperty(new OtcBooleanProperty(node.getLabel(), node.getKey(),
                  ((ChoiceNode) node).getValue(),
                  options.get(0).getValue(), options.get(1).getValue()));
        } else {
          group.addProperty(new OtcOptionProperty(
                  node.getLabel(),
                  node.getKey(),
                  ((ChoiceNode) node).getValue(),
                  options.stream().map(o -> new SelectOption(o.getLabel(), o.getValue())).collect(Collectors.toList())) //
          ); //
        }
      } else if (node instanceof EntryNode) {
        group.addProperty(new OtcTextProperty(node.getLabel(), node.getKey(), ((EntryNode) node).getValue()));

      } else if (node instanceof GroupNode || node instanceof SectionNode) {
        OtcPropertyGroup group1 = new OtcPropertyGroup(node.getLabel());
        node.getChildren().forEach(n -> extractChildren(n, group1));
        group.addGroup(group1);
      }

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

  public List<Item> createItems(Set<Client> clients) {
    return clients.stream().map(client -> new Item(client.getName(), Item.Type.CLIENT)).collect(Collectors.toList());
  }
}
