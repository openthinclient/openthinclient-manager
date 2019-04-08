package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.ValueContext;
import com.vaadin.data.validator.AbstractValidator;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.BorderStyle;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.api.rest.appliance.TokenManager;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.*;
import org.openthinclient.ldap.DirectoryException;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.exception.AllItemsListException;
import org.openthinclient.web.thinclient.exception.ProfileNotSavedException;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.model.SelectOption;
import org.openthinclient.web.thinclient.presenter.ProfilePanelPresenter;
import org.openthinclient.web.thinclient.property.OtcOptionProperty;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.openthinclient.web.thinclient.property.OtcTextProperty;
import org.openthinclient.web.thinclient.util.ClientIPAddressFinder;
import org.openthinclient.web.ui.ManagerSideBarSections;
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
import java.util.stream.Stream;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = ClientView.NAME)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT,  captionCode="UI_CLIENT_HEADER", order = 88)
@ThemeIcon("icon/logo-white.svg")
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
  private ApplicationGroupService applicationGroupService;
  @Autowired
  private ClientGroupService clientGroupService;
  @Autowired
  private SchemaProvider schemaProvider;
  @Autowired
  private UnrecognizedClientService unrecognizedClientService;
  @Autowired
  private TokenManager tokenManager;

   private final IMessageConveyor mc;
   private ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();

   public ClientView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
     super(UI_CLIENT_HEADER, eventBus, notificationService);
     mc = new MessageConveyor(UI.getCurrent().getLocale());

     showCreateClientAction();
   }


  @PostConstruct
  private void setup() {
     try {
       setItems(getAllItems());
     } catch (AllItemsListException e) {
       showError(e);
     }
  }

  @Override
  public HashSet getAllItems() throws AllItemsListException {
     try {
       return (HashSet) clientService.findAll();
     } catch (Exception e) {
       throw new AllItemsListException("Cannot load client-items", e);
     }
  }

  @Override
  public Schema getSchema(String schemaName) {
    return schemaProvider.getSchema(Client.class, schemaName);
  }

  @Override
  public String[] getSchemaNames() {
    return schemaProvider.getSchemaNames(Client.class);
  }

  public ProfilePanel createProfilePanel (DirectoryObject directoryObject) throws BuildProfileException {

   Profile profile = (Profile) directoryObject;

   List<OtcPropertyGroup> otcPropertyGroups = builder.getOtcPropertyGroups(getSchemaNames(), profile);

    OtcPropertyGroup meta = otcPropertyGroups.get(0);
    String type = meta.getProperty("type").get().getConfiguration().getValue();

    ProfilePanel profilePanel = new ProfilePanel(profile.getName() + " (" + type + ")", profile.getClass());
    ProfilePanelPresenter presenter = new ProfilePanelPresenter(this, profilePanel, profile);
    presenter.addPanelCaptionComponent(createVNCButton());
    presenter.addPanelCaptionComponent(createLOGButton());

    // set MetaInformation
    List<Component> informationComponents = createDefaultMetaInformationComponents(profile);
    informationComponents.addAll(createClientMetaInformations((Client) profile));
    presenter.setPanelMetaInformation(informationComponents);

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
    showReference(profilePanel, client.getApplicationGroups(), mc.getMessage(UI_APPLICATIONGROUP_HEADER),
        applicationGroupService.findAll(), ApplicationGroup.class,
        values -> saveReference(profile, values, applicationGroupService.findAll(), ApplicationGroup.class),
        getApplicationsForApplicationGroupFunction(client), false
    );

   showReference(profile, profilePanel, client.getApplications(), mc.getMessage(UI_APPLICATION_HEADER), applicationService.findAll(), Application.class);
   showReference(profile, profilePanel, client.getPrinters(), mc.getMessage(UI_PRINTER_HEADER), printerService.findAll(), Printer.class);

   return profilePanel;
  }

  protected List<Component> createClientMetaInformations(Client client) {
    List<Component> information = new ArrayList<>();

    information.add(new HorizontalLayout(new Label(mc.getMessage(UI_CLIENT_META_INFORMATION_LABEL, client.getMacAddress(), client.getIpHostNumber()))));

    String location = client.getLocation() != null ? client.getLocation().getName() : "";
    String hwtype   = client.getHardwareType() != null ? client.getHardwareType().getName() : "";
    information.add(new HorizontalLayout(new Label(mc.getMessage(UI_CLIENT_META_INFORMATION_LABEL2,location, hwtype))));

    return information;
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
        Stream<? extends DirectoryObject> stream = first.get().getApplications().stream()
                                                              .sorted(Comparator.comparing(DirectoryObject::getName, String::compareToIgnoreCase));
        return stream.map(m -> new Item(m.getName(), Item.Type.APPLICATION)).collect(Collectors.toList());
      } else {
        return new ArrayList<>();
      }
    };
  }

  private Component createVNCButton() {
    Button button = new Button();
    button.setCaption(mc.getMessage(UI_COMMON_VNC_LABEL));
    button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
    button.addStyleName(ValoTheme.BUTTON_SMALL);
//    button.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    button.addClickListener(this::openNoVncInNewBrowserWindow);
    return button;
  }

  private Component createLOGButton() {
    Button button = new Button();
    button.setIcon(VaadinIcons.FILE_TEXT_O);
    button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
    button.addStyleName(ValoTheme.BUTTON_SMALL);
    button.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
    button.addClickListener(this::showClientLogs);
    return button;
  }

  @Override
  protected ProfilePanel createProfileMetadataPanel(Profile p) {

    Client profile = (Client) p;
    ProfilePanel profilePanel = new ProfilePanel(mc.getMessage(UI_PROFILE_PANEL_NEW_CLIENT_HEADER), profile.getClass());
    profilePanel.hideMetaInformation();
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
    // remove default validators and add custom validator to 'name'-property
    configuration.getProperty("name").ifPresent(nameProperty -> {
      nameProperty.getConfiguration().getValidators().clear();
      nameProperty.getConfiguration().addValidator(new RegexpValidator(mc.getMessage(UI_PROFILE_THINCLIENT_NAME_REGEXP), "^[a-zA-Z0-9][a-zA-Z0-9\\-\\.]+[a-z-A-Z0-9]$"));
      nameProperty.getConfiguration().getValidators().add(new AbstractValidator<String>(mc.getMessage(UI_PROFILE_NAME_ALREADY_EXISTS)) {
        @Override
        public ValidationResult apply(String value, ValueContext context) {
          DirectoryObject directoryObject = getFreshProfile(value);
          return directoryObject == null ? ValidationResult.ok() : ValidationResult.error(mc.getMessage(UI_PROFILE_NAME_ALREADY_EXISTS));
        }
      });
    });

    // MAC-Address
    OtcTextProperty macaddress = new OtcTextProperty(mc.getMessage(UI_THINCLIENT_MAC), mc.getMessage(UI_THINCLIENT_MAC_TIP), "macaddress", profile.getMacAddress());
    ItemConfiguration macaddressConfiguration = new ItemConfiguration("macaddress", profile.getMacAddress());
    macaddressConfiguration.addValidator(new RegexpValidator(mc.getMessage(UI_THINCLIENT_MAC_VALIDATOR_ADDRESS), "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"));
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
          try {
            setItems(getAllItems()); // refresh item list
            selectItem(profile);
          } catch (AllItemsListException e) {
            showError(e);
          }
        }

    });
    return configuration;
  }

  @Override
  public <T extends DirectoryObject> T getFreshProfile(String name) {
    // if there are special characters in directory, quote them before search
//    String reg = "(?>[^\\w^+^\\s^-])";
//    String _name = name.replaceAll(reg, "\\\\$0");
    Client profile = clientService.findByName(name);

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

  private void showClientLogs(Button.ClickEvent event) {
    String macAddress = ((Client) getSelectedItem()).getMacAddress();
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

  private void openNoVncInNewBrowserWindow(Button.ClickEvent event) {

    String ipHostNumber = ((Client) getFreshProfile(getSelectedItem().getName())).getIpHostNumber();
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

}
