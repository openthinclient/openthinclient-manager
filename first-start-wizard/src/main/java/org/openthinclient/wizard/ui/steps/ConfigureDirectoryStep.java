package org.openthinclient.wizard.ui.steps;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import org.openthinclient.common.model.OrganizationalUnit;
import org.openthinclient.common.model.User;
import org.openthinclient.wizard.model.SystemSetupModel;
import org.vaadin.teemu.wizards.Wizard;

import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_DESCRIPTION;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_HEADLINE;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_ADMINISTRATOR;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_DESCRIPTION;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_FIRSTNAME;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_LASTNAME;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_PASSWD;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_PASSWD_REPEAT;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_SYSTEM;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_SYSTEMDESCRIPTION;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_SYSTEMNAME;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_USERNAME;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_TITLE;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_ONLYDIGITS;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_REQUIRED;

public class ConfigureDirectoryStep extends AbstractStep {

  private final SystemSetupModel systemSetupModel;

  private final Binder<OrganizationalUnit>  primaryOUBinder;
  private final Binder<User>  userBinder;

  public ConfigureDirectoryStep(Wizard wizard, SystemSetupModel systemSetupModel) {
    
    this.systemSetupModel = systemSetupModel;

    this.primaryOUBinder = new Binder<>();
    this.primaryOUBinder.setBean(systemSetupModel.getDirectoryModel().getPrimaryOU());

    this.userBinder = new Binder<>();
    this.userBinder.setBean(systemSetupModel.getDirectoryModel().getAdministratorUser());

    final VerticalLayout contents = new VerticalLayout();
    contents.setSpacing(true);
    contents.setMargin(true);


    contents.addComponent(createLabelH1(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_HEADLINE)));

    // TODO: add a description
    final Label descriptionLabel = createLabelLarge(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_DESCRIPTION), ContentMode.HTML);
    descriptionLabel.setWidth(100, Sizeable.Unit.PERCENTAGE);
    contents.addComponent(descriptionLabel);
    
    contents.addComponent(createPrimaryOUPanel());
    contents.addComponent(createAdministratorUserPanel());

    setContent(contents);

  }

  private Component createAdministratorUserPanel() {
    final FormLayout formLayout = new FormLayout();
    Label section = new Label(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_ADMINISTRATOR));
    section.addStyleName(ValoTheme.LABEL_H3);
    section.addStyleName(ValoTheme.LABEL_COLORED);
    formLayout.addComponent(section);

    formLayout.setStyleName(ValoTheme.FORMLAYOUT_LIGHT);
    formLayout.setWidth(100, Sizeable.Unit.PERCENTAGE);
    formLayout.setMargin(true);

    TextField username = new TextField(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_USERNAME));

    this.userBinder.forField(username)
            .asRequired(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_REQUIRED))
            .withValidator(new StringLengthValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_REQUIRED), 1, null))
            .withValidator(new RegexpValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_ONLYDIGITS), "[a-zA-Z0-9]+"))
            .bind(User::getName, User::setName);
    username.setRequiredIndicatorVisible(false);
    formLayout.addComponent(username);

    TextField givenName = new TextField(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_FIRSTNAME));
    this.userBinder.forField(givenName)
            .asRequired(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_REQUIRED))
            .withValidator(new StringLengthValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_REQUIRED), 1, null))
            .bind(User::getGivenName, User::setGivenName);
    givenName.setRequiredIndicatorVisible(false);
    formLayout.addComponent(givenName);

    TextField sn = new TextField(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_LASTNAME));
    this.userBinder.forField(sn)
            .asRequired(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_REQUIRED))
            .withValidator(new StringLengthValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_REQUIRED), 1, null))
            .bind(User::getSn, User::setSn);
    sn.setRequiredIndicatorVisible(false);
    formLayout.addComponent(sn);

    TextArea description = new TextArea(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_DESCRIPTION));
    description.setRows(2);
    this.userBinder.forField(description).bind(User::getDescription, User::setDescription);
    description.setRequiredIndicatorVisible(false);
    formLayout.addComponent(description);


    final PasswordField password = new PasswordField(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_PASSWD));
    this.userBinder.forField(password)
            .asRequired(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_REQUIRED)) //
            .withValidator(new StringLengthValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_REQUIRED), 1, null))
            .withConverter(String::getBytes, String::new)
            .withNullRepresentation(new byte[0]) //
            .bind(User::getUserPassword, User::setUserPassword);
    formLayout.addComponent(password);

    final PasswordField passwordVerify = new PasswordField(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_PASSWD_REPEAT));
    this.userBinder.forField(passwordVerify)
            .asRequired(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_REQUIRED))
            .withNullRepresentation("") //
            .withValidator(new PasswordIdenticalValidator(password))
            .withValidator(new StringLengthValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_REQUIRED), 1, null))
            .bind(User::getVerifyPassword, User::setVerifyPassword);
    formLayout.addComponent(passwordVerify);

    return formLayout;

  }

  private Component createPrimaryOUPanel() {

    final FormLayout formLayout = new FormLayout();
    formLayout.setStyleName("primaryoupanel");

    Label section = new Label(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_SYSTEM));
    section.addStyleName(ValoTheme.LABEL_H3);
    section.addStyleName(ValoTheme.LABEL_COLORED);
    formLayout.addComponent(section);

    formLayout.addStyleName(ValoTheme.FORMLAYOUT_LIGHT);
    formLayout.setWidth(100, Sizeable.Unit.PERCENTAGE);
    formLayout.setMargin(true);

    TextField systemName = new TextField(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_SYSTEMNAME), "name");
    this.primaryOUBinder.forField(systemName)
            .asRequired(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_REQUIRED))
            .withValidator(new StringLengthValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_REQUIRED), 1, null))
            .withValidator(new RegexpValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_ONLYDIGITS), "[a-zA-Z0-9]+"))
            .bind(OrganizationalUnit::getName, OrganizationalUnit::setName);
    systemName.setRequiredIndicatorVisible(false);
    formLayout.addComponent(systemName);

    TextArea description = new TextArea(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_SYSTEMDESCRIPTION), "description");
    description.setRows(1);
    this.primaryOUBinder.bind(description, OrganizationalUnit::getDescription, OrganizationalUnit::setDescription);
    formLayout.addComponent(description);

    return formLayout;
  }

  @Override
  public String getCaption() {
    return mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_TITLE);
  }

  @Override
  public boolean onAdvance() {

    try {
      primaryOUBinder.writeBean(systemSetupModel.getDirectoryModel().getPrimaryOU());
    } catch (ValidationException e) {
      notifyUser(e);
      return false;
    }
    try {
      userBinder.writeBean(systemSetupModel.getDirectoryModel().getAdministratorUser());
    } catch (ValidationException e) {
      notifyUser(e);

      return false;
    }

    // everything went well. Apply the password and proceed
    // About the different password properties:
    // NewPassword: the password that the user entered during a password change dialog or the first start wizard
    // VerifiedPassword: the NewPassword provided a second time to ensure that the used didn't mistype his password
    // UserPassword: the actual password stored in LDAP
    String password = new String(systemSetupModel.getDirectoryModel().getAdministratorUser().getUserPassword());
    systemSetupModel.getDirectoryModel().getAdministratorUser().setNewPassword(password);

    return true;
  }

  private void notifyUser(ValidationException e) {
    e.getFieldValidationErrors() //
            .stream() //
            .filter(s -> s.getField() instanceof Component) //
            .findFirst() //
            .ifPresent(s -> {
              final String caption = ((Component) s.getField()).getCaption();
              Notification.show(caption, s.getMessage().get(), Notification.Type.ERROR_MESSAGE);
            });
  }

  @Override
  public boolean onBack() {
    return true;
  }
}
