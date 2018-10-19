package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.UI;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.*;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
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
import java.util.Map;
import java.util.Set;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = HardwaretypeView.NAME)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT,  captionCode="UI_HWTYPE_HEADER", order = 93)
@ThemeIcon("icon/drive.svg")
public final class HardwaretypeView extends ThinclientView {

  private static final Logger LOGGER = LoggerFactory.getLogger(HardwaretypeView.class);

  public static final String NAME = "hartdwaretype_view";

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

  public ProfilePanel createProfilePanel (Profile profile) {

       ProfilePanel profilePanel = new ProfilePanel(profile.getName(), profile.getClass());

       List<OtcPropertyGroup> otcPropertyGroups = null;
       try {
         otcPropertyGroups = builder.getOtcPropertyGroups(profile);
       } catch (BuildProfileException e) {
         showError(e);
         return null;
       }

       // attach save-action
       otcPropertyGroups.forEach(group -> group.setValueWrittenHandlerToAll(ipg -> saveValues(ipg, profile)));

       // put to panel
       profilePanel.setItemGroups(otcPropertyGroups);

       HardwareType hardwareType = (HardwareType) profile;
       Set<? extends DirectoryObject> members = hardwareType.getMembers();
       // TODO: Feature oder Bug: Hardwaretypen sind kaputt
       showReference(profile, profilePanel, members, mc.getMessage(UI_CLIENT_HEADER) + "(KAPUTT - readonly)", clientService.findAll(), Client.class);

       Map<Class, Set<? extends DirectoryObject>> associatedObjects = hardwareType.getAssociatedObjects();
       Set<? extends DirectoryObject> devices = associatedObjects.get(Device.class);
       showDeviceAssociations(deviceService.findAll(), hardwareType, profilePanel, devices);

    return profilePanel;
    }



  @Override
  public <T extends Profile> T getFreshProfile(T profile) {
     return (T) hardwareTypeService.findByName(profile.getName());
  }

  @Override
  public void save(Profile profile) {
    hardwareTypeService.save((HardwareType) profile);
  }


}
