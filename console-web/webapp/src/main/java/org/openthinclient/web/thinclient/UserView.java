package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.LocationService;
import org.openthinclient.common.model.service.PrinterService;
import org.openthinclient.common.model.service.UserService;
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
@SpringView(name = UserView.NAME)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT, captionCode="UI_USER_HEADER", order = 91)
@ThemeIcon("icon/user.svg")
public final class UserView extends ThinclientView {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserView.class);

  public static final String NAME = "user_view";

  @Autowired
  private PrinterService printerService;
  @Autowired
  private UserService userService;
  @Autowired
  private ClientService clientService;
  @Autowired
  private LocationService locationService;
  @Autowired
  private SchemaProvider schemaProvider;

   private final IMessageConveyor mc;
   private VerticalLayout right;
   private ProfilePropertiesBuilder builder = new ProfilePropertiesBuilder();

   public UserView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
     super(UI_PRINTER_HEADER, eventBus, notificationService);
     mc = new MessageConveyor(UI.getCurrent().getLocale());

     showCreateUserAction();
   }


   @PostConstruct
   private void setup() {
     setItems(getAllItems());
   }

  @Override
  public HashSet getAllItems() {
    return (HashSet) userService.findAll();
  }

  @Override
  public Schema getSchema(String schemaName) {
    return schemaProvider.getSchema(User.class, schemaName);
  }

  @Override
  public String[] getSchemaNames() {
    return schemaProvider.getSchemaNames(User.class);
  }

  public ProfilePanel createProfilePanel (Profile profile) {

       ProfilePanel profilePanel = new ProfilePanel(profile.getName(), profile.getClass());

       List<OtcPropertyGroup> otcPropertyGroups = null;
       try {
         otcPropertyGroups = builder.getOtcPropertyGroups(getSchemaNames(), profile);
       } catch (BuildProfileException e) {
         showError(e);
         return null;
       }

       // attach save-action
       otcPropertyGroups.forEach(group -> group.setValueWrittenHandlerToAll(ipg -> saveValues(ipg, profile)));
       // put to panel
       profilePanel.setItemGroups(otcPropertyGroups);

       // TODO: show members
//       Set<DirectoryObject> members = ((User) profile).getMembers();
//       showReference(profile, profilePanel, members, mc.getMessage(UI_CLIENT_HEADER), clientService.findAll(), Client.class);
//       showReference(profile, profilePanel, members, mc.getMessage(UI_LOCATION_HEADER), locationService.findAll(), Location.class);
//       showReference(profile, profilePanel, members, mc.getMessage(UI_USER_HEADER), userService.findAll(), User.class);

    return profilePanel;
    }

  @Override
  public <T extends Profile> T getFreshProfile(String name) {

//     return (T) userService.findByName(profile.getName());
    return null;
  }

  @Override
  public void save(Profile profile) {
     // TODO: implement save
//     userService.save((User) profile);
  }

}
