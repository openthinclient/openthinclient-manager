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
@SpringView(name = HardwaretypeView.NAME, ui= ManagerUI.class)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT,  captionCode="UI_HWTYPE_HEADER", order = 70)
@ThemeIcon(HardwaretypeView.ICON)
public final class HardwaretypeView extends AbstractThinclientView<HardwareType> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractThinclientView.class);

  public static final String NAME = "hardwaretype_view";
  public static final String ICON = "icon/hardwaretype.svg";
  public static final ConsoleWebMessages TITLE_KEY = UI_HWTYPE_HEADER;

  @Autowired
  private DeviceService deviceService;
  @Autowired
  private HardwareTypeService hardwareTypeService;
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
  public Set<HardwareType> getAllItems() {
    try {
      return hardwareTypeService.findAll();
    } catch (Exception e) {
      LOGGER.warn("Cannot find directory-objects: " + e.getMessage());
      showError(e);
    }
    return Collections.emptySet();
  }

  @Override
  public Schema getSchema(String schemaName) {
    return schemaProvider.getSchema(HardwareType.class, schemaName);
  }

  @Override
  public Map<String, String> getSchemaNames() {
    return Stream.of(schemaProvider.getSchemaNames(HardwareType.class))
                 .collect( Collectors.toMap(schemaName -> schemaName, schemaName -> getSchema(schemaName).getLabel()));
  }

  public ProfilePanel createProfilePanel(HardwareType profile) throws BuildProfileException {
    List<OtcPropertyGroup> otcPropertyGroups = builder.getOtcPropertyGroups(getSchemaNames(), profile);

    OtcPropertyGroup meta = otcPropertyGroups.get(0);
    addProfileNameAlreadyExistsValidator(meta);

    ProfilePanel       profilePanel = new ProfilePanel(profile.getName(), profile.getClass());
    ProfilePanelPresenter presenter = new ProfilePanelPresenter(this, profilePanel, profile);
    presenter.setDeleteMandate(createDeleteMandateFunction());

    // put to panel
    presenter.setItemGroups(otcPropertyGroups);
    presenter.onValuesWritten(profilePanel1 -> saveValues(presenter, profile));

    return profilePanel;
  }

  public ProfileReferencesPanel createReferencesPanel(HardwareType hardwareType) {
    ProfileReferencesPanel referencesPanel = new ProfileReferencesPanel(HardwareType.class);
    ReferencePanelPresenter   refPresenter = new ReferencePanelPresenter(referencesPanel);

    Set<Device> allDevices = deviceService.findAll();
    refPresenter.showReference(hardwareType.getDevices(), mc.getMessage(UI_DEVICE_HEADER),
                                allDevices, Device.class,
                                values -> saveReference(hardwareType, values, allDevices, Device.class));

    refPresenter.showReferenceReadOnly(hardwareType.getMembers(), mc.getMessage(UI_CLIENT_HEADER),
                                        Client.class);

    return referencesPanel;
  }

  @Override
  protected Function<DirectoryObject, DeleteMandate> createDeleteMandateFunction() {
    return directoryObject -> {
      HardwareType hwtype = (HardwareType) directoryObject;
      if (hwtype.getMembers().size() > 0) {
        return new DeleteMandate(false, mc.getMessage(UI_COMMON_DELETE_HWTYPE_DENIED, hwtype.getName()));
      }
      return new DeleteMandate(true, "");
    };
  }

  @Override
  protected HardwareType newProfile() {
    return new HardwareType();
  }

  @Override
  public HardwareType getFreshProfile(String name) {
     return hardwareTypeService.findByName(name);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected <D extends DirectoryObject> Set<D> getMembers(HardwareType profile, Class<D> clazz) {
    if (clazz == Device.class ) {
      return (Set<D>)profile.getDevices();
    } else {
      return (Set<D>)profile.getMembers();
    }
  }

  @Override
  public void save(HardwareType profile) {
    LOGGER.info("Save: " + profile);
    hardwareTypeService.save(profile);
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
    deviceSideBar.selectItem(NAME, directoryObject, getAllItems());
  }

}
