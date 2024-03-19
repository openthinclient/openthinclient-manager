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
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.openthinclient.api.ldif.export.LdifExporterService;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.service.*;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.web.Audit;
import org.openthinclient.web.ClientStatus;
import org.openthinclient.web.component.Popup;
import org.openthinclient.common.Events.ClientCountChangeEvent;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.component.ProfilesListOverviewPanel;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.exception.ProfileNotDeletedException;
import org.openthinclient.web.thinclient.exception.ProfileNotSavedException;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.model.SelectOption;
import org.openthinclient.web.thinclient.presenter.ProfilesListOverviewPanelPresenter;
import org.openthinclient.web.thinclient.presenter.ProfilePanelPresenter;
import org.openthinclient.web.thinclient.presenter.ReferencePanelPresenter;
import org.openthinclient.web.thinclient.property.OtcMacProperty;
import org.openthinclient.web.thinclient.property.OtcOptionProperty;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.openthinclient.web.ui.ManagerUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;
import org.vaadin.spring.sidebar.annotation.ThemeIcon;

import javax.annotation.PostConstruct;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.openthinclient.common.model.service.ClientService.DEFAULT_CLIENT_MAC;
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
  @Autowired
  private RealmService realmService;
  @Autowired
  private ClientStatus clientStatus;
  @Autowired
  private ApplicationContext applicationContext;

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

    boolean isDefaultClient = DEFAULT_CLIENT_MAC.equals(profile.getMacAddress());

    String subtitle = mc.getMessage(isDefaultClient? UI_DEFAULT_CLIENT: UI_CLIENT);
    ProfilePanel profilePanel = new ProfilePanel(profile.getName(),
                                                  subtitle,
                                                  Client.class);

    if (clientStatus.isOnline(profile.getMacAddress())) {
      profilePanel.addStyleName("online");
    } else if (profile.getMacAddress() != null && !isDefaultClient) {
      profilePanel.addStyleName("offline");
    }

    ProfilePanelPresenter presenter = new ProfilePanelPresenter(this, profilePanel, profile);
    if (!isDefaultClient) {
      presenter.addPanelCaptionComponent(createWOLButton(profile));
      presenter.addPanelCaptionComponent(createRestartButton(profile));
      presenter.addPanelCaptionComponent(createShutdownButton(profile));
      String ip = profile.getIpHostNumber();
      if (ip != null && !ip.isEmpty() && !ip.equals("0.0.0.0")) {
        presenter.addPanelCaptionComponent(createIPButton(profile));
      }
      presenter.addPanelCaptionComponent(createVNCButton(profile));
      presenter.addPanelCaptionComponent(createLOGButton(profile));
    }
    presenter.hideCopyButton();

    // replace default metadata-group with client-metadata
    otcPropertyGroups.remove(0);
    otcPropertyGroups.add(0, createClientMetadataPropertyGroup(profile, presenter));

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
    if (hwtype != null) {
      refPresenter.showReference(Collections.singleton(hwtype),
      mc.getMessage(UI_HWTYPE));
    }

    Location location = client.getLocation();
    if (location != null) {
      refPresenter.showReference(Collections.singleton(location),
                                  mc.getMessage(UI_LOCATION));
    }

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

    if (hwtype != null) {
      Set<Device> hwtypeDevices = hwtype.getDevices();
      if (hwtypeDevices.size() > 0) {
        refPresenter.showReferenceAddendum(hwtypeDevices,
                                            mc.getMessage(UI_FROM_HWTYPE_HEADER));
      }
    }

    Set<Printer> allPrinters = printerService.findAll();
    refPresenter.showReference(client.getPrinters(), mc.getMessage(UI_PRINTER_HEADER),
                                allPrinters,
                                values -> saveReference(client, values, allPrinters, Printer.class));

    if (location != null) {
      Set<Printer> locationPrinters = location.getPrinters();
      if (locationPrinters.size() > 0) {
        refPresenter.showReferenceAddendum(locationPrinters,
                                            mc.getMessage(UI_FROM_LOCATION_HEADER));
      }
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

  private Component createIPButton(Client profile) {
    Button button = new Button();
    button.addStyleNames("ip", "copy-on-click");
    button.setCaption(profile.getIpHostNumber());
    button.setDescription(mc.getMessage(UI_PROFILE_PANEL_BUTTON_ALT_TEXT_IP));
    button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
    button.addStyleName(ValoTheme.BUTTON_SMALL);
    return button;
  }

  private Component createWOLButton(Client profile) {
    return createIconButton(
        VaadinIcons.PLAY,
        UI_PROFILE_PANEL_BUTTON_ALT_TEXT_WOL,
        "wol",
        ev -> wakeOnLan(profile)
    );
  }

  private Component createRestartButton(Client profile) {
    return createIconButton(
        VaadinIcons.REFRESH,
        UI_PROFILE_PANEL_BUTTON_RESTART,
        "restart",
        ev -> restartClients(profile));
  }

  private Component createShutdownButton(Client profile) {
    return createIconButton(
        VaadinIcons.STOP,
        UI_PROFILE_PANEL_BUTTON_SHUTDOWN,
        "shutdown",
        ev -> shutdownClients(profile));
  }

  private Component createVNCButton(Client profile) {
    return createIconButton(
        VaadinIcons.DESKTOP,
        UI_PROFILE_PANEL_BUTTON_ALT_TEXT_VNC,
        "vnc",
        ev -> openNoVncInNewBrowserWindow(profile.getName())
    );
  }

  private Component createLOGButton(Client profile) {
    return createIconButton(
        VaadinIcons.FILE_TEXT_O,
        UI_PROFILE_PANEL_BUTTON_ALT_TEXT_CLIENTLOG,
        ev -> showClientLogs(profile)
    );
  }

  private Component createIconButton( VaadinIcons icon,
                                      ConsoleWebMessages description,
                                      ClickListener clickListener ) {
    return createIconButton(icon, description, null, clickListener);
  }

  private Component createIconButton( VaadinIcons icon,
                                      ConsoleWebMessages description,
                                      String styleName,
                                      ClickListener clickListener ) {
    Button button = new Button();
    button.setIcon(icon);
    button.setDescription(mc.getMessage(description));
    button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
    button.addStyleName(ValoTheme.BUTTON_SMALL);
    button.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    if (styleName != null) {
      button.addStyleName(styleName);
    }
    button.addClickListener(clickListener);
    return button;
  }


  @Override
  public ProfilesListOverviewPanelPresenter createOverviewItemlistPanel(ConsoleWebMessages i18nTitleKey, Set items, boolean enabled) {

    ProfilesListOverviewPanel plop = new ProfilesListOverviewPanel(i18nTitleKey, enabled);
    ProfilesListOverviewPanelPresenter plopPresenter = new ProfilesListOverviewPanelPresenter(this, plop, new LdifExporterService(realmService.getDefaultRealm().getConnectionDescriptor()),
        applicationContext);

    ListDataProvider<DirectoryObject> dataProvider = DataProvider.ofCollection(items);
    dataProvider.setSortComparator(Comparator.comparing(DirectoryObject::getName, String::compareToIgnoreCase)::compare);
    plop.setDataProvider(dataProvider, clientStatus.getOnlineMACs());
    plopPresenter.addWolButtonClickHandler(clients -> {
      wakeOnLan(clients.toArray(new ClientMetaData[0]));
    });
    plopPresenter.addRestartButtonClickHandler(clients -> {
      restartClients(clients.toArray(new ClientMetaData[0]));
    });
    plopPresenter.addShutdownButtonClickHandler(clients -> {
      shutdownClients(clients.toArray(new ClientMetaData[0]));
    });
    plopPresenter.setVisible(true);
    return plopPresenter;
  }


  @Override
  protected ProfilePanel createProfileMetadataPanel(Client profile) {

    ProfilePanel profilePanel = new ProfilePanel(mc.getMessage(UI_PROFILE_PANEL_NEW_CLIENT_HEADER), profile.getClass());
    ProfilePanelPresenter presenter = new ProfilePanelPresenter(this, profilePanel, profile);
    presenter.hideCopyButton();
    presenter.hideDeleteButton();

    OtcPropertyGroup configuration = createClientMetadataPropertyGroup(profile, presenter);

    // put property-group to panel
    presenter.setItemGroups(Arrays.asList(configuration, new OtcPropertyGroup()));
    presenter.onValuesWritten(profilePanel1 -> saveValues(presenter, profile));

    return profilePanel;
  }

  private OtcPropertyGroup createClientMetadataPropertyGroup(Client profile, ProfilePanelPresenter presenter) {

    OtcPropertyGroup configuration = builder.createDirectoryObjectMetaDataGroup(profile);
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

    boolean isNew = profile.getDn() == null;

    // Location
    Location location = profile.getLocation();
    Set<Location> allLocations = locationService.findAll();
    String selectedLocationDn = null;
    if(location != null)  {
      selectedLocationDn = location.getDn();
    } else if(isNew && allLocations.size() == 1) {
      selectedLocationDn = allLocations.stream().findFirst().get().getDn();
    }

    OtcProperty locationProp = new OtcOptionProperty(mc.getMessage(UI_LOCATION),
                                                      null,
                                                      "location",
                                                      selectedLocationDn,
                                                      null,
                                                      allLocations.stream()
                                                                  .map(o -> new SelectOption(o.getName(), o.getDn()))
                                                                  .collect(Collectors.toList()));
    ItemConfiguration locationConfig = new ItemConfiguration("location",
                                                              location != null ? location.getDn() : null);
    locationConfig.setRequired(true);
    locationProp.setConfiguration(locationConfig);
    configuration.addProperty(locationProp);

    // Hardwaretype
    HardwareType hardwareType = profile.getHardwareType();
    Set<HardwareType> allHardwareTypes = hardwareTypeService.findAll();
    String selectedHardwareTypeDn = null;
    if(location != null)  {
      selectedHardwareTypeDn = location.getDn();
    } else if(isNew && allHardwareTypes.size() == 1) {
      selectedHardwareTypeDn = allHardwareTypes.stream().findFirst().get().getDn();
    }
    OtcProperty hwProp = new OtcOptionProperty(mc.getMessage(UI_HWTYPE),
                                                null,
                                                "hwtype",
                                                selectedHardwareTypeDn,
                                                null,
                                                allHardwareTypes.stream()
                                                                  .map(o -> new SelectOption(o.getName(), o.getDn()))
                                                                  .collect(Collectors.toList()));
    ItemConfiguration hwtypeConfig = new ItemConfiguration("hwtype",
                                                            hardwareType != null ? hardwareType.getDn() : null);
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
  public void saveValues(ProfilePanelPresenter profilePanelPresenter, Client client) {

    LOGGER.debug("Save values for client: " + client);

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
      navigateTo(client);
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
    String macAddress = profile.getMacAddress();
    Optional<UnrecognizedClient> optionalUnrecognizedClient = unrecognizedClientService.findAll().stream().filter(unrecognizedClient -> unrecognizedClient.getMacAddress().equals(macAddress)).findFirst();
    if (optionalUnrecognizedClient.isPresent()) {
      Realm realm = optionalUnrecognizedClient.get().getRealm();
      try {
        realm.getDirectory().delete(optionalUnrecognizedClient.get());
      } catch (DirectoryException e) {
        throw new ProfileNotSavedException("Cannot delete object " + profile, e);
      }
    }

    applicationContext.publishEvent(new ClientCountChangeEvent());

  }

  @Override
  public void delete(Client profile) throws ProfileNotDeletedException {
    String mac = profile.getMacAddress();
    Path logDir = managerHome.getLocation().toPath().resolve("logs").resolve("syslog");
    File[] logFiles = logDir.toFile().listFiles((d, name) -> name.startsWith(mac));
    if(logFiles != null) {
      for(File file: logFiles) {
        file.delete();
      }
    }
    super.delete(profile);
    applicationContext.publishEvent(new ClientCountChangeEvent());
  }

  private void showClientLogs(Client profile) {
    Path logs = managerHome.getLocation().toPath().resolve("logs").resolve("syslog");
    (new FileContentWindow(logs, profile.getName(), profile.getMacAddress())).open();
  }

  class FileContentWindow extends Popup {

    // Show the last MAX_DISPLAY_LINES (of all log files combined).
    private final int MAX_DISPLAY_LINES = 2048;

    // Don't open next ZIP if we're only a few lines short of MAX_DISPLAY_LINES.
    private final int MAX_MISSING_LINES = 15;

    // Unique indicator for IO error (must not be Collections.emptyLst())
    private final List<String> FILE_READ_ERROR = new ArrayList<>();

    public FileContentWindow(Path logDir, String name, String macAddress) {
      super(mc.getMessage(ConsoleWebMessages.UI_THINCLIENT_LOG_CAPTION, name, macAddress), "logview");

      setWidth("642px");
      setMaximized(true);

      List<String> lines = new ArrayList<>();
      int linesLeft = MAX_DISPLAY_LINES;

      Iterator<List<String>> lineChunks = readLogLines(logDir.toAbsolutePath(),
                                                        macAddress).iterator();
      while(lineChunks.hasNext() && linesLeft > MAX_MISSING_LINES) {
        List<String> srcLines = lineChunks.next();
        if(srcLines == FILE_READ_ERROR) { // Something went wrong
          if(lines.size() > 0) {          // Abort and display what we've got so far
            break;
          } else {                        // … or present an error if nothing was read
            setMessage(ConsoleWebMessages.UI_THINCLIENT_LOG_ERROR);
            return;
          }
        }
        ListIterator<String> lineIter = srcLines.listIterator(srcLines.size());

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
            line.append(String.format("<div class=\"logline %s\">", parts[2].trim()))
                .append(parts[0])
                .append(parts[1])
                .append(parts[2])
                .append(parts[5])
                .append("</div>");
            lines.add(0, line.toString());
          }
        }
      }
      if (lines.size() > 0) {
        addContent(new Label(String.join("\n", lines), ContentMode.HTML));
      } else {
        setMessage(ConsoleWebMessages.UI_THINCLIENT_LOG_EMPTY);
      }
    }

    private Stream<List<String>> readLogLines(Path logDir, String macaddress) {
      // Read the current log file;
      Stream<List<String>> logLines;
      Path logPath = logDir.resolve(String.format("%s.log", macaddress.replace(":", "-")));
      if(logPath.toFile().exists()) {
        try {
          logLines = Stream.of(Files.readAllLines(logPath));
        } catch (IOException ex) {
          LOGGER.error(String.format("Failed to read from log file %s", logPath),
                        ex);
          return Stream.of(FILE_READ_ERROR);
        }
      } else {
        logLines = Stream.empty();
      }

      // Append the rolled over logs
      String[] zipFileNames = logDir.toFile()
                              .list((d, name) -> name.startsWith(macaddress)
                                                    && name.endsWith(".zip"));
      if(zipFileNames == null) {
        LOGGER.error("Could not list files in {}", logDir);
        return Stream.concat(logLines, Stream.of(FILE_READ_ERROR));
      }
      return Stream.concat( logLines,
                            Stream.of(zipFileNames)
                                  .sorted()
                                  .map(logDir::resolve)
                                  .map(this::readLinesfromZip) );
    }

    private List<String> readLinesfromZip(Path filePath) {
      try {
        ZipFile zipFile = new ZipFile(filePath.toFile());
        if(zipFile.size() != 1) {
          LOGGER.error("Unexpected amount of files ({}) in zipped syslog {}",
                        zipFile.size(), zipFile.getName());
          return Collections.emptyList();
        }
        ZipEntry zipEntry = zipFile.entries().nextElement();
        return new BufferedReader(new InputStreamReader(zipFile.getInputStream(zipEntry)))
                .lines().collect(Collectors.toList());

      } catch(IOException ex)  {
        LOGGER.error(String.format("Failed to read from log file %s", filePath),
                      ex);
        return FILE_READ_ERROR;
      }
    }

    private void setMessage(ConsoleWebMessages messageKey, Object... args) {
      Label messageLabel = new Label(mc.getMessage(messageKey, args));
      messageLabel.addStyleName("message");
      addContent(messageLabel);
    }
  }

  private void wakeOnLan(ClientMetaData... profiles) {
    List<String> failed = new ArrayList<>(profiles.length);
    for(ClientMetaData profile: profiles) {
      try {
        String macAddress = profile.getMacAddress();
        LOGGER.info("Sending WOL packet to " + macAddress);
        WakeOnLan.wake(macAddress);
      } catch(Exception ex) {
        LOGGER.error("Failed to send WOL packet", ex);
      }
      boolean single = profiles.length == 1;
      boolean errors = failed.size() > 0;
      String message;
      if(!errors) {
        message = mc.getMessage(single? ConsoleWebMessages.UI_PROFILE_WOL_SUCCESS:
                                        ConsoleWebMessages.UI_PROFILE_WOL_SUCCESS_ALL);
      } else if(failed.size() == profiles.length) {
        message = mc.getMessage(single? ConsoleWebMessages.UI_PROFILE_WOL_ERROR:
                                        ConsoleWebMessages.UI_PROFILE_WOL_ERROR_ALL);
      } else {
        message = mc.getMessage(ConsoleWebMessages.UI_PROFILE_WOL_ERROR_SOME,
                                String.join(", ", failed));
      }
      Notification.show(message, errors ? Notification.Type.ERROR_MESSAGE:
                                          Notification.Type.HUMANIZED_MESSAGE);
    }
  }

  private void restartClients(ClientMetaData... profiles) {
    if (profiles.length == 0) {
      return;
    }
    (new ConfimationPopup(
        ConsoleWebMessages.UI_PROFILE_CONFIRM_RESTART_TITLE,
        profiles.length == 1?
            ConsoleWebMessages.UI_PROFILE_CONFIRM_RESTART_SINGLE_MESSAGE:
            ConsoleWebMessages.UI_PROFILE_CONFIRM_RESTART_MULTI_MESSAGE,
        profiles.length == 1?
            ConsoleWebMessages.UI_PROFILE_CONFIRM_RESTART_SINGLE_OK:
            ConsoleWebMessages.UI_PROFILE_CONFIRM_RESTART_MULTI_OK,
        () -> clientStatus.restartClients(
                  Arrays.stream(profiles)
                        .map(ClientMetaData::getMacAddress)
                        .collect(Collectors.toList()))
    )).open();
  }

  private void shutdownClients(ClientMetaData... profiles) {
    if (profiles.length == 0) {
      return;
    }
    (new ConfimationPopup(
        ConsoleWebMessages.UI_PROFILE_CONFIRM_SHUTDOWN_TITLE,
        profiles.length == 1? UI_PROFILE_CONFIRM_SHUTDOWN_SINGLE_MESSAGE
                            : UI_PROFILE_CONFIRM_SHUTDOWN_MULTI_MESSAGE,
        profiles.length == 1?
            ConsoleWebMessages.UI_PROFILE_CONFIRM_SHUTDOWN_SINGLE_OK:
            ConsoleWebMessages.UI_PROFILE_CONFIRM_SHUTDOWN_MULTI_OK,
        () -> clientStatus.shutdownClients(
                  Arrays.stream(profiles)
                        .map(ClientMetaData::getMacAddress)
                        .collect(Collectors.toList()))
    )).open();
  }

  private void openNoVncInNewBrowserWindow(String clientName) {
    String ipHostNumber = getFreshProfile(clientName).getIpHostNumber();
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

  class ConfimationPopup extends Popup {
    ConfimationPopup(
        ConsoleWebMessages title_key,
        ConsoleWebMessages message_key,
        ConsoleWebMessages cofirmation_button_key,
        Runnable onConfirm
    ) {
      super(title_key);
      setWidth("420px");
      addContent(new Label(mc.getMessage(message_key), ContentMode.HTML));
      addButton(
          new Button(mc.getMessage(UI_BUTTON_CANCEL), ev -> {
            close();
          }),
          new Button(mc.getMessage(cofirmation_button_key), ev -> {
            close();
            onConfirm.run();
          }));
    }
  }
}
