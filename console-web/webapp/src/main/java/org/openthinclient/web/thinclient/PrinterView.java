package org.openthinclient.web.thinclient;

import com.vaadin.spring.annotation.SpringView;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.*;
import org.openthinclient.web.Audit;
import org.openthinclient.web.OTCSideBar;
import org.openthinclient.web.i18n.ConsoleWebMessages;
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
public final class PrinterView extends AbstractThinclientView<Printer> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PrinterView.class);

  public static final String NAME = "printer_view";
  public static final String ICON = "icon/printer.svg";
  public static final ConsoleWebMessages TITLE_KEY = UI_PRINTER_HEADER;

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

  private ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();

  @PostConstruct
  private void setup() {
    addStyleName(NAME);
  }

  @Override
  public Set<Printer> getAllItems() {
    try {
      return printerService.findAll();
    } catch (Exception e) {
      LOGGER.warn("Cannot find directory-objects: " + e.getMessage());
      showError(e);
    }
    return Collections.emptySet();
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

  @Override
  public ConsoleWebMessages getViewTitleKey() {
    return TITLE_KEY;
  }

  public ProfilePanel createProfilePanel(Printer profile) throws BuildProfileException {
    Map<String, String> schemaNames = getSchemaNames();

    List<OtcPropertyGroup> otcPropertyGroups = builder.getOtcPropertyGroups(schemaNames, profile);

    OtcPropertyGroup meta = otcPropertyGroups.get(0);
    addProfileNameAlreadyExistsValidator(meta);
    String type = meta.getProperty("type").get().getConfiguration().getValue();

    ProfilePanel profilePanel = new ProfilePanel(profile.getName() + " (" + schemaNames.getOrDefault(type, type) + ")", profile.getClass());
    ProfilePanelPresenter presenter = new ProfilePanelPresenter(this, profilePanel, profile);

    presenter.setItemGroups(otcPropertyGroups);
    presenter.onValuesWritten(profilePanel1 -> saveValues(presenter, profile));

    return profilePanel;
  }

  @Override
  public ProfileReferencesPanel createReferencesPanel(Printer printer) {
    ProfileReferencesPanel referencesPanel = new ProfileReferencesPanel(Printer.class);
    ReferencePanelPresenter refPresenter = new ReferencePanelPresenter(referencesPanel);

    Set<DirectoryObject> members = printer.getMembers();

    Set<Location> allLocations = locationService.findAll();
    refPresenter.showReference(members, mc.getMessage(UI_LOCATION_HEADER),
                                allLocations, Location.class,
                                values -> saveReference(printer, values, allLocations, Location.class));

    Set<ClientMetaData> allClients = clientService.findAllClientMetaData();
    refPresenter.showReference(members, mc.getMessage(UI_CLIENT_HEADER),
                                allClients, Client.class,
                                values -> saveReference(printer, values, allClients, Client.class));

    Set<User> allUsers = userService.findAll();
    refPresenter.showReference(members, mc.getMessage(UI_USER_HEADER),
                                allUsers, User.class,
                                values -> saveReference(printer, values, allUsers, User.class));

    Set<UserGroup> userGroups = userGroupService.findAll();
    refPresenter.showReference(members, mc.getMessage(UI_USERGROUP_HEADER),
                                userGroups, UserGroup.class,
                                values -> saveReference(printer, values, userGroups, UserGroup.class));

    return referencesPanel;
  }

  @Override
  protected Printer newProfile() {
    return new Printer();
  }

  @Override
  public Printer getFreshProfile(String name) {
     return printerService.findByName(name);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected <D extends DirectoryObject> Set<D> getMembers(Printer profile, Class<D> clazz) {
    return (Set<D>)profile.getMembers();
  }

  @Override
  public void save(Printer profile) {
    LOGGER.info("Save: " + profile);
    printerService.save((Printer) profile);
    Audit.logSave(profile);
  }

  @Override
  public void selectItem(DirectoryObject directoryObject) {
    LOGGER.info("sideBar: "+ deviceSideBar);
    deviceSideBar.selectItem(NAME, directoryObject, getAllItems());
  }

}
