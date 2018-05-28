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
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.openthinclient.api.rest.model.AbstractProfileObject;
import org.openthinclient.common.model.Device;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.DeviceService;
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
@SpringView(name = "devices_poc_view")
@SideBarItem(sectionId = DashboardSections.DEVICE_MANAGEMENT, caption="Devices", order = 99)
public final class DeviceView extends Panel implements View {

   private static final Logger LOGGER = LoggerFactory.getLogger(DeviceView.class);

  @Autowired
  private ManagerHome managerHome;
  @Autowired
  private DeviceService deviceService;

   private final IMessageConveyor mc;
   private final VerticalLayout root;

   public DeviceView() {

      mc = new MessageConveyor(UI.getCurrent().getLocale());
      
      addStyleName(ValoTheme.PANEL_BORDERLESS);
      setSizeFull();
      DashboardEventBus.register(this);

      root = new VerticalLayout();
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

   NativeSelect<String> devices;

   @PostConstruct
   private void init() {

     VerticalLayout vl = new VerticalLayout();
     vl.setMargin(new MarginInfo(false, true, false, true));

     devices = new NativeSelect<>("Available devices", deviceService.findAll().stream().map(d -> d.getName()).collect(Collectors.toList()));
     devices.setEmptySelectionAllowed(false);
     vl.addComponent(devices);
     vl.setComponentAlignment(devices, Alignment.TOP_LEFT);

      devices.addValueChangeListener(event -> {
        if (vl.getComponentCount() > 1) {
          vl.removeComponent(vl.getComponent(vl.getComponentCount() - 1));
        }
        Component c = buildContent();
        vl.addComponent(c);
        vl.setExpandRatio(c, 2);
      });

      vl.setExpandRatio(devices, 1);
      root.addComponent(vl);
   }

   private Component buildContent() {

     //  UpdateObject profile = RepoDummy.getApplication("Citrix Storefront"); // broken schema
     Device profile = deviceService.findByName(devices.getSelectedItem().get());

     ProfileFormBuilder pfb = new ProfileFormBuilder(managerHome.getLocation().toPath(), profile);
     ProfileFormLayout  pfl = pfb.getContent();
     pfl.onValuesSaved(() -> {
       LOGGER.info("Saved device profile " + profile);
       try {
         deviceService.save(profile);
       } catch (Exception e) {
         pfl.setError(e.getMessage());
       }
     });

     return pfl.getContent();
   }



  @Override
   public void enter(ViewChangeEvent event) {

   }


}
