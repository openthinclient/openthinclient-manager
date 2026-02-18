package org.openthinclient.web.thinclient;

import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;
import com.vaadin.data.validator.AbstractValidator;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.server.VaadinSession;

import org.openthinclient.common.model.*;
import org.openthinclient.web.thinclient.exception.BuildProfileException;
import org.openthinclient.web.thinclient.model.DeleteMandate;
import org.openthinclient.web.thinclient.model.ItemConfiguration;
import org.openthinclient.web.thinclient.presenter.DirectoryObjectPanelPresenter;
import org.openthinclient.web.thinclient.property.OtcOptionProperty;
import org.openthinclient.web.thinclient.property.OtcPasswordProperty;
import org.openthinclient.web.thinclient.property.OtcProperty;
import org.openthinclient.web.thinclient.property.OtcPropertyGroup;
import org.openthinclient.web.thinclient.property.OtcTextProperty;
import org.openthinclient.web.thinclient.model.SelectOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.function.Function;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

public abstract class AbstractUserView extends AbstractDirectoryObjectView<User> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUserView.class);

  protected boolean isSecondaryDirectory = false;
  protected boolean isAdminView = false;

  protected void onSave(User profile, boolean success, boolean isNew) {
    if (success) {
      selectItem(profile);
      if (isNew) {
        navigateTo(profile);
      }
    }
  }

  protected boolean isLoggedInUser(User user) {
    try {
      SecurityContext securityContext = (SecurityContext) VaadinSession
          .getCurrent().getSession()
          .getAttribute("SPRING_SECURITY_CONTEXT");
      UserDetails userDetails = (UserDetails) securityContext
          .getAuthentication().getPrincipal();
      return userDetails.getUsername().equals(user.getName());
    } catch (Exception ex) {
      LOGGER.error("Failed to get name of logged in user", ex);
    }
    return false;
  }

  @Override
  protected Class<User> getItemClass() {
    return User.class;
  }

  @Override
  public ProfilePanel createProfilePanel (User profile) {
    return createUserProfilePanel(profile, false);
  }

  protected
  OtcPropertyGroup createUserMetadataPropertyGroup(
      User user, boolean userIsNew, boolean isSecondaryDirectory) {
    OtcPropertyGroup configuration = new OtcPropertyGroup();

    // Name
    OtcTextProperty name = new OtcTextProperty(
      mc.getMessage(UI_LOGIN_USERNAME),
      mc.getMessage(UI_USERS_USERNAME_TIP),
      "name",
      user.getName(),
      null);
    ItemConfiguration nameConfiguration;
    nameConfiguration = new ItemConfiguration("name", user.getName());
    if (!isSecondaryDirectory) {
      nameConfiguration.addValidator(new StringLengthValidator(
          mc.getMessage(UI_PROFILE_NAME_VALIDATOR),
          1,
          null
      ));
      nameConfiguration.addValidator(new StringLengthValidator(
          mc.getMessage(UI_USERS_USERNAME_VALIDATOR_LENGTH),
          null,
          32
      ));
      nameConfiguration.addValidator(new RegexpValidator(
          mc.getMessage(    UI_USERS_USERNAME_VALIDATOR_REGEXP),
          "[a-zA-Z_][a-zA-Z0-9._-]*[$]?"
      ));
      nameConfiguration.addValidator(new AbstractValidator<String>(
        mc.getMessage(UI_USERS_USERNAME_VALIDATOR_NAME_EXISTS)) {
          @Override
          public ValidationResult apply(String value, ValueContext context) {
            if (!userIsNew && value.equals(user.getName())) {
              return ValidationResult.ok();
            }
            User existingUser = getFreshProfile(value.toString());
            return toResult(value, existingUser == null);
          }
        }
      );
    } else {
      nameConfiguration.disable();
    }
    name.setConfiguration(nameConfiguration);
    configuration.addProperty(name);

    // Description
    OtcTextProperty desc = new OtcTextProperty(mc.getMessage(UI_COMMON_DESCRIPTION_LABEL), null, "description", user.getDescription(), null);
    ItemConfiguration descConfig = new ItemConfiguration("description", user.getDescription());
    if (isSecondaryDirectory) descConfig.disable();
    desc.setConfiguration(descConfig);
    configuration.addProperty(desc);

    // Password
    OtcPasswordProperty pwd = new OtcPasswordProperty(mc.getMessage(UI_COMMON_PASSWORD_LABEL), mc.getMessage(UI_USERS_PASSWORD_VALIDATOR_LENGTH), "password", null);
    ItemConfiguration pwdConfig = new ItemConfiguration("password", null);
    pwdConfig.addValidator(new Validator<String>() {
      @Override
      public ValidationResult apply(String value, ValueContext context) {
        if (!userIsNew || (value != null && !value.isEmpty())) {
          return ValidationResult.ok();
        }
        return ValidationResult.error(
            mc.getMessage(UI_USERS_PASSWORD_VALIDATOR_LENGTH));
      }
    });
    if (isSecondaryDirectory) pwdConfig.disable();
    pwd.setConfiguration(pwdConfig);
    configuration.addProperty(pwd);

    OtcPasswordProperty pwdRetype = new OtcPasswordProperty(mc.getMessage(UI_COMMON_PASSWORD_RETYPE_LABEL), null, "passwordRetype", null);
    ItemConfiguration pwdRetypeConfig = new ItemConfiguration("passwordRetype", null);
    pwdRetypeConfig.addValidator(new AbstractValidator<String>(mc.getMessage(UI_USERS_PASSWORD_RETYPE_VALIDATOR)) {
      @Override
      public ValidationResult apply(String value, ValueContext context) {
        return toResult(value, pwdConfig.getValue() != null && pwdConfig.getValue().equals(value));
      }
    });
    if (isSecondaryDirectory) pwdRetypeConfig.disable();
    pwdRetype.setConfiguration(pwdRetypeConfig);
    configuration.addProperty(pwdRetype);

    if(!isSecondaryDirectory) {
      List<SelectOption> options = new ArrayList<>();
      options.add(
          new SelectOption(mc.getMessage(UI_USERS_ROLE_USER),  "user"));
      options.add(
          new SelectOption(mc.getMessage(UI_USERS_ROLE_ADMIN), "admin"));
      OtcOptionProperty role = new OtcOptionProperty(
          mc.getMessage(UI_USERS_ROLE_LABEL),
          null,
          "userRole",
          user.getRole(),
          "user",
          options
      );
      role.setConfiguration(new ItemConfiguration("userRole", user.getRole()));
      configuration.addProperty(role);
    }

    return configuration;
  }

  @Override
  protected Function<DirectoryObject, DeleteMandate> createDeleteMandateFunction() {
    return directoryObject -> {
      User user = (User) directoryObject;
      if (isLoggedInUser(user)) {
        return new DeleteMandate(
          false,
          mc.getMessage(UI_COMMON_DELETE_LOGGED_IN_USER_DENIED,
                        user.getName()));
      }
      return new DeleteMandate(true, "");
    };
  }

  @Override
  protected User newProfile() {
    return new User();
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
  public void showProfileMetadata(User profile) {
    ProfilePanel profilePanel = createUserProfilePanel(profile, true);
    showProfileMetadataPanel(profilePanel);
  }

  protected
  ProfilePanel createUserProfilePanel(User profile, boolean userIsNew) {
    OtcPropertyGroup propertyGroup = createUserMetadataPropertyGroup(
        profile, userIsNew, isSecondaryDirectory);
    String panelCaption = userIsNew?
        mc.getMessage(UI_PROFILE_PANEL_NEW_PROFILE_HEADER) : profile.getName();
    ProfilePanel profilePanel = new ProfilePanel(panelCaption,
                                                  mc.getMessage(UI_USER),
                                                  User.class);
    DirectoryObjectPanelPresenter ppp = new DirectoryObjectPanelPresenter(
          this, profilePanel, profile);
    ppp.setItemGroups(Arrays.asList(propertyGroup, new OtcPropertyGroup()));

    if (isSecondaryDirectory) {
      ppp.setDisabledMode();
    }
    if (userIsNew || isSecondaryDirectory || isAdminView) {
      ppp.hideCopyButton();
    }
    if (userIsNew || isSecondaryDirectory || isLoggedInUser(profile)) {
      ppp.hideDeleteButton();
    }

    if(!isSecondaryDirectory) {
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
                if (value != null && !value.isEmpty()) {
                  profile.setUserPassword(value.getBytes());
                  profile.setNewPassword(value);
                }
                break;
              case "passwordRetype":
                if (value != null && !value.isEmpty()) {
                  profile.setVerifyPassword(value);
                }
                break;
              case "userRole":
                profile.setRole(value);
                break;
            }
          });

          // save
          boolean success = saveProfile(profile, ppp);
          onSave(profile, success, userIsNew);
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
    ProfileReferencesPanel profileReferencesPanel = null;
    try {
      profileReferencesPanel = createReferencesPanel(profile);
    } catch (BuildProfileException ex) {
      LOGGER.error("Failed to build profile!", ex);
      showError(ex);
    }
    displayProfilePanel(profilePanel, profileReferencesPanel);
  }
}
