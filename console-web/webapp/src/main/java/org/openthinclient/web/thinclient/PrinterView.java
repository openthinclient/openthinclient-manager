package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.LocationService;
import org.openthinclient.common.model.service.PrinterService;
import org.openthinclient.common.model.service.UserService;
import org.openthinclient.web.OTCSideBar;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.presenter.ProfilePanelPresenter;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;
import org.vaadin.spring.sidebar.annotation.ThemeIcon;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = PrinterView.NAME)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT, captionCode="UI_PRINTER_HEADER", order = 60)
@ThemeIcon("icon/printer-white.svg")
public final class PrinterView extends ThinclientView {

  private static final Logger LOGGER = LoggerFactory.getLogger(PrinterView.class);

  public static final String NAME = "printer_view";

  @Autowired
  private PrinterService printerService;
  @Autowired
  private UserService userService;
  @Autowired
  private ClientService clientService;
  @Autowired
  private LocationService locationService;
  @Autowired
  private SchemaProvider schemaProvider;
  @Autowired
  private OTCSideBar sideBar;

   private final IMessageConveyor mc;
   private VerticalLayout right;
   private ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();

   public PrinterView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
     super(UI_PRINTER_HEADER, eventBus, notificationService);
     mc = new MessageConveyor(UI.getCurrent().getLocale());

     showCreatePrinterAction();
   }

  @PostConstruct
  private void setup() {
//   // setItems(getAllItems());
  }

  @Override
  public HashSet getAllItems() {
    return (HashSet) printerService.findAll();
  }

  @Override
  public Schema getSchema(String schemaName) {
    return schemaProvider.getSchema(Printer.class, schemaName);
  }

  @Override
  public String[] getSchemaNames() {
    return schemaProvider.getSchemaNames(Printer.class);
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

//    Set<DirectoryObject> members = ((Printer) profile).getMembers();
//    showReference(profile, profilePanel, members, mc.getMessage(UI_CLIENT_HEADER), clientService.findAll(), Client.class);
//    showReference(profile, profilePanel, members, mc.getMessage(UI_LOCATION_HEADER), locationService.findAll(), Location.class);
//    showReference(profile, profilePanel, members, mc.getMessage(UI_USER_HEADER), userService.findAll(), User.class);

    return profilePanel;
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
  protected void selectItem(DirectoryObject directoryObject) {
    LOGGER.info("sideBar: "+ sideBar);
    sideBar.selectItem(NAME, directoryObject, getAllItems());
  }

}
