package org.openthinclient.wizard.ui.steps;

import com.vaadin.data.Binder;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.server.Sizeable;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.common.model.OrganizationalUnit;
import org.openthinclient.common.model.User;
import org.openthinclient.wizard.model.SystemSetupModel;
import org.vaadin.teemu.wizards.Wizard;

import static org.openthinclient.wizard.FirstStartWizardMessages.*;

public class ConfigureDirectoryStep extends AbstractStep {

//  private final BeanFieldGroup<User> adminUserfieldGroup;
//  private final FieldGroup adminPasswordGroup;
//  private final PropertysetItem adminPasswordItemSource;
  private final SystemSetupModel systemSetupModel;
//  private final BeanFieldGroup<OrganizationalUnit> primaryOUFieldGroup;

  private final Binder<OrganizationalUnit>  primaryOUBinder;
  private final Binder<User>  userBinder;

  public ConfigureDirectoryStep(Wizard wizard, SystemSetupModel systemSetupModel) {
    
    this.systemSetupModel = systemSetupModel;

    this.primaryOUBinder = new Binder<>();
    this.primaryOUBinder.setBean(systemSetupModel.getDirectoryModel().getPrimaryOU());

    this.userBinder = new Binder<>();
    this.userBinder.setBean(systemSetupModel.getDirectoryModel().getAdministratorUser());


//    primaryOUFieldGroup = new BeanFieldGroup<>(OrganizationalUnit.class);
//    primaryOUFieldGroup.setItemDataSource(systemSetupModel.getDirectoryModel().getPrimaryOU());

//    adminUserfieldGroup = new BeanFieldGroup<>(User.class);
//    adminUserfieldGroup.setItemDataSource(systemSetupModel.getDirectoryModel().getAdministratorUser());

    final VerticalLayout contents = new VerticalLayout();
    contents.setSpacing(true);
    contents.setMargin(true);


    contents.addComponent(createLabelH1(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_HEADLINE)));

    // TODO: JN Beschreibung hinzufügen,  (Weißflächen) zwischen den Konfigurationspunkten entfernen
    contents.addComponent(createLabelLarge(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_DESCRIPTION), ContentMode.HTML));
    
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

//    final Field<?> username = fieldGroup.buildAndBind(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_USERNAME), "name");
//    requiredField(username);
//    username.addValidator(new RegexpValidator("[a-zA-Z0-9]+", mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_ONLYDIGITS)));
//    formLayout.addComponent(username);
    TextField username = new TextField(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_USERNAME), "name");
    this.userBinder.forField(username)
            .withValidator(new StringLengthValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_REQUIRED), 1, null))
            .withValidator(new RegexpValidator("[a-zA-Z0-9]+", mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_ONLYDIGITS)))
            .bind(User::getName, User::setName);
    formLayout.addComponent(username);

//    final Field<?> givenName = fieldGroup.buildAndBind(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_FIRSTNAME), "givenName");
//    requiredField(givenName);
//    formLayout.addComponent(givenName);
    TextField givenName = new TextField(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_FIRSTNAME), "name");
    this.userBinder.forField(givenName)
            .withValidator(new StringLengthValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_REQUIRED), 1, null))
            .bind(User::getGivenName, User::setGivenName);
    formLayout.addComponent(givenName);

//    final Field<?> sn = fieldGroup.buildAndBind(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_LASTNAME), "sn");
//    requiredField(sn);
//    formLayout.addComponent(sn);
    TextField sn = new TextField(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_LASTNAME), "sn");
    this.userBinder.forField(sn)
            .withValidator(new StringLengthValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_REQUIRED), 1, null))
            .bind(User::getSn, User::setSn);
    formLayout.addComponent(sn);

    TextArea description = new TextArea(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_DESCRIPTION), "description");
    description.setRows(2);
    this.userBinder.forField(description).bind(User::getDescription, User::setDescription);
    formLayout.addComponent(description);

//    adminPasswordItemSource = new PropertysetItem();
//    adminPasswordItemSource.addItemProperty("password", new ObjectProperty<>("", String.class));
//    adminPasswordItemSource.addItemProperty("passwordVerify", new ObjectProperty<>("", String.class));
//    adminPasswordGroup = new FieldGroup(adminPasswordItemSource);

    final PasswordField password = new PasswordField(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_PASSWD), "password");
    this.userBinder.forField(password)
            .withValidator(new StringLengthValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_REQUIRED), 1, null))
            .withConverter(String::getBytes, String::valueOf)
            .bind(User::getUserPassword, User::setUserPassword);
    formLayout.addComponent(password);

    final PasswordField passwordVerify = new PasswordField(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_PASSWD_REPEAT), "passwordVerify");
    this.userBinder.forField(passwordVerify)
            .withValidator(new PasswordIdenticalValidator(password))
            .withValidator(new StringLengthValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_REQUIRED), 1, null))
            .bind(User::getVerifyPassword, User::setVerifyPassword);
    formLayout.addComponent(passwordVerify);

    return formLayout;

  }

//  private void requiredField(Field<?> field) {
////    field.setRequired(true);
////    field.setRequiredError("This field is required");
//    field.setRequired(true);
//    field.addValidator(new StringLengthValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_REQUIRED), 1, null, false));
//  }

  private Component createPrimaryOUPanel() {

    final FormLayout formLayout = new FormLayout();
    formLayout.setStyleName("primaryoupanel");

    Label section = new Label(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_SYSTEM));
    section.addStyleName(ValoTheme.LABEL_H3);
    section.addStyleName(ValoTheme.LABEL_COLORED);
    formLayout.addComponent(section);

    formLayout.setStyleName(ValoTheme.FORMLAYOUT_LIGHT);
    formLayout.setWidth(100, Sizeable.Unit.PERCENTAGE);
    formLayout.setMargin(true);

//    final Field<?> systemName = fieldGroup.buildAndBind(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_SYSTEMNAME), "name");
//    requiredField(systemName);
//    systemName.addValidator(new RegexpValidator("[a-zA-Z0-9]+", mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_ONLYDIGITS)));
    TextField systemName = new TextField(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_SYSTEMNAME), "name");
    this.primaryOUBinder.forField(systemName)
            .withValidator(new StringLengthValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_REQUIRED), 1, null))
            .withValidator(new RegexpValidator("[a-zA-Z0-9]+", mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_ONLYDIGITS)))
            .bind(OrganizationalUnit::getName, OrganizationalUnit::setName);
    formLayout.addComponent(systemName);

//    TextArea description = fieldGroup.buildAndBind(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_SYSTEMDESCRIPTION), "description", TextArea.class);
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


//    try {
//      primaryOUFieldGroup.commit();
//    } catch (FieldGroup.CommitException e) {
//      return false;
//    }
    this.primaryOUBinder.writeBeanIfValid(systemSetupModel.getDirectoryModel().getPrimaryOU());

//    try {
//      adminUserfieldGroup.commit();
//    } catch (FieldGroup.CommitException e) {
//      return false;
//    }
    this.userBinder.writeBeanIfValid(systemSetupModel.getDirectoryModel().getAdministratorUser());

//    try {
//      adminPasswordGroup.commit();
//    } catch (FieldGroup.CommitException e) {
//      return false;
//    }

    // everything went well. Apply the password and proceed
//    String password = (String) adminPasswordItemSource.getItemProperty("password").getValue();
    // FIXME: was ist der Unterschied zwischen: UserPassword, VerifiedPassword, NewPassword??
    String password = new String(systemSetupModel.getDirectoryModel().getAdministratorUser().getUserPassword());
    systemSetupModel.getDirectoryModel().getAdministratorUser().setNewPassword(password);

    return true;
  }

  @Override
  public boolean onBack() {
    return true;
  }
}
