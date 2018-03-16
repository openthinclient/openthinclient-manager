package org.openthinclient.web.support;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_BUTTON_RESET;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_BUTTON_SAVE;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_CONFIGURATION_PROXY_CONNECTION_AUTH;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_CONFIGURATION_PROXY_CONNECTION_HOST_INVALID;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_CONFIGURATION_PROXY_CONNECTION_HOST_MISSING;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_CONFIGURATION_PROXY_CONNECTION_PORT_INVALID;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_CONFIGURATION_PROXY_ENABLED;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_CONFIGURATION_PROXY_HOSTNAME;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_CONFIGURATION_PROXY_PASSWORD;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_CONFIGURATION_PROXY_PORT;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_CONFIGURATION_PROXY_USERNAME;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.Binder;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import org.openthinclient.manager.util.http.config.NetworkConfiguration;
import org.openthinclient.manager.util.http.config.NetworkConfiguration.ProxyConfiguration;
import org.vaadin.viritin.button.MButton;

public class ProxyConfigurationForm extends CustomComponent {
  
  private final TextField hostField;
  private final TextField portField;
  private final TextField userField;
  private final PasswordField passwordField;
  private final CheckBox authenticationCheckbox;
  private final CheckBox useProxyCheckbox;
  private final HorizontalLayout buttonLine = new HorizontalLayout();

  private final Binder<NetworkConfiguration.ProxyConfiguration> binder;

  private final ProxyConfiguration proxyConfiguration;

  public ProxyConfigurationForm(ProxyConfiguration proxyConfiguration) {
    
    IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());

    this.proxyConfiguration = proxyConfiguration;

    authenticationCheckbox = new CheckBox(mc.getMessage(UI_CONFIGURATION_PROXY_CONNECTION_AUTH));
    authenticationCheckbox.setValue(this.proxyConfiguration.getUser() != null);

    this.binder = new Binder<>();

    useProxyCheckbox = new CheckBox(mc.getMessage(UI_CONFIGURATION_PROXY_ENABLED));
    userField = new TextField(mc.getMessage(UI_CONFIGURATION_PROXY_USERNAME));
    userField.setPlaceholder("Username");
    passwordField = new PasswordField(mc.getMessage(UI_CONFIGURATION_PROXY_PASSWORD));
    passwordField.setPlaceholder("Password");

    this.binder.forField(useProxyCheckbox)
               .bind(NetworkConfiguration.ProxyConfiguration::isEnabled, NetworkConfiguration.ProxyConfiguration::setEnabled);
    this.binder.forField(userField) //
            .withNullRepresentation("") // empty string instead of "null"
            .bind(NetworkConfiguration.ProxyConfiguration::getUser, NetworkConfiguration.ProxyConfiguration::setUser);
    this.binder.forField(passwordField) //
            .withNullRepresentation("") //
            .bind(NetworkConfiguration.ProxyConfiguration::getPassword, NetworkConfiguration.ProxyConfiguration::setPassword);

    // hostField
    hostField = new TextField(mc.getMessage(UI_CONFIGURATION_PROXY_HOSTNAME));
    hostField.setPlaceholder("proxy.example.com");
    this.binder.forField(hostField)
            .withValidator(new StringLengthValidator(mc.getMessage(UI_CONFIGURATION_PROXY_CONNECTION_HOST_MISSING), 1, null))
            .withValidator(new HostnameValidator(mc.getMessage(UI_CONFIGURATION_PROXY_CONNECTION_HOST_INVALID)))
            .withNullRepresentation("")
            .bind(NetworkConfiguration.ProxyConfiguration::getHost, NetworkConfiguration.ProxyConfiguration::setHost);

    // portField with converter to NOT use thousand separator on 'port'-field
    portField = new TextField(mc.getMessage(UI_CONFIGURATION_PROXY_PORT));
    this.binder.forField(portField)
               .withConverter(new StringToIntegerConverter(mc.getMessage(UI_CONFIGURATION_PROXY_CONNECTION_PORT_INVALID)) {
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
              .withValidator(new IntegerRangeValidator(mc.getMessage(UI_CONFIGURATION_PROXY_CONNECTION_PORT_INVALID), 1, 65535))
              .bind(NetworkConfiguration.ProxyConfiguration::getPort, NetworkConfiguration.ProxyConfiguration::setPort);

    // read the bean last, as it will only update bindings that already exist.
    this.binder.readBean(proxyConfiguration);

    final FormLayout form = new FormLayout();
    form.addComponent(useProxyCheckbox);
    form.addComponent(hostField);
    form.addComponent(portField);
    form.addComponent(authenticationCheckbox);
    form.addComponent(userField);
    form.addComponent(passwordField);
    form.addComponent(buttonLine);

    buttonLine.addComponent(new MButton(mc.getMessage(UI_BUTTON_RESET)).withListener((ClickListener) e -> resetValues()));
    buttonLine.addComponent(new MButton(mc.getMessage(UI_BUTTON_SAVE) ).withListener((ClickListener) e -> saveValues()));

    authenticationCheckbox.addValueChangeListener(e -> updateEnabledState());
    useProxyCheckbox.addValueChangeListener(e -> updateEnabledState());

    updateEnabledState();

    setCompositionRoot(form);

  }

  public void saveValues() {  }

  /**
   * enable/disable host, port and authCheckBox field if proxy is enabled/disabled
   */
  private void updateEnabledState() {

    hostField.setEnabled(useProxyCheckbox.getValue());
    portField.setEnabled(useProxyCheckbox.getValue());
    authenticationCheckbox.setEnabled(useProxyCheckbox.getValue());

    userField.setEnabled(authenticationCheckbox.getValue());
    passwordField.setEnabled(authenticationCheckbox.getValue());
  }

  public void commit() {

    if (!authenticationCheckbox.getValue()) {
      userField.setValue("");
      passwordField.setValue("");
    }

    binder.writeBeanIfValid(proxyConfiguration);
  }

  public void resetValues() {
    this.binder.readBean(proxyConfiguration);
    authenticationCheckbox.setValue(this.proxyConfiguration.getUser() != null);
  }

}
