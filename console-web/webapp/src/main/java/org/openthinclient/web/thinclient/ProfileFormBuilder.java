package org.openthinclient.web.thinclient;

import com.vaadin.ui.Component;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.Device;
import org.openthinclient.common.model.Printer;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.schema.ChoiceNode;
import org.openthinclient.common.model.schema.ChoiceNode.Option;
import org.openthinclient.common.model.schema.EntryNode;
import org.openthinclient.common.model.schema.GroupNode;
import org.openthinclient.common.model.schema.Node;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.property.OtcOptionProperty;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.openthinclient.web.thinclient.property.OtcTextProperty;

/**
 *
 */
public class ProfileFormBuilder {

  private Path managerHomePath;
  private Profile profile;

  public ProfileFormBuilder(Path managerHomePath , Profile profile) {
    this.managerHomePath = managerHomePath;
    this.profile = profile;
  }

  public Component getContent() {

    // build structure from schema
    List<OtcPropertyGroup> propertyGroups = createPropertyStructure(profile);

    // apply model to configuration-structure
    bindModel2Properties(profile, propertyGroups);

    // build layout
    ProfileFormLayout layout = new ProfileFormLayout(profile.getName(), profile.getClass()) {
      @Override
      public void onSuccess() {
        super.onSuccess();
        // get back values and put them to profile-configuration
        List<OtcProperty> otcPropertyList = propertyGroups.stream()
            .flatMap(otcPropertyGroup -> otcPropertyGroup.getAllOtcProperties().stream())
            .collect(Collectors.toList());
        otcPropertyList.forEach(otcProperty -> {
          ItemConfiguration bean = otcProperty.getBean();
          // profile.getConfiguration().setAdditionalProperty(bean.getKey(), bean.getValue()); // json
          profile.setValue(bean.getKey(), bean.getValue());
        });
        // call success
        ProfileFormBuilder.this.onSuccess();
      }
    };
    propertyGroups.forEach(layout::addProperty);


    return layout.getContent();

  }

  public void onSuccess() { }

  private void bindModel2Properties(Profile profile, List<OtcPropertyGroup> propertyGroups) {
    propertyGroups.forEach(otcPropertyGroup -> {
      otcPropertyGroup.getOtcProperties().forEach(otcProperty -> {
        // Object o = profile.getConfiguration().getAdditionalProperties().get(otcProperty.getKey()); // json
        String s = profile.getValue(otcProperty.getKey());
        ItemConfiguration ic = new ItemConfiguration(otcProperty.getKey(), s != null ? s : "");
        otcProperty.setBean(ic);
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
      // json
      // ProfileType profileType = profile.getType();
      // String profileSubtype = profile.getSubtype();
      String filePath;
      // if (profileType == ProfileType.APPLICATION || profileType == ProfileType.DEVICE || profileType == ProfileType.PRINTER) {
      //  filePath = profileType.name().toLowerCase() + "/" + profileSubtype + ".xml";
      // } else {
      //  filePath = profileSubtype + ".xml";
      // }

      // das ist genau der Mist warum der alte LDAP Kram hier nix zu suchen hat: deprecated, instanceof
      if (profile instanceof Application || profile instanceof Device || profile instanceof Printer) {
        filePath = profile.getClass().getSimpleName().toLowerCase() + "/" + profile.getProperties().getDescription() + ".xml";
      } else {
        filePath = profile.getProperties().getDescription() + ".xml";
      }

      File file = Paths.get(managerHomePath.toString(),"/nfs/root/schema/", filePath).toFile();
      final Schema<?> schema = read(file);

      OtcPropertyGroup group = new OtcPropertyGroup(null);
      schema.getChildren().forEach(node -> {
          extractChildren(node, group);
      });
      properties.add(group);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return properties;
  }

  private void extractChildren(Node node, OtcPropertyGroup group) {
      if (node instanceof ChoiceNode) {
        List<Option> options = ((ChoiceNode) node).getOptions();
        // TODO: wir brauchen keys _und_ values der choice
        // TODO: default value muss noch übergeben werden
        group.addProperty(new OtcOptionProperty(node.getLabel(), node.getKey(), options.stream().map(o -> o.getLabel()).collect(Collectors.toList()))); //

      } else if (node instanceof EntryNode) {
        // TODO: boolean-property erkennen
        group.addProperty(new OtcTextProperty(node.getLabel(), node.getKey()));

      } else if (node instanceof GroupNode) {
        OtcPropertyGroup group1 = new OtcPropertyGroup(node.getLabel());
        node.getChildren().forEach(n -> extractChildren(n, group1));
        group.addGroup(group1);
      }

  }


  protected <T extends Profile> Schema<T> read(File file) throws Exception {
    // this is essentially a copy of AbstractSchemaProvider.loadSchema
    JAXBContext CONTEXT = JAXBContext.newInstance(Schema.class);
    final Unmarshaller unmarshaller = CONTEXT.createUnmarshaller();
    return (Schema<T>) unmarshaller.unmarshal(file);
  }


}
