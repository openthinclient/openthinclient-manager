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
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.property.*;
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
@SpringView(name = ApplicationView.NAME)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT,  captionCode="UI_APPLICATION_HEADER", order = 89)
@ThemeIcon("icon/packages.svg")
public final class ApplicationView extends ThinclientView {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationView.class);

  public static final String NAME = "application_view";

  @Autowired
  private ApplicationService applicationService;
  @Autowired
  private ClientService clientService;
  @Autowired
  private UserService userService;
  @Autowired
  private ApplicationGroupService applicationGroupService;
  @Autowired
  private SchemaProvider schemaProvider;

   private final IMessageConveyor mc;

   public ApplicationView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
     super(UI_APPLICATION_HEADER, eventBus, notificationService);
     mc = new MessageConveyor(UI.getCurrent().getLocale());

     showCreateApplicationAction();
     showCreateDeviceAction();
     showCreateLocationAction();
     showCreateUserAction();
     showCreateHardwareTypeAction();
   }

  @PostConstruct
  private void setup() {
      setItems(getAllItems());
   }

  @Override
  public HashSet getAllItems() {
     return (HashSet) applicationService.findAll();
   }

  @Override
  public Schema getSchema(String schemaName) {
    return schemaProvider.getSchema(Application.class, schemaName);
  }

  @Override
  public String[] getSchemaNames() {
    return schemaProvider.getSchemaNames(Application.class);
  }

  @Override
   public ProfilePanel createProfilePanel(Profile profile) {

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

       // put properties to panel
       profilePanel.setItemGroups(otcPropertyGroups);

       // add references
       Set<DirectoryObject> members = ((Application) profile).getMembers();
       showReference(profile, profilePanel, members, mc.getMessage(UI_CLIENT_HEADER), clientService.findAll(), Client.class);
       showReference(profile, profilePanel, members, mc.getMessage(UI_APPLICATIONGROUP_HEADER), applicationGroupService.findAll(), ApplicationGroup.class);
       showReference(profile, profilePanel, members, mc.getMessage(UI_USER_HEADER), userService.findAll(), User.class);

       return profilePanel;
    }

  @Override
  public <T extends Profile> T getFreshProfile(T profile) {
     return (T) applicationService.findByName(profile.getName());
  }

  @Override
  public void save(Profile profile) {
    applicationService.save((Application) profile);
  }

  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {
    if (event.getParameters() != null) {
      // split at "/", add each part as a label
      String[] params = event.getParameters().split("/");
      if (params.length == 1 && params.equals("create")) {
        showProfileMetaData(new Application());
      }
    }
  }

}
