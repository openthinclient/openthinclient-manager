package org.openthinclient.web.thinclient;

import com.vaadin.spring.annotation.SpringView;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.service.*;
import org.openthinclient.service.store.LDAPConnection;
import org.openthinclient.web.Audit;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.exception.ProfileNotDeletedException;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.presenter.ReferencePanelPresenter;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.openthinclient.web.ui.ManagerUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.annotation.SideBarItem;
import org.vaadin.spring.sidebar.annotation.ThemeIcon;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = UserView.NAME, ui= ManagerUI.class)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT, captionCode="UI_USER_HEADER", order = 40)
@ThemeIcon(UserView.ICON)
public final class UserView extends AbstractUserView {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserView.class);

  public static final String NAME = "user_view";
  public static final String ICON = "icon/user.svg";
  public static final ConsoleWebMessages TITLE_KEY = UI_USER_HEADER;

  @Autowired
  private UserGroupView userGroupView;
  @Autowired
  private PrinterService printerService;
  @Autowired
  private UserService userService;
  @Autowired
  private UserGroupService userGroupService;
  @Autowired
  private ApplicationGroupService applicationGroupService;
  @Autowired
  private ApplicationService applicationService;

  @PostConstruct
  private void setup() {
    isSecondaryDirectory = getRealmService().getDefaultRealm().isSecondaryConfigured();

    addStyleName(NAME);
  }

  @Override
  public Set<User> getAllItems() {
    try (LDAPConnection connection = new LDAPConnection()) {
      Collection<String> adminDNs = connection.loadAdminDNs();
      Set<User> users = userService.findAll();
      for (User user: users) {
        if (adminDNs.contains(user.getDn())) {
          user.setRole("admin");
        } else {
          user.setRole("user");
        }
      }
      return users;
    } catch (Exception ex) {
      LOGGER.error("Cannot find directory-objects: " + ex.getMessage(), ex);
      showError(ex);
    }
    return Collections.emptySet();
  }

  @Override
  public ProfileReferencesPanel createReferencesPanel(User user) {
    ProfileReferencesPanel referencesPanel = new ProfileReferencesPanel(User.class);
    ReferencePanelPresenter refPresenter = new ReferencePanelPresenter(referencesPanel);

    Set<UserGroup> userGroups = user.getUserGroups();

    Set<UserGroup> allUserGroups = userGroupService.findAll();
    Consumer<List<Item>> profileReferenceChangeConsumer = null;
    if(!isSecondaryDirectory) {
      profileReferenceChangeConsumer = values -> saveReference(user, values, allUserGroups, UserGroup.class);
    }
    refPresenter.showReference(userGroups,
                                mc.getMessage(UI_USERGROUP_HEADER),
                                allUserGroups,
                                profileReferenceChangeConsumer,
                                null);

    Set<Application> allApplications = applicationService.findAll();
    refPresenter.showReference(user.getApplications(),
                                mc.getMessage(UI_APPLICATION_HEADER),
                                allApplications,
                                values -> saveReference(user, values, allApplications, Application.class));

    Set<UserGroup> userGroupsWithApplications = userGroups.stream()
                                                .filter(group -> group.getApplications().size() > 0)
                                                .collect(Collectors.toSet());
    if (userGroupsWithApplications.size() > 0) {
      refPresenter.showReferenceAddendum(userGroupsWithApplications,
                                          mc.getMessage(UI_FROM_USERGROUP_HEADER),
                                          ApplicationsFromUserGroupFunction(user));
    }

    Set<ApplicationGroup> allApplicationGroups = applicationGroupService.findAll();
    refPresenter.showReference(user.getApplicationGroups(),
                                mc.getMessage(UI_APPLICATIONGROUP_HEADER),
                                allApplicationGroups,
                                values -> saveReference(user, values, allApplicationGroups, ApplicationGroup.class),
                                getApplicationsForApplicationGroupFunction(user));

    Set<UserGroup> userGroupsWithApplicationGroups = userGroups.stream()
                                                      .filter(group -> group.getApplicationGroups().size() > 0)
                                                      .collect(Collectors.toSet());
    if (userGroupsWithApplicationGroups.size() > 0) {
      Set<ApplicationGroup> appGroups = userGroupsWithApplicationGroups.stream()
                                        .flatMap(userGroup -> userGroup.getApplicationGroups().stream())
                                        .collect(Collectors.toSet());
      refPresenter.showReferenceAddendum(appGroups,
                                          mc.getMessage(UI_FROM_USERGROUP_HEADER),
                                          getApplicationsForUserGroupApplicationGroupFunction(user));
    }

    Set<Printer> allPrinters = printerService.findAll();
    refPresenter.showReference(user.getPrinters(),
                                mc.getMessage(UI_PRINTER_HEADER),
                                allPrinters,
                                values -> saveReference(user, values, allPrinters, Printer.class));

    Set<UserGroup> userGroupsWithPrinters = userGroups.stream()
                                              .filter(group -> group.getPrinters().size() > 0)
                                              .collect(Collectors.toSet());
    if (userGroupsWithPrinters.size() > 0) {
      refPresenter.showReferenceAddendum(userGroupsWithPrinters,
                                          mc.getMessage(UI_FROM_USERGROUP_HEADER),
                                          PrintersFromUserGroupFunction(user));
    }

    return referencesPanel;
  }

  private Function<Item, List<Item>> getApplicationsForApplicationGroupFunction(User user) {
    return item -> user.getApplicationGroups().stream()
                    .filter(group -> group.getName().equals(item.getName()))
                    .findFirst()
                    .map(group -> ProfilePropertiesBuilder.createItems(group.getApplications()))
                    .orElse(Collections.emptyList());
  }

  private Function<Item, List<Item>> ApplicationsFromUserGroupFunction(User user) {
    return item -> user.getUserGroups().stream()
                    .filter(group -> group.getName().equals(item.getName()))
                    .findFirst()
                    .map(group -> ProfilePropertiesBuilder.createItems(group.getApplications()))
                    .orElse(Collections.emptyList());
  }

  private Function<Item, List<Item>> getApplicationsForUserGroupApplicationGroupFunction(User user) {
    return item -> user.getUserGroups().stream()
                    .flatMap(userGroup -> userGroup.getApplicationGroups().stream())
                    .filter(group -> group.getName().equals(item.getName()))
                    .findFirst()
                    .map(group -> ProfilePropertiesBuilder.createItems(group.getApplications()))
                    .orElse(Collections.emptyList());
  }

  private Function<Item, List<Item>> PrintersFromUserGroupFunction(User user) {
    return item -> user.getUserGroups().stream()
                    .filter(group -> group.getName().equals(item.getName()))
                    .findFirst()
                    .map(group -> ProfilePropertiesBuilder.createItems(group.getPrinters()))
                    .orElse(Collections.emptyList());
  }

  @Override
  public User getFreshProfile(String name) {
    try (LDAPConnection connection = new LDAPConnection()) {
      User profile = userService.findByName(name);
      if (profile != null) {
        Collection<String> adminDNs = connection.loadAdminDNs();
        boolean isAdmin = adminDNs.contains(profile.getDn());
        profile.setRole(isAdmin ? "admin" : "user");
      }
      return profile;
    } catch (Exception ex) {
      LOGGER.error("Cannot find directory-objects: " + ex.getMessage(), ex);
      return null;
    }
  }

  @Override
  public void save(User profile) {
    LOGGER.info("Save: " + profile);
    try (LDAPConnection connection = new LDAPConnection()) {
      userService.save((User) profile);
      String dn = profile.getDn();
      connection.updateAdminDNs(dn, "admin".equals(profile.getRole()));
      Audit.logSave(profile);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to save user", ex);
    }
  }

  @Override
  public void delete(User profile) throws ProfileNotDeletedException {
    if (isLoggedInUser(profile)) {
      LOGGER.warn("{} attempted to delete their own profile",
                  profile.getName());
      return;
    }
    super.delete(profile);
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
    super.showOverview(!isSecondaryDirectory);
    overviewCL.addComponent(
      userGroupView.createOverviewItemlistPanel(userGroupView.getViewTitleKey(), userGroupView.getAllItems(), !isSecondaryDirectory).getPanel()
    );
  }
}
