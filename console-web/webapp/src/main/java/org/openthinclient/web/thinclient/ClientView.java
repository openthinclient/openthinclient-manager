package org.openthinclient.web.thinclient;

import com.jamierf.wol.WakeOnLan;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.BorderStyle;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.service.*;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.web.Audit;
import org.openthinclient.web.component.Popup;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.exception.ProfileNotDeletedException;
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
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.openthinclient.web.ui.ManagerUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = ClientView.NAME, ui= ManagerUI.class)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT,  captionCode="UI_CLIENT_HEADER", order = 20)
@ThemeIcon(ClientView.ICON)
public final class ClientView extends AbstractProfileView<Client> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientView.class);

  public static final String NAME = "client_view";
  public static final String ICON = "icon/thinclient.svg";
  public static final ConsoleWebMessages TITLE_KEY = UI_CLIENT_HEADER;

  private EventBus.SessionEventBus eventBus;

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
  private UnrecognizedClientService unrecognizedClientService;

  private ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();

  public ClientView(EventBus.SessionEventBus eventBus) {
    super();
    this.eventBus = eventBus;
  }

  @PostConstruct
  public void setup() {
    addStyleName(NAME);
  }

  @Override
  public Set<ClientMetaData> getAllItems() {
    try {
      long start = System.currentTimeMillis();
      Set<ClientMetaData> all = clientService.findAllClientMetaData();
      LOGGER.debug("GetAllItems clients took: " + (System.currentTimeMillis() - start) + "ms");
      return all;
    } catch (Exception e) {
      LOGGER.warn("Cannot find directory-objects: " + e.getMessage());
      showError(e);
    }
    return Collections.emptySet();
  }

  @Override
  protected Class<Client> getItemClass() {
    return Client.class;
  }

  @Override
  public ProfilePanel createProfilePanel (Client profile) throws BuildProfileException {
    List<OtcPropertyGroup> otcPropertyGroups = builder.getOtcPropertyGroups(getSchemaNames(), profile);

    ProfilePanel profilePanel = new ProfilePanel(profile.getName(), profile.getClass());
    ProfilePanelPresenter presenter = new ProfilePanelPresenter(this, profilePanel, profile);
    if(!"00:00:00:00:00:00".equals(((Client)profile).getMacAddress())){
      presenter.addPanelCaptionComponent(createWOLButton(profile));
      presenter.addPanelCaptionComponent(createVNCButton(profile));
      presenter.addPanelCaptionComponent(createLOGButton(profile));
    }
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
  public ProfileReferencesPanel createReferencesPanel(Client client) {
    ProfileReferencesPanel referencesPanel = new ProfileReferencesPanel(Client.class);
    ReferencePanelPresenter refPresenter = new ReferencePanelPresenter(referencesPanel);

    HardwareType hwtype = client.getHardwareType();
    refPresenter.showReference(Collections.singleton(hwtype),
                                mc.getMessage(UI_HWTYPE));

    Location location = client.getLocation();
    refPresenter.showReference(Collections.singleton(location),
                                mc.getMessage(UI_LOCATION));

    Set<Application> allApplications = applicationService.findAll();
    refPresenter.showReference(client.getApplications(),
                                mc.getMessage(UI_APPLICATION_HEADER),
                                allApplications,
                                values -> saveReference(client, values, allApplications, Application.class));

    Set<ApplicationGroup> allApplicationGroups = applicationGroupService.findAll();
    refPresenter.showReference(client.getApplicationGroups(),
                                mc.getMessage(UI_APPLICATIONGROUP_HEADER),
                                allApplicationGroups,
                                values -> saveReference(client, values, allApplicationGroups, ApplicationGroup.class),
                                getApplicationsForApplicationGroupFunction(client));

    Set<? extends DirectoryObject> devices = client.getDevices();
    Set<Device> allDevices = deviceService.findAll();
    refPresenter.showReference(devices,
                                mc.getMessage(UI_DEVICE_HEADER),
                                allDevices,
                                values -> saveReference(client, values, allDevices, Device.class));


    Set<Device> hwtypeDevices = hwtype.getDevices();
    if (hwtypeDevices.size() > 0) {
      refPresenter.showReferenceAddendum(hwtypeDevices,
                                          mc.getMessage(UI_FROM_HWTYPE_HEADER));
    }

    Set<Printer> allPrinters = printerService.findAll();
    refPresenter.showReference(client.getPrinters(), mc.getMessage(UI_PRINTER_HEADER),
                                allPrinters,
                                values -> saveReference(client, values, allPrinters, Printer.class));

    Set<Printer> locationPrinters = location.getPrinters();
    if (locationPrinters.size() > 0) {
      refPresenter.showReferenceAddendum(locationPrinters,
                                          mc.getMessage(UI_FROM_LOCATION_HEADER));
    }

    return referencesPanel;
  }

  /**
   * Supplier for ApplicationGroup Members of given client and supplied item as ApplicationGroup
   * @param client Client which has ApplicationGroups
   * @return List of members mapped to Item-list or empty list
   */
  private Function<Item, List<Item>> getApplicationsForApplicationGroupFunction(Client client) {
    return item -> {
      Optional<ApplicationGroup> first = client.getApplicationGroups().stream()
                                            .filter(ag -> ag.getName().equals(item.getName()))
                                            .findFirst();
      if (first.isPresent()) {
        return first.get().getApplications().stream()
          .map(m -> new Item(m.getName(), Item.Type.APPLICATION))
          .collect(Collectors.toList());
      } else {
        return new ArrayList<>();
      }
    };
  }

  private Component createWOLButton(Client profile) {
    Button button = new Button();
    button.setDescription(mc.getMessage(UI_PROFILE_PANEL_BUTTON_ALT_TEXT_WOL));
    button.setIcon(VaadinIcons.POWER_OFF);
    button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
    button.addStyleName(ValoTheme.BUTTON_SMALL);
    button.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    button.addClickListener(ev -> wakeOnLan((Client) profile));
    return button;
  }

  private Component createVNCButton(Client profile) {
    Button button = new Button();
    button.setDescription(mc.getMessage(UI_PROFILE_PANEL_BUTTON_ALT_TEXT_VNC));
    button.setCaption(mc.getMessage(UI_COMMON_VNC_LABEL));
    button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
    button.addStyleName(ValoTheme.BUTTON_SMALL);
    button.addClickListener(ev -> openNoVncInNewBrowserWindow(profile.getName()));
    return button;
  }

  private Component createLOGButton(Client profile) {
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
  protected ProfilePanel createProfileMetadataPanel(Client p) {

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
      nameProperty.getConfiguration().addValidator(new RegexpValidator(mc.getMessage(UI_PROFILE_THINCLIENT_NAME_REGEXP), "^[a-zA-Z0-9][a-zA-Z0-9\\-\\.]+[a-zA-Z0-9]$"));
    });

    String mac = profile.getMacAddress();

    // MAC-Address
    OtcMacProperty macaddress = new OtcMacProperty(mc.getMessage(UI_THINCLIENT_MAC), mc.getMessage(UI_THINCLIENT_MAC_TIP), "macaddress", profile.getMacAddress(), null, unrecognizedClientService);
    ItemConfiguration macaddressConfiguration = new ItemConfiguration("macaddress", mac);
    macaddressConfiguration.setRequired(mac == null);
    macaddressConfiguration.addValidator(new RegexpValidator(mc.getMessage(UI_THINCLIENT_MAC_VALIDATOR_ADDRESS), "^\\s*([0-9A-Fa-f]{2}[:]){5}([0-9A-Fa-f]{2})\\s*$"));
    macaddressConfiguration.addValidator(new Validator<String>() {
      public ValidationResult apply(String value, ValueContext context) {
        if(value != null && !value.equalsIgnoreCase(mac)) {
          Optional<Client> client = clientService.findByHwAddress(value.trim()).stream().findFirst();
          if(client.isPresent()) {
            return ValidationResult.error(mc.getMessage(UI_MAC_ADDRESS_ALREADY_EXISTS, client.get().getName()));
          }
        }
        return ValidationResult.ok();
      }
    });
    macaddress.setConfiguration(macaddressConfiguration);
    configuration.addProperty(macaddress);

    //IP address
    if(!"00:00:00:00:00:00".equals(mac)) {
      OtcTextProperty ipaddress = new OtcTextProperty(mc.getMessage(UI_THINCLIENT_IP_HOST), null, "ipaddress", profile.getIpHostNumber(), null);
      ItemConfiguration ipaddressConfiguration = new ItemConfiguration("ipaddress", profile.getIpHostNumber());
      ipaddressConfiguration.disable();
      ipaddress.setConfiguration(ipaddressConfiguration);
      configuration.addProperty(ipaddress);
    }

    // Location
    OtcProperty locationProp = new OtcOptionProperty(mc.getMessage(UI_LOCATION), null, "location", profile.getLocation() != null ? profile.getLocation().getDn() : null, null, locationService.findAll().stream().map(o -> new SelectOption(o.getName(), o.getDn())).collect(Collectors.toList()));
    ItemConfiguration locationConfig = new ItemConfiguration("location", profile.getLocation() != null ? profile.getLocation().getDn() : null);
    locationConfig.setRequired(true);
    locationProp.setConfiguration(locationConfig);
    configuration.addProperty(locationProp);

    // Hardwaretype
    OtcProperty hwProp = new OtcOptionProperty(mc.getMessage(UI_HWTYPE), null, "hwtype", profile.getHardwareType() != null ? profile.getHardwareType().getDn() : null, null, hardwareTypeService.findAll().stream().map(o -> new SelectOption(o.getName(), o.getDn())).collect(Collectors.toList()));
    ItemConfiguration hwtypeConfig = new ItemConfiguration("hwtype", profile.getHardwareType() != null ? profile.getHardwareType().getDn() : null);
    hwtypeConfig.setRequired(true);
    hwProp.setConfiguration(hwtypeConfig);
    configuration.addProperty(hwProp);

    return configuration;
  }

  /**
   * Set form-values to client
   * @param profilePanelPresenter ProfilePanelPresenter contains ItemGroupPanels with form components
   * @param client Profile to set the values
   */
  @Override
  public void saveValues(ProfilePanelPresenter profilePanelPresenter, Client profile) {

    LOGGER.debug("Save values for client: " + profile);

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
                LOGGER.debug(" Apply value for " + propertyKey + "=" + org + " with new value '" + current + "'");
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
                  LOGGER.debug(" Apply null value for description");
                  client.setDescription(null);
                } else {
                  LOGGER.debug(" Remove empty value for " + propertyKey);
                  client.removeValue(propertyKey);
                }
              }
            } else {
              LOGGER.debug(" Unchanged " + propertyKey + "=" + org);
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
  protected Client newProfile() {
    return new Client();
  }

  @Override
  public Client getFreshProfile(String name) {
    long start = System.currentTimeMillis();
    Client profile = clientService.findByName(name);
    LOGGER.debug("GetFreshProfile for client took: " + (System.currentTimeMillis() - start) + "ms");

    return profile;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected <D extends DirectoryObject> Set<D> getMembers(Client profile, Class<D> clazz) {
    if (clazz == ClientGroup.class) {
      return (Set<D>)profile.getClientGroups();
    } else if (clazz == Device.class) {
      return (Set<D>)profile.getDevices();
    } else if (clazz == Printer.class) {
      return (Set<D>)profile.getPrinters();
    } else if (clazz == Application.class) {
      return (Set<D>)profile.getApplications();
    } else if (clazz == ApplicationGroup.class) {
      return (Set<D>)profile.getApplicationGroups();
    } else {
      return Collections.emptySet();
    }
  }

  @Override
  public void save(Client profile) throws ProfileNotSavedException {
    LOGGER.info("Save client: " + profile);
    clientService.save(profile);
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

    eventBus.publish(this, new DashboardEvent.ClientCountChangeEvent());

  }

  @Override
  public void delete(Client profile) throws ProfileNotDeletedException {
    super.delete(profile);
    eventBus.publish(this, new DashboardEvent.ClientCountChangeEvent());
  }

  private void showClientLogs(Client profile) {
    Path logs = managerHome.getLocation().toPath().resolve("logs").resolve("syslog.log");
    (new FileContentWindow(logs, profile.getName(), profile.getMacAddress())).open();
  }

  class FileContentWindow extends Popup {

    public FileContentWindow(Path doc, String name, String macAddress) {
      super(mc.getMessage(ConsoleWebMessages.UI_THINCLIENT_LOG_CAPTION, name, macAddress), "logview");

      setWidth("642px");
      setMaximized(true);

      List<String> lines = new ArrayList<>();
      try {
        List<String> srcLines = Files.readAllLines(doc.toAbsolutePath());
        ListIterator<String> lineIter = srcLines.listIterator(srcLines.size());
        int linesLeft = 2048;
        while (lineIter.hasPrevious() && linesLeft > 0) {
          String[] parts = StringEscapeUtils.escapeHtml(lineIter.previous()).split("(?! +)(?<= )", 6);
          if(parts.length < 6) {
            // Malformed lines (e.g. trailing lines of a well-formed log entry) are ignored since we
            // can't easily attribute them to a specific client.
            continue;
          }
          if (parts[3].startsWith(macAddress)) {
            linesLeft--;
            StringBuilder line = new StringBuilder();
            line.append(String.format("<div class=\"logline %s\">", parts[2].trim())).append(parts[0]).append(parts[1])
                .append(parts[2]).append(parts[5]).append("</div>");
            lines.add(0, line.toString());
          }
        }
        if (lines.size() != 0) {
          addContent(new Label(String.join("\n", lines), ContentMode.HTML));
        } else {
          setMessage(ConsoleWebMessages.UI_THINCLIENT_LOG_EMPTY);
        }
      } catch (IOException ex) {
        setMessage(ConsoleWebMessages.UI_THINCLIENT_LOG_ERROR);
        LOGGER.error("Cannot read file " + doc.toAbsolutePath(), ex);
      }
    }

    private void setMessage(ConsoleWebMessages messageKey, Object... args) {
      Label messageLabel = new Label(mc.getMessage(messageKey, args));
      messageLabel.addStyleName("message");
      addContent(messageLabel);
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
    boolean isNoVNCConsoleEncrypted = false;
    String noVNCConsolePort = "5900";
    String noVNCConsoleAutoconnect = "true";
    String noVNCConsoleAllowfullscreen = "true";

    ExternalResource tr = new ExternalResource("/VAADIN/themes/openthinclient/novnc/vnc.html?host=" + ipHostNumber +
        "&port=" + noVNCConsolePort +
        "&encrypt=" + (isNoVNCConsoleEncrypted ? "1" : "0") +
        "&allowfullscreen=" + noVNCConsoleAllowfullscreen +
        "&autoconnect=" + noVNCConsoleAutoconnect
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

}
