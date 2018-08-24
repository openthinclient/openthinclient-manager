package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.UI;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.service.*;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_HWTYPE_HEADER;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_LOCATION_HEADER;

@SuppressWarnings("serial")
@SpringView(name = "hardwaretype_view")
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT,  captionCode="UI_HWTYPE_HEADER", order = 93)
public final class HardwaretypeView extends ThinclientView {

  private static final Logger LOGGER = LoggerFactory.getLogger(HardwaretypeView.class);

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
   private ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();

   public HardwaretypeView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
     super(UI_HWTYPE_HEADER, eventBus, notificationService);
     mc = new MessageConveyor(UI.getCurrent().getLocale());
   }


   @PostConstruct
   private void setup() {
      init();
      setItems((HashSet) hardwareTypeService.findAll());
   }

  public ProfilePanel createProfilePanel (Profile profile) {

       ProfilePanel profilePanel = new ProfilePanel(profile.getName(), profile.getClass());

       List<OtcPropertyGroup> otcPropertyGroups = null;
       try {
         otcPropertyGroups = builder.getOtcPropertyGroups(profile);
       } catch (BuildProfileException e) {
         showError(e);
         return null;
       }

       // attach save-action
       otcPropertyGroups.forEach(group -> group.setValueWrittenHandlerToAll(ipg -> saveValues(ipg, profile)));

       // put to panel
       profilePanel.setItemGroups(otcPropertyGroups);

       HardwareType hardwareType = (HardwareType) profile;
       Set<? extends DirectoryObject> members = hardwareType.getMembers();
       // TODO: Feature oder Bug: Hardwaretypen sind kaputt
       showReference(profile, profilePanel, members, "Clients (hinzufügen kaputt)", clientService.findAll(), Client.class);

       Map<Class, Set<? extends DirectoryObject>> associatedObjects = hardwareType.getAssociatedObjects();
       Set<? extends DirectoryObject> devices = associatedObjects.get(Device.class);
       showDeviceAssociations(deviceService.findAll(), hardwareType, profilePanel, devices);

    return profilePanel;
    }



  @Override
  public <T extends Profile> T getFreshProfile(T profile) {
     return (T) hardwareTypeService.findByName(profile.getName());
  }

  @Override
  public void save(Profile profile) {
    hardwareTypeService.save((HardwareType) profile);
  }


}
