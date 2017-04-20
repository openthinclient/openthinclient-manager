package org.openthinclient.wizard.ui.steps.net;

import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.advisor.check.CheckExecutionEngine;
import org.openthinclient.advisor.check.CheckInternetConnection;
import org.openthinclient.wizard.model.SystemSetupModel;
import org.openthinclient.wizard.ui.CheckingProgressPresenter;
import org.openthinclient.wizard.ui.CheckingProgressWindow;
import org.openthinclient.wizard.ui.steps.AbstractStep;
import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.WizardStep;

import static org.openthinclient.wizard.FirstStartWizardMessages.*;

public class ConfigureNetworkStep extends AbstractStep implements WizardStep {

  private final CheckBox directConnectionCheckBox;
  private final CheckBox proxyConnectionCheckBox;
  private final ProxyConfigurationForm proxyConfigurationForm;
  //  private final CheckBox noConnectionCheckBox;
  private final Wizard wizard;
  private final CheckExecutionEngine checkExecutionEngine;
  private final SystemSetupModel systemSetupModel;
  private boolean checkSucessfullyRun;

  public ConfigureNetworkStep(Wizard wizard, CheckExecutionEngine checkExecutionEngine, SystemSetupModel systemSetupModel) {
    
    this.wizard = wizard;
    this.checkExecutionEngine = checkExecutionEngine;
    this.systemSetupModel = systemSetupModel;
    final VerticalLayout layout = new VerticalLayout();
    layout.setSpacing(true);
    layout.setMargin(true);

    final Label title = createLabelH1(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_HEADLINE));
    layout.addComponent(title);
    layout.setComponentAlignment(title, Alignment.MIDDLE_LEFT);

    this.directConnectionCheckBox = new CheckBox(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_DIRECT_CONNECTION), systemSetupModel.getNetworkConfigurationModel().getDirectConnectionProperty());
    this.directConnectionCheckBox.setStyleName(ValoTheme.CHECKBOX_LARGE);
    layout.addComponent(this.directConnectionCheckBox);

    this.proxyConnectionCheckBox = new CheckBox(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION), systemSetupModel.getNetworkConfigurationModel().getProxyConnectionProperty());
    this.proxyConnectionCheckBox.setStyleName(ValoTheme.CHECKBOX_LARGE);

    // proxy connection type, including the required form
    // enable or disable the proxy configuration form, depending on whether the proxy connection has been selected
    this.proxyConfigurationForm = createProxyConfigurationForm();
    this.proxyConnectionCheckBox.addValueChangeListener(e -> {
      this.proxyConfigurationForm.setEnabled(this.proxyConnectionCheckBox.getValue());
      this.directConnectionCheckBox.setValue(!this.proxyConnectionCheckBox.getValue());

      if (this.proxyConnectionCheckBox.getValue()) {
        systemSetupModel.getNetworkConfigurationModel().enableProxyConnectionProperty();
      }
    });
    // enable/disable connection settings
    this.directConnectionCheckBox.addValueChangeListener(event -> {
      this.proxyConfigurationForm.setEnabled(!this.directConnectionCheckBox.getValue());
      this.proxyConnectionCheckBox.setValue(!this.directConnectionCheckBox.getValue());
      if (this.directConnectionCheckBox.getValue()) {
        systemSetupModel.getNetworkConfigurationModel().enableDirectConnectionProperty();
      }
    });

    // initialize the form state
    this.proxyConfigurationForm.setEnabled(this.proxyConnectionCheckBox.getValue());
    final HorizontalLayout proxyConfig = new HorizontalLayout(this.proxyConnectionCheckBox, this.proxyConfigurationForm);
    proxyConfig.setSpacing(true);
    layout.addComponent(proxyConfig);

    setContent(layout);

  }

  @Override
  public String getCaption() {
    return mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_TITLE);
  }

  protected ProxyConfigurationForm createProxyConfigurationForm() {
    return new ProxyConfigurationForm(systemSetupModel.getNetworkConfigurationModel());

  }

  @Override
  public boolean onAdvance() {

    if (systemSetupModel.getNetworkConfigurationModel().getNoConnectionProperty()) {
      // the system has been configured not to use a internet connection
      return true;
    }

    if (checkSucessfullyRun) {
      return true;
    }

    // apply proxy-from values to model-object
    if (proxyConnectionCheckBox.getValue()) {
        proxyConfigurationForm.commit();
    }

    // We require a internet connection check.
    final CheckingProgressWindow checkingProgressWindow = new CheckingProgressWindow();

    wizard.getUI().addWindow(checkingProgressWindow);

    final CheckInternetConnection check = new CheckInternetConnection(UI.getCurrent().getLocale());
    if (systemSetupModel.getNetworkConfigurationModel().getProxyConnectionProperty()) {
      // we're using a proxy configuration
      check.setProxyConfiguration(systemSetupModel.getNetworkConfigurationModel().getProxyConfiguration());
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
