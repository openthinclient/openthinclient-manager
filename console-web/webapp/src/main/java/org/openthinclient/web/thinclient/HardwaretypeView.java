package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.UI;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.*;
import org.openthinclient.web.OTCSideBar;
import org.openthinclient.web.dashboard.DashboardNotificationService;
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
import org.vaadin.spring.events.EventBus;
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
public final class HardwaretypeView extends AbstractThinclientView {

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

   private final IMessageConveyor mc;
   private ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();

   public HardwaretypeView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
     super(UI_HWTYPE_HEADER, eventBus, notificationService);
     mc = new MessageConveyor(UI.getCurrent().getLocale());
   }


   @PostConstruct
   private void setup() {
     addStyleName(NAME);
     addCreateActionButton(mc.getMessage(UI_THINCLIENT_ADD_HWTYPE_LABEL), ICON, NAME + "/create");
   }

  @Override
  public Set getAllItems() {
    try {
      return hardwareTypeService.findAll();
    } catch (Exception e) {
      LOGGER.warn("Cannot find directory-objects: " + e.getMessage());
      showError(e);
    }
    return Collections.EMPTY_SET;
  }

  @Override
  public Schema getSchema(String schemaName) {
    return schemaProvider.getSchema(HardwareType.class, schemaName);
  }

  @Override
  public Client getClient(String name) {
    return null;
  }

  @Override
  public Map<String, String> getSchemaNames() {
    return Stream.of(schemaProvider.getSchemaNames(HardwareType.class))
                 .collect( Collectors.toMap(schemaName -> schemaName, schemaName -> getSchema(schemaName).getLabel()));
  }

  public ProfilePanel createProfilePanel(DirectoryObject directoryObject) throws BuildProfileException {

    Profile profile = (Profile) directoryObject;

    List<OtcPropertyGroup> otcPropertyGroups = builder.getOtcPropertyGroups(getSchemaNames(), profile);

    OtcPropertyGroup meta = otcPropertyGroups.get(0);
    addProfileNameAlreadyExistsValidator(meta);

    ProfilePanel profilePanel = new ProfilePanel(profile.getName(), profile.getClass());
    ProfilePanelPresenter presenter = new ProfilePanelPresenter(this, profilePanel, profile);
    presenter.setDeleteMandate(createDeleteMandateFunction());

    // set MetaInformation
//    presenter.setPanelMetaInformation(createDefaultMetaInformationComponents(profile));
//    ProfilePanel panel = createProfileMetadataPanel(profile);


    // attach save-action
//    otcPropertyGroups.forEach(group -> group.setValueWrittenHandlerToAll(ipg -> saveValues(presenter, profile)));
    // put to panel
    presenter.setItemGroups(otcPropertyGroups);
    presenter.onValuesWritten(profilePanel1 -> saveValues(presenter, profile));

//    HardwareType hardwareType = (HardwareType) profile;
//    Set<? extends DirectoryObject> members = hardwareType.getMembers();
//    showReference(profilePanel, members, mc.getMessage(UI_CLIENT_HEADER) + " (readonly)", Collections.emptySet(), Client.class, values -> saveReference(profile, values, Collections.emptySet(), Client.class),null, true);
//
//    Map<Class, Set<? extends DirectoryObject>> associatedObjects = hardwareType.getAssociatedObjects();
//    Set<? extends DirectoryObject> devices = associatedObjects.get(Device.class);
//    showDeviceAssociations(deviceService.findAll(), hardwareType, profilePanel, devices);

    return profilePanel;
  }

  public ProfileReferencesPanel createReferencesPanel(DirectoryObject directoryObject) {
    HardwareType hardwareType = (HardwareType) directoryObject;

    ProfileReferencesPanel referencesPanel = new ProfileReferencesPanel(HardwareType.class);
    ReferencePanelPresenter   refPresenter = new ReferencePanelPresenter(referencesPanel);

    Set<? extends DirectoryObject> members = hardwareType.getMembers();
    refPresenter.showReference(members, mc.getMessage(UI_CLIENT_HEADER) + " (readonly)",
                                Collections.emptySet(), Client.class,
                                values -> saveReference(hardwareType, values, Collections.emptySet(), Client.class),
                                null, true);

    Map<Class, Set<? extends DirectoryObject>> associatedObjects = hardwareType.getAssociatedObjects();
    Set<? extends DirectoryObject> devices = associatedObjects.get(Device.class);
    Set<Device> all = deviceService.findAll();
    refPresenter.showDeviceAssociations(all, devices,
                                        values -> saveAssociations(hardwareType, values, all, Device.class));

    return referencesPanel;
  }

  private Function<DirectoryObject, DeleteMandate> createDeleteMandateFunction() {
    return directoryObject -> {
      HardwareType hwtype = (HardwareType) directoryObject;
      if (hwtype.getMembers().size() > 0) {
        return new DeleteMandate(false, mc.getMessage(UI_COMMON_DELETE_HWTYPE_DENIED));
      }
      return new DeleteMandate(true, "");
    };
  }


  @Override
  public <T extends DirectoryObject> T getFreshProfile(String name) {
     return (T) hardwareTypeService.findByName(name);
  }

  @Override
  public void save(DirectoryObject profile) {
    LOGGER.info("Save: " + profile);
    hardwareTypeService.save((HardwareType) profile);
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
