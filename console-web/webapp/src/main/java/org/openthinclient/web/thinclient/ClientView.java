package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.jamierf.wol.WakeOnLan;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.BorderStyle;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.commons.lang3.StringUtils;
import org.openthinclient.api.rest.appliance.TokenManager;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.*;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.web.Audit;
import org.openthinclient.web.OTCSideBar;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.exception.ProfileNotSavedException;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.model.SelectOption;
import org.openthinclient.web.thinclient.presenter.ProfilePanelPresenter;
import org.openthinclient.web.thinclient.presenter.ReferencePanelPresenter;
import org.openthinclient.web.thinclient.property.OtcMacProperty;
import org.openthinclient.web.thinclient.property.OtcOptionProperty;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.openthinclient.web.thinclient.property.OtcTextProperty;
import org.openthinclient.web.thinclient.util.ClientIPAddressFinder;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.openthinclient.web.ui.ManagerUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;
import org.vaadin.spring.sidebar.annotation.ThemeIcon;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = ClientView.NAME, ui= ManagerUI.class)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT,  captionCode="UI_CLIENT_HEADER", order = 20)
@ThemeIcon(ClientView.ICON)
public final class ClientView extends AbstractThinclientView {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientView.class);

  public static final String NAME = "client_view";
  public static final String ICON = "icon/thinclient.svg";
  public static final ConsoleWebMessages TITLE_KEY = UI_CLIENT_HEADER;

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
  private ApplicationGroupService applicationGroupService;
  @Autowired
  private ClientGroupService clientGroupService;
  @Autowired
  private SchemaProvider schemaProvider;
  @Autowired
  private UnrecognizedClientService unrecognizedClientService;
  @Autowired
  private TokenManager tokenManager;
  @Autowired @Qualifier("deviceSideBar")
  private OTCSideBar deviceSideBar;

  private final IMessageConveyor mc;
  private ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();

  public ClientView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
   super(UI_CLIENT_HEADER, eventBus, notificationService);
   mc = new MessageConveyor(UI.getCurrent().getLocale());
  }

  @PostConstruct
  public void setup() {
    addStyleName(NAME);
    addCreateActionButton(mc.getMessage(UI_THINCLIENT_ADD_CLIENT_LABEL), ICON, NAME + "/create");
  }

  @Override
  public Client getClient(String name) {
    return clientService.findByName(name);
  }

  @Override
  public Set getAllItems() {
    try {
      long start = System.currentTimeMillis();
      Set all = clientService.findAllClientMetaData();
      LOGGER.info("GetAllItems clients took: " + (System.currentTimeMillis() - start) + "ms");
      return  all;
    } catch (Exception e) {
      LOGGER.warn("Cannot find directory-objects: " + e.getMessage());
      showError(e);
    }
    return Collections.EMPTY_SET;
  }

  @Override
  public Schema getSchema(String schemaName) {
    return schemaProvider.getSchema(Client.class, schemaName);
  }

  @Override
  public Map<String, String> getSchemaNames() {
    return Stream.of(schemaProvider.getSchemaNames(Client.class))
                 .collect(Collectors.toMap(schemaName -> schemaName, schemaName -> getSchema(schemaName).getLabel()));
  }

  @Override
  public ProfilePanel createProfilePanel (DirectoryObject directoryObject) throws BuildProfileException {

    Profile profile = (Profile) directoryObject;

    List<OtcPropertyGroup> otcPropertyGroups = builder.getOtcPropertyGroups(getSchemaNames(), profile);

    OtcPropertyGroup meta = otcPropertyGroups.get(0);

    ProfilePanel profilePanel = new ProfilePanel(profile.getName(), profile.getClass());
    ProfilePanelPresenter presenter = new ProfilePanelPresenter(this, profilePanel, profile);
    presenter.addPanelCaptionComponent(createWOLButton(profile));
    presenter.addPanelCaptionComponent(createVNCButton(profile));
    presenter.addPanelCaptionComponent(createLOGButton(profile));
    presenter.hideCopyButton();

    // replace default metadata-group with client-metadata
    otcPropertyGroups.remove(0);
    otcPropertyGroups.add(0, createClientMetadataPropertyGroup((Client) profile, presenter));

    // put to panel
    presenter.setItemGroups(otcPropertyGroups);
    presenter.onValuesWritten(profilePanel1 -> saveValues(presenter, profile));

    return profilePanel;

  }

  @Override
  public ProfileReferencesPanel createReferencesPanel(DirectoryObject item) {
    Client client = (Client) item;

    ProfileReferencesPanel referencesPanel = new ProfileReferencesPanel(item.getClass());
    ReferencePanelPresenter refPresenter = new ReferencePanelPresenter(referencesPanel);

    Set<ApplicationGroup> allApplicationGroups = applicationGroupService.findAll();
    refPresenter.showReference(client.getApplicationGroups(), mc.getMessage(UI_APPLICATIONGROUP_HEADER),
        allApplicationGroups, ApplicationGroup.class,
        values -> saveReference(item, values, allApplicationGroups, ApplicationGroup.class),
        getApplicationsForApplicationGroupFunction(client), false
    );

    Set<Application> allApplications = applicationService.findAll();
    refPresenter.showReference(client.getApplications(), mc.getMessage(UI_APPLICATION_HEADER),
                                allApplications, Application.class,
                                values -> saveReference(item, values, allApplications, Application.class));

    Map<Class, Set<? extends DirectoryObject>> associatedObjects = client.getAssociatedObjects();
    Set<? extends DirectoryObject> devices = associatedObjects.get(Device.class);
    Set<Device> allDevices = deviceService.findAll();
    refPresenter.showDeviceAssociations(allDevices, devices,
                                        values -> saveAssociations(client, values, allDevices, Device.class));

    Set<Printer> allPrinters = printerService.findAll();
    refPresenter.showReference(client.getPrinters(), mc.getMessage(UI_PRINTER_HEADER),
                                allPrinters, Printer.class,
                                values -> saveReference(item, values, allPrinters, Printer.class));

    return referencesPanel;
  }

  /**
   * Supplier for ApplicationGroup Members of given client and supplied item as ApplicationGroup
   * @param client Client which has ApplicationGroups
   * @return List of members mapped to Item-list or empty list
   */
  private Function<Item, List<Item>> getApplicationsForApplicationGroupFunction(Client client) {
    return item -> {
      Optional<ApplicationGroup> first = client.getApplicationGroups().stream().filter(ag -> ag.getName().equals(item.getName())).findFirst();
      if (first.isPresent()) {
        return first.get().getApplications().stream()
          .map(m -> new Item(m.getName(), Item.Type.APPLICATION))
          .collect(Collectors.toList());
      } else {
        return new ArrayList<>();
      }
    };
  }

  private Component createWOLButton(Profile profile) {
    Button button = new Button();
    button.setDescription(mc.getMessage(UI_PROFILE_PANEL_BUTTON_ALT_TEXT_WOL));
    button.setIcon(VaadinIcons.POWER_OFF);
    button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
    button.addStyleName(ValoTheme.BUTTON_SMALL);
    button.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    button.addClickListener(ev -> wakeOnLan((Client) profile));
    return button;
  }

  private Component createVNCButton(Profile profile) {
    Button button = new Button();
    button.setDescription(mc.getMessage(UI_PROFILE_PANEL_BUTTON_ALT_TEXT_VNC));
    button.setCaption(mc.getMessage(UI_COMMON_VNC_LABEL));
    button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
    button.addStyleName(ValoTheme.BUTTON_SMALL);
    button.addClickListener(ev -> openNoVncInNewBrowserWindow(profile.getName()));
    return button;
  }

  private Component createLOGButton(Profile profile) {
    Button button = new Button();
    button.setDescription(mc.getMessage(UI_PROFILE_PANEL_BUTTON_ALT_TEXT_CLIENTLOG));
    button.setIcon(VaadinIcons.FILE_TEXT_O);
    button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
    button.addStyleName(ValoTheme.BUTTON_SMALL);
    button.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    button.addClickListener(ev -> showClientLogs((Client) profile));
    return button;
  }

  @Override
  protected ProfilePanel createProfileMetadataPanel(Profile p) {

    Client profile = (Client) p;
    ProfilePanel profilePanel = new ProfilePanel(mc.getMessage(UI_PROFILE_PANEL_NEW_CLIENT_HEADER), profile.getClass());
    ProfilePanelPresenter presenter = new ProfilePanelPresenter(this, profilePanel, profile);
    presenter.hideCopyButton();
    presenter.hideDeleteButton();

    OtcPropertyGroup configuration = createClientMetadataPropertyGroup(profile, presenter);

    // put property-group to panel
    presenter.setItemGroups(Arrays.asList(configuration, new OtcPropertyGroup()));
    presenter.onValuesWritten(profilePanel1 -> saveValues(presenter, p));

    return profilePanel;
  }

  private OtcPropertyGroup createClientMetadataPropertyGroup(Client profile, ProfilePanelPresenter presenter) {

    OtcPropertyGroup configuration = builder.createProfileMetaDataGroup(getSchemaNames(), profile);
    // remove default validators and add custom validator to 'name'-property
    addProfileNameAlreadyExistsValidator(configuration);
    configuration.getProperty("name").ifPresent(nameProperty -> {
//      nameProperty.getConfiguration().getValidators().clear();
      nameProperty.getConfiguration().addValidator(new RegexpValidator(mc.getMessage(UI_PROFILE_THINCLIENT_NAME_REGEXP), "^[a-zA-Z0-9][a-zA-Z0-9\\-\\.]+[a-zA-Z0-9]$"));
//      nameProperty.getConfiguration().getValidators().add(new AbstractValidator<String>(mc.getMessage(UI_PROFILE_NAME_ALREADY_EXISTS)) {
//        @Override
//        public ValidationResult apply(String value, ValueContext context) {
//          DirectoryObject directoryObject = getFreshProfile(value);
//          return (nameProperty.getInitialValue() == null &&  directoryObject == null) ||  // name-property wasn't set before and no object was found
//                 (nameProperty.getInitialValue() != null && nameProperty.getInitialValue().equals(value) && directoryObject != null) || // name property not changed, and directorObject found, the profile changed case
//                 (nameProperty.getInitialValue() != null && !nameProperty.getInitialValue().equals(value) && directoryObject == null)   // property changed, but no directoryObject found, name is unique
//                 ? ValidationResult.ok() : ValidationResult.error(mc.getMessage(UI_PROFILE_NAME_ALREADY_EXISTS));
//        }
//      });
    });

    // MAC-Address
    OtcMacProperty macaddress = new OtcMacProperty(mc.getMessage(UI_THINCLIENT_MAC), mc.getMessage(UI_THINCLIENT_MAC_TIP), "macaddress", profile.getMacAddress(), null, unrecognizedClientService);
    String mac = profile.getMacAddress();
    ItemConfiguration macaddressConfiguration = new ItemConfiguration("macaddress", mac);
    macaddressConfiguration.setRequired(mac == null);
    macaddressConfiguration.addValidator(new RegexpValidator(mc.getMessage(UI_THINCLIENT_MAC_VALIDATOR_ADDRESS), "^([0-9A-Fa-f]{2}[:]){5}([0-9A-Fa-f]{2})$"));
    macaddress.setConfiguration(macaddressConfiguration);
    configuration.addProperty(macaddress);

    //IP address
    OtcTextProperty ipaddress = new OtcTextProperty(mc.getMessage(UI_THINCLIENT_IP_HOST), null, "ipaddress", profile.getIpHostNumber(), null);
    ItemConfiguration ipaddressConfiguration = new ItemConfiguration("ipaddress", profile.getIpHostNumber());
    ipaddressConfiguration.disable();
    ipaddress.setConfiguration(ipaddressConfiguration);
    configuration.addProperty(ipaddress);

    // Location
    OtcProperty locationProp = new OtcOptionProperty(mc.getMessage(UI_LOCATION_HEADER), null, "location", profile.getLocation() != null ? profile.getLocation().getDn() : null, null, locationService.findAll().stream().map(o -> new SelectOption(o.getName(), o.getDn())).collect(Collectors.toList()));
    ItemConfiguration locationConfig = new ItemConfiguration("location", profile.getLocation() != null ? profile.getLocation().getDn() : null);
    locationConfig.setRequired(true);
    locationProp.setConfiguration(locationConfig);
    configuration.addProperty(locationProp);

    // Hardwaretype
    OtcProperty hwProp = new OtcOptionProperty(mc.getMessage(UI_HWTYPE_HEADER), null, "hwtype", profile.getHardwareType() != null ? profile.getHardwareType().getDn() : null, null, hardwareTypeService.findAll().stream().map(o -> new SelectOption(o.getName(), o.getDn())).collect(Collectors.toList()));
    ItemConfiguration hwtypeConfig = new ItemConfiguration("hwtype", profile.getHardwareType() != null ? profile.getHardwareType().getDn() : null);
    hwtypeConfig.setRequired(true);
    hwProp.setConfiguration(hwtypeConfig);
    configuration.addProperty(hwProp);

    return configuration;
  }

  @Override
  /**
   * Set form-values to client
   * @param profilePanelPresenter ProfilePanelPresenter contains ItemGroupPanels with form components
   * @param client Profile to set the values
   */
  public void saveValues(ProfilePanelPresenter profilePanelPresenter, Profile profile) {

    LOGGER.info("Save values for client: " + profile);

    Client client = (Client) profile;
    profilePanelPresenter.getItemGroupPanels().forEach(itemGroupPanel -> {
      // write values back from bean to client
      itemGroupPanel.propertyComponents().stream()
          .map(propertyComponent -> (OtcProperty) propertyComponent.getBinder().getBean())
          .collect(Collectors.toList())
          .forEach(otcProperty -> {
            ItemConfiguration bean = otcProperty.getConfiguration();
            String propertyKey = otcProperty.getKey();
            String org;
            switch (propertyKey) {
              case "iphostnumber":org = client.getIpHostNumber();  break;
              case "macaddress":  org = client.getMacAddress();  break;
              case "location":    org = client.getLocation() != null ? client.getLocation().getDn() : null;  break;
              case "hwtype":      org = client.getHardwareType() != null ? client.getHardwareType().getDn() : null;  break;
              case "type":        {
                try {
                  org = client.getSchema(client.getRealm()).getName();
                } catch (Exception e) {
                  LOGGER.warn(" Cannot load schema for " + client.getName() + " to obtain original value for property 'type', using null.");
                  org = null;
                }
                break;
              }
              case "name":        org = client.getName(); break;
              case "description": org = client.getDescription(); break;
              default:            org = client.getValue(propertyKey); break;
            }

            String current;
            if (bean.getValue() == null || bean.getValue().length() == 0) {
              current = null;
            } else {
              current = bean.getValue();
            }

            if (!StringUtils.equals(org, current)) {
              if (current != null) {
                LOGGER.info(" Apply value for " + propertyKey + "=" + org + " with new value '" + current + "'");
                switch (propertyKey) {
                  case "iphostnumber": client.setIpHostNumber(current);  break;
                  case "macaddress":   client.setMacAddress(current != null ? current : "");  break;
                  case "location":     client.setLocation(locationService.findAll().stream().filter(l -> l.getDn().equals(current)).findFirst().get());  break;
                  case "hwtype":       client.setHardwareType(hardwareTypeService.findAll().stream().filter(h -> h.getDn().equals(current)).findFirst().get());  break;
                  case "type": {
                    client.setSchema(getSchema(current));
                    break;
                  }
                  case "name": client.setName(current); break;
                  case "description": client.setDescription(current); break;
                  default: client.setValue(propertyKey, current); break;
                }
              } else {
                if (propertyKey.equals("description")) {
                  LOGGER.info(" Apply null value for description");
                  client.setDescription(null);
                } else {
                  LOGGER.info(" Remove empty value for " + propertyKey);
                  client.removeValue(propertyKey);
                }
              }
            } else {
              LOGGER.info(" Unchanged " + propertyKey + "=" + org);
            }
          });
    });

    // save
    boolean success = saveProfile(client, profilePanelPresenter);
    // update view
    if (success) {
      selectItem(client);
      navigateTo(profile);
    }
  }

  @Override
  public <T extends DirectoryObject> T getFreshProfile(String name) {
    // if there are special characters in directory, quote them before search
//    String reg = "(?>[^\\w^+^\\s^-])";
//    String _name = name.replaceAll(reg, "\\\\$0");
    long start = System.currentTimeMillis();
    Client profile = clientService.findByName(name);
    LOGGER.info("GetFreshProfile for client took: " + (System.currentTimeMillis() - start) + "ms");

    // determine current IP-address
    if (profile != null && profile.getMacAddress() != null) {
      ClientIPAddressFinder.findIPAddress(profile.getMacAddress(), managerHome.getLocation()).ifPresent(profile::setIpHostNumber);
    }

    return (T) profile;
  }

  @Override
  public void save(DirectoryObject profile) throws ProfileNotSavedException {
    LOGGER.info("Save client: " + profile);
    clientService.save((Client) profile);
    Audit.logSave(profile);

    // remove MAC-address from unrecognizedClientService
    String macAddress = ((Client) profile).getMacAddress();
    Optional<UnrecognizedClient> optionalUnrecognizedClient = unrecognizedClientService.findAll().stream().filter(unrecognizedClient -> unrecognizedClient.getMacAddress().equals(macAddress)).findFirst();
    if (optionalUnrecognizedClient.isPresent()) {
      Realm realm = optionalUnrecognizedClient.get().getRealm();
      try {
        realm.getDirectory().delete(optionalUnrecognizedClient.get());
      } catch (DirectoryException e) {
        throw new ProfileNotSavedException("Cannot delete object " + profile, e);
      }
    }

  }

  private void showClientLogs(Client profile) {
    String macAddress = profile.getMacAddress();
    Path logs = managerHome.getLocation().toPath().resolve("logs").resolve("syslog.log");
    UI.getCurrent().addWindow(new FileContentWindow(logs, macAddress));
  }

  class FileContentWindow extends Window {

    public FileContentWindow(Path doc,String filter) {
      IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());

      addCloseListener(event -> {
        UI.getCurrent().removeWindow(this);
      });

      setCaption(mc.getMessage(ConsoleWebMessages.UI_FILEBROWSER_SUBWINDOW_VIEWFILE_CAPTION, doc.getFileName()));
      setHeight("400px");
      setWidth("500px");
      setModal(true);
      center();

      VerticalLayout subContent = new VerticalLayout();
      subContent.setMargin(true);
      subContent.setSizeFull();
      setContent(subContent);

      TextArea text = new TextArea();
      try {
        List<String> collect = Files.readAllLines(doc.toAbsolutePath()).stream().filter(l -> l.contains(filter)).collect(Collectors.toList());
        if (collect.size() == 0) collect.add("NoEntrysForTC" + filter);
        text.setValue(String.join("\n", collect));
      } catch (IOException e) {
        throw new RuntimeException("Cannot read file " + doc.toAbsolutePath());
      }

      text.setSizeFull();
      subContent.addComponent(text);

    }
  }

  private void wakeOnLan(Client profile) {
    try {
      String macAddress = profile.getMacAddress();
      LOGGER.info("Sending WOL packet to " + macAddress);
      WakeOnLan.wake(macAddress);
      Notification.show(mc.getMessage(ConsoleWebMessages.UI_PROFILE_WOL_SUCCESS));
    } catch(Exception ex) {
      LOGGER.error("Failed to send WOL packet", ex);
      Notification.show(mc.getMessage(ConsoleWebMessages.UI_PROFILE_WOL_ERROR), Notification.Type.ERROR_MESSAGE);
    }
  }

  private void openNoVncInNewBrowserWindow(String clientName) {
    String ipHostNumber = ((Client) getFreshProfile(clientName)).getIpHostNumber();
    // TODO: following properties should be configurable (at client)
    boolean isNoVNCConsoleEncrypted = false;
    String noVNCConsolePort = "5900";
    String noVNCConsoleAutoconnect = "true";
    String noVNCConsoleAllowfullscreen = "true";

    ExternalResource tr = new ExternalResource("/VAADIN/themes/openthinclient/novnc/vnc.html?host=" + ipHostNumber +
        "&port=" + noVNCConsolePort +
        "&encrypt=" + (isNoVNCConsoleEncrypted ? "1" : "0") +
        "&allowfullscreen=" + noVNCConsoleAllowfullscreen +
        "&autoconnect=" + noVNCConsoleAutoconnect+
        "&path=?token=" + tokenManager.createToken(VaadinRequest.getCurrent().getRemoteAddr())
    );

    Page.getCurrent().open(tr.getURL(), "_blank", 800, 600, BorderStyle.DEFAULT);
  }

  @Override
  public String getViewName() {
    return NAME;
  }

  @Override
  public ConsoleWebMessages getViewTitleKey() {
    return TITLE_KEY;
  }

  @Override
  public void selectItem(DirectoryObject directoryObject) {
    LOGGER.info("sideBar: "+ deviceSideBar);
    deviceSideBar.selectItem(NAME, directoryObject, getAllItems());
  }

}
