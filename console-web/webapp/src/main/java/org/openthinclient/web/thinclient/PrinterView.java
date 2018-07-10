package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.navigator.View;
import com.vaadin.server.Responsive;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.commons.lang3.StringUtils;
import org.openthinclient.common.model.Application;
import org.openthinclient.common.model.Printer;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.DeviceService;
import org.openthinclient.common.model.service.HardwareTypeService;
import org.openthinclient.common.model.service.PrinterService;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.thinclient.component.ItemGroupPanel;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.openthinclient.web.view.DashboardSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
@SpringView(name = "printer_view")
@SideBarItem(sectionId = DashboardSections.DEVICE_MANAGEMENT, caption="Printer", order = 90)
public final class PrinterView extends Panel implements View {

  private static final Logger LOGGER = LoggerFactory.getLogger(PrinterView.class);

  @Autowired
  private ManagerHome managerHome;
  @Autowired
  private PrinterService printerService;
  @Autowired
  private ApplicationService applicationService;
  @Autowired
  private DeviceService deviceService;
  @Autowired
  private HardwareTypeService hardwareTypeService;

   private final IMessageConveyor mc;
   private VerticalLayout right;
   private ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();

   public PrinterView() {

      mc = new MessageConveyor(UI.getCurrent().getLocale());
      
      addStyleName(ValoTheme.PANEL_BORDERLESS);
      setSizeFull();
      DashboardEventBus.register(this);

   }

   @Override
   public String getCaption() {
      return "Printer";
   }

   NativeSelect<String> application;

   @PostConstruct
   private void init() {

     HorizontalSplitPanel main = new HorizontalSplitPanel();
     main.setSizeFull();
     main.setSplitPosition(250, Unit.PIXELS);

     // left selection grid
     VerticalLayout left = new VerticalLayout();
     left.setMargin(new MarginInfo(false, false, false, false));
     left.addStyleName("profileItemSelectionBar");
     left.setHeight(100, Unit.PERCENTAGE);
     main.setFirstComponent(left);

     TextField filter = new TextField();
     filter.addStyleNames("profileItemFilter");
     filter.setPlaceholder("Filter");
     left.addComponent(filter);

     Grid<Profile> printersGrid = new Grid<>();
     printersGrid.addStyleNames("profileSelectionGrid");

     // items
     ArrayList<Profile> items = new ArrayList<>(printerService.findAll());
     items.addAll(applicationService.findAll());
     items.addAll(deviceService.findAll());
     items.addAll(hardwareTypeService.findAll());

     printersGrid.setDataProvider(DataProvider.ofCollection(items));
     printersGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
     printersGrid.addColumn(Profile::getName);
     printersGrid.addSelectionListener(selectionEvent -> showContent(selectionEvent.getFirstSelectedItem()));
     printersGrid.setSizeFull();
     printersGrid.removeHeaderRow(0);
     left.addComponent(printersGrid);
     left.setExpandRatio(printersGrid, 1);

     // right main content
     right = new VerticalLayout();
     right.setMargin(new MarginInfo(false, false, false, false));
     main.setSecondComponent(right);
     showContent(Optional.empty());

     setContent(main);
     Responsive.makeResponsive(main);

   }

  private void showContent(Optional<Profile> selectedItems) {

     if (selectedItems.isPresent()) {
       right.removeAllComponents();

       Profile profile = selectedItems.get();
       ProfilePanel profilePanel = new ProfilePanel(profile.getName(), profile.getClass());
       profilePanel.onValuesWritten(ipg -> saveValues(ipg, profile));
       profilePanel.setItemGroups(builder.getOtcPropertyGroups(profile));
       right.addComponent(profilePanel);

     } else {
       right.removeAllComponents();
       right.addComponent(new Label("<span style=\"margin:50, 20, 0, 0px;\">Bitte ein Profil auswählen</span>", ContentMode.HTML));
     }

  }

  private void saveValues(ItemGroupPanel itemGroupPanel, Profile profile) {

    LOGGER.info("Save profile: " + profile);

    // write values back from bean to profile
    itemGroupPanel.propertyComponents().stream()
            .map(propertyComponent -> (OtcProperty) propertyComponent.getBinder().getBean())
            .collect(Collectors.toList())
            .forEach(otcProperty -> {
              ItemConfiguration bean = otcProperty.getConfiguration();
              String org = profile.getValue(bean.getKey());
              String current = bean.getValue();
              if (current != null && !StringUtils.equals(org, current)) {
                LOGGER.info("Apply value for " + bean.getKey() + "=" + org + " with new value '" + current + "'");
                profile.setValue(bean.getKey(), bean.getValue());
              } else {
                LOGGER.info("Unchanged " + bean.getKey() + "=" + org);
              }
    });

    try {
      if (profile instanceof Printer) {
        printerService.save((Printer) profile);
      } else if (profile instanceof Application) {
        applicationService.save((Application) profile);
      }
      itemGroupPanel.setInfo("Saved successfully.");
    } catch (Exception e) {
      LOGGER.error("Cannot save profile", e);
      itemGroupPanel.setError(e.getMessage());
    }

  }

}
