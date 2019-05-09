package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.UI;
import org.openthinclient.common.model.DirectoryObject;
import org.openthinclient.common.model.Location;
import org.openthinclient.common.model.Printer;
import org.openthinclient.common.model.Profile;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.LocationService;
import org.openthinclient.common.model.service.PrinterService;
import org.openthinclient.web.OTCSideBar;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.model.DeleteMandate;
import org.openthinclient.web.thinclient.presenter.ProfilePanelPresenter;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;
import org.vaadin.spring.sidebar.annotation.SideBarSection;
import org.vaadin.spring.sidebar.annotation.ThemeIcon;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = LocationView.NAME)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT,  captionCode="UI_LOCATION_HEADER", order = 80)
@ThemeIcon("icon/place-white.svg")
public final class LocationView extends ThinclientView {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocationView.class);

  public static final String NAME = "location_view";

  @Autowired
  private ClientService clientService;
  @Autowired
  private LocationService locationService;
  @Autowired
  private SchemaProvider schemaProvider;
  @Autowired
  OTCSideBar sideBar;

   private final IMessageConveyor mc;
   private ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();

   public LocationView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
     super(UI_LOCATION_HEADER, eventBus, notificationService);
     mc = new MessageConveyor(UI.getCurrent().getLocale());

//     showCreateLocationAction();
   }

   @PostConstruct
   private void setup() {
     // setItems(getAllItems());
   }

  @Override
  public HashSet getAllItems() {
    return (HashSet) locationService.findAll();
  }

  @Override
  public Schema getSchema(String schemaName) {
    return schemaProvider.getSchema(Location.class, schemaName);
  }

  @Override
  public String[] getSchemaNames() {
    return schemaProvider.getSchemaNames(Location.class);
  }

  public ProfilePanel createProfilePanel(DirectoryObject directoryObject) throws BuildProfileException {

    Profile profile = (Profile) directoryObject;

    List<OtcPropertyGroup> otcPropertyGroups = builder.getOtcPropertyGroups(getSchemaNames(), profile);

    OtcPropertyGroup meta = otcPropertyGroups.get(0);
    addProfileNameAlreadyExistsValidator(meta);
    String type = meta.getProperty("type").get().getConfiguration().getValue();

    ProfilePanel profilePanel = new ProfilePanel(profile.getName() + " (" + type + ")", profile.getClass());
    ProfilePanelPresenter presenter = new ProfilePanelPresenter(this, profilePanel, profile);
    presenter.setDeleteMandate(createDeleteMandateFunction());

    // set MetaInformation
//    presenter.setPanelMetaInformation(createDefaultMetaInformationComponents(profile));

    // attach save-action
//    otcPropertyGroups.forEach(group -> group.setValueWrittenHandlerToAll(ipg -> saveValues(presenter, profile)));
    // put to panel
    presenter.setItemGroups(otcPropertyGroups);
    presenter.onValuesWritten(profilePanel1 -> saveValues(presenter, profile));
//    Location location = ((Location) profile);
//    showReference(profile, profilePanel, location.getPrinters(), mc.getMessage(UI_PRINTER_HEADER), printerService.findAll(), Printer.class);

    return profilePanel;
  }

  private Function<DirectoryObject, DeleteMandate> createDeleteMandateFunction() {
     return directoryObject -> {
       Location location = (Location) directoryObject;
       boolean optionalClient = clientService.findAll().stream().anyMatch(client -> client.getLocation() != null && client.getLocation().equals(location));
       if (optionalClient || location.getPrinters().size() > 0) {
         return new DeleteMandate(false, mc.getMessage(UI_COMMON_DELETE_LOCATION_DENIED));
       }
       return new DeleteMandate(true, "");
     };
  }

  @Override
  public <T extends DirectoryObject> T getFreshProfile(String name) {
//     return (T) locationService.findByName(name);  // findByName is NOT working
    Optional<Location> location = locationService.findAll().stream().filter(l -> l.getName().equals(name)).findFirst();
    return (T) location.orElse(null);
  }

  @Override
  public void save(DirectoryObject profile) {
    LOGGER.info("Save: " + profile);
    locationService.save((Location) profile);
  }

  @Override
  public void selectItem(DirectoryObject directoryObject) {
    LOGGER.info("sideBar: "+ sideBar);
    sideBar.selectItem(NAME, directoryObject, getAllItems());
  }

  @Override
  public String getViewName() {
    return NAME;
  }

}
