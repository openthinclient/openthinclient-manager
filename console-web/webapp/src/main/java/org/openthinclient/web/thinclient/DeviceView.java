package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = DeviceView.NAME)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT,  captionCode="UI_DEVICE_HEADER", order = 91)
@ThemeIcon("icon/display.svg")
public final class DeviceView extends ThinclientView {

  private static final Logger LOGGER = LoggerFactory.getLogger(DeviceView.class);

  public static final String NAME = "device_view";

  @Autowired
  private DeviceService deviceService;
  @Autowired
  private HardwareTypeService hardwareTypeService;
  @Autowired
  private ClientService clientService;
  @Autowired
  private SchemaProvider schemaProvider;


   private final IMessageConveyor mc;
   private ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();

   public DeviceView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
     super(UI_DEVICE_HEADER, eventBus, notificationService);
     mc = new MessageConveyor(UI.getCurrent().getLocale());

     showCreateApplicationAction();
     showCreateDeviceAction();
     showCreateLocationAction();
     showCreateUserAction();
     showCreatePrinterAction();

   }

   @PostConstruct
   private void setup() {
     setItems(getAllItems());
   }

  @Override
  public HashSet getAllItems() {
    return (HashSet) deviceService.findAll();
  }

  @Override
  public Schema getSchema(String schemaName) {
    return schemaProvider.getSchema(Device.class, schemaName);
  }

  @Override
  public String[] getSchemaNames() {
    return schemaProvider.getSchemaNames(Device.class);
  }

  public ProfilePanel createProfilePanel(DirectoryObject directoryObject) {

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

    Set members = ((Device) profile).getMembers();
    showReference(profile, profilePanel, members, mc.getMessage(UI_CLIENT_HEADER), clientService.findAll(), Client.class);
    showReference(profile, profilePanel, members, mc.getMessage(UI_HWTYPE_HEADER), hardwareTypeService.findAll(), HardwareType.class);

    return profilePanel;
  }

  @Override
  public <T extends DirectoryObject> T getFreshProfile(String name) {
     return (T) deviceService.findByName(name);
  }

  @Override
  public void save(DirectoryObject profile) {
    deviceService.save((Device) profile);
  }

}
