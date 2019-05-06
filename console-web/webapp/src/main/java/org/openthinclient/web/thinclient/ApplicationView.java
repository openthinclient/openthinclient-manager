package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.UI;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.ApplicationGroupService;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.UserService;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.model.Item;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = ApplicationView.NAME)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT,  captionCode="UI_APPLICATION_HEADER", order = 30)
@ThemeIcon("icon/packages-white.svg")
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
      // setItems(getAllItems());
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
   public ProfilePanel createProfilePanel(DirectoryObject directoryObject) throws BuildProfileException {

     Profile profile = (Profile) directoryObject;

     List<OtcPropertyGroup> otcPropertyGroups = builder.getOtcPropertyGroups(getSchemaNames(), profile);

     OtcPropertyGroup meta = otcPropertyGroups.get(0);
     String type = meta.getProperty("type").get().getConfiguration().getValue();

     ProfilePanel profilePanel = new ProfilePanel(profile.getName() + " (" + type + ")", profile.getClass());
     ProfilePanelPresenter presenter = new ProfilePanelPresenter(this, profilePanel, profile);

     // set MetaInformation
//     presenter.setPanelMetaInformation(createDefaultMetaInformationComponents(profile));

     // attach save-action
//     otcPropertyGroups.forEach(group -> group.setValueWrittenHandlerToAll(ipg -> saveValues(presenter, profile)));

     // put properties to panel
     presenter.setItemGroups(otcPropertyGroups);
     presenter.onValuesWritten(profilePanel1 -> saveValues(presenter, profile));

     // add references
//     Set<DirectoryObject> members = ((Application) profile).getMembers();
//     showReference(profile, profilePanel, members, mc.getMessage(UI_CLIENT_HEADER), clientService.findAll(), Client.class);
//     showReference(profile, profilePanel, members, mc.getMessage(UI_USER_HEADER), userService.findAll(), User.class);
//     // application with sub-groups
//     Set<ApplicationGroup> allApplicationGroups = applicationGroupService.findAll();
//     Set<ApplicationGroup> applicationGroupsByApplication = allApplicationGroups.stream().filter(ag -> ag.getApplications().contains(profile)).collect(Collectors.toSet());
//     showReference(profilePanel, applicationGroupsByApplication, mc.getMessage(UI_APPLICATIONGROUP_HEADER),
//        allApplicationGroups, ApplicationGroup.class,
//        values -> saveApplicationGroupReference(((Application) profile), values),
//        getApplicationsForApplicationGroupFunction(), false
//     );

     return profilePanel;
  }

  /**
   * Save application-group application-assignments
   * @param application Application to save
   * @param values ApplicationGroup selections
   */
  private void saveApplicationGroupReference(Application application, List<Item> values) {

    Set<DirectoryObject> oldValues = applicationService.findByName(application.getName()).getMembers();
    LOGGER.debug("Old application-groups: {}", oldValues);

    oldValues.forEach(oldItem -> {
      if (values.stream().anyMatch(a -> a.getName().equals(oldItem.getName()))) {
        LOGGER.info("Keep oldValue as member: " + oldItem);
      } else {
        LOGGER.info("Remove oldValue from application: " + oldItem);
        if (application.getMembers().contains(oldItem)) {
          application.getMembers().remove(oldItem);
          applicationService.save(application);
        } else {
          LOGGER.info("ApplicationGroup (to remove) not found in members of " + oldItem);
        }
      }
    });

    values.forEach(newValue -> {
      ApplicationGroup applicationGroup1 = applicationGroupService.findByName(newValue.getName());
      if (applicationGroup1 != null) {
        if (!oldValues.contains(applicationGroup1)) {
          LOGGER.info("Add ApplicationGroup {} as member of {}", applicationGroup1.getName(), application);
          application.getMembers().add(applicationGroup1);
          applicationService.save(application);
        }
      } else {
        LOGGER.info("ApplicationGroup not found for " + newValue);
      }
    });

  }

  @Override
  public <T extends DirectoryObject> T getFreshProfile(String profileName) {
     return (T) applicationService.findByName(profileName);
  }

  @Override
  public void save(DirectoryObject profile) {
    LOGGER.info("Save: " + profile);
    applicationService.save((Application) profile);
  }

  /**
   * Supplier for ApplicationGroup Members of given client and supplied item as ApplicationGroup
   * @return List of members mapped to Item-list or empty list
   */
  private Function<Item, List<Item>> getApplicationsForApplicationGroupFunction() {
    return appGroup -> {
      Optional<ApplicationGroup> first = applicationGroupService.findAll().stream().filter(ag -> ag.getName().equals(appGroup.getName())).findFirst();
      if (first.isPresent()) {
        ApplicationGroup applicationGroup =  first.get();
        LOGGER.info("ApplicationGroup {} with applications {} loaded.", applicationGroup.getName(), applicationGroup.getApplications());
        Stream<? extends DirectoryObject> stream = applicationGroup.getApplications().stream()
            .sorted(Comparator.comparing(DirectoryObject::getName, String::compareToIgnoreCase));
        return stream.map(m -> new Item(m.getName(), Item.Type.APPLICATION)).collect(Collectors.toList());
      } else {
        return new ArrayList<>();
      }
    };
  }

  @Override
  public String getViewName() {
    return NAME;
  }
}
