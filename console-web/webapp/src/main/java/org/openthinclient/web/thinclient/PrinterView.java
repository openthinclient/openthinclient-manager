package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.*;
import org.openthinclient.web.OTCSideBar;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.presenter.ProfilePanelPresenter;
import org.openthinclient.web.thinclient.presenter.ReferencePanelPresenter;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = PrinterView.NAME, ui= ManagerUI.class)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT, captionCode="UI_PRINTER_HEADER", order = 60)
@ThemeIcon(PrinterView.ICON)
public final class PrinterView extends AbstractThinclientView {

  private static final Logger LOGGER = LoggerFactory.getLogger(PrinterView.class);

  public static final String NAME = "printer_view";
  public static final String ICON = "icon/printer.svg";

  @Autowired
  private PrinterService printerService;
  @Autowired
  private UserGroupService userGroupService;
  @Autowired
  private UserService userService;
  @Autowired
  private ClientService clientService;
  @Autowired
  private LocationService locationService;
  @Autowired
  private SchemaProvider schemaProvider;
  @Autowired @Qualifier("deviceSideBar")
  OTCSideBar deviceSideBar;

   private final IMessageConveyor mc;
   private VerticalLayout right;
   private ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();

   public PrinterView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
     super(UI_PRINTER_HEADER, eventBus, notificationService);
     mc = new MessageConveyor(UI.getCurrent().getLocale());
   }

  @PostConstruct
  private void setup() {
    addStyleName(NAME);
    addCreateActionButton(mc.getMessage(UI_THINCLIENT_ADD_PRINTER_LABEL), ICON, NAME + "/create");
    addOverviewItemlistPanel(UI_PRINTER_HEADER, getAllItems());
  }

  @Override
  public Set getAllItems() {
    try {
      return printerService.findAll();
    } catch (Exception e) {
      LOGGER.warn("Cannot find directory-objects: " + e.getMessage());
      showError(e);
    }
    return Collections.EMPTY_SET;
  }

  @Override
  public Schema getSchema(String schemaName) {
    return schemaProvider.getSchema(Printer.class, schemaName);
  }

  @Override
  public Map<String, String> getSchemaNames() {
    return Stream.of(schemaProvider.getSchemaNames(Printer.class))
                 .collect( Collectors.toMap(schemaName -> schemaName, schemaName -> getSchema(schemaName).getLabel()));
  }

  @Override
  public String getViewName() {
    return NAME;
  }

  public ProfilePanel createProfilePanel(DirectoryObject directoryObject) throws BuildProfileException {

    Profile profile = (Profile) directoryObject;

    List<OtcPropertyGroup> otcPropertyGroups = builder.getOtcPropertyGroups(getSchemaNames(), profile);

    OtcPropertyGroup meta = otcPropertyGroups.get(0);
    addProfileNameAlreadyExistsValidator(meta);
    String type = meta.getProperty("type").get().getConfiguration().getValue();

    ProfilePanel profilePanel = new ProfilePanel(profile.getName() + " (" + type + ")", profile.getClass());
    ProfilePanelPresenter presenter = new ProfilePanelPresenter(this, profilePanel, profile);

    // set MetaInformation
    presenter.setPanelMetaInformation(createDefaultMetaInformationComponents(profile));

    // attach save-action
//    otcPropertyGroups.forEach(group -> group.setValueWrittenHandlerToAll(ipg -> saveValues(presenter, profile)));
    // put to panel
    presenter.setItemGroups(otcPropertyGroups);
    presenter.onValuesWritten(profilePanel1 -> saveValues(presenter, profile));

    return profilePanel;
  }

  @Override
  public ProfileReferencesPanel createReferencesPanel(DirectoryObject item) {
    ProfileReferencesPanel referencesPanel = new ProfileReferencesPanel(item.getClass());
    ReferencePanelPresenter refPresenter = new ReferencePanelPresenter(referencesPanel);

    Set<DirectoryObject> members = ((Printer) item).getMembers();
    Set<ClientMetaData> allClients = clientService.findAllClientMetaData();
    refPresenter.showReference(members, mc.getMessage(UI_CLIENT_HEADER), allClients, Client.class, values -> saveReference(item, values, allClients, Client.class));
    Set<Location> allLocations = locationService.findAll();
    refPresenter.showReference(members, mc.getMessage(UI_LOCATION_HEADER), allLocations, Location.class, values -> saveReference(item, values, allLocations, Location.class));
    Set<User> allUsers = userService.findAll();
    refPresenter.showReference(members, mc.getMessage(UI_USER_HEADER), allUsers, User.class, values -> saveReference(item, values, allUsers, User.class));
    Set<UserGroup> userGroups = userGroupService.findAll();
    refPresenter.showReference(members, mc.getMessage(UI_USERGROUP_HEADER), userGroups, UserGroup.class, values -> saveReference(item, values, userGroups, UserGroup.class));

    return referencesPanel;
  }

  @Override
  public <T extends DirectoryObject> T getFreshProfile(String name) {
     return (T) printerService.findByName(name);
  }

  @Override
  public void save(DirectoryObject profile) {
    LOGGER.info("Save: " + profile);
    printerService.save((Printer) profile);
  }

  @Override
  public Client getClient(String name) {
    return clientService.findByName(name);
  }

  @Override
  public void selectItem(DirectoryObject directoryObject) {
    LOGGER.info("sideBar: "+ deviceSideBar);
    deviceSideBar.selectItem(NAME, directoryObject, getAllItems());
  }

}
