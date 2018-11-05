package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.*;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.model.SelectOption;
import org.openthinclient.web.thinclient.presenter.ProfilePanelPresenter;
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
import org.vaadin.spring.sidebar.annotation.ThemeIcon;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = ClientView.NAME)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT,  captionCode="UI_CLIENT_HEADER", order = 88)
@ThemeIcon("icon/logo.svg")
public final class ClientView extends ThinclientView {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientView.class);

  public static final String NAME = "client_view";

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
  @Autowired
  private SchemaProvider schemaProvider;

   private final IMessageConveyor mc;
   private ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();

   public ClientView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
     super(UI_CLIENT_HEADER, eventBus, notificationService);
     mc = new MessageConveyor(UI.getCurrent().getLocale());

     showCreateClientAction();
   }


   @PostConstruct
   private void setup() {
     setItems(getAllItems());
   }

  @Override
  public HashSet getAllItems() {
    return (HashSet) clientService.findAll();
  }

  @Override
  public Schema getSchema(String schemaName) {
    return schemaProvider.getSchema(Client.class, schemaName);
  }

  @Override
  public String[] getSchemaNames() {
    return schemaProvider.getSchemaNames(Client.class);
  }

  public ProfilePanel createProfilePanel (Profile profile) {

       ProfilePanel profilePanel = new ProfilePanel(profile.getName(), profile.getClass());
       ProfilePanelPresenter presenter = new ProfilePanelPresenter(this, profilePanel, profile);
       presenter.hideCopyButton();
       presenter.addPanelCaptionComponent(createVNCButton());
       presenter.addPanelCaptionComponent(createLOGButton());

       List<OtcPropertyGroup> otcPropertyGroups = null;
       try {
         otcPropertyGroups = builder.getOtcPropertyGroups(getSchemaNames(), profile);
       } catch (BuildProfileException e) {
         showError(e);
         return null;
       }

       // attach save-action
       otcPropertyGroups.forEach(group -> group.setValueWrittenHandlerToAll(ipg -> saveValues(ipg, profile)));

       // replace default metadata-group with client-metadata
       otcPropertyGroups.remove(0);
       otcPropertyGroups.add(0, createClientMetadataPropertyGroup((Client) profile));

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

  private Component createVNCButton() {
    Button button = new Button();
    button.setCaption("VNC");
    button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
    button.addStyleName(ValoTheme.BUTTON_SMALL);
//    button.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    return button;
  }

  private Component createLOGButton() {
    Button button = new Button();
    button.setIcon(VaadinIcons.FILE_TEXT_O);
    button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
    button.addStyleName(ValoTheme.BUTTON_SMALL);
    button.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    return button;
  }

  @Override
  protected ProfilePanel createProfileMetadataPanel(Profile p) {

    Client profile = (Client) p;

    String label;
    if (profile.getName() == null || profile.getName().length() == 0) {
      label = "Neuer Client";
    } else {
      label = profile.getName() + " bearbeiten";
    }

    ProfilePanel profilePanel = new ProfilePanel(label, profile.getClass());
    ProfilePanelPresenter presenter = new ProfilePanelPresenter(this, profilePanel, profile);
    presenter.hideCopyButton();
    presenter.hideEditButton();
    presenter.hideDeleteButton();

    OtcPropertyGroup configuration = createClientMetadataPropertyGroup(profile);

    // put property-group to panel
    profilePanel.setItemGroups(Arrays.asList(configuration, new OtcPropertyGroup(null, null)));
    presenter.expandMetaData();

    return profilePanel;
  }

  private OtcPropertyGroup createClientMetadataPropertyGroup(Client profile) {

    OtcPropertyGroup configuration = builder.createProfileMetaDataGroup(getSchemaNames(), profile);

    // MAC-Address
    OtcTextProperty macaddress = new OtcTextProperty(mc.getMessage(UI_THINCLIENT_MAC), "Format beachten xx:xx:xx:xx:xx:xx", "macaddress", profile.getMacAddress());
    ItemConfiguration macaddressConfiguration = new ItemConfiguration("macaddress", profile.getMacAddress());
    macaddressConfiguration.addValidator(new RegexpValidator("Not valid mac-address", "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"));
    macaddress.setConfiguration(macaddressConfiguration);
    configuration.addProperty(macaddress);

    // Location
    OtcProperty locationProp = new OtcOptionProperty(mc.getMessage(UI_LOCATION_HEADER), null, "location", profile.getLocation() != null ? profile.getLocation().getDn() : null, locationService.findAll().stream().map(o -> new SelectOption(o.getName(), o.getDn())).collect(Collectors.toList()));
    ItemConfiguration locationConfig = new ItemConfiguration("location", profile.getLocation() != null ? profile.getLocation().getDn() : null);
    locationConfig.setRequired(true);
    locationProp.setConfiguration(locationConfig);
    configuration.addProperty(locationProp);

    // Hardwaretype
    OtcProperty hwProp = new OtcOptionProperty(mc.getMessage(UI_HWTYPE_HEADER), null, "hwtype", profile.getHardwareType() != null ? profile.getHardwareType().getDn() : null, hardwareTypeService.findAll().stream().map(o -> new SelectOption(o.getName(), o.getDn())).collect(Collectors.toList()));
    ItemConfiguration hwtypeConfig = new ItemConfiguration("hwtype", profile.getHardwareType() != null ? profile.getHardwareType().getDn() : null);
    hwtypeConfig.setRequired(true);
    hwProp.setConfiguration(hwtypeConfig);
    configuration.addProperty(hwProp);

    // Save handler, for each property we need to call dedicated setter
    configuration.onValueWritten(ipg -> {
        ipg.propertyComponents().forEach(propertyComponent -> {
          OtcProperty bean = (OtcProperty) propertyComponent.getBinder().getBean();
          String key   = bean.getKey();
          String value = bean.getConfiguration().getValue();
          switch (key) {
            case "iphostnumber": profile.setIpHostNumber(value);  break;
            case "macaddress":   profile.setMacAddress(value != null ? value : "");  break;
            case "location":     profile.setLocation(locationService.findAll().stream().filter(l -> l.getDn().equals(value)).findFirst().get());  break;
            case "hwtype":       profile.setHardwareType(hardwareTypeService.findAll().stream().filter(h -> h.getDn().equals(value)).findFirst().get());  break;
            case "type": {
              profile.setSchema(getSchema(value));
              profile.getProperties().setName("profile");
              profile.getProperties().setDescription(value);
              break;
            }
            case "name": profile.setName(value); break;
            case "description": profile.setDescription(value); break;
          }
        });

        // save
        boolean success = saveProfile(profile, ipg);
        // update view
        if (success) {
          setItems(getAllItems()); // refresh item list
          selectItem(profile);
        }

    });
    return configuration;
  }

  /**
   * Create a special PropertyGroup for display which contains Client-related configuration properties
   * @param profile of Client
   * @return OtcPropertyGroup with properties and save handler
   */
//  private OtcPropertyGroup createClientConfigurationGroup(Client profile) {
//
//    OtcPropertyGroup configuration = new OtcPropertyGroup(mc.getMessage(UI_THINCLIENT_CONFIG));
//
//    // MAC-Address
//    configuration.addProperty(new OtcTextProperty(mc.getMessage(UI_THINCLIENT_MAC), null, "macaddress", profile.getMacAddress(), "0:0:0:0:0:0"));
//    // Location
//    OtcProperty locationProp = new OtcOptionProperty(mc.getMessage(UI_LOCATION_HEADER), null, "location", profile.getLocation() != null ? profile.getLocation().getDn() : null, locationService.findAll().stream().map(o -> new SelectOption(o.getName(), o.getDn())).collect(Collectors.toList()));
//    locationProp.setConfiguration(new ItemConfiguration("location", profile.getLocation() != null ? profile.getLocation().getDn() : null));
//    configuration.addProperty(locationProp);
//    // Hardwaretype
//    OtcProperty hwProp = new OtcOptionProperty(mc.getMessage(UI_HWTYPE_HEADER), null, "hwtype", profile.getHardwareType() != null ? profile.getHardwareType().getDn() : null, hardwareTypeService.findAll().stream().map(o -> new SelectOption(o.getName(), o.getDn())).collect(Collectors.toList()));
//    hwProp.setConfiguration(new ItemConfiguration("hwtype", profile.getHardwareType() != null ? profile.getHardwareType().getDn() : null));
//    configuration.addProperty(hwProp);
//
//    // Save handler, for each property we need to call dedicated setter
//    configuration.onValueWritten(ipg -> {
//      ipg.propertyComponents().forEach(propertyComponent -> {
//        OtcProperty bean = (OtcProperty) propertyComponent.getBinder().getBean();
//        String key   = bean.getKey();
//        String value = bean.getConfiguration().getValue();
//        switch (key) {
//          case  "iphostnumber": profile.setIpHostNumber(value);  break;
//          case  "macaddress":   profile.setMacAddress(value != null ? value : "");  break; // TODO: null-Value should be prevented by validator
//          case  "location":     profile.setLocation(locationService.findAll().stream().filter(l -> l.getDn().equals(value)).findFirst().get());  break;
//          case  "hwtype":       profile.setHardwareType(hardwareTypeService.findAll().stream().filter(h -> h.getDn().equals(value)).findFirst().get());  break;
//        }
//      });
//      saveProfile(profile, ipg);
//    });
//
//    return configuration;
//  }

  @Override
  public <T extends Profile> T getFreshProfile(String name) {
     return (T) clientService.findByName(name);
  }

  @Override
  public void save(Profile profile) {
    clientService.save((Client) profile);
  }

}
