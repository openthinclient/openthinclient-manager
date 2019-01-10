package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.UI;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.*;
import org.openthinclient.service.common.home.ManagerHome;
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
import java.util.*;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = HardwaretypeView.NAME)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT,  captionCode="UI_HWTYPE_HEADER", order = 93)
@ThemeIcon("icon/drive.svg")
public final class HardwaretypeView extends ThinclientView {

  private static final Logger LOGGER = LoggerFactory.getLogger(HardwaretypeView.class);

  public static final String NAME = "hardwaretype_view";

  @Autowired
  private DeviceService deviceService;
  @Autowired
  private HardwareTypeService hardwareTypeService;
  @Autowired
  private SchemaProvider schemaProvider;

   private final IMessageConveyor mc;
   private ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();

   public HardwaretypeView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
     super(UI_HWTYPE_HEADER, eventBus, notificationService);
     mc = new MessageConveyor(UI.getCurrent().getLocale());

     showCreateHardwareTypeAction();
     showCreateDeviceAction();
     showCreateLocationAction();
     showCreatePrinterAction();
   }


   @PostConstruct
   private void setup() {
     setItems(getAllItems());
   }

  @Override
  public HashSet getAllItems() {
    return (HashSet) hardwareTypeService.findAll();
  }

  @Override
  public Schema getSchema(String schemaName) {
    return schemaProvider.getSchema(HardwareType.class, schemaName);
  }

  @Override
  public String[] getSchemaNames() {
    return schemaProvider.getSchemaNames(HardwareType.class);
  }

  public ProfilePanel createProfilePanel (DirectoryObject directoryObject) {

    Profile profile = (Profile) directoryObject;

    List<OtcPropertyGroup> otcPropertyGroups;
    try {
      otcPropertyGroups = builder.getOtcPropertyGroups(getSchemaNames(), profile);
    } catch (BuildProfileException e) {
      showError(e);
      return null;
    }

    OtcPropertyGroup meta = otcPropertyGroups.get(0);
    String type = meta.getProperty("type").get().getConfiguration().getValue();

    ProfilePanel profilePanel = new ProfilePanel(profile.getName() + " (" + type + ")", profile.getClass());
    ProfilePanelPresenter presenter = new ProfilePanelPresenter(this, profilePanel, profile);

    // set MetaInformation
    presenter.setPanelMetaInformation(createDefaultMetaInformationComponents(profile));

    // attach save-action
    otcPropertyGroups.forEach(group -> group.setValueWrittenHandlerToAll(ipg -> saveValues(ipg, profile)));

    // put to panel
    profilePanel.setItemGroups(otcPropertyGroups);

    HardwareType hardwareType = (HardwareType) profile;
    Set<? extends DirectoryObject> members = hardwareType.getMembers();
    showReference(profile, profilePanel, members, mc.getMessage(UI_CLIENT_HEADER) + " (readonly)", Collections.emptySet(), Client.class);

    Map<Class, Set<? extends DirectoryObject>> associatedObjects = hardwareType.getAssociatedObjects();
    Set<? extends DirectoryObject> devices = associatedObjects.get(Device.class);
    showDeviceAssociations(deviceService.findAll(), hardwareType, profilePanel, devices);

    return profilePanel;
  }

  @Override
  public <T extends DirectoryObject> T getFreshProfile(String name) {
     return (T) hardwareTypeService.findByName(name);
  }

  @Override
  public void save(DirectoryObject profile) {
    hardwareTypeService.save((HardwareType) profile);
  }

}