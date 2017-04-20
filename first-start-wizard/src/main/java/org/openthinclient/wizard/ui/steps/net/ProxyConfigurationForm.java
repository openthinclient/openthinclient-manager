package org.openthinclient.wizard.ui.steps.net;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.Binder;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.ui.*;
import org.openthinclient.manager.util.http.config.NetworkConfiguration;
import org.openthinclient.wizard.model.NetworkConfigurationModel;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import static org.openthinclient.wizard.FirstStartWizardMessages.*;

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

    this.binder = new Binder();
    this.binder.readBean(networkConfigurationModel.getProxyConfiguration());

    userField = new TextField("Username", networkConfigurationModel.getProxyConfiguration().getUser());
    passwordField = new PasswordField("Password", networkConfigurationModel.getProxyConfiguration().getPassword());

    this.binder.bind(userField, NetworkConfiguration.ProxyConfiguration::getUser, NetworkConfiguration.ProxyConfiguration::setUser);
    this.binder.bind(passwordField, NetworkConfiguration.ProxyConfiguration::getPassword, NetworkConfiguration.ProxyConfiguration::setPassword);

    // hostField
    hostField = new TextField("Hostname", networkConfigurationModel.getProxyConfiguration().getHost());
    this.binder.forField(hostField)
               .withValidator(new StringLengthValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_HOST_MISSING), 1, null))
               .withValidator(new HostnameValidator(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION_HOST_INVALID)))
               .bind(NetworkConfiguration.ProxyConfiguration::getHost, NetworkConfiguration.ProxyConfiguration::setHost);

    // portField with converter to NOT use thousand separator on 'port'-field
    portField = new TextField("Port", String.valueOf(networkConfigurationModel.getProxyConfiguration().getPort()));
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
