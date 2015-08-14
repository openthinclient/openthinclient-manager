package org.openthinclient.wizard.ui;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import org.openthinclient.advisor.check.CheckExecutionEngine;
import org.openthinclient.wizard.model.SystemSetupModel;
import org.openthinclient.wizard.ui.steps.CheckEnvironmentStep;
import org.openthinclient.wizard.ui.steps.ConfigureManagerHomeStep;
import org.openthinclient.wizard.ui.steps.IntroStep;
import org.openthinclient.wizard.ui.steps.ReadyToInstallStep;
import org.openthinclient.wizard.ui.steps.net.ConfigureNetworkStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.annotation.VaadinUI;
import org.vaadin.spring.annotation.VaadinUIScope;
import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

@Theme("otc-wizard")
@VaadinUI
@VaadinUIScope
@Push
public class FirstStartWizardUI extends UI {

  @Autowired
  private SystemSetupModel systemSetupModel;
  @Autowired
  private CheckExecutionEngine checkExecutionEngine;

  @Override
  protected void init(VaadinRequest request) {

    // create the root layout and add the wizard
    final VerticalLayout root = new VerticalLayout();
    root.setSizeFull();

    root.addComponent(createHeader());

    if (systemSetupModel.getInstallModel().isInstallInProgress()) {
      initInstallProgress(root);
    } else {
      initWizard(root);
    }

    setContent(root);


  }

  private void initInstallProgress(VerticalLayout root) {

    final SystemInstallProgressView progressView = new SystemInstallProgressView();

    final VerticalLayout viewWrapper = new VerticalLayout();
    viewWrapper.setMargin(true);
    viewWrapper.setSpacing(true);
    viewWrapper.setSizeFull();
    viewWrapper.addComponent(progressView);

    root.addComponent(viewWrapper);
    root.setExpandRatio(viewWrapper, 1f);

    final SystemInstallProgressPresenter presenter = new SystemInstallProgressPresenter(systemSetupModel.getInstallModel());
    presenter.present(getUI(), progressView);
  }

  private void initWizard(final VerticalLayout root) {
    final Wizard wizard = createWizard();

    final VerticalLayout wizardWrapper = new VerticalLayout();
    wizardWrapper.setMargin(true);
    wizardWrapper.setSpacing(true);
    wizardWrapper.setSizeFull();
    wizardWrapper.addComponent(wizard);

    root.addComponent(wizardWrapper);
    root.setExpandRatio(wizardWrapper, 1.0f);

    wizard.addListener(new WizardProgressListener() {
      @Override
      public void activeStepChanged(WizardStepActivationEvent event) {

      }

      @Override
      public void stepSetChanged(WizardStepSetChangedEvent event) {

      }

      @Override
      public void wizardCompleted(WizardCompletedEvent event) {

        // FIXME implement a way to select a base system to install
        systemSetupModel.getInstallModel().installSystem(systemSetupModel.getInstallModel().getInstallableDistributions().get(0));

        root.removeComponent(wizardWrapper);

        initInstallProgress(root);
      }

      @Override
      public void wizardCancelled(WizardCancelledEvent event) {

      }
    });
  }

  private Wizard createWizard() {
    Wizard wizard = new Wizard();
    wizard.setSizeFull();
    wizard.setUriFragmentEnabled(true);

    // disabling the cancel button, as the wizard can not really be cancelled.
    wizard.getCancelButton().setVisible(false);

    wizard.addStep(new IntroStep(), "welcome");
    wizard.addStep(new ConfigureNetworkStep(wizard, checkExecutionEngine, systemSetupModel), "config-network");
    wizard.addStep(new CheckEnvironmentStep(wizard, systemSetupModel), "environment-check");
    wizard.addStep(new ConfigureManagerHomeStep(wizard, systemSetupModel), "home-setup");
    wizard.addStep(new ReadyToInstallStep(wizard), "install-ready");
    return wizard;
  }

  private CssLayout createHeader() {
    final CssLayout header = new CssLayout();

    final Image otcLogo = new Image();
    otcLogo.setSource(new ThemeResource("img/otc_toplogo32.png"));
    header.addComponent(otcLogo);
    return header;
  }
}
