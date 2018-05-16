package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Responsive;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Component;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.openthinclient.api.importer.model.ImportableHardwareType;
import org.openthinclient.api.importer.model.ProfileType;
import org.openthinclient.api.rest.model.AbstractProfileObject;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.schema.ChoiceNode;
import org.openthinclient.common.model.schema.ChoiceNode.Option;
import org.openthinclient.common.model.schema.EntryNode;
import org.openthinclient.common.model.schema.GroupNode;
import org.openthinclient.common.model.schema.Node;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.model.RepoDummy;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

@SuppressWarnings("serial")
@SpringView(name = "devices_poc_view")
@SideBarItem(sectionId = DashboardSections.DEVICE_MANAGEMENT, caption="Devices", order = 99)
public final class ThinClientView extends Panel implements View {

   private static final Logger LOGGER = LoggerFactory.getLogger(ThinClientView.class);

  @Autowired
  private ManagerHome managerHome;

   private final IMessageConveyor mc;
   private final VerticalLayout root;

   public ThinClientView() {

      mc = new MessageConveyor(UI.getCurrent().getLocale());
      
      addStyleName(ValoTheme.PANEL_BORDERLESS);
      setSizeFull();
      DashboardEventBus.register(this);

      root = new VerticalLayout();
      root.setSizeFull();
      root.setMargin(false);
      root.addStyleName("dashboard-view");
      setContent(root);
      Responsive.makeResponsive(root);

      root.addComponent(new ViewHeader("Profile"));

   }


   @Override
   public String getCaption() {
      return "Profile";
   }

   @PostConstruct
   private void init() {

      Component content = buildContent();
      root.addComponent(content);
      root.setExpandRatio(content, 1);
   }

   private Component buildContent() {

     //  Update entity: get model-data form db
     AbstractProfileObject profile = RepoDummy.getHardwareType("DE - Nvidia-Grafik - 64 Bit - home im RAM - autologin");
//     AbstractProfileObject profile = RepoDummy.getApplication("Windows TS - FreeRDP");

     // Create an entity
      // TODO

     // build structure from schema
     List<OtcPropertyGroup> propertyGroups = createPropertyStructure(profile);

     // apply model to configuration-structure
     bindModel2Properties(profile, propertyGroups);

     // build layout
     OtcPropertyLayout layout = new OtcPropertyLayout(profile.getName()) {
       @Override
       public void onSuccess() {
         super.onSuccess();
         // get back values and put them to profile-configuration
         List<OtcProperty> otcPropertyList = propertyGroups.stream()
             .flatMap(otcPropertyGroup -> otcPropertyGroup.getOtcProperties().stream())
             .collect(Collectors.toList());
         otcPropertyList.forEach(otcProperty -> {
           ItemConfiguration bean = otcProperty.getBean();
           profile.getConfiguration().setAdditionalProperty(bean.getKey(), bean.getValue());
         });

         // save
         RepoDummy.saveProfile(profile);
       }
     };
     propertyGroups.forEach(layout::addProperty);


     return layout.getContent();
   }

  private void bindModel2Properties(AbstractProfileObject profile, List<OtcPropertyGroup> propertyGroups) {
    propertyGroups.forEach(otcPropertyGroup -> {
      otcPropertyGroup.getOtcProperties().forEach(otcProperty -> {
        Object o = profile.getConfiguration().getAdditionalProperties().get(otcProperty.getKey());
        ItemConfiguration ic = new ItemConfiguration(otcProperty.getKey(), o != null ? o.toString() : "");
        otcProperty.setBean(ic);
      });
    });
  }

  /**
   * TODO: Schem muss anhand des Item-Typs (device, Application, usw.) ausgesucht werden
   * @return
   * @param profile
   */
  private List<OtcPropertyGroup> createPropertyStructure(AbstractProfileObject profile) {

     List<OtcPropertyGroup> properties = new ArrayList<>();
    try {
      ProfileType profileType = profile.getType();
      String profileSubtype = profile.getSubtype();
      Path managerHomePath = managerHome.getLocation().toPath();

//      final Schema<Application> schema = read("/schemas/browser/schema/application/browser.xml");
//      final Schema<Application> schema = read("/schemas/cmdline/schema/application/cmdline.xml");
//      final Schema<Application> schema = read("/schemas/rdesktop/schema/application/rdesktop.xml");
//      final Schema<Application> schema = read("/schemas/freerdp-git/schema/application/freerdp-git.xml");
//      final Schema<HardwareType> schema = read("/schemas/tcos-devices/schema/hardwaretype.xml");
      String filePath;
      if (profileType == ProfileType.APPLICATION || profileType == ProfileType.DEVICE || profileType == ProfileType.PRINTER) {
        filePath = profileType.name().toLowerCase() + "/" + profileSubtype + ".xml";
      } else {
        filePath = profileSubtype + ".xml";
      }

      File file = Paths.get(managerHomePath.toString(),"/nfs/root/schema/", filePath).toFile();
      final Schema<?> schema = read(file);

      schema.getChildren().forEach(node -> {
        if (node instanceof GroupNode) {
          OtcPropertyGroup group = new OtcPropertyGroup(node.getLabel());
          extractChildren(node, group);
          properties.add(group);
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
    }

    return properties;
  }

  private void extractChildren(Node parent, OtcPropertyGroup group) {
    parent.getChildren().forEach(node -> {
      if (node instanceof ChoiceNode) {
        List<Option> options = ((ChoiceNode) node).getOptions();
        // TODO: wir brauchen keys _und_ values der choice
        // TODO: default value muss noch übergeben werden
        group.addProperty(new OtcOptionProperty(node.getLabel(), node.getKey(),
                                                options.stream().map(o -> o.getLabel()).collect(Collectors.toList())));
      } else if (node instanceof EntryNode) {
        // TODO: boolean-property erkennen
        group.addProperty(new OtcTextProperty(node.getLabel(), node.getKey()));
      } else  if (node instanceof GroupNode) {
        OtcPropertyGroup group1 = new OtcPropertyGroup(node.getLabel());
        extractChildren(node, group1);
        group.addGroup(group1);
      }
    });
  }

  @Override
   public void enter(ViewChangeEvent event) {

   }

  protected <T extends Profile> Schema<T> read(File file) throws Exception {
    // this is essentially a copy of AbstractSchemaProvider.loadSchema
    JAXBContext CONTEXT = JAXBContext.newInstance(Schema.class);
    final Unmarshaller unmarshaller = CONTEXT.createUnmarshaller();
    return (Schema<T>) unmarshaller.unmarshal(file);
  }


}
