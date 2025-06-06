package org.openthinclient.web.thinclient;

import com.vaadin.data.ValidationResult;
import com.vaadin.data.ValueContext;
import com.vaadin.data.validator.AbstractValidator;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.spring.annotation.SpringView;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.service.*;
import org.openthinclient.web.Audit;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.model.Item;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.presenter.DirectoryObjectPanelPresenter;
import org.openthinclient.web.thinclient.presenter.ReferencePanelPresenter;
import org.openthinclient.web.thinclient.property.OtcPasswordProperty;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.openthinclient.web.thinclient.property.OtcTextProperty;
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
public final class UserView extends AbstractDirectoryObjectView<User> {

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

  private boolean secondaryDirectory = false;

  @PostConstruct
  private void setup() {
    secondaryDirectory = getRealmService().getDefaultRealm().isSecondaryConfigured();

    addStyleName(NAME);
  }

  @Override
  public Set<User> getAllItems() {
    try {
      Set<User> users = userService.findAll();
      getRealmService().findAllRealms().forEach(realm ->
        users.removeAll(realm.getAdministrators().getMembers())
      );
      return users;
    } catch (Exception e) {
      LOGGER.warn("Cannot find directory-objects: " + e.getMessage());
      showError(e);
    }
    return Collections.emptySet();
  }

  @Override
  protected Class<User> getItemClass() {
    return User.class;
  }

  public ProfilePanel createProfilePanel (User profile) {
    return createUserProfilePanel(profile, false);
  }

  @Override
  public ProfileReferencesPanel createReferencesPanel(User user) {
    ProfileReferencesPanel referencesPanel = new ProfileReferencesPanel(User.class);
    ReferencePanelPresenter refPresenter = new ReferencePanelPresenter(referencesPanel);

    Set<UserGroup> userGroups = user.getUserGroups();

    Set<UserGroup> allUserGroups = userGroupService.findAll();
    Consumer<List<Item>> profileReferenceChangeConsumer = null;
    if(!secondaryDirectory) {
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

  private OtcPropertyGroup createUserMetadataPropertyGroup(User user) {

    OtcPropertyGroup configuration = new OtcPropertyGroup();

    // Name
    OtcTextProperty name = new OtcTextProperty(mc.getMessage(UI_LOGIN_USERNAME), mc.getMessage(UI_USERS_USERNAME_TIP), "name", user.getName(), null);
    ItemConfiguration nameConfiguration = new ItemConfiguration("name", user.getName());
    nameConfiguration.addValidator(new StringLengthValidator(mc.getMessage(UI_PROFILE_NAME_VALIDATOR), 1, null));
    nameConfiguration.addValidator(new StringLengthValidator(mc.getMessage(UI_USERS_USERNAME_VALIDATOR_LENGTH), null, 32));
    nameConfiguration.addValidator(new RegexpValidator(mc.getMessage(UI_USERS_USERNAME_VALIDATOR_REGEXP), "[a-zA-Z_][a-zA-Z0-9._-]*[$]?"));
    nameConfiguration.addValidator(new AbstractValidator<String>(mc.getMessage(UI_USERS_USERNAME_VALIDATOR_NAME_EXISTS)) {
      @Override
      public ValidationResult apply(String value, ValueContext context) {
        User existingUser = userService.findByName(value.toString());
        return toResult(value, existingUser == null);
      }
    });
    if (secondaryDirectory) nameConfiguration.disable();
    name.setConfiguration(nameConfiguration);
    configuration.addProperty(name);

    // Description
    OtcTextProperty desc = new OtcTextProperty(mc.getMessage(UI_COMMON_DESCRIPTION_LABEL), null, "description", user.getDescription(), null);
    ItemConfiguration descConfig = new ItemConfiguration("description", user.getDescription());
    if (secondaryDirectory) descConfig.disable();
    desc.setConfiguration(descConfig);
    configuration.addProperty(desc);

    // Password
    String pwdValue = user.getUserPassword() != null ? new String(user.getUserPassword()) : null;
    OtcPasswordProperty pwd = new OtcPasswordProperty(mc.getMessage(UI_COMMON_PASSWORD_LABEL), mc.getMessage(UI_USERS_PASSWORD_VALIDATOR_LENGTH), "password", pwdValue);
    ItemConfiguration pwdConfig = new ItemConfiguration("password", pwdValue);
    pwdConfig.addValidator(new StringLengthValidator(mc.getMessage(UI_USERS_PASSWORD_VALIDATOR_LENGTH), 1, null));
    if (secondaryDirectory) pwdConfig.disable();
    pwd.setConfiguration(pwdConfig);
    configuration.addProperty(pwd);

    OtcPasswordProperty pwdRetype = new OtcPasswordProperty(mc.getMessage(UI_COMMON_PASSWORD_RETYPE_LABEL), null, "passwordRetype", pwdValue);
    ItemConfiguration pwdRetypeConfig = new ItemConfiguration("passwordRetype", pwdValue);
    pwdRetypeConfig.addValidator(new AbstractValidator<String>(mc.getMessage(UI_USERS_PASSWORD_RETYPE_VALIDATOR)) {
      @Override
      public ValidationResult apply(String value, ValueContext context) {
        return toResult(value, pwdConfig.getValue() != null && pwdConfig.getValue().equals(value));
      }
    });
    if (secondaryDirectory) pwdRetypeConfig.disable();
    pwdRetype.setConfiguration(pwdRetypeConfig);
    configuration.addProperty(pwdRetype);

    return configuration;
  }

  @Override
  protected User newProfile() {
    return new User();
  }

  @Override
  public User getFreshProfile(String name) {
     return userService.findByName(name);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected <D extends DirectoryObject> Set<D> getMembers(User profile, Class<D> clazz) {
    if (clazz == UserGroup.class) {
      return (Set<D>)profile.getUserGroups();
    } else if (clazz == ApplicationGroup.class) {
      return (Set<D>)profile.getApplicationGroups();
    } else if (clazz == Printer.class) {
      return (Set<D>)profile.getPrinters();
    } else if (clazz == Application.class) {
      return (Set<D>)profile.getApplications();
    } else {
      return Collections.emptySet();
    }
  }

  @Override
  public void save(User profile) {
    LOGGER.info("Save: " + profile);
    userService.save((User) profile);
    Audit.logSave(profile);
  }

  public void showProfileMetadata(User profile) {
    ProfilePanel profilePanel = createUserProfilePanel(profile, true);
    showProfileMetadataPanel(profilePanel);
  }

  protected ProfilePanel createUserProfilePanel(User profile, boolean userIsNew) {
    OtcPropertyGroup propertyGroup = createUserMetadataPropertyGroup(profile);
    if (!userIsNew) {
      // disable name-property: do not change name or validate name if user already exists (only on 'new'-user action)
      propertyGroup.getProperty("name").ifPresent(otcProperty -> {
        OtcTextProperty name = (OtcTextProperty) otcProperty;
        name.getConfiguration().getValidators().clear();
        name.getConfiguration().disable();
      });
    }
    String panelCaption = userIsNew? mc.getMessage(UI_PROFILE_PANEL_NEW_PROFILE_HEADER) : profile.getName();
    ProfilePanel profilePanel = new ProfilePanel(panelCaption,
                                                  mc.getMessage(UI_USER),
                                                  User.class);
    // put property-group to panel
    DirectoryObjectPanelPresenter ppp = new DirectoryObjectPanelPresenter(this, profilePanel, profile);
    ppp.setItemGroups(Arrays.asList(propertyGroup, new OtcPropertyGroup()));
    if (userIsNew) {
      // hide, if user will be created
      ppp.hideCopyButton();
      ppp.hideDeleteButton();
    }

    if (secondaryDirectory) {
      // disable save if data received from secondary directory
      ppp.setDisabledMode();
      ppp.hideCopyButton();
      ppp.hideDeleteButton();

    } else {
      // add save handler
      ppp.onValuesWritten(profilePanel1 -> {
        ppp.getItemGroupPanels().forEach(igp -> {
          igp.propertyComponents().forEach(propertyComponent -> {
            OtcProperty bean = (OtcProperty) propertyComponent.getBinder().getBean();
            String key = bean.getKey();
            String value = bean.getConfiguration().getValue();
            switch (key) {
              case "name":
                profile.setName(value);
                break;
              case "description":
                profile.setDescription(value);
                break;
              case "password":
                profile.setUserPassword(value.getBytes());
                break;
              case "passwordRetype":
                profile.setVerifyPassword(value);
                break;
            }
          });

          // save
          boolean success = saveProfile(profile, ppp);
          if (success) {
            selectItem(profile);
            if (userIsNew) {
              navigateTo(profile);
            }
          }

        });
      });
    }
    return profilePanel;
  }

  @Override
  public void showProfile(User profile) {
    String message = mc.getMessage(UI_PROFILE_PANEL_COPY_TARGET_NAME, "").trim();
    boolean userIsNew = profile.getName().indexOf(message) == 0;
    ProfilePanel profilePanel = createUserProfilePanel(profile, userIsNew);
    ProfileReferencesPanel profileReferencesPanel = createReferencesPanel(profile);
    displayProfilePanel(profilePanel, profileReferencesPanel);
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
    super.showOverview(!secondaryDirectory);
    overviewCL.addComponent(
      userGroupView.createOverviewItemlistPanel(userGroupView.getViewTitleKey(), userGroupView.getAllItems(), !secondaryDirectory).getPanel()
    );
  }
}
