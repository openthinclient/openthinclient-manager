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

       Device device = ((Device) profile);
       showReference(profile, profilePanel, device.getMembers(), mc.getMessage(UI_CLIENT_HEADER), clientService.findAll(), Client.class);
       showReference(profile, profilePanel, device.getMembers(), mc.getMessage(UI_HWTYPE_HEADER), hardwareTypeService.findAll(), HardwareType.class);

      return profilePanel;
    }



  @Override
  public <T extends Profile> T getFreshProfile(T profile) {
     return (T) deviceService.findByName(profile.getName());
  }

  @Override
  public void save(Profile profile) {
    deviceService.save((Device) profile);
  }


}
