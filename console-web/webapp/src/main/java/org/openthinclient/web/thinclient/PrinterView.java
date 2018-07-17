package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.icons.VaadinIcons;
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
//     filter.setIcon(VaadinIcons.FILTER);
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
     // Profile-Type based colors
//     printersGrid.setStyleGenerator(profile -> profile.getClass().getSimpleName());
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

       Profile profile = getFreshProfile(selectedItems.get());
       ProfilePanel profilePanel = new ProfilePanel(profile.getName(), profile.getClass());
       profilePanel.onValuesWritten(ipg -> saveValues(ipg, profile));
       profilePanel.setItemGroups(builder.getOtcPropertyGroups(profile));

       if (profile instanceof Printer) {
         Set<DirectoryObject> members = ((Printer) profile).getMembers();
         showReference(profile, profilePanel, members, "Clients", clientService.findAll(), Client.class);
         showReference(profile, profilePanel, members, "Location", locationService.findAll(), Location.class);
         showReference(profile, profilePanel, members, "User", userService.findAll(), User.class);
       }
       else if (profile instanceof Application) {
         Set<DirectoryObject> members = ((Application) profile).getMembers();
         showReference(profile, profilePanel, members, "Clients", clientService.findAll(), Client.class);
         showReference(profile, profilePanel, members, "ApplicationGroups", applicationGroupService.findAll(), ApplicationGroup.class);
         showReference(profile, profilePanel, members, "User", userService.findAll(), User.class);
       }
       else if (profile instanceof HardwareType) {
         HardwareType hardwareType = (HardwareType) profile;
         Set<? extends DirectoryObject> members = hardwareType.getMembers();
         // TODO: Feature oder Bug: Hardwaretypen sind kaputt
         showReference(profile, profilePanel, members, "Clients (hinzufügen kaputt)", clientService.findAll(), Client.class);

         Map<Class, Set<? extends DirectoryObject>> associatedObjects = hardwareType.getAssociatedObjects();
         Set<? extends DirectoryObject> devices = associatedObjects.get(Device.class);
         showDeviceAssociations(hardwareType, profilePanel, devices);
       }
       else if (profile instanceof Device) {
         Device device = ((Device) profile);
         showReference(profile, profilePanel, device.getMembers(), "Clients", clientService.findAll(), Client.class);
         showReference(profile, profilePanel, device.getMembers(), "Hardwartypes", hardwareTypeService.findAll(), HardwareType.class);
       }
       else if (profile instanceof Client) {
         Client client = (Client) profile;
         Map<Class, Set<? extends DirectoryObject>> associatedObjects = client.getAssociatedObjects();
         Set<? extends DirectoryObject> devices = associatedObjects.get(Device.class);
         showDeviceAssociations(client, profilePanel, devices);

         showReference(profile, profilePanel, client.getClientGroups(), "ClientGroups", clientGroupService.findAll(), ClientGroup.class);
         showReference(profile, profilePanel, client.getApplicationGroups(), "ApplicationGroups", applicationGroupService.findAll(), ApplicationGroup.class);
         showReference(profile, profilePanel, client.getApplications(), "Application", applicationService.findAll(), Application.class);
         showReference(profile, profilePanel, client.getPrinters(), "Printers", printerService.findAll(), Printer.class);
       }
       else if (profile instanceof Location) {
         Location location = ((Location) profile);
         showReference(profile, profilePanel, location.getPrinters(), "Printers", printerService.findAll(), Printer.class);
       }

       right.addComponent(profilePanel);

     } else {
       right.removeAllComponents();
       Label emptyScreenHint = new Label(
                  VaadinIcons.SELECT.getHtml() + "&nbsp;&nbsp;&nbsp;Bitte links ein Profil auswählen<br><br>" +
                       VaadinIcons.FILTER.getHtml() +  "&nbsp;&nbsp;&nbsp;Liste der Verzeichnisobjekt filtern (bald)",
               ContentMode.HTML);
       emptyScreenHint.setStyleName("emptyScreenHint");
       right.addComponent(emptyScreenHint);
     }

  }

  /**
   * Only for testing purpose with WebConsole-Client: load profiles every time
   * @param profile
   * @param <T>
   * @return
   */
  private <T extends Profile> T getFreshProfile(T profile) {
    if (profile instanceof Printer) {
      return (T) printerService.findByName(profile.getName());
    } else if (profile instanceof Application) {
      return (T) applicationService.findByName(profile.getName());
    } else if (profile instanceof HardwareType) {
      return (T) hardwareTypeService.findByName(profile.getName());
    } else if (profile instanceof Device) {
      return (T) deviceService.findByName(profile.getName());
    } else if (profile instanceof Client) {
      return (T) clientService.findByName(profile.getName());
    } else if (profile instanceof Location) {
//      return (T) locationService.findByName(profile.getName()); // <- funtioniert nicht...
      return (T) locationService.findAll().stream().filter(location -> location.getName().equals(profile.getName())).findFirst().get();
    } else {
      throw new RuntimeException("Unsupported profile: " + profile);
    }

  }

  // show device associations
  private void showDeviceAssociations(AssociatedObjectsProvider profile, ProfilePanel profilePanel, Set<? extends DirectoryObject> members) {

    Set<Device> all = deviceService.findAll();
    List<Item> allDevices = builder.createItems(all);
    List<Item> deviceMembers = builder.createFilteredItemsFromDO(members, Device.class);
    ReferenceComponentPresenter presenter = profilePanel.addReferences("Associated Devices", allDevices, deviceMembers);
    presenter.setProfileReferenceChangedConsumer(values -> saveAssociations(profile, values, all, Device.class));
  }

  // show references
  private void showReference(Profile profile, ProfilePanel profilePanel, Set<? extends DirectoryObject> members,
                             String title, Set<? extends DirectoryObject> allObjects, Class clazz) {
    List<Item> memberItems = builder.createFilteredItemsFromDO(members, clazz);
    ReferenceComponentPresenter presenter = profilePanel.addReferences(title, builder.createItems(allObjects), memberItems);
    presenter.setProfileReferenceChangedConsumer(values -> saveReference(profile, values, allObjects, clazz));
  }

  /**
   *
   * @param profile to be changed
   * @param values the state of value to be saved
   */
  private <T extends DirectoryObject> void saveAssociations(AssociatedObjectsProvider profile, List<Item> values, Set<T> directoryObjects, Class<T> clazz) {

    Map<Class, Set<? extends DirectoryObject>> associatedObjects = profile.getAssociatedObjects();
    Set<T> association = (Set<T>) associatedObjects.get(clazz);

    List<Item> oldValues = builder.createFilteredItemsFromDO(association, clazz);
    oldValues.forEach(oldItem -> {
      if (values.contains(oldItem)) {
        LOGGER.info("Keep oldValue as member: " + oldItem);
      } else {
        LOGGER.info("Remove oldValue from members: " + oldItem);
        // get values from available-values set and remove members
        Optional<? extends DirectoryObject> directoryObject = directoryObjects.stream().filter(o -> o.getName().equals(oldItem.getName())).findFirst();
        if (directoryObject.isPresent()) {
          association.remove(directoryObject.get());
        } else {
          LOGGER.info("Device (to remove) not found for " + oldItem);
        }
      }
    });

    values.forEach(newValue -> {
      if (!oldValues.contains(newValue)) {
        LOGGER.info("Add newValue to members: " + newValue);
        // get values from available-values set and add to members
        Optional<? extends DirectoryObject> directoryObject = directoryObjects.stream().filter(o -> o.getName().equals(newValue.getName())).findFirst();
        if (directoryObject.isPresent()) {
          association.add((T) directoryObject.get());
        } else {
          LOGGER.info("DirectoryObject not found for " + newValue);
        }
      }
    });

    saveProfile((Profile) profile, null);
  }


  /**
   *
   * @param profile to be changed
   * @param values the state of value to be saved
   * @param clazz subset of member-types which has been modified
   */
  private <T extends DirectoryObject> void saveReference(Profile profile, List<Item> values, Set<T> profileAndDirectoryObjects, Class<T> clazz) {

    Set<T> members;
    if (profile instanceof Application) {
      members = (Set<T>) ((Application) profile).getMembers();

    } else if (profile instanceof Printer) {
      members = (Set<T>) ((Printer) profile).getMembers();

    } else if (profile instanceof HardwareType) {
      // TODO: nur ThinclientGruppen werden vom LDAP als 'members' behandelt, Thinclients werden ignoriert
      Set<? extends DirectoryObject> clients = ((HardwareType) profile).getMembers();
      clients.stream().forEach(o -> {
        LOGGER.info("This class should be of Type Client.class: {}" + ((DirectoryObject) o).getClass());
      });
      members = (Set<T>) clients;

    } else if (profile instanceof Device) {
      members = ((Device) profile).getMembers();

    } else if (profile instanceof Client) {
        if (clazz.equals(ClientGroup.class)) {
          members = (Set<T>) ((Client) profile).getClientGroups();
        } else if (clazz.equals(Device.class)) {
          members = (Set<T>) ((Client) profile).getDevices();
        } else if (clazz.equals(Printer.class)) {
          members = (Set<T>) ((Client) profile).getPrinters();
        } else if (clazz.equals(Application.class)) {
          members = (Set<T>) ((Client) profile).getApplications();
        } else if (clazz.equals(ApplicationGroup.class)) {
          members = (Set<T>) ((Client) profile).getApplicationGroups();
        } else {
          members = null;
        }

    } else if (profile instanceof Location) {
      members = (Set<T>) ((Location) profile).getPrinters();

    } else {
      throw new RuntimeException("Not implemented Profile-ype: " + profile);
    }

    List<Item> oldValues = builder.createFilteredItemsFromDO(members, clazz);
    oldValues.forEach(oldItem -> {
      if (values.contains(oldItem)) {
        LOGGER.info("Keep oldValue as member: " + oldItem);
      } else {
        LOGGER.info("Remove oldValue from members: " + oldItem);
        // get values from available-values set and remove members
        Optional<? extends DirectoryObject> directoryObject = profileAndDirectoryObjects.stream().filter(o -> o.getName().equals(oldItem.getName())).findFirst();
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
        Optional<? extends DirectoryObject> directoryObject = profileAndDirectoryObjects.stream().filter(o -> o.getName().equals(newValue.getName())).findFirst();
        if (directoryObject.isPresent()) {
          T dirObj = (T) directoryObject.get();
          members.add(dirObj);
        } else {
          LOGGER.info("DirectoryObject not found for " + newValue);
        }
      }
    });

    saveProfile(profile, null);
  }

  private void saveProfile(Profile profile, ItemGroupPanel panel) {
    try {
      if (profile instanceof Printer) {
        printerService.save((Printer) profile);
      } else if (profile instanceof Application) {
        applicationService.save((Application) profile);
      } else if (profile instanceof HardwareType) {
        hardwareTypeService.save((HardwareType) profile);
        LOGGER.info("Save member of HardwareType: {}", ((HardwareType) profile).getMembers());
      } else if (profile instanceof Device) {
        deviceService.save((Device) profile);
      } else if ((profile instanceof Client)) {
        clientService.save((Client) profile);
      } else if ((profile instanceof Location)) {
        locationService.save((Location) profile);
      }

      LOGGER.info("Profile saved {}", profile);
      if (panel != null) {
        panel.setInfo("Saved successfully.");
      }
    } catch (Exception e) {
      LOGGER.error("Cannot save profile", e);
      if (panel != null) {
        panel.setError(e.getMessage());
      }
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

    saveProfile(profile, itemGroupPanel);

  }

}
