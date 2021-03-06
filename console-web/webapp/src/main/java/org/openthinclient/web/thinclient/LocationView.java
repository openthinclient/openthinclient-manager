package org.openthinclient.web.thinclient;

import com.vaadin.spring.annotation.SpringView;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.LocationService;
import org.openthinclient.common.model.service.PrinterService;
import org.openthinclient.web.Audit;
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
import org.vaadin.spring.sidebar.annotation.SideBarItem;
import org.vaadin.spring.sidebar.annotation.ThemeIcon;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.function.Function;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = LocationView.NAME, ui= ManagerUI.class)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT,  captionCode="UI_LOCATION_HEADER", order = 80)
@ThemeIcon(LocationView.ICON)
public final class LocationView extends AbstractProfileView<Location> {

  private static final Logger LOGGER = LoggerFactory.getLogger(LocationView.class);

  public static final String NAME = "location_view";
  public static final String ICON = "icon/location.svg";
  public static final ConsoleWebMessages TITLE_KEY = UI_LOCATION_HEADER;

  @Autowired
  private ClientService clientService;
  @Autowired
  private LocationService locationService;
  @Autowired
  private PrinterService printerService;

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
  protected Class<Location> getItemClass() {
    return Location.class;
  }

  public ProfilePanel createProfilePanel(Location profile) throws BuildProfileException {
    List<OtcPropertyGroup> otcPropertyGroups = builder.getOtcPropertyGroups(getSchemaNames(), profile);

    OtcPropertyGroup meta = otcPropertyGroups.get(0);
    addProfileNameAlreadyExistsValidator(meta);

    ProfilePanel profilePanel = new ProfilePanel(profile.getName(),
                                                  mc.getMessage(UI_LOCATION),
                                                  profile.getClass());
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

    Set<Printer> allPrinters = printerService.findAll();
    refPresenter.showReference(location.getPrinters(),
                                mc.getMessage(UI_PRINTER_HEADER),
                                allPrinters,
                                values -> saveReference(location, values, allPrinters, Printer.class));

    refPresenter.showReference(clientService.findByLocation(location.getName()),
                                mc.getMessage(UI_CLIENT_HEADER));

    return referencesPanel;
  }

  @Override
  protected Function<DirectoryObject, DeleteMandate> createDeleteMandateFunction() {
     return directoryObject -> {
       Location location = (Location) directoryObject;
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
  public String getViewName() {
    return NAME;
  }

  @Override
  public ConsoleWebMessages getViewTitleKey() {
    return TITLE_KEY;
  }

}
