package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Responsive;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Component;
import com.vaadin.ui.NativeSelect;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

@SuppressWarnings("serial")
@SpringView(name = "application_poc_view")
@SideBarItem(sectionId = DashboardSections.DEVICE_MANAGEMENT, caption="Application", order = 90)
public final class ApplicationView extends Panel implements View {

   private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationView.class);

  @Autowired
  private ManagerHome managerHome;
  @Autowired
  private ApplicationService applicationService;

   private final IMessageConveyor mc;
   private final VerticalLayout root;

   public ApplicationView() {

      mc = new MessageConveyor(UI.getCurrent().getLocale());
      
      addStyleName(ValoTheme.PANEL_BORDERLESS);
      setSizeFull();
      DashboardEventBus.register(this);

      root = new VerticalLayout();
      root.setMargin(false);
      root.addStyleName("dashboard-view");
      setContent(root);
      Responsive.makeResponsive(root);

      root.addComponent(new ViewHeader("Application"));

   }

   @Override
   public String getCaption() {
      return "Application";
   }

   NativeSelect<String> application;

   @PostConstruct
   private void init() {

     VerticalLayout vl = new VerticalLayout();
     vl.setMargin(new MarginInfo(false, true, false, true));

     application = new NativeSelect<>("Available Application", applicationService.findAll().stream().map(a -> a.getName()).collect(Collectors.toList()));
     application.setEmptySelectionAllowed(false);
     vl.addComponent(application);

      application.addValueChangeListener(event -> {
        if (vl.getComponentCount() > 1) {
          vl.removeComponent(vl.getComponent(vl.getComponentCount() - 1));
        }
        Component c = buildContent();
        vl.addComponent(c);
        vl.setExpandRatio(c, 2);
      });

      root.addComponent(vl);
   }

   private Component buildContent() {

     Application profile =  applicationService.findByName(application.getSelectedItem().get());

     ProfilePropertiesBuilder pfb = new ProfilePropertiesBuilder();
     ProfileFormLayout  pfl = pfb.getContent(profile);
     pfl.onValuesSaved(() -> {
       LOGGER.info("Saved application profile " + profile);
       try {
         applicationService.save(profile);
         pfl.setInfo("Saved successfully.");
       } catch (Exception e) {
         LOGGER.error("Cannot save profile", e);
         pfl.setError(e.getMessage());
       }
     });

     return pfl.getContent();
   }



  @Override
   public void enter(ViewChangeEvent event) {

   }


}
