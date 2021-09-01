package org.openthinclient.web.thinclient;

import com.vaadin.spring.annotation.SpringView;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.service.*;
import org.openthinclient.web.Audit;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.presenter.ProfilePanelPresenter;
import org.openthinclient.web.thinclient.presenter.ReferencePanelPresenter;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.openthinclient.web.ui.ManagerUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.annotation.SideBarItem;
import org.vaadin.spring.sidebar.annotation.ThemeIcon;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = ApplicationView.NAME, ui= ManagerUI.class)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT,  captionCode="UI_APPLICATION_HEADER", order = 30)
@ThemeIcon(ApplicationView.ICON)
public final class ApplicationView extends AbstractProfileView<Application> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationView.class);

  public static final String NAME = "application_view";
  public static final String ICON = "icon/application.svg";
  public static final ConsoleWebMessages TITLE_KEY = UI_APPLICATION_HEADER;

  @Autowired
  private ApplicationGroupView applicationGroupView;
  @Autowired
  private ApplicationService applicationService;
  @Autowired
  private UserGroupService userGroupService;
  @Autowired
  private ClientService clientService;
  @Autowired
  private UserService userService;
  @Autowired
  private ApplicationGroupService applicationGroupService;

  @PostConstruct
  public void setup() {
    addStyleName(NAME);
  }

  @Override
  public Set<Application> getAllItems() {
    try {
     return applicationService.findAll();
    } catch (Exception e) {
      LOGGER.warn("Cannot find directory-objects: " + e.getMessage());
      showError(e);
    }
    return Collections.emptySet();
   }

   @Override
   protected Class<Application> getItemClass() {
     return Application.class;
   }

  @Override
   public ProfilePanel createProfilePanel(Application profile) throws BuildProfileException {
     Map<String, String> schemaNames = getSchemaNames();

     List<OtcPropertyGroup> otcPropertyGroups = builder.getOtcPropertyGroups(schemaNames, profile);

     OtcPropertyGroup meta = otcPropertyGroups.get(0);
     addProfileNameAlreadyExistsValidator(meta);
     String type = meta.getProperty("type").get().getConfiguration().getValue();

     ProfilePanel profilePanel = new ProfilePanel(profile.getName(),
                                                  schemaNames.getOrDefault(type, type),
                                                  Application.class);
     ProfilePanelPresenter presenter = new ProfilePanelPresenter(this, profilePanel, profile);

     // put properties to panel
     presenter.setItemGroups(otcPropertyGroups);
     presenter.onValuesWritten(profilePanel1 -> saveValues(presenter, profile));

     return profilePanel;
  }

  @Override
  public ProfileReferencesPanel createReferencesPanel(Application profile) {

    ProfileReferencesPanel referencesPanel = new ProfileReferencesPanel(Application.class);
    ReferencePanelPresenter refPresenter = new ReferencePanelPresenter(referencesPanel);

    Set<DirectoryObject> members = profile.getMembers();

    Set<ApplicationGroup> allApplicationGroups = applicationGroupService.findAll();
    refPresenter.showReference(members, ApplicationGroup.class,
                                mc.getMessage(UI_APPLICATIONGROUP_HEADER),
                                allApplicationGroups,
                                values -> saveApplicationGroupReference(profile, values),
                                getApplicationsForApplicationGroupFunction()
     );


    Set<User> allUsers = userService.findAll();
    getRealmService().findAllRealms().forEach(realm ->
      allUsers.removeAll(realm.getAdministrators().getMembers())
    );
    refPresenter.showReference(members, User.class,
                                mc.getMessage(UI_USER_HEADER),
                                allUsers,
                                values -> saveReference(profile, values, allUsers, User.class));

    Set<UserGroup> userGroups = userGroupService.findAll();
    refPresenter.showReference(members, UserGroup.class,
                                mc.getMessage(UI_USERGROUP_HEADER),
                                userGroups,
                                values -> saveReference(profile, values, userGroups, UserGroup.class));

    Set<ClientMetaData> allClients = clientService.findAllClientMetaData();
    refPresenter.showReference(members, Client.class,
                                mc.getMessage(UI_CLIENT_HEADER),
                                allClients,
                                values -> saveReference(profile, values, allClients, Client.class));

    return referencesPanel;
  }

  /**
   * Save application-group application-assignments
   * @param application Application to save
   * @param values ApplicationGroup selections
   */
  private void saveApplicationGroupReference(Application application, List<Item> values) {

    Set<DirectoryObject> oldValues = applicationService.findByName(application.getName()).getMembers();
    LOGGER.debug("Old application-groups: {}", oldValues);

    oldValues.stream().filter(directoryObject -> directoryObject instanceof ApplicationGroup).forEach(oldItem -> {
      if (values.stream().anyMatch(a -> a.getName().equals(oldItem.getName()))) {
        LOGGER.info("Keep oldValue as member: " + oldItem);
      } else {
        LOGGER.info("Remove oldValue from application: " + oldItem);
        if (application.getMembers().contains(oldItem)) {
          application.getMembers().remove(oldItem);
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
        }
      } else {
        LOGGER.info("ApplicationGroup not found for " + newValue);
      }
    });
    applicationService.save(application);
  }

  @Override
  protected Application newProfile() {
    return new Application();
  }

  @Override
  public Application getFreshProfile(String profileName) {
     return applicationService.findByName(profileName);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected <D extends DirectoryObject> Set<D> getMembers(Application profile, Class<D> clazz) {
    return (Set<D>)profile.getMembers();
  }

  @Override
  public void save(Application profile) {
    LOGGER.info("Save: " + profile);
    applicationService.save(profile);
    Audit.logSave(profile);
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
        return applicationGroup.getApplications().stream()
          .map(m -> new Item(m.getName(), Item.Type.APPLICATION))
          .collect(Collectors.toList());
      } else {
        return new ArrayList<>();
      }
    };
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
  public void showOverview() {
    super.showOverview();
    overviewCL.addComponent(
      applicationGroupView.createOverviewItemlistPanel(applicationGroupView.getViewTitleKey(), applicationGroupView.getAllItems(), true).getPanel()
    );
  }
}
