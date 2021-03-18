package org.openthinclient.web.thinclient;

import com.vaadin.spring.annotation.SpringView;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.LocationService;
import org.openthinclient.common.model.service.PrinterService;
import org.openthinclient.web.Audit;
import org.openthinclient.web.OTCSideBar;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.model.DeleteMandate;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = LocationView.NAME, ui= ManagerUI.class)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT,  captionCode="UI_LOCATION_HEADER", order = 80)
@ThemeIcon(LocationView.ICON)
public final class LocationView extends AbstractThinclientView<Location> {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocationView.class);

  public static final String NAME = "location_view";
  public static final String ICON = "icon/location.svg";
  public static final ConsoleWebMessages TITLE_KEY = UI_LOCATION_HEADER;

  @Autowired
  private ClientService clientService;
  @Autowired
  private LocationService locationService;
  @Autowired
  private SchemaProvider schemaProvider;
  @Autowired
  private PrinterService printerService;
  @Autowired @Qualifier("deviceSideBar")
  OTCSideBar deviceSideBar;

   private ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();

   @PostConstruct
   private void setup() {
     addStyleName(NAME);
   }

  @Override
  public Set<Location> getAllItems() {
    try {
      return locationService.findAll();
    } catch (Exception e) {
      LOGGER.warn("Cannot find directory-objects: " + e.getMessage());
      showError(e);
    }
    return Collections.emptySet();
  }

  @Override
  public Schema getSchema(String schemaName) {
    return schemaProvider.getSchema(Location.class, schemaName);
  }

  @Override
  public Map<String, String> getSchemaNames() {
    return Stream.of(schemaProvider.getSchemaNames(Location.class))
                 .collect( Collectors.toMap(schemaName -> schemaName, schemaName -> getSchema(schemaName).getLabel()));
  }

  public ProfilePanel createProfilePanel(Location profile) throws BuildProfileException {
    List<OtcPropertyGroup> otcPropertyGroups = builder.getOtcPropertyGroups(getSchemaNames(), profile);

    OtcPropertyGroup meta = otcPropertyGroups.get(0);
    addProfileNameAlreadyExistsValidator(meta);

    ProfilePanel profilePanel = new ProfilePanel(profile.getName(), profile.getClass());
    ProfilePanelPresenter presenter = new ProfilePanelPresenter(this, profilePanel, profile);
    presenter.setDeleteMandate(createDeleteMandateFunction());

    // put to panel
    presenter.setItemGroups(otcPropertyGroups);
    presenter.onValuesWritten(profilePanel1 -> saveValues(presenter, profile));


    return profilePanel;
  }

  @Override
  public ProfileReferencesPanel createReferencesPanel(Location location) {
    ProfileReferencesPanel referencesPanel = new ProfileReferencesPanel(Location.class);
    ReferencePanelPresenter refPresenter = new ReferencePanelPresenter(referencesPanel);

    Set<ClientMetaData> clients = clientService.findByLocation(location.getName());
    refPresenter.showReference(clients, mc.getMessage(UI_CLIENT_HEADER) + " (readonly)",
                              Collections.emptySet(), ClientMetaData.class,
                              values -> saveReference(location, values, Collections.emptySet(), ClientMetaData.class),
                              null, true);

    Set<Printer> all = printerService.findAll();
    refPresenter.showReference(location.getPrinters(), mc.getMessage(UI_PRINTER_HEADER), all, Printer.class, values -> saveReference(location, values, all, Printer.class));

    return referencesPanel;
  }

  @Override
  protected Function<DirectoryObject, DeleteMandate> createDeleteMandateFunction() {
     return directoryObject -> {
       Location location = (Location) directoryObject;
       // TODO: Performance: eigene Query bauen für: finde clients mit gegebener Location
       boolean optionalClient = clientService.findAll().stream().anyMatch(client -> client.getLocation() != null && client.getLocation().equals(location));
       if (optionalClient) {
         return new DeleteMandate(false, mc.getMessage(UI_COMMON_DELETE_LOCATION_DENIED, location.getName()));
       }
       return new DeleteMandate(true, "");
     };
  }

  @Override
  protected Location newProfile() {
    return new Location();
  }

  @Override
  public Location getFreshProfile(String name) {
    return locationService.findByName(name);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected <D extends DirectoryObject> Set<D> getMembers(Location profile, Class<D> clazz) {
    return (Set<D>)profile.getPrinters();
  }

  @Override
  public void save(Location profile) {
    LOGGER.info("Save: " + profile);
    locationService.save(profile);
    Audit.logSave(profile);
  }

  @Override
  public void selectItem(DirectoryObject directoryObject) {
    LOGGER.info("sideBar: "+ deviceSideBar);
    deviceSideBar.selectItem(NAME, directoryObject, getAllItems());
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
