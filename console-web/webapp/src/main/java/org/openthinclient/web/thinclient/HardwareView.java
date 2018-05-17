package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Responsive;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.NativeSelect;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import javax.annotation.PostConstruct;
import org.openthinclient.api.rest.model.AbstractProfileObject;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.thinclient.model.RepoDummy;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

@SuppressWarnings("serial")
@SpringView(name = "hardware_poc_view")
@SideBarItem(sectionId = DashboardSections.DEVICE_MANAGEMENT, caption="Hardware", order = 95)
public final class HardwareView extends Panel implements View {

   private static final Logger LOGGER = LoggerFactory.getLogger(HardwareView.class);

  @Autowired
  private ManagerHome managerHome;

   private final IMessageConveyor mc;
   private final VerticalLayout root;

   public HardwareView() {

      mc = new MessageConveyor(UI.getCurrent().getLocale());
      
      addStyleName(ValoTheme.PANEL_BORDERLESS);
      setSizeFull();
      DashboardEventBus.register(this);

      root = new VerticalLayout();
//      root.setSizeFull();
      root.setMargin(false);
      root.addStyleName("dashboard-view");
      setContent(root);
      Responsive.makeResponsive(root);

      root.addComponent(new ViewHeader("Hardware"));

   }

   @Override
   public String getCaption() {
      return "Hardware";
   }

   NativeSelect<String> hardware;

   @PostConstruct
   private void init() {

     VerticalLayout vl = new VerticalLayout();
     vl.setMargin(new MarginInfo(false, true, false, true));

     hardware = new NativeSelect<>("Available hardware", RepoDummy.getHardware());
     hardware.setEmptySelectionAllowed(false);
     vl.addComponent(hardware);
     vl.setComponentAlignment(hardware, Alignment.TOP_LEFT);

      hardware.addValueChangeListener(event -> {
        if (vl.getComponentCount() > 1) {
          vl.removeComponent(vl.getComponent(vl.getComponentCount() - 1));
        }
        Component c = buildContent();
        vl.addComponent(c);
        vl.setExpandRatio(c, 2);
      });

      vl.setExpandRatio(hardware, 1);
      root.addComponent(vl);
   }

   private Component buildContent() {

     AbstractProfileObject profile = RepoDummy.getHardwareType(hardware.getSelectedItem().get());
     ProfileFormBuilder pfb = new ProfileFormBuilder(managerHome.getLocation().toPath(), profile) {
       @Override
       public void onSuccess() {
         RepoDummy.saveProfile(profile);
       }
     };
     return pfb.getContent();
   }



  @Override
   public void enter(ViewChangeEvent event) {

   }


}
