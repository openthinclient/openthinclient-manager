package org.openthinclient.wizard.ui.steps.net;

import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.PasswordField;
import org.openthinclient.wizard.model.NetworkConfigurationModel;

public class ProxyConfigurationForm extends CustomComponent {

  private final Field<?> hostField;
  private final Field<?> portField;
  private final Field<?> userField;
  private final PasswordField passwordField;
  private final CheckBox authenticationCheckbox;
  private final FieldGroup fieldGroup;

  public ProxyConfigurationForm(NetworkConfigurationModel networkConfigurationModel) {

    authenticationCheckbox = new CheckBox("Proxy requires Authentication");
    this.fieldGroup = new FieldGroup(networkConfigurationModel.getProxyConfigurationItem());
    hostField = this.fieldGroup.buildAndBind("Hostname", "host");
    userField = this.fieldGroup.buildAndBind("Username", "user");
    passwordField = this.fieldGroup.buildAndBind("Password", "password", PasswordField.class);
    portField = this.fieldGroup.buildAndBind("Port", "port");

    final FormLayout form = new FormLayout();

    hostField.addValidator(new StringLengthValidator("No hostname specified", 1, null, false));
    hostField.addValidator(new HostnameValidator("Not a valid hostname or IP"));

    portField.addValidator(new IntegerRangeValidator("Invalid port number", 1, 65535));

    form.addComponent(hostField);
    form.addComponent(portField);
    form.addComponent(authenticationCheckbox);
    form.addComponent(userField);
    form.addComponent(passwordField);

    authenticationCheckbox.addValueChangeListener(e -> {
      updateEnabledState();
    });

    updateEnabledState();

    setCompositionRoot(form);

  }

  private void updateEnabledState() {

    hostField.setEnabled(isEnabled());
    portField.setEnabled(isEnabled());
    boolean userEditEnabled = isEnabled() && authenticationCheckbox.getValue();
    userField.setEnabled(userEditEnabled);
    passwordField.setEnabled(userEditEnabled);
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    updateEnabledState();
  }

  public void commit() throws FieldGroup.CommitException {
    fieldGroup.commit();
  }

  public void discard() {
    fieldGroup.discard();
  }

  public boolean isValid() {
    return fieldGroup.isValid();
  }

  public boolean isModified() {
    return fieldGroup.isModified();
  }

  public void clear() {
    fieldGroup.clear();
  }
}
