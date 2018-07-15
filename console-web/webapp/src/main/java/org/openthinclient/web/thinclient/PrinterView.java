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
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.service.*;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.thinclient.component.ItemGroupPanel;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.presenter.ReferenceComponentPresenter;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.openthinclient.web.view.DashboardSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
  @Autowired
  private UserGroupService userGroupService;
  @Autowired
  private ApplicationGroupService applicationGroupService;
  @Autowired
  private ClientGroupService clientGroupService;

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
         Set<DirectoryObject> members = ((Printer) profile).getMembers();
         showClientReferences(profile, profilePanel, members);
         showLocationReferences(profile, profilePanel, members);
         showUserReferences(profile, profilePanel, members);
       }
       if (profile instanceof Application) {
         Set<DirectoryObject> members = ((Application) profile).getMembers();
         showClientReferences(profile, profilePanel, members);
         showApplicationGroupReferences(profile, profilePanel, members);
         showUserReferences(profile, profilePanel, members);
       }
//       if (profile instanceof HardwareType) {
//         Set<? extends DirectoryObject> members = ((HardwareType) profile).getMembers();
//         showHardwareReferences(profile, profilePanel, members);
//       }

       right.addComponent(profilePanel);

     } else {
       right.removeAllComponents();
       right.addComponent(new Label("<span style=\"margin:50, 20, 0, 0px;\">Bitte ein Profil auswählen</span>", ContentMode.HTML));
     }

  }

  // show user references
  private void showUserReferences(Profile profile, ProfilePanel profilePanel, Set<DirectoryObject> members) {


    Set<User> allUser = userService.findAll();
    Set<UserGroup> allGrps = userGroupService.findAll();

    List<Item> userAndGroups = builder.createFilteredItemsFromDO(members, User.class, UserGroup.class);
    List<Item> allUserGroups = builder.createItems(allUser, allGrps);

    ReferenceComponentPresenter presenter = profilePanel.addReferences("User and Groups", allUserGroups, userAndGroups);
    presenter.setProfileReferenceChangedConsumer(rpp -> saveReference(profile, rpp, Stream.concat(allUser.stream(), allGrps.stream()), User.class, UserGroup.class));
  }

  // show application references
  private void showApplicationReferences(Profile profile, ProfilePanel profilePanel, Set<DirectoryObject> members) {
    Set<Application> allApplications = applicationService.findAll();
    Set<ApplicationGroup> allApplicationGrps = applicationGroupService.findAll();

    List<Item> applicationAndGroups = builder.createFilteredItemsFromDO(members, Application.class, ApplicationGroup.class);
    List<Item> allApplicationGroups = builder.createItems(allApplications, allApplicationGrps);

    ReferenceComponentPresenter presenter = profilePanel.addReferences("Application and Groups", allApplicationGroups, applicationAndGroups);
    presenter.setProfileReferenceChangedConsumer(rpp -> saveReference(profile, rpp, Stream.concat(allApplications.stream(), allApplicationGrps.stream()), Application.class, ApplicationGroup.class));
  }

  // show application references
  private void showLocationReferences(Profile profile, ProfilePanel profilePanel, Set<DirectoryObject> members) {
    Set<Location> allLocations = locationService.findAll();
    List<Item> locations = builder.createFilteredItemsFromDO(members, Location.class);
    ReferenceComponentPresenter presenter = profilePanel.addReferences("Locations", builder.createItems(allLocations), locations);
    presenter.setProfileReferenceChangedConsumer(rpp -> saveReference(profile, rpp, allLocations.stream(), Location.class));
  }

  /**
   * show application references
   * @param profile
   * @param profilePanel
   * @param members
   * TODO: ggf unterschiedliche change/save behandlung
   */
  private void showApplicationGroupReferences(Profile profile, ProfilePanel profilePanel, Set<DirectoryObject> members) {
    Set<ApplicationGroup> allApplication = applicationGroupService.findAll();
    List<Item> applicationGroups = builder.createFilteredItemsFromDO(members, ApplicationGroup.class);
    ReferenceComponentPresenter presenter = profilePanel.addReferences("ApplicationGroups", builder.createItems(allApplication), applicationGroups);
    presenter.setProfileReferenceChangedConsumer(rpp -> saveReference(profile, rpp, allApplication.stream(), ApplicationGroup.class));
  }

  private void showClientReferences(Profile profile, ProfilePanel profilePanel, Set<DirectoryObject> members) {
    // show client references
    Set<Client> allTC = clientService.findAll();
    Set<ClientGroup> allTCGrps = clientGroupService.findAll();

    List<Item> clientAndGroups     = builder.createFilteredItemsFromDO(members, Client.class, ClientGroup.class);
    List<Item> allClientsAndGroups = builder.createItems(allTC, allTCGrps);
    ReferenceComponentPresenter presenter = profilePanel.addReferences("Thinclients and Groups", allClientsAndGroups, clientAndGroups);
    presenter.setProfileReferenceChangedConsumer(rpp -> saveReference(profile, rpp, Stream.concat(allTC.stream(), allTCGrps.stream()), Client.class, ClientGroup.class));
  }

  /**
   *
   * @param profile to be changed
   * @param values the state of value to be saved
   * @param clazz subset of member-types which has been modified
   */
  private void saveReference(Profile profile, List<Item> values, Stream<? extends DirectoryObject> profileAndDirectoryObjects, Class<?>... clazz) {

    Set<DirectoryObject> members;
    if (profile instanceof Application) {
      members = ((Application) profile).getMembers();
    } else if (profile instanceof Printer) {
      members = ((Printer) profile).getMembers();
    } else {
      throw new RuntimeException("Not implemented Profile-ype");
    }

    List<Item> oldValues = builder.createFilteredItemsFromDO(members, clazz);
    oldValues.forEach(oldItem -> {
      if (values.contains(oldItem)) {
        LOGGER.info("Keep oldValue as member: " + oldItem);
      } else {
        LOGGER.info("Remove oldValue from members: " + oldItem);
        // get values from available-values set and remove members
        Optional<? extends DirectoryObject> directoryObject = profileAndDirectoryObjects.filter(o -> o.getName().equals(oldItem.getName())).findFirst();
        if (directoryObject.isPresent()) {
          members.remove(directoryObject.get());
        } else {
          LOGGER.info("DirectoryObject (to remove) not found for " + oldItem);
        }
      }
    });

    values.forEach(newValue -> {
      if (!oldValues.contains(newValue)) {
        LOGGER.info("Add newValue to members: " + newValue);
        // get values from available-values set and add to members
        Optional<? extends DirectoryObject> directoryObject = profileAndDirectoryObjects.filter(o -> o.getName().equals(newValue.getName())).findFirst();
        if (directoryObject.isPresent()) {
          members.add(directoryObject.get());
        } else {
          LOGGER.info("DirectoryObject not found for " + newValue);
        }
      }
    });

    try {
      if (profile instanceof Printer) {
        printerService.save((Printer) profile);
      } else if (profile instanceof Application) {
        applicationService.save((Application) profile);
      }
//      itemGroupPanel.setInfo("Saved successfully.");
    } catch (Exception e) {
      LOGGER.error("Cannot save profile", e);
//      itemGroupPanel.setError(e.getMessage());
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
