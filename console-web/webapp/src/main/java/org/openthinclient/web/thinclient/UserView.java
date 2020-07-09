package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.ValueContext;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.data.validator.AbstractValidator;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.*;
import org.openthinclient.web.Audit;
import org.openthinclient.web.OTCSideBar;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.presenter.DirectoryObjectPanelPresenter;
import org.openthinclient.web.thinclient.presenter.ProfilePanelPresenter;
import org.openthinclient.web.thinclient.presenter.ProfilesListOverviewPanelPresenter;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;
import org.vaadin.spring.sidebar.annotation.ThemeIcon;
import org.vaadin.viritin.button.MButton;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name = UserView.NAME, ui= ManagerUI.class)
@SideBarItem(sectionId = ManagerSideBarSections.DEVICE_MANAGEMENT, captionCode="UI_USER_HEADER", order = 40)
@ThemeIcon(UserView.ICON)
public final class UserView extends AbstractThinclientView {

  // TODO: user nur aus dem festgelegten LDAP (primary/seonary)
  //       lesen/schreiben je nach einstellung des LDAP (Bereich Settings/RealmConfig/ Nutzer/GRuppenverwaltung)

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
  @Autowired
  private SchemaProvider schemaProvider;
  @Autowired @Qualifier("deviceSideBar")
  OTCSideBar deviceSideBar;

  private boolean secondaryDirectory = false;

  private final IMessageConveyor mc;

  public UserView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
   super(UI_USER_HEADER, eventBus, notificationService);
   mc = new MessageConveyor(UI.getCurrent().getLocale());
  }

  @PostConstruct
  private void setup() {
    secondaryDirectory = "secondary".equals(getRealmService().getDefaultRealm().getValue("UserGroupSettings.DirectoryVersion"));

    addStyleName(NAME);
    if (!secondaryDirectory) {
      addCreateActionButton(mc.getMessage(UI_THINCLIENT_ADD_USER_LABEL), ICON, NAME + "/create");
    }
  }

  @Override
  public Set getAllItems() {
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
    return Collections.EMPTY_SET;
  }

  @Override
  public Client getClient(String name) {
    return null;
  }

  @Override
  public Schema getSchema(String schemaName) {
    return schemaProvider.getSchema(User.class, schemaName);
  }

  @Override
  public Map<String, String> getSchemaNames() {
    return Stream.of(schemaProvider.getSchemaNames(User.class))
                 .collect( Collectors.toMap(schemaName -> schemaName, schemaName -> getSchema(schemaName).getLabel()));
  }

  public ProfilePanel createProfilePanel (DirectoryObject directoryObject) {

    ProfilePanel profilePanel = createUserProfilePanel((User) directoryObject, false);
//    ProfilePanel profilePanel = new ProfilePanel(directoryObject.getName(), directoryObject.getClass());
//    OtcPropertyGroup configuration = createUserMetadataPropertyGroup((User) directoryObject);
//    // disable name-property: do not change name or validate name if user already exists (only on 'new'-user action)
//    configuration.getProperty("name").ifPresent(otcProperty -> {
//      OtcTextProperty name = (OtcTextProperty) otcProperty;
//      name.getConfiguration().getValidators().clear();
//      name.getConfiguration().disable();
//    });

    // put property-group to panel
//    DirectoryObjectPanelPresenter ppp = new DirectoryObjectPanelPresenter(this, profilePanel, directoryObject);
//    ppp.setItemGroups(Arrays.asList(configuration, new OtcPropertyGroup(null, null)));
//    ppp.onValuesWritten(profilePanel1 -> saveValues(ppp, directoryObject));

    // MetaInformation

//    ProfilePanel profileMetaPanel = new ProfilePanel(mc.getMessage(UI_PROFILE_PANEL_NEW_PROFILE_HEADER), directoryObject.getClass());
////    profileMetaPanel.hideMetaInformation();
//    // put property-group to panel
//    OtcPropertyGroup propertyGroup = createUserMetadataPropertyGroup((User) directoryObject);
//    profileMetaPanel.setItemGroups(Arrays.asList(propertyGroup, new OtcPropertyGroup(null, null)));
//    showProfileMetadataPanel(profileMetaPanel);
////    ppp.setPanelMetaInformation(createDefaultMetaInformationComponents(directoryObject));
//    attachSaveHandler((User) directoryObject, propertyGroup, ppp);

    return profilePanel;
  }

  @Override
  public ProfileReferencesPanel createReferencesPanel(DirectoryObject item) {
    ProfileReferencesPanel referencesPanel = new ProfileReferencesPanel(item.getClass());
    ReferencePanelPresenter refPresenter = new ReferencePanelPresenter(referencesPanel);

    User user = (User) item;

    Set<UserGroup> allUserGroups = userGroupService.findAll();
    refPresenter.showReference(user.getUserGroups(),  mc.getMessage(UI_USERGROUP_HEADER),
                                allUserGroups, UserGroup.class,
                                values -> saveReference(item, values, allUserGroups, UserGroup.class),
                                null, secondaryDirectory);

    Set<Application> allApplicatios = applicationService.findAll();
    refPresenter.showReference(user.getApplications(), mc.getMessage(UI_APPLICATION_HEADER),
                                allApplicatios, Application.class,
                                values -> saveReference(item, values, allApplicatios, Application.class));

    Set<ApplicationGroup> allApplicationGroups = applicationGroupService.findAll();
    refPresenter.showReference(user.getApplicationGroups(), mc.getMessage(UI_APPLICATIONGROUP_HEADER),
                                allApplicationGroups, ApplicationGroup.class,
                                values -> saveReference(item, values, allApplicationGroups, ApplicationGroup.class));

    Set<Printer> allPrinters = printerService.findAll();
    refPresenter.showReference(user.getPrinters(), mc.getMessage(UI_PRINTER_HEADER),
                                allPrinters, Printer.class,
                                values -> saveReference(item, values, allPrinters, Printer.class));

    return referencesPanel;
  }

  private OtcPropertyGroup createUserMetadataPropertyGroup(User user) {

    OtcPropertyGroup configuration = new OtcPropertyGroup();

    // Name
    OtcTextProperty name = new OtcTextProperty(mc.getMessage(UI_LOGIN_USERNAME), mc.getMessage(UI_USERS_USERNAME_TIP), "name", user.getName(), null);
    ItemConfiguration nameConfiguration = new ItemConfiguration("name", user.getName());
    nameConfiguration.addValidator(new StringLengthValidator(mc.getMessage(UI_PROFILE_NAME_VALIDATOR), 1, null));
    nameConfiguration.addValidator(new StringLengthValidator(mc.getMessage(UI_USERS_USERNAME_VALIDATOR_LENGTH), null, 32));
    nameConfiguration.addValidator(new RegexpValidator(mc.getMessage(UI_USERS_USERNAME_VALIDATOR_REGEXP), "[a-zA-Z_][a-zA-Z0-9._-]*[$]?"));
    nameConfiguration.addValidator(new AbstractValidator(mc.getMessage(UI_USERS_USERNAME_VALIDATOR_NAME_EXISTS)) {
      @Override
      public ValidationResult apply(Object value, ValueContext context) {
        Optional<User> optional = userService.findBySAMAccountName(value.toString());
        return toResult(value, !optional.isPresent());
      }
      @Override
      public Object apply(Object o, Object o2) {
        return null;
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
    pwdRetypeConfig.addValidator(new AbstractValidator(mc.getMessage(UI_USERS_PASSWORD_RETYPE_VALIDATOR)) {
      @Override
      public ValidationResult apply(Object value, ValueContext context) {
        return toResult(value, pwdConfig.getValue() != null && pwdConfig.getValue().equals(value));
      }
      @Override
      public Object apply(Object o, Object o2) {
        return null;
      }
    });
    if (secondaryDirectory) pwdRetypeConfig.disable();
    pwdRetype.setConfiguration(pwdRetypeConfig);
    configuration.addProperty(pwdRetype);

    return configuration;
  }


  @Override
  public <T extends DirectoryObject> T getFreshProfile(String name) {
     return (T) userService.findByName(name);
  }

  @Override
  public void save(DirectoryObject profile) {
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
    ProfilePanel profilePanel = new ProfilePanel(panelCaption, profile.getClass());
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
  public void showProfile(DirectoryObject profile) {
    String message = mc.getMessage(UI_PROFILE_PANEL_COPY_TARGET_NAME, "").trim();
    boolean userIsNew = profile.getName().indexOf(message) == 0;
    ProfilePanel profilePanel = createUserProfilePanel((User) profile, userIsNew);
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
  public void selectItem(DirectoryObject directoryObject) {
    LOGGER.info("sideBar: "+ deviceSideBar);
    deviceSideBar.selectItem(NAME, directoryObject, getAllItems());
  }

  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {
    LOGGER.debug("enter -> source={}, navigator-state=", event.getSource(), event.getNavigator().getState());
    String[] params = Optional.ofNullable(event.getParameters()).orElse("").split("/", 2);
    if (params.length > 0) {
      if ("create".equals(params[0])) {
        showProfileMetadata(new User());
      } else {
        super.enter(event);
      }
    }
  }

  @Override
  public void showOverview() {
    super.showOverview();
    overviewCL.addComponent(
      userGroupView.createOverviewItemlistPanel(userGroupView.getViewTitleKey(), userGroupView.getAllItems()).getPanel()
    );
  }
}
