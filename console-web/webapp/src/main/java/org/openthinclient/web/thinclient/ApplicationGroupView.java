package org.openthinclient.web.thinclient;

import com.vaadin.spring.annotation.SpringView;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.*;
import org.openthinclient.web.Audit;
import org.openthinclient.web.OTCSideBar;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.presenter.ReferencePanelPresenter;
import org.openthinclient.web.ui.ManagerUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.vaadin.spring.sidebar.annotation.ThemeIcon;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = ApplicationGroupView.NAME, ui= ManagerUI.class)
@ThemeIcon(ApplicationGroupView.ICON)
public final class ApplicationGroupView extends AbstractThinclientGroupView<ApplicationGroup> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationGroupView.class);

  public static final String NAME = "applicationgroup_view";
  public static final String ICON = "icon/applicationgroup-white.svg";
  public static final ConsoleWebMessages TITLE_KEY = UI_APPLICATIONGROUP_HEADER;

  @Autowired
  private ClientService clientService;
  @Autowired
  private ApplicationGroupService applicationGroupService;
  @Autowired
  private ApplicationService applicationService;
  @Autowired
  private UserService userService;
  @Autowired
  private UserGroupService userGroupService;
  @Autowired
  private SchemaProvider schemaProvider;
  @Autowired @Qualifier("deviceSideBar")
  private OTCSideBar deviceSideBar;

  @PostConstruct
  public void setup() {
    addStyleName(ApplicationView.NAME);
  }

  @Override
  public Set<ApplicationGroup> getAllItems() {
    return applicationGroupService.findAll();
  }

  @Override
  public Schema getSchema(String schemaName) {
    return schemaProvider.getSchema(ApplicationGroup.class, schemaName);
  }

  @Override
  public Map<String, String> getSchemaNames() {
    return Stream.of(schemaProvider.getSchemaNames(ApplicationGroup.class))
                 .collect( Collectors.toMap(schemaName -> schemaName, schemaName -> getSchema(schemaName).getLabel()));
  }

  @Override
  public ProfileReferencesPanel createReferencesPanel(ApplicationGroup applicationGroup) {
    Set<DirectoryObject> members = applicationGroup.getMembers();

    ProfileReferencesPanel referencesPanel = new ProfileReferencesPanel(ApplicationGroup.class);
    ReferencePanelPresenter refPresenter = new ReferencePanelPresenter(referencesPanel);

    Set<Application> allApplications = applicationService.findAll();
    refPresenter.showReference(applicationGroup.getApplications(), mc.getMessage(UI_APPLICATION_HEADER),
                                allApplications, Application.class,
                                values -> saveApplicationGroupReference(applicationGroup, values), null, false);

    Set<ClientMetaData> allClients = clientService.findAllClientMetaData();
    refPresenter.showReference(members, mc.getMessage(UI_CLIENT_HEADER),
                                allClients, Client.class,
                                values -> saveReference(applicationGroup, values, allClients, Client.class));

    Set<User> allUsers = userService.findAll();
    getRealmService().findAllRealms().forEach(realm ->
      allUsers.removeAll(realm.getAdministrators().getMembers())
    );
    refPresenter.showReference(members, mc.getMessage(UI_USER_HEADER),
                                allUsers, User.class,
                                values -> saveReference(applicationGroup, values, allUsers, User.class));

    Set<UserGroup> allUserGroups = userGroupService.findAll();
    refPresenter.showReference(members, mc.getMessage(UI_USERGROUP_HEADER),
                                allUserGroups, UserGroup.class,
                                values -> saveReference(applicationGroup, values, allUserGroups, UserGroup.class));

    return referencesPanel;
  }

  /**
   * Save application-group assignments (off applications) at application-directory-object (NOT at application-group-object)
   */
  private void saveApplicationGroupReference(ApplicationGroup applicationGroup, List<Item> values) {

    List<Application> oldValues = applicationService.findAll().stream().filter(application -> application.getMembers().contains(applicationGroup)).collect(Collectors.toList());
    LOGGER.debug(applicationGroup.getName() + " applications: {}", oldValues);

    oldValues.forEach(oldItem -> {
      if (values.stream().anyMatch(a -> a.getName().equals(oldItem.getName()))) {
        LOGGER.info("Keep oldValue as member: " + oldItem);
      } else {
        LOGGER.info("Remove oldValue from applicationGroup: " + oldItem);
        if (oldItem.getMembers().contains(applicationGroup)) {
          oldItem.getMembers().remove(applicationGroup);
          applicationService.save(oldItem);
        } else {
          LOGGER.info("ApplicationGroup (to remove) not found in members of " + oldItem);
        }
      }
    });

    values.forEach(newValue -> {
        Application application = applicationService.findByName(newValue.getName());
        if (application != null) {
           if (!oldValues.contains(application)) {
              LOGGER.info("Add ApplicationGroup to members of: " + newValue);
              applicationGroup.getApplications().add(application); // mandatory, otherwise it doesn't work
              application.getMembers().add(applicationGroup);
              applicationService.save(application);
          }
        } else {
          LOGGER.info("Application not found for " + newValue);
        }
    });

  }

  @Override
  protected ApplicationGroup newProfile() {
    return new ApplicationGroup();
  }

  @Override
  public ApplicationGroup getFreshProfile(String name) {
     return applicationGroupService.findByName(name);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected <D extends DirectoryObject> Set<D> getMembers(ApplicationGroup profile, Class<D> clazz) {
    if (clazz == Application.class) {
      return (Set<D>)profile.getApplications();
    } else {
      return (Set<D>)profile.getMembers();
    }
  }

  @Override
  public void save(ApplicationGroup profile) {
    LOGGER.info("Save: " + profile);
    applicationGroupService.save((ApplicationGroup) profile);
    Audit.logSave(profile);
  }

  @Override
  public String getViewName() {
    return NAME;
  }

  @Override
  public String getParentViewName() {
    return ApplicationView.NAME;
  }

  @Override
  public ConsoleWebMessages getViewTitleKey() {
    return TITLE_KEY;
  }

  @Override
  public void selectItem(DirectoryObject directoryObject) {
    LOGGER.info("sideBar: "+ deviceSideBar);
    deviceSideBar.selectItem(NAME, directoryObject, getAllItems());
  }
}
