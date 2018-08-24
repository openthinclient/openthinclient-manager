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
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.model.SelectOption;
import org.openthinclient.web.thinclient.presenter.ReferenceComponentPresenter;
import org.openthinclient.web.thinclient.property.OtcOptionProperty;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.openthinclient.web.thinclient.property.OtcTextProperty;
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
import java.util.stream.Collectors;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = "client_view")
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT,  captionCode="UI_CLIENT_HEADER", order = 88)
public final class ClientView extends ThinclientView {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientView.class);

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

   public ClientView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
     super(UI_CLIENT_HEADER, eventBus, notificationService);
     mc = new MessageConveyor(UI.getCurrent().getLocale());
   }


   @PostConstruct
   private void setup() {
      init();
      setItems((HashSet) clientService.findAll());
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
       // Add client configuration to top
       otcPropertyGroups.get(0).addGroup(0, createClientConfigurationGroup((Client) profile));

       // put to panel
       profilePanel.setItemGroups(otcPropertyGroups);

       Client client = (Client) profile;
       Map<Class, Set<? extends DirectoryObject>> associatedObjects = client.getAssociatedObjects();
       Set<? extends DirectoryObject> devices = associatedObjects.get(Device.class);
       showDeviceAssociations(deviceService.findAll(), client, profilePanel, devices);

       showReference(profile, profilePanel, client.getClientGroups(), mc.getMessage(UI_CLIENTGROUP_HEADER), clientGroupService.findAll(), ClientGroup.class);
       showReference(profile, profilePanel, client.getApplicationGroups(), mc.getMessage(UI_APPLICATIONGROUP_HEADER), applicationGroupService.findAll(), ApplicationGroup.class);
       showReference(profile, profilePanel, client.getApplications(), mc.getMessage(UI_APPLICATION_HEADER), applicationService.findAll(), Application.class);
       showReference(profile, profilePanel, client.getPrinters(), mc.getMessage(UI_PRINTER_HEADER), printerService.findAll(), Printer.class);

       return profilePanel;
  }

  /**
   * Create a special PropertyGroup for display which contains Client-related configuration properties
   * @param profile of Client
   * @return OtcPropertyGroup with properties and save handler
   */
  private OtcPropertyGroup createClientConfigurationGroup(Client profile) {

    OtcPropertyGroup configuration = new OtcPropertyGroup(mc.getMessage(UI_THINCLIENT_CONFIG));

    // IP-Address
    configuration.addProperty(new OtcTextProperty(mc.getMessage(UI_THINCLIENT_IP_HOST), "iphostnumber", profile.getIpHostNumber(), "0.0.0.0"));
    // MAC-Address
    configuration.addProperty(new OtcTextProperty(mc.getMessage(UI_THINCLIENT_MAC), "macaddress", profile.getMacAddress(), "0:0:0:0:0:0"));
    // Location
    OtcProperty locationProp = new OtcOptionProperty(mc.getMessage(UI_LOCATION_HEADER), "location", profile.getLocation().getDn(), locationService.findAll().stream().map(o -> new SelectOption(o.getName(), o.getDn())).collect(Collectors.toList()));
    locationProp.setConfiguration(new ItemConfiguration("location", profile.getLocation().getDn()));
    configuration.addProperty(locationProp);
    // Hardwaretype
    OtcProperty hwProp = new OtcOptionProperty(mc.getMessage(UI_HWTYPE_HEADER), "hwtype", profile.getHardwareType().getDn(), hardwareTypeService.findAll().stream().map(o -> new SelectOption(o.getName(), o.getDn())).collect(Collectors.toList()));
    hwProp.setConfiguration(new ItemConfiguration("hwtype", profile.getHardwareType().getDn()));
    configuration.addProperty(hwProp);

    // Save handler, for each property we need to call dedicated setter
    configuration.onValueWritten(ipg -> {
      ipg.propertyComponents().forEach(propertyComponent -> {
        OtcProperty bean = (OtcProperty) propertyComponent.getBinder().getBean();
        String key   = bean.getKey();
        String value = bean.getConfiguration().getValue();
        switch (key) {
          case  "iphostnumber": profile.setIpHostNumber(value);  break;
          case  "macaddress":   profile.setMacAddress(value);  break;
          case  "location":     profile.setLocation(locationService.findAll().stream().filter(l -> l.getDn().equals(value)).findFirst().get());  break;
          case  "hwtype":       profile.setHardwareType(hardwareTypeService.findAll().stream().filter(h -> h.getDn().equals(value)).findFirst().get());  break;
        }
      });
      saveProfile(profile, ipg);
    });

    return configuration;
  }

  @Override
  public <T extends Profile> T getFreshProfile(T profile) {
     return (T) clientService.findByName(profile.getName());
  }

  @Override
  public void save(Profile profile) {
    clientService.save((Client) profile);
  }


}
