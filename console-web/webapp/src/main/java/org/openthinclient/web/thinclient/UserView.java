package org.openthinclient.web.thinclient;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.ValueContext;
import com.vaadin.data.validator.AbstractValidator;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import org.openthinclient.common.model.*;
import org.openthinclient.common.model.schema.Schema;
import org.openthinclient.common.model.schema.provider.SchemaProvider;
import org.openthinclient.common.model.service.*;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.model.SelectOption;
import org.openthinclient.web.thinclient.presenter.DirectoryObjectPanelPresenter;
import org.openthinclient.web.thinclient.presenter.ProfilePanelPresenter;
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
import java.util.stream.Collectors;

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
  private UserGroupService userGroupService;
  @Autowired
  private ApplicationGroupService applicationGroupService;
  @Autowired
  private ApplicationService applicationService;
  @Autowired
  private SchemaProvider schemaProvider;

   private final IMessageConveyor mc;

   public UserView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
     super(UI_USER_HEADER, eventBus, notificationService);
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

  public ProfilePanel createProfilePanel (DirectoryObject directoryObject) {

    ProfilePanel profilePanel = new ProfilePanel(directoryObject.getName(), directoryObject.getClass());
    OtcPropertyGroup configuration = createUserMetadataPropertyGroup((User) directoryObject);
    // disable name-property: do not change name or validate name
    configuration.getProperty("name").ifPresent(otcProperty -> {
      OtcTextProperty name = (OtcTextProperty) otcProperty;
      name.getConfiguration().getValidators().clear();
      name.getConfiguration().disable();
    });

    // put property-group to panel
    profilePanel.setItemGroups(Arrays.asList(configuration, new OtcPropertyGroup(null, null)));
    DirectoryObjectPanelPresenter ppp = new DirectoryObjectPanelPresenter(this, profilePanel, directoryObject);
    ppp.hideCopyButton();
    // set MetaInformation
    ppp.setPanelMetaInformation(createDefaultMetaInformationComponents(directoryObject));

    User user = (User) directoryObject;
    showReference(user, profilePanel, user.getUserGroups(), "UserGroups", userGroupService.findAll(), UserGroup.class);
    showReference(user, profilePanel, user.getApplicationGroups(), mc.getMessage(UI_APPLICATIONGROUP_HEADER), applicationGroupService.findAll(), ApplicationGroup.class);
    showReference(user, profilePanel, user.getApplications(), mc.getMessage(UI_APPLICATION_HEADER), applicationService.findAll(), Application.class);
    showReference(user, profilePanel, user.getPrinters(), mc.getMessage(UI_PRINTER_HEADER), printerService.findAll(), Printer.class);

    return profilePanel;
  }

  private OtcPropertyGroup createUserMetadataPropertyGroup(User user) {

    OtcPropertyGroup configuration = new OtcPropertyGroup(null);
    configuration.setCollapseOnDisplay(false);
    configuration.setDisplayHeaderLabel(false);

    // Name
    OtcTextProperty name = new OtcTextProperty(mc.getMessage(UI_LOGIN_USERNAME), "Nutzername, wird für login verwendet.", "name", user.getName());
    ItemConfiguration nameConfiguration = new ItemConfiguration("name", user.getName());
    nameConfiguration.addValidator(new StringLengthValidator("Name muss mindesten 5 Zeichen lang sein.", 5, 15));
    nameConfiguration.addValidator(new AbstractValidator("Der Name existiert bereits") {
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
    OtcTextProperty desc = new OtcTextProperty("Beschreibung", null, "description", user.getDescription());
    ItemConfiguration descConfig = new ItemConfiguration("description", user.getDescription());
    desc.setConfiguration(descConfig);
    configuration.addProperty(desc);

    // Password
    String pwdValue = user.getUserPassword() != null ? new String(user.getUserPassword()) : null;
    OtcPasswordProperty pwd = new OtcPasswordProperty("Passwort", "Das Passwort muss mindesten 5 Zeichen lang sein.", "password", pwdValue);
    ItemConfiguration pwdConfig = new ItemConfiguration("password", pwdValue);
    pwdConfig.addValidator(new StringLengthValidator("Das Passwort muss mindesten 5 Zeichen lang sein.", 5, 15));
    pwd.setConfiguration(pwdConfig);
    configuration.addProperty(pwd);

    OtcPasswordProperty pwdRetype = new OtcPasswordProperty("Passwort retype", null, "passwordRetype", pwdValue);
    ItemConfiguration pwdRetypeConfig = new ItemConfiguration("passwordRetype", pwdValue);
    pwdRetypeConfig.addValidator(new AbstractValidator("Das Passwort stimmt nicht überein.") {
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

    // Save handler, for each property we need to call dedicated setter
    configuration.onValueWritten(ipg -> {
      ipg.propertyComponents().forEach(propertyComponent -> {
        OtcProperty bean = (OtcProperty) propertyComponent.getBinder().getBean();
        String key   = bean.getKey();
        String value = bean.getConfiguration().getValue();
        switch (key) {
          case "name": user.setName(value); break;
          case "description": user.setDescription(value); break;
          case "password": user.setUserPassword(value.getBytes()); break;
          case "passwordRetype": user.setVerifyPassword(value); break;
        }
      });

      // save
      boolean success = saveProfile(user, ipg);
      // update view
      if (success) {
        setItems(getAllItems()); // refresh item list
        selectItem(user);
      }

    });
    return configuration;
  }


  @Override
  public <T extends DirectoryObject> T getFreshProfile(String name) {
     return (T) userService.findByName(name);
  }

  @Override
  public void save(DirectoryObject profile) {
     userService.save((User) profile);
  }

  public void showProfileMetadata(User profile) {
    OtcPropertyGroup propertyGroup = createUserMetadataPropertyGroup(profile);

    String label;
    if (profile.getName() == null || profile.getName().length() == 0) {
      label = "Neues Profil";
    } else {
      label = profile.getName() + " bearbeiten";
    }

    ProfilePanel profilePanel = new ProfilePanel(label, profile.getClass());
    profilePanel.hideMetaInformation();
    // put property-group to panel
    profilePanel.setItemGroups(Arrays.asList(propertyGroup, new OtcPropertyGroup(null, null)));
    // show metadata properties, default is hidden
    DirectoryObjectPanelPresenter ppp = new DirectoryObjectPanelPresenter(this, profilePanel, profile);
    ppp.expandMetaData();
    ppp.hideCopyButton();
    ppp.hideEditButton();
    ppp.hideDeleteButton();

    showProfileMetadataPanel(profilePanel);
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
      }

    }
  }

}