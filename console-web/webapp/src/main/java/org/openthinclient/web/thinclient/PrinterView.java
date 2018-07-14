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
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.service.*;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.thinclient.component.ItemGroupPanel;
import org.openthinclient.web.thinclient.component.ReferencePanel;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.openthinclient.web.view.DashboardSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
  @Autowired
  private ClientService clientService;
  @Autowired
  private LocationService locationService;
  @Autowired
  private UserService userService;

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
     items.addAll(clientService.findAll());
     items.addAll(locationService.findAll());

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

       if (profile instanceof Printer) {
         showClientReferences(profile, profilePanel, ((Printer) profile).getMembers());
         showLocationReferences(profile, profilePanel, ((Printer) profile).getMembers());
         showUserReferences(profile, profilePanel, ((Printer) profile).getMembers());
       }
       if (profile instanceof Application) {
         showClientReferences(profile, profilePanel, ((Application) profile).getMembers());
         showApplicationGroupReferences(profile, profilePanel, ((Application) profile).getMembers());
         showUserReferences(profile, profilePanel, ((Application) profile).getMembers());
       }

       right.addComponent(profilePanel);

     } else {
       right.removeAllComponents();
       right.addComponent(new Label("<span style=\"margin:50, 20, 0, 0px;\">Bitte ein Profil auswählen</span>", ContentMode.HTML));
     }

  }

  private void showUserReferences(Profile profile, ProfilePanel profilePanel, Set<DirectoryObject> members) {
    // show user references
    List<Item> users      = builder.createFilteredItemsFromDO(members, User.class);
    List<Item> userGroups = builder.createFilteredItemsFromDO(members, UserGroup.class);
    users.addAll(userGroups);
    profilePanel.addReferences("User",
            builder.createItemsFromDO(userService.findAll()),
            users);
    profilePanel.onProfileReferenceChanged(rpp -> saveReference(rpp, profile));
  }

  private void showApplicationReferences(Profile profile, ProfilePanel profilePanel, Set<DirectoryObject> members) {
    // show application references
    List<Item> applications      = builder.createFilteredItems(members, Application.class);
    List<Item> applicationGroups = builder.createFilteredItemsFromDO(members, ApplicationGroup.class);
    applications.addAll(applicationGroups);
    profilePanel.addReferences("Application",
                               builder.createItems(applicationService.findAll()),
                               applications);
    profilePanel.onProfileReferenceChanged(rpp -> saveReference(rpp, profile));
  }

  private void showLocationReferences(Profile profile, ProfilePanel profilePanel, Set<DirectoryObject> members) {
    // show application references
    List<Item> locations = builder.createFilteredItems(members, Location.class);
    profilePanel.addReferences("Locations", builder.createItems(locationService.findAll()), locations);
    profilePanel.onProfileReferenceChanged(rpp -> saveReference(rpp, profile));
  }

  /**
   * show application references
   * @param profile
   * @param profilePanel
   * @param members
   * TODO: ggf unterschiedliche change/save behandlung
   */
  private void showApplicationGroupReferences(Profile profile, ProfilePanel profilePanel, Set<DirectoryObject> members) {
    //
    List<Item> applicationGroups = builder.createFilteredItemsFromDO(members, ApplicationGroup.class);
    profilePanel.addReferences("ApplicationGroups", builder.createItems(applicationService.findAll()), applicationGroups);
    profilePanel.onProfileReferenceChanged(rpp -> saveReference(rpp, profile));
  }

  private void showClientReferences(Profile profile, ProfilePanel profilePanel, Set<DirectoryObject> members) {
    // show client references

    List<Item> clients      = builder.createFilteredItems(members, Client.class);
    List<Item> clientGroups = builder.createFilteredItemsFromDO(members, ClientGroup.class);
    clients.addAll(clientGroups);
    profilePanel.addReferences("Clients", builder.createItems(clientService.findAll()), clients);
    profilePanel.onProfileReferenceChanged(rpp -> saveReference(rpp, profile));
  }

  private void saveReference(ReferencePanel rpp, Profile profile) {

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
