package org.openthinclient.wizard.ui;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.server.Page;
import com.vaadin.server.Responsive;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import org.openthinclient.advisor.check.CheckExecutionEngine;
import org.openthinclient.api.distributions.InstallableDistribution;
import org.openthinclient.api.distributions.InstallableDistributions;
import org.openthinclient.i18n.LocaleUtil;
import org.openthinclient.manager.util.http.config.NetworkConfiguration;
import org.openthinclient.wizard.FirstStartWizardMessages;
import org.openthinclient.wizard.model.SystemSetupModel;
import org.openthinclient.wizard.ui.steps.*;
import org.openthinclient.wizard.ui.steps.net.ConfigureNetworkStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.event.*;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;

@Theme("otc-wizard")
@SpringUI(path="/first-start")
@Push
public class FirstStartWizardUI extends UI {

  /** serialVersionUID   */
  private static final long serialVersionUID = 1127863296116812758L;

  Logger logger = LoggerFactory.getLogger(getClass());
  
  @Autowired
  private SystemSetupModel systemSetupModel;
  @Autowired
  private CheckExecutionEngine checkExecutionEngine;
  @Autowired
  private ApplicationEventPublisher publisher;

  @Override
  protected void init(VaadinRequest request) {
    
    setLocale(LocaleUtil.getLocaleForMessages(FirstStartWizardMessages.class, UI.getCurrent().getLocale()));

    IMessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
    Page.getCurrent().setTitle(mc.getMessage(FirstStartWizardMessages.UI_PAGE_TITLE));
    
    Responsive.makeResponsive(this);
    
    // create the root layout and add the wizard
    final VerticalLayout root = new VerticalLayout();
    root.setMargin(false);

    root.addComponent(createHeader());

    if (systemSetupModel.getInstallModel().isInstallInProgress()) {
      initInstallProgress(root);
    } else {
      initWizard(root);
    }

    setContent(root);


  }

  private void initInstallProgress(VerticalLayout root) {

    root.setWidth(100, Unit.PERCENTAGE);
    root.setHeight(null);

    final SystemInstallProgressView progressView = new SystemInstallProgressView();

    final VerticalLayout viewWrapper = new VerticalLayout();
    viewWrapper.setMargin(true);
    viewWrapper.setSpacing(true);
    viewWrapper.setSizeFull();
    viewWrapper.addComponent(progressView);

    root.addComponent(viewWrapper);
    root.setExpandRatio(viewWrapper, 1f);

    final SystemInstallProgressPresenter presenter = new SystemInstallProgressPresenter(publisher, systemSetupModel.getInstallModel());
    presenter.present(getUI(), progressView);
  }

  private void initWizard(final VerticalLayout root) {
    root.setSizeFull();

    final Wizard wizard = createWizard();

    final VerticalLayout wizardWrapper = new VerticalLayout();
    wizardWrapper.setMargin(false);
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

        // get official distribution or fallback
        InstallableDistribution installableDistribution = systemSetupModel.getInstallModel().getInstallableDistributions().get(0);
        try {
            URL officialURL = InstallableDistributions.OFFICIAL_DISTRIBUTIONS_XML.toURL();
            InstallableDistributions officialDistribution;
            if (systemSetupModel.getNetworkConfigurationModel().getDirectConnectionProperty().booleanValue()) {
              officialDistribution = InstallableDistributions.load(officialURL);
            } else {
              NetworkConfiguration.ProxyConfiguration proxyConf = systemSetupModel.getNetworkConfigurationModel().getProxyConfiguration();
              SocketAddress addr = new InetSocketAddress(proxyConf.getHost(), proxyConf.getPort());
              officialDistribution = InstallableDistributions.load(officialURL, new Proxy(Proxy.Type.HTTP, addr));
            }
            installableDistribution = officialDistribution.getPreferred();
            logger.info("Using official distribution: " + officialURL);
          } catch (Exception e) {
            logger.warn("Cannot load preferred official distribution: " + InstallableDistributions.OFFICIAL_DISTRIBUTIONS_XML +
                        ", falling back to " + installableDistribution);
          }

        systemSetupModel.getInstallModel().installSystem(systemSetupModel.getFactory(), installableDistribution);

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
//    wizard.addStep(new ConfigureManagerHomeStep(wizard, systemSetupModel), "home-setup");
    wizard.addStep(new CheckEnvironmentStep(wizard, systemSetupModel), "environment-check");
    wizard.addStep(new ConfigureDatabaseStep(systemSetupModel), "config-database");
    wizard.addStep(new ConfigureDirectoryStep(wizard, systemSetupModel), "directory");
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
