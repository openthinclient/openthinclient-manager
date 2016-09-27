package org.openthinclient.wizard.ui.steps;

import org.openthinclient.common.model.OrganizationalUnit;
import org.openthinclient.common.model.User;
import static org.openthinclient.wizard.FirstStartWizardMessages.*;
import org.openthinclient.wizard.model.SystemSetupModel;
import org.vaadin.teemu.wizards.Wizard;

import com.vaadin.data.fieldgroup.BeanFieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.data.util.PropertysetItem;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class ConfigureDirectoryStep extends AbstractStep {

  private final BeanFieldGroup<User> adminUserfieldGroup;
  private final FieldGroup adminPasswordGroup;
  private final PropertysetItem adminPasswordItemSource;
  private final SystemSetupModel systemSetupModel;
  private final BeanFieldGroup<OrganizationalUnit> primaryOUFieldGroup;

  public ConfigureDirectoryStep(Wizard wizard, SystemSetupModel systemSetupModel) {
    
    this.systemSetupModel = systemSetupModel;

    primaryOUFieldGroup = new BeanFieldGroup<>(OrganizationalUnit.class);
    primaryOUFieldGroup.setItemDataSource(systemSetupModel.getDirectoryModel().getPrimaryOU());

    adminUserfieldGroup = new BeanFieldGroup<>(User.class);
    adminUserfieldGroup.setItemDataSource(systemSetupModel.getDirectoryModel().getAdministratorUser());

    final VerticalLayout contents = new VerticalLayout();
    contents.setSpacing(true);
    contents.setMargin(true);

    adminPasswordItemSource = new PropertysetItem();
    adminPasswordItemSource.addItemProperty("password", new ObjectProperty<>("", String.class));
    adminPasswordItemSource.addItemProperty("passwordVerify", new ObjectProperty<>("", String.class));
    adminPasswordGroup = new FieldGroup(adminPasswordItemSource);

    contents.addComponent(createLabelH1(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_HEADLINE)));

    contents.addComponent(createPrimaryOUPanel(primaryOUFieldGroup));
    contents.addComponent(createAdministratorUserPanel(adminUserfieldGroup, adminPasswordGroup));

    setContent(contents);

  }

  private Component createAdministratorUserPanel(BeanFieldGroup<User> fieldGroup, FieldGroup adminPasswordGroup) {
    final FormLayout formLayout = new FormLayout();
    Label section = new Label(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_ADMINISTRATOR));
    section.addStyleName(ValoTheme.LABEL_H3);
    section.addStyleName(ValoTheme.LABEL_COLORED);
    formLayout.addComponent(section);

    formLayout.setStyleName(ValoTheme.FORMLAYOUT_LIGHT);
    formLayout.setWidth(100, Sizeable.Unit.PERCENTAGE);
    formLayout.setMargin(true);

    final Field<?> username = fieldGroup.buildAndBind(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_USERNAME), "name");
    requiredField(username);
    username.addValidator(new RegexpValidator("[a-zA-Z0-9]+", mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_ONLYDIGITS)));
    formLayout.addComponent(username);
    final Field<?> givenName = fieldGroup.buildAndBind(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_FIRSTNAME), "givenName");
    requiredField(givenName);
    formLayout.addComponent(givenName);
    final Field<?> sn = fieldGroup.buildAndBind(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_LASTNAME), "sn");
    requiredField(sn);
    formLayout.addComponent(sn);
    formLayout.addComponent(fieldGroup.buildAndBind(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_DESCRIPTION), "description", TextArea.class));

    final PasswordField password = adminPasswordGroup.buildAndBind(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_PASSWD), "password", PasswordField.class);
    requiredField(password);
    formLayout.addComponent(password);
    final PasswordField passwordVerify = adminPasswordGroup.buildAndBind(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_PASSWD_REPEAT), "passwordVerify", PasswordField.class);
    requiredField(passwordVerify);
    passwordVerify.addValidator(new PasswordIdenticalValidator(password));
    formLayout.addComponent(passwordVerify);

    return formLayout;

  }

  private void requiredField(Field<?> field) {
//    field.setRequired(true);
//    field.setRequiredError("This field is required");
    field.setRequired(true);
    field.addValidator(new StringLengthValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_REQUIRED), 1, null, false));
  }

  private Component createPrimaryOUPanel(BeanFieldGroup<OrganizationalUnit> fieldGroup) {
    final FormLayout formLayout = new FormLayout();

    Label section = new Label(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_SYSTEM));
    section.addStyleName(ValoTheme.LABEL_H3);
    section.addStyleName(ValoTheme.LABEL_COLORED);
    formLayout.addComponent(section);

    formLayout.setStyleName(ValoTheme.FORMLAYOUT_LIGHT);
    formLayout.setWidth(100, Sizeable.Unit.PERCENTAGE);
    formLayout.setMargin(true);

    final Field<?> systemName = fieldGroup.buildAndBind(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_SYSTEMNAME), "name");
    requiredField(systemName);
    systemName.addValidator(new RegexpValidator("[a-zA-Z0-9]+", mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_VALIDATOR_FIELD_ONLYDIGITS)));

    formLayout.addComponent(systemName);
    formLayout.addComponent(fieldGroup.buildAndBind(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_LABEL_DIR_SYSTEMDESCRIPTION), "description", TextArea.class));

    return formLayout;
  }

  @Override
  public String getCaption() {
    return mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGUREDIRECTORYSTEP_TITLE);
  }

  @Override
  public boolean onAdvance() {


    try {
      primaryOUFieldGroup.commit();
    } catch (FieldGroup.CommitException e) {
      return false;
    }

    try {
      adminUserfieldGroup.commit();
    } catch (FieldGroup.CommitException e) {
      return false;
    }

    try {
      adminPasswordGroup.commit();
    } catch (FieldGroup.CommitException e) {
      return false;
    }

    // everything went well. Apply the password and proceed
    String password = (String) adminPasswordItemSource.getItemProperty("password").getValue();
    systemSetupModel.getDirectoryModel().getAdministratorUser().setNewPassword(password);

    return true;
  }

  @Override
  public boolean onBack() {
    return true;
  }
}
