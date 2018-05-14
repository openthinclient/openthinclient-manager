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
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

@SuppressWarnings("serial")
@SpringView(name = "thinclientview")
@SideBarItem(sectionId = DashboardSections.COMMON, caption="ThinClient", order = 99)
public final class ThinClientView extends Panel implements View {

   private static final Logger LOGGER = LoggerFactory.getLogger(ThinClientView.class);

   private final IMessageConveyor mc;
   private final VerticalLayout root;

   private List<File> visibleItems = new ArrayList<>();

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

      root.addComponent(new ViewHeader("ThinClient"));

   }


   @Override
   public String getCaption() {
      return "ThinClient";
   }

   @PostConstruct
   private void init() {
      Component content = buildContent();
      root.addComponent(content);
      root.setExpandRatio(content, 1);
   }

   private Component buildContent() {

     // UserCase: Create entity (bspw. Browser)
     // UseCase: Update entity


     List<OtcPropertyGroup> propertyGroups = createPropertyStructure();

     OtcPropertyLayout layout = new OtcPropertyLayout();
     propertyGroups.forEach(layout::addProperty);

     return layout.getContent();
   }

  private List<OtcPropertyGroup> createPropertyStructure() {
     List<OtcPropertyGroup> properties = new ArrayList<>();
    try {
//      final Schema<Application> schema = read("/schemas/browser/schema/application/browser.xml");
//      final Schema<Application> schema = read("/schemas/cmdline/schema/application/cmdline.xml");
//      final Schema<Application> schema = read("/schemas/rdesktop/schema/application/rdesktop.xml");
      final Schema<Application> schema = read("/schemas/freerdp-git/schema/application/freerdp-git.xml");
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
        group.addProperty(new OtcOptionProperty(node.getLabel(), null,
                                                options.stream().map(o -> o.getLabel()).collect(Collectors.toList())));
      } else if (node instanceof EntryNode) {
        // TODO: node.getKey() zum sichern in DB
        group.addProperty(new OtcTextProperty(node.getLabel(), null));
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
