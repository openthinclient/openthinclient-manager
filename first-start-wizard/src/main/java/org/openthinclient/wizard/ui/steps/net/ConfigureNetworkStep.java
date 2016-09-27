package org.openthinclient.wizard.ui.steps.net;

import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_DIRECT_CONNECTION;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_NO_CONNECTION;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_TITLE;

import org.openthinclient.advisor.check.CheckExecutionEngine;
import org.openthinclient.advisor.check.CheckInternetConnection;
import org.openthinclient.wizard.model.SystemSetupModel;
import org.openthinclient.wizard.ui.CheckingProgressPresenter;
import org.openthinclient.wizard.ui.CheckingProgressWindow;
import org.openthinclient.wizard.ui.steps.AbstractStep;
import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.WizardStep;

import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class ConfigureNetworkStep extends AbstractStep implements WizardStep {

  private final CheckBox directConnectionCheckBox;
  private final CheckBox proxyConnectionCheckBox;
  private final ProxyConfigurationForm proxyConfigurationForm;
  private final CheckBox noConnectionCheckBox;
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

    final Label title = createLabelH1(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_TITLE));
    layout.addComponent(title);
    layout.setComponentAlignment(title, Alignment.MIDDLE_CENTER);

    this.directConnectionCheckBox = new CheckBox(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_DIRECT_CONNECTION), systemSetupModel.getNetworkConfigurationModel().getDirectConnectionProperty());
    this.directConnectionCheckBox.setStyleName(ValoTheme.CHECKBOX_LARGE);
    layout.addComponent(this.directConnectionCheckBox);

    this.proxyConnectionCheckBox = new CheckBox(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_PROXY_CONNECTION), systemSetupModel.getNetworkConfigurationModel().getProxyConnectionProperty());
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


    this.noConnectionCheckBox = new CheckBox(mc.getMessage(UI_FIRSTSTART_INSTALLSTEPS_CONFIGURENETWORKSTEP_NO_CONNECTION), systemSetupModel.getNetworkConfigurationModel().getNoConnectionProperty());
    this.noConnectionCheckBox.setStyleName(ValoTheme.CHECKBOX_LARGE);
    layout.addComponent(this.noConnectionCheckBox);

    setContent(layout);

  }

  @Override
  public String getCaption() {
    return "Network";
  }

  protected ProxyConfigurationForm createProxyConfigurationForm() {
    return new ProxyConfigurationForm(systemSetupModel.getNetworkConfigurationModel());

  }

  @Override
  public boolean onAdvance() {

    if (systemSetupModel.getNetworkConfigurationModel().getNoConnectionProperty().getValue()) {
      // the system has been configured not to use a internet connection
      return true;
    }

    if (checkSucessfullyRun) {
      return true;
    }

    if (systemSetupModel.getNetworkConfigurationModel().getProxyConnectionProperty().getValue()) {
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
    if (systemSetupModel.getNetworkConfigurationModel().getProxyConnectionProperty().getValue()) {
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
