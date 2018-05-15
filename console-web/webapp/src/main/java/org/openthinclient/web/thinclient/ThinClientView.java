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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.schema.ChoiceNode;
import org.openthinclient.common.model.schema.ChoiceNode.Option;
import org.openthinclient.common.model.schema.EntryNode;
import org.openthinclient.common.model.schema.GroupNode;
import org.openthinclient.common.model.schema.Node;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.model.RepoDummy;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

@SuppressWarnings("serial")
@SpringView(name = "devices_poc_view")
@SideBarItem(sectionId = DashboardSections.DEVICE_MANAGEMENT, caption="Devices", order = 99)
public final class ThinClientView extends Panel implements View {

   private static final Logger LOGGER = LoggerFactory.getLogger(ThinClientView.class);

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

      root.addComponent(new ViewHeader("Devices"));

   }


   @Override
   public String getCaption() {
      return "Devices";
   }

   @PostConstruct
   private void init() {

      Component content = buildContent();
      root.addComponent(content);
      root.setExpandRatio(content, 1);
   }

   private Component buildContent() {

     //  Update entity: get model-data form db
     Item config = RepoDummy.findSingleDevice();
     // Create an entity
//     Item config = new Item("MyDevice", Type.DEVICE);

     // build structure from schema
     List<OtcPropertyGroup> propertyGroups = createPropertyStructure();

     // apply model to configuration-structure
     bindModel2Properties(config, propertyGroups);

     // build layout
     OtcPropertyLayout layout = new OtcPropertyLayout() {
       @Override
       public void onSuccess() {
         super.onSuccess();
         // save values
         propertyGroups.stream().map(pg -> pg.getOtcProperties()).forEach(System.out::println);
       }
     };
     propertyGroups.forEach(layout::addProperty);


     return layout.getContent();
   }

  private void bindModel2Properties(Item config, List<OtcPropertyGroup> propertyGroups) {
    propertyGroups.forEach(otcPropertyGroup -> {
      otcPropertyGroup.getOtcProperties().forEach(otcProperty -> {
        ItemConfiguration ic = config.getConfiguration(otcProperty.getKey());
        otcProperty.setBean(ic);
      });
    });
  }

  /**
   * TODO: Schem muss anhand des Item-Typs (device, Application, usw.) ausgesucht werden
   * @return
   */
  private List<OtcPropertyGroup> createPropertyStructure() {
     List<OtcPropertyGroup> properties = new ArrayList<>();
    try {
//      final Schema<Application> schema = read("/schemas/browser/schema/application/browser.xml");
//      final Schema<Application> schema = read("/schemas/cmdline/schema/application/cmdline.xml");
//      final Schema<Application> schema = read("/schemas/rdesktop/schema/application/rdesktop.xml");
//      final Schema<Application> schema = read("/schemas/freerdp-git/schema/application/freerdp-git.xml");
      final Schema<Application> schema = read("/schemas/tcos-devices/schema/device/display.xml");
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

  protected <T extends Profile> Schema<T> read(String path) throws Exception {
    // this is essentially a copy of AbstractSchemaProvider.loadSchema
    JAXBContext CONTEXT = JAXBContext.newInstance(Schema.class);
    final Unmarshaller unmarshaller = CONTEXT.createUnmarshaller();
    return (Schema<T>) unmarshaller.unmarshal(getClass().getResourceAsStream(path));
  }


}
