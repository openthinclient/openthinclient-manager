package org.openthinclient.wizard.ui.steps;

import org.openthinclient.common.model.OrganizationalUnit;
import org.openthinclient.common.model.User;
import org.openthinclient.wizard.model.SystemSetupModel;
import org.vaadin.spring.i18n.I18N;
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

  public ConfigureDirectoryStep(I18N i18n, Wizard wizard, SystemSetupModel systemSetupModel) {
    super(i18n);
    
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

    contents.addComponent(createLabelH1("Configure Environment"));

    contents.addComponent(createPrimaryOUPanel(primaryOUFieldGroup));
    contents.addComponent(createAdministratorUserPanel(adminUserfieldGroup, adminPasswordGroup));

    setContent(contents);

  }

  private Component createAdministratorUserPanel(BeanFieldGroup<User> fieldGroup, FieldGroup adminPasswordGroup) {
    final FormLayout formLayout = new FormLayout();
    Label section = new Label("Administration User");
    section.addStyleName(ValoTheme.LABEL_H3);
    section.addStyleName(ValoTheme.LABEL_COLORED);
    formLayout.addComponent(section);

    formLayout.setStyleName(ValoTheme.FORMLAYOUT_LIGHT);
    formLayout.setWidth(100, Sizeable.Unit.PERCENTAGE);
    formLayout.setMargin(true);

    final Field<?> username = fieldGroup.buildAndBind("Username", "name");
    requiredField(username);
    username.addValidator(new RegexpValidator("[a-zA-Z0-9]+", "Only digits and characters are allowed"));
    formLayout.addComponent(username);
    final Field<?> givenName = fieldGroup.buildAndBind("First Name", "givenName");
    requiredField(givenName);
    formLayout.addComponent(givenName);
    final Field<?> sn = fieldGroup.buildAndBind("Last Name", "sn");
    requiredField(sn);
    formLayout.addComponent(sn);
    formLayout.addComponent(fieldGroup.buildAndBind("Description", "description", TextArea.class));

    final PasswordField password = adminPasswordGroup.buildAndBind("Password", "password", PasswordField.class);
    requiredField(password);
    formLayout.addComponent(password);
    final PasswordField passwordVerify = adminPasswordGroup.buildAndBind("Repeat", "passwordVerify", PasswordField.class);
    requiredField(passwordVerify);
    passwordVerify.addValidator(new PasswordIdenticalValidator(password));
    formLayout.addComponent(passwordVerify);

    return formLayout;

  }

  private void requiredField(Field<?> field) {
//    field.setRequired(true);
//    field.setRequiredError("This field is required");
    field.setRequired(true);
    field.addValidator(new StringLengthValidator("This field is required", 1, null, false));
  }

  private Component createPrimaryOUPanel(BeanFieldGroup<OrganizationalUnit> fieldGroup) {
    final FormLayout formLayout = new FormLayout();

    Label section = new Label("Common System Details");
    section.addStyleName(ValoTheme.LABEL_H3);
    section.addStyleName(ValoTheme.LABEL_COLORED);
    formLayout.addComponent(section);

    formLayout.setStyleName(ValoTheme.FORMLAYOUT_LIGHT);
    formLayout.setWidth(100, Sizeable.Unit.PERCENTAGE);
    formLayout.setMargin(true);

    final Field<?> systemName = fieldGroup.buildAndBind("System name", "name");
    requiredField(systemName);
    systemName.addValidator(new RegexpValidator("[a-zA-Z0-9]+", "Only digits and characters are allowed"));

    formLayout.addComponent(systemName);
    formLayout.addComponent(fieldGroup.buildAndBind("Description", "description", TextArea.class));

    return formLayout;
  }

  @Override
  public String getCaption() {
    return "Configuration";
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
