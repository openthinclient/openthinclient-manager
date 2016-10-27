package org.openthinclient.wizard.ui.steps.net;

import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_AUTH;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_HOST_INVALID;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_HOST_MISSING;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_PORT_INVALID;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import org.openthinclient.wizard.model.NetworkConfigurationModel;

import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.util.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

public class ProxyConfigurationForm extends CustomComponent {

  /** serialVersionUID */
  private static final long serialVersionUID = -120512201002490319L;
  
  private final Field<?> hostField;
  private final TextField portField;
  private final Field<?> userField;
  private final PasswordField passwordField;
  private final CheckBox authenticationCheckbox;
  private final FieldGroup fieldGroup;

  public ProxyConfigurationForm(NetworkConfigurationModel networkConfigurationModel) {
    
    IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());

    authenticationCheckbox = new CheckBox(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_AUTH));
    this.fieldGroup = new FieldGroup(networkConfigurationModel.getProxyConfigurationItem());
    hostField = this.fieldGroup.buildAndBind("Hostname", "host");
    userField = this.fieldGroup.buildAndBind("Username", "user");
    passwordField = this.fieldGroup.buildAndBind("Password", "password", PasswordField.class);
    portField = this.fieldGroup.buildAndBind("Port", "port", TextField.class);
    
    // Set special converter to NOT use thousand separator on 'port'-field
    portField.setConverter(new StringToIntegerConverter() {
      private static final long serialVersionUID = -6464686484330572080L;
      @Override
      protected NumberFormat getFormat(Locale locale) {
        // do not use a thousands separator, as HTML5 input type
        // number expects a fixed wire/DOM number format regardless
        // of how the browser presents it to the user (which could
        // depend on the browser locale)
        DecimalFormat format = new DecimalFormat();
        format.setMaximumFractionDigits(0);
        format.setDecimalSeparatorAlwaysShown(false);
        format.setParseIntegerOnly(true);
        format.setGroupingUsed(false);
        return format;
      }
    });
    
    final FormLayout form = new FormLayout();

    hostField.addValidator(new StringLengthValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_HOST_MISSING), 1, null, false));
    hostField.addValidator(new HostnameValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_HOST_INVALID)));

    portField.addValidator(new IntegerRangeValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_PORT_INVALID), 1, 65535));


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
