package org.openthinclient.wizard.ui.steps.net;

import com.vaadin.data.Binder;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

import org.openthinclient.manager.util.http.config.NetworkConfiguration;
import org.openthinclient.wizard.model.NetworkConfigurationModel;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_AUTH;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_HOST_INVALID;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_HOST_MISSING;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_PORT_INVALID;

public class ProxyConfigurationForm extends CustomComponent {

  /** serialVersionUID */
  private static final long serialVersionUID = -120512201002490319L;
  
  private final TextField hostField;
  private final TextField portField;
  private final TextField userField;
  private final PasswordField passwordField;
  private final CheckBox authenticationCheckbox;
//  private final FieldGroup fieldGroup;
  private final Binder<NetworkConfiguration.ProxyConfiguration> binder;

  private final NetworkConfigurationModel networkConfigurationModel;

  public ProxyConfigurationForm(NetworkConfigurationModel networkConfigurationModel) {
    
    IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());

    this.networkConfigurationModel = networkConfigurationModel;

    authenticationCheckbox = new CheckBox(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_AUTH));
//    this.fieldGroup = new FieldGroup(networkConfigurationModel.getProxyConfigurationItem());

    this.binder = new Binder<>();

    userField = new TextField("Username");
    userField.setPlaceholder("Username");
    passwordField = new PasswordField("Password");
    passwordField.setPlaceholder("Password");

    this.binder.forField(userField) //
            .withNullRepresentation("") // empty string instead of "null"
            .bind(NetworkConfiguration.ProxyConfiguration::getUser, NetworkConfiguration.ProxyConfiguration::setUser);
    this.binder.forField(passwordField) //
            .withNullRepresentation("") //
            .bind(NetworkConfiguration.ProxyConfiguration::getPassword, NetworkConfiguration.ProxyConfiguration::setPassword);

    // hostField
    hostField = new TextField("Hostname");
    hostField.setPlaceholder("proxy.example.com");
    this.binder.forField(hostField)
            .withValidator(new StringLengthValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_HOST_MISSING), 1, null))
            .withValidator(new HostnameValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_HOST_INVALID)))
            .withNullRepresentation("")
            .bind(NetworkConfiguration.ProxyConfiguration::getHost, NetworkConfiguration.ProxyConfiguration::setHost);

    // portField with converter to NOT use thousand separator on 'port'-field
    portField = new TextField("Port");
    this.binder.forField(portField)
               .withConverter(new StringToIntegerConverter(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_PORT_INVALID)) {
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
                })
              .withValidator(new IntegerRangeValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_PORT_INVALID), 1, 65535))
              .bind(NetworkConfiguration.ProxyConfiguration::getPort, NetworkConfiguration.ProxyConfiguration::setPort);

    // read the bean last, as it will only update bindings that already exist.
    this.binder.readBean(networkConfigurationModel.getProxyConfiguration());

    final FormLayout form = new FormLayout();
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

  public void commit() {
    binder.writeBeanIfValid(networkConfigurationModel.getProxyConfiguration());
  }

}
