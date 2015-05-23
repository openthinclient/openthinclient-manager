package org.openthinclient.wizard.ui.steps;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import org.vaadin.teemu.wizards.WizardStep;

public class ConfigureNetworkStep extends AbstractStep implements WizardStep {

  private final NetworkConfigurationModel model = new NetworkConfigurationModel();
  private final CheckBox directConnectionCheckBox;
  private final CheckBox proxyConnectionCheckBox;
  private final ProxyConfigurationForm proxyConfigurationForm;
  private final CheckBox noConnectionCheckBox;

  public ConfigureNetworkStep() {
    final VerticalLayout layout = new VerticalLayout();
    layout.setSpacing(true);
    layout.setMargin(true);

    final Label title = createLabelH1("Configure Network");
    layout.addComponent(title);
    layout.setComponentAlignment(title, Alignment.MIDDLE_CENTER);

    this.directConnectionCheckBox = new CheckBox("Direct internet connection", model.directConnectionProperty);
    this.directConnectionCheckBox.setStyleName(ValoTheme.CHECKBOX_LARGE);
    layout.addComponent(this.directConnectionCheckBox);

    this.proxyConnectionCheckBox = new CheckBox("Internet connection using proxy", model.proxyConnectionProperty);
    this.proxyConnectionCheckBox.setStyleName(ValoTheme.CHECKBOX_LARGE);

    // proxy connection type, including the required form
    // enable or disable the proxy configuration form, depending on whether the proxy connection has been selected
    this.proxyConfigurationForm = createProxyConfigurationForm();
    this.proxyConnectionCheckBox.addValueChangeListener(e -> this.proxyConfigurationForm.setEnabled(this.proxyConnectionCheckBox.getValue()));
    // initialize the form state
    this.proxyConfigurationForm.setEnabled(this.proxyConnectionCheckBox.getValue());
    final HorizontalLayout proxyConfig = new HorizontalLayout(this.proxyConnectionCheckBox, this.proxyConfigurationForm);
    proxyConfig.setSpacing(true);
    layout.addComponent(proxyConfig);


    this.noConnectionCheckBox = new CheckBox("No internet access", model.noConnectionProperty);
    this.noConnectionCheckBox.setStyleName(ValoTheme.CHECKBOX_LARGE);
    layout.addComponent(this.noConnectionCheckBox);

    setContent(layout);

  }

  @Override
  public String getCaption() {
    return "Configure Network";
  }

  protected ProxyConfigurationForm createProxyConfigurationForm() {
    return new ProxyConfigurationForm(model);

  }

  @Override
  public boolean onAdvance() {
    return false;
  }

  @Override
  public boolean onBack() {
    return true;
  }
}
