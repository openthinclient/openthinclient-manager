package org.openthinclient.wizard.ui.steps.net;

import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.advisor.check.CheckExecutionEngine;
import org.openthinclient.advisor.check.CheckInternetConnection;
import org.openthinclient.wizard.model.NetworkConfigurationModel;
import org.openthinclient.wizard.ui.CheckingProgressPresenter;
import org.openthinclient.wizard.ui.CheckingProgressWindow;
import org.openthinclient.wizard.ui.steps.AbstractStep;
import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.WizardStep;

public class ConfigureNetworkStep extends AbstractStep implements WizardStep {

  private final NetworkConfigurationModel model = new NetworkConfigurationModel();
  private final CheckBox directConnectionCheckBox;
  private final CheckBox proxyConnectionCheckBox;
  private final ProxyConfigurationForm proxyConfigurationForm;
  private final CheckBox noConnectionCheckBox;
  private final Wizard wizard;
  private final CheckExecutionEngine checkExecutionEngine;
  private boolean checkSucessfullyRun = false;

  public ConfigureNetworkStep(Wizard wizard, CheckExecutionEngine checkExecutionEngine) {
    this.wizard = wizard;
    this.checkExecutionEngine = checkExecutionEngine;
    final VerticalLayout layout = new VerticalLayout();
    layout.setSpacing(true);
    layout.setMargin(true);

    final Label title = createLabelH1("Configure Network");
    layout.addComponent(title);
    layout.setComponentAlignment(title, Alignment.MIDDLE_CENTER);

    this.directConnectionCheckBox = new CheckBox("Direct internet connection", model.getDirectConnectionProperty());
    this.directConnectionCheckBox.setStyleName(ValoTheme.CHECKBOX_LARGE);
    layout.addComponent(this.directConnectionCheckBox);

    this.proxyConnectionCheckBox = new CheckBox("Internet connection using proxy", model.getProxyConnectionProperty());
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


    this.noConnectionCheckBox = new CheckBox("No internet access", model.getNoConnectionProperty());
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

    if (model.getNoConnectionProperty().getValue()) {
      // the system has been configured not to use a internet connection
      return true;
    }

    if (checkSucessfullyRun) {
      return true;
    }

    if (model.getProxyConnectionProperty().getValue()) {
      try {
        proxyConfigurationForm.commit();
      } catch (FieldGroup.CommitException e) {
        // the given values are not valid
        return false;
      }
    }


    // We require a internet connection check.
    final CheckingProgressWindow checkingProgressWindow = new CheckingProgressWindow();

    wizard.getUI().addWindow(checkingProgressWindow);

    final CheckInternetConnection check = new CheckInternetConnection();
    if (model.getProxyConnectionProperty().getValue()) {
      // we're using a proxy configuration
      check.setProxyConfiguration(model.getProxyConfiguration());
    }

    final CheckingProgressPresenter presenter = new CheckingProgressPresenter(checkExecutionEngine, checkingProgressWindow, result -> {
      if (result == CheckingProgressPresenter.Result.SUCCESS_OK) {
        checkSucessfullyRun = true;
        wizard.getUI().access(wizard::next);
      }
    });
    presenter.execute(check);

    return false;
  }

  @Override
  public boolean onBack() {
    return true;
  }


}
