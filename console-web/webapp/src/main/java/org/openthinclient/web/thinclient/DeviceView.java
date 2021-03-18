package org.openthinclient.web.thinclient;

import com.vaadin.spring.annotation.SpringView;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.DeviceService;
import org.openthinclient.common.model.service.HardwareTypeService;
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
@SpringView(name = DeviceView.NAME, ui= ManagerUI.class)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT,  captionCode="UI_DEVICE_HEADER", order = 50)
@ThemeIcon(DeviceView.ICON)
public final class DeviceView extends AbstractThinclientView<Device> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DeviceView.class);

  public static final String NAME = "device_view";
  public static final String ICON = "icon/device.svg";
  public static final ConsoleWebMessages TITLE_KEY = UI_DEVICE_HEADER;

  @Autowired
  private DeviceService deviceService;
  @Autowired
  private HardwareTypeService hardwareTypeService;
  @Autowired
  private ClientService clientService;
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
  public Set<Device> getAllItems() {
    try {
      return deviceService.findAll();
    } catch (Exception e) {
      LOGGER.warn("Cannot find directory-objects: " + e.getMessage());
      showError(e);
    }
    return Collections.emptySet();
  }

  @Override
  public Schema getSchema(String schemaName) {
    return schemaProvider.getSchema(Device.class, schemaName);
  }

  @Override
  public Map<String, String> getSchemaNames() {
    return Stream.of(schemaProvider.getSchemaNames(Device.class))
                 .collect( Collectors.toMap(schemaName -> schemaName, schemaName -> getSchema(schemaName).getLabel()));
  }

  public ProfilePanel createProfilePanel(Device profile) throws BuildProfileException {
    Map<String, String> schemaNames = getSchemaNames();

    List<OtcPropertyGroup> otcPropertyGroups = builder.getOtcPropertyGroups(schemaNames, profile);

    OtcPropertyGroup meta = otcPropertyGroups.get(0);
    addProfileNameAlreadyExistsValidator(meta);
    String type = meta.getProperty("type").get().getConfiguration().getValue();

    ProfilePanel profilePanel = new ProfilePanel(profile.getName() + " (" + schemaNames.getOrDefault(type, type) + ")", profile.getClass());
    ProfilePanelPresenter presenter = new ProfilePanelPresenter(this, profilePanel, profile);

    // put to panel
    presenter.setItemGroups(otcPropertyGroups);
    presenter.onValuesWritten(profilePanel1 -> saveValues(presenter, profile));


    return profilePanel;
  }

  @Override
  public ProfileReferencesPanel createReferencesPanel(Device device) {
    ProfileReferencesPanel referencesPanel = new ProfileReferencesPanel(Device.class);
    ReferencePanelPresenter refPresenter = new ReferencePanelPresenter(referencesPanel);

    Set<? extends DirectoryObject> members = device.getMembers();

    Set<HardwareType> allHwTypes = hardwareTypeService.findAll();
    refPresenter.showReference(members, mc.getMessage(UI_HWTYPE_HEADER),
                                allHwTypes, HardwareType.class,
                                values -> saveReference(device, values, allHwTypes, HardwareType.class));

    Set<ClientMetaData> allClients = clientService.findAllClientMetaData();
    refPresenter.showReference(members, mc.getMessage(UI_CLIENT_HEADER),
                                allClients, Client.class,
                                values -> saveReference(device, values, allClients, Client.class));

    return referencesPanel;
  }

  @Override
  protected Device newProfile() {
    return new Device();
  }

  @Override
  public Device getFreshProfile(String name) {
     return deviceService.findByName(name);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected <D extends DirectoryObject> Set<D> getMembers(Device profile, Class<D> clazz) {
    return (Set<D>)profile.getMembers();
  }

  @Override
  public void save(Device profile) {
    LOGGER.info("Save: " + profile);
    deviceService.save(profile);
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

  @Override
  public void selectItem(DirectoryObject directoryObject) {
    LOGGER.info("sideBar: "+ deviceSideBar);
    deviceSideBar.selectItem(NAME, directoryObject, getAllItems());
  }
}
