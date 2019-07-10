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
import org.openthinclient.web.OTCSideBar;
import org.openthinclient.web.dashboard.DashboardNotificationService;
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

  private final IMessageConveyor mc;

  public UserView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
   super(UI_USER_HEADER, eventBus, notificationService);
   mc = new MessageConveyor(UI.getCurrent().getLocale());
  }

  @PostConstruct
  private void setup() {
    addStyleName(NAME);
    addCreateActionButton(mc.getMessage(UI_THINCLIENT_ADD_USER_LABEL), ICON, NAME + "/create");

    Set<UserGroup> userGroups = Collections.EMPTY_SET;
    try {
      userGroups = userGroupService.findAll();
    } catch (Exception e) {
      LOGGER.warn("Cannot find userGroups: " + e.getMessage());
    }
    ProfilesListOverviewPanelPresenter agpp = addOverviewItemlistPanel(UI_USERGROUP_HEADER, userGroups);
    agpp.addNewButtonClickHandler(event -> {
      // ... ohne Worte
      VerticalLayout content = new VerticalLayout();
      Window window = new Window(null, content);
      window.setModal(true);
      window.setPositionX(200);
      window.setPositionY(50);
      window.setCaption("Usergruppe erstellen");
      content.addComponent(new Label("Legen Sie bitte den Usergruppenname fest:"));
      TextField input = new TextField();
      content.addComponent(input);
      HorizontalLayout hl = new HorizontalLayout();
      hl.addComponents(new MButton(mc.getMessage(UI_BUTTON_CANCEL), event1 -> window.close()),
          new MButton(mc.getMessage(UI_BUTTON_SAVE), event1 -> {
            UserGroup byName = userGroupService.findByName(input.getValue());
            if (byName == null) {
              UserGroup ug = new UserGroup();
              ug.setName(input.getValue());
              userGroupService.save(ug);
              // update
              ListDataProvider<DirectoryObject> dataProvider = DataProvider.ofCollection((Set) userGroupService.findAll());
              dataProvider.setSortComparator(Comparator.comparing(DirectoryObject::getName, String::compareToIgnoreCase)::compare);
              agpp.setDataProvider(dataProvider);
              window.close();
              UI.getCurrent().removeWindow(window);
            } else {
              content.addComponent(new Label("Der Name ist schon vergeben."));
            }
          }));
      content.addComponent(hl);
      window.setContent(content);
      UI.getCurrent().addWindow(window);
    });
    agpp.setItemsSupplier(() -> (Set) userGroupService.findAll());
    agpp.setItemButtonClickedConsumer(null); // disable Item-Button-Click-event

    addOverviewItemlistPanel(UI_USER_HEADER, getAllItems());
  }

  @Override
  public Set getAllItems() {
    try {
      return userService.findAll().stream().filter(user -> !user.getName().equals("administrator")).collect(Collectors.toSet());
    } catch (Exception e) {
      LOGGER.warn("Cannot find directory-objects: " + e.getMessage());
      showError(e);
    }
    return Collections.EMPTY_SET;
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
    refPresenter.showReference(user.getUserGroups(),  mc.getMessage(UI_USERGROUP_HEADER), allUserGroups, UserGroup.class, values -> saveReference(item, values, allUserGroups, UserGroup.class));
    Set<ApplicationGroup> allApplicationGroups = applicationGroupService.findAll();
    refPresenter.showReference(user.getApplicationGroups(), mc.getMessage(UI_APPLICATIONGROUP_HEADER), allApplicationGroups, ApplicationGroup.class, values -> saveReference(item, values, allApplicationGroups, ApplicationGroup.class));
    Set<Application> allApplicatios = applicationService.findAll();
    refPresenter.showReference(user.getApplications(), mc.getMessage(UI_APPLICATION_HEADER), allApplicatios, Application.class, values -> saveReference(item, values, allApplicatios, Application.class));
    Set<Printer> allPrinters = printerService.findAll();
    refPresenter.showReference(user.getPrinters(), mc.getMessage(UI_PRINTER_HEADER), allPrinters, Printer.class, values -> saveReference(item, values, allPrinters, Printer.class));

    return referencesPanel;
  }

  private OtcPropertyGroup createUserMetadataPropertyGroup(User user) {

    OtcPropertyGroup configuration = new OtcPropertyGroup(null);
    configuration.setDisplayHeaderLabel(false);

    // Name
    OtcTextProperty name = new OtcTextProperty(mc.getMessage(UI_LOGIN_USERNAME), mc.getMessage(UI_USERS_USERNAME_TIP), "name", user.getName());
    ItemConfiguration nameConfiguration = new ItemConfiguration("name", user.getName());
    nameConfiguration.addValidator(new StringLengthValidator(mc.getMessage(UI_USERS_USERNAME_VALIDATOR_LENGTH), 1, null));
    nameConfiguration.addValidator(new RegexpValidator(mc.getMessage(UI_FILEBROWSER_SUBWINDOW_CREATEFOLDER_VALIDATION_REGEX), "[a-zA-Z0-9]+"));
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
    name.setConfiguration(nameConfiguration);
    configuration.addProperty(name);

    // Description
    OtcTextProperty desc = new OtcTextProperty(mc.getMessage(UI_COMMON_DESCRIPTION_LABEL), null, "description", user.getDescription());
    ItemConfiguration descConfig = new ItemConfiguration("description", user.getDescription());
    desc.setConfiguration(descConfig);
    configuration.addProperty(desc);

    // Password
    String pwdValue = user.getUserPassword() != null ? new String(user.getUserPassword()) : null;
    OtcPasswordProperty pwd = new OtcPasswordProperty(mc.getMessage(UI_COMMON_PASSWORD_LABEL), mc.getMessage(UI_USERS_PASSWORD_VALIDATOR_LENGTH), "password", pwdValue);
    ItemConfiguration pwdConfig = new ItemConfiguration("password", pwdValue);
    pwdConfig.addValidator(new StringLengthValidator(mc.getMessage(UI_USERS_PASSWORD_VALIDATOR_LENGTH), 1, null));
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
    ProfilePanel profilePanel = new ProfilePanel(mc.getMessage(UI_PROFILE_PANEL_NEW_PROFILE_HEADER), profile.getClass());
    // put property-group to panel
    DirectoryObjectPanelPresenter ppp = new DirectoryObjectPanelPresenter(this, profilePanel, profile);
    ppp.setItemGroups(Arrays.asList(propertyGroup, new OtcPropertyGroup(null, null)));
    if (userIsNew) {
      // hide, if user will be created
      ppp.hideCopyButton();
      ppp.hideDeleteButton();
    }

    // add save handler
    ppp.onValuesWritten(profilePanel1 -> {
      ppp.getItemGroupPanels().forEach(igp -> {
        igp.propertyComponents().forEach(propertyComponent -> {
          OtcProperty bean = (OtcProperty) propertyComponent.getBinder().getBean();
          String key   = bean.getKey();
          String value = bean.getConfiguration().getValue();
          switch (key) {
            case "name": profile.setName(value); break;
            case "description": profile.setDescription(value); break;
            case "password": profile.setUserPassword(value.getBytes()); break;
            case "passwordRetype": profile.setVerifyPassword(value); break;
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
    return profilePanel;
  }

  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {
    if (event.getParameters() != null) {
      // split at "/", add each part as a label
      String[] params = event.getParameters().split("/");

      // handle create action
      if (params.length == 1 && params[0].equals("create")) {
        switch (event.getViewName()) {
          case UserView.NAME: showProfileMetadata(new User()); break;
        }
      } else if (params.length == 1 && params[0].length() > 0) {
        DirectoryObject profile = getFreshProfile(params[0]);
        if (profile != null) {
          // treat copied users as 'new' to edit the user-name
          String message = mc.getMessage(UI_PROFILE_PANEL_COPY_TARGET_NAME, "").trim();
          boolean userIsNew = profile.getName().indexOf(message) == 0;
          ProfilePanel profilePanel = createUserProfilePanel((User) profile, userIsNew);
          ProfileReferencesPanel profileReferencesPanel = createReferencesPanel(profile);
          displayProfilePanel(profilePanel, profileReferencesPanel);
        } else {
          LOGGER.info("No profile found for name '" + params[0] + "'.");
        }
      }

    }
  }

  @Override
  public String getViewName() {
    return NAME;
  }


  @Override
  public void selectItem(DirectoryObject directoryObject) {
    LOGGER.info("sideBar: "+ deviceSideBar);
    deviceSideBar.selectItem(NAME, directoryObject, getAllItems());
  }
}
