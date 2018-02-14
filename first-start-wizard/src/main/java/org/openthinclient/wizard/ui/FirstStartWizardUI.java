package org.openthinclient.wizard.ui;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.server.Page;
import com.vaadin.server.Responsive;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.openthinclient.advisor.check.CheckExecutionEngine;
import org.openthinclient.api.distributions.InstallableDistribution;
import org.openthinclient.api.distributions.InstallableDistributions;
import org.openthinclient.common.model.OrganizationalUnit;
import org.openthinclient.db.DatabaseConfiguration;
import org.openthinclient.db.DatabaseConfiguration.DatabaseType;
import org.openthinclient.i18n.LocaleUtil;
import org.openthinclient.manager.util.http.config.NetworkConfiguration;
import org.openthinclient.manager.util.installation.InstallationDirectoryUtil;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.service.apacheds.DirectoryServiceConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.openthinclient.wizard.FirstStartWizardMessages;
import org.openthinclient.wizard.model.DatabaseModel;
import org.openthinclient.wizard.model.DatabaseModel.MySQLConfiguration;
import org.openthinclient.wizard.model.DirectoryModel;
import org.openthinclient.wizard.model.NetworkConfigurationModel;
import org.openthinclient.wizard.model.SystemSetupModel;
import org.openthinclient.wizard.ui.steps.CheckEnvironmentStep;
import org.openthinclient.wizard.ui.steps.ConfigureDatabaseStep;
import org.openthinclient.wizard.ui.steps.ConfigureDirectoryStep;
import org.openthinclient.wizard.ui.steps.IntroStep;
import org.openthinclient.wizard.ui.steps.ReadyToInstallStep;
import org.openthinclient.wizard.ui.steps.net.ConfigureNetworkStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.vaadin.teemu.wizards.Wizard;
import org.vaadin.teemu.wizards.event.WizardCancelledEvent;
import org.vaadin.teemu.wizards.event.WizardCompletedEvent;
import org.vaadin.teemu.wizards.event.WizardProgressListener;
import org.vaadin.teemu.wizards.event.WizardStepActivationEvent;
import org.vaadin.teemu.wizards.event.WizardStepSetChangedEvent;

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
    } else if (systemSetupModel.isInstallationResume()) {
      resumeInstallProgress(root);
    } else {
      initWizard(root);
    }

    setContent(root);
  }

  private void resumeInstallProgress(VerticalLayout root) {

    root.setWidth(100, Unit.PERCENTAGE);
    root.setHeight(null);

    Panel resumePanel = new Panel();
    resumePanel.setWidth(95, Unit.PERCENTAGE);
    resumePanel.addStyleName("resume-panel");

    final VerticalLayout layout = new VerticalLayout();

    layout.setMargin(true);
    layout.setSpacing(true);
//    layout.setSizeFull();

    final Image logoImage = new Image();
    logoImage.setSource(new ThemeResource("img/OpenThinClient-logo.svg.png"));
    layout.addComponent(logoImage);
    layout.setComponentAlignment(logoImage, Alignment.MIDDLE_CENTER);

    Label title = new Label("Resume");
    title.setStyleName(ValoTheme.LABEL_HUGE);
    layout.addComponent(title);
    Label text = new Label(
        "Wir werden jetzt versuchen die Einstellungen der kaputten Installation wieder herzustellen.</br>Anschließend starten wir die Installation neu.", ContentMode.HTML);
    text.setStyleName(ValoTheme.LABEL_LARGE);
    layout.addComponent(text);

    HorizontalLayout buttonLine = new HorizontalLayout();
    Button startInstallation = new Button("Start Installation");
    startInstallation.setVisible(false);
    startInstallation.addClickListener(e ->  {
      root.removeComponent(resumePanel);
      initWizard(root);
    });
    Button skipResumeButton = new Button("Skip resume");
    skipResumeButton.addClickListener(e -> {
      root.removeComponent(resumePanel);
      initWizard(root);
    });
    Button resumeButton = new Button("Start Resume");
    resumeButton.addClickListener(e -> {
      List<String> list = restoreSavedProperties();
      Label result = new Label("We did our best:<ul>" + list.stream().map(s -> "<li>" + s + "</li>").collect(Collectors.joining()) + "</ul>", ContentMode.HTML);
      result.setStyleName(ValoTheme.LABEL_LARGE);
      layout.addComponent(result, layout.getComponentIndex(text) + 1);
      startInstallation.setVisible(true);
      resumeButton.setVisible(false);
      skipResumeButton.setVisible(false);
    });
    buttonLine.addComponents(skipResumeButton, resumeButton, startInstallation);
    layout.addComponent(buttonLine);

    resumePanel.setContent(layout);
    root.addComponent(resumePanel);
    root.setExpandRatio(resumePanel, 1.0f);
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

  private List<String> restoreSavedProperties() {

    ManagerHomeFactory managerHomeFactory = systemSetupModel.getFactory();
    List<String> resumeResult = new ArrayList<>();

    // check if there is an broken installation and try to restore already set up properties
    if (InstallationDirectoryUtil.existsInstallationProgressFile(managerHomeFactory.getManagerHomeDirectory())) {
      logger.info("Found existing installation file, try to read already setup properties.");

      ManagerHome managerHome = managerHomeFactory.create();

      try {
        PackageManagerConfiguration packageManagerConfiguration = managerHome.getConfiguration(PackageManagerConfiguration.class); //
        NetworkConfigurationModel networkConfigurationModel = systemSetupModel.getNetworkConfigurationModel();
        if (packageManagerConfiguration.getProxyConfiguration() != null) {
          logger.info("Restore previous setup proxy settings.");
          networkConfigurationModel.getProxyConfiguration().setEnabled(true);
          networkConfigurationModel.getProxyConfiguration().setPort(packageManagerConfiguration.getProxyConfiguration().getPort());
          networkConfigurationModel.getProxyConfiguration().setHost(packageManagerConfiguration.getProxyConfiguration().getHost());
          networkConfigurationModel.getProxyConfiguration().setUser(packageManagerConfiguration.getProxyConfiguration().getUser());
          networkConfigurationModel.getProxyConfiguration().setPassword(packageManagerConfiguration.getProxyConfiguration().getPassword());
          resumeResult.add("Proxy configuration restored.");
        } else {
          logger.info("No proxy settings found, using defaults.");
          resumeResult.add("No proxy settings found, using defaults.");
        }
      } catch (Exception e) {
        logger.error("Cannot restore proxy settings", e);
        resumeResult.add("Failed to restore proxy settings, using defaults.");
      }

      try {
        DatabaseConfiguration databaseConfiguration = managerHome.getConfiguration(DatabaseConfiguration.class);
        DatabaseModel databaseModel = systemSetupModel.getDatabaseModel();
        if (databaseConfiguration != null && databaseConfiguration.getType() != null) {
          databaseModel.setType(databaseConfiguration.getType());
          if (databaseConfiguration.getType() == DatabaseType.MYSQL) {
            MySQLConfiguration mySQLConfiguration = databaseModel.getMySQLConfiguration();
            String url = databaseConfiguration.getUrl();
            try {
              URI uri = new URI(url.substring(5));
              mySQLConfiguration.setDatabase(uri.getPath().substring(1));
              mySQLConfiguration.setHostname(uri.getHost());
              mySQLConfiguration.setPort(uri.getPort());
            } catch (URISyntaxException e) {
              logger.error("Cannot parse database uri, using defaults.");
            }
            mySQLConfiguration.setUsername(databaseConfiguration.getUsername());
            mySQLConfiguration.setPassword(databaseConfiguration.getPassword());
          }
          resumeResult.add("Database configuration restored.");
        } else {
          logger.info("No database settings found, using defaults.");
          resumeResult.add("No database settings found, using defaults.");
        }
      } catch (Exception e) {
        logger.error("Cannot restore database settings", e);
        resumeResult.add("Failed to restore database settings, using defaults.");
      }

      try {
        DirectoryServiceConfiguration directoryServiceConfiguration = managerHome.getConfiguration(DirectoryServiceConfiguration.class);
        DirectoryModel directoryModel = systemSetupModel.getDirectoryModel();
        if (directoryServiceConfiguration != null) {
          OrganizationalUnit primaryOU = directoryModel.getPrimaryOU();
          primaryOU.setName(directoryServiceConfiguration.getPrimaryOU());
          resumeResult.add("Directory configuration restored.");
        } else {
          logger.info("No directory settings found, using defaults.");
          resumeResult.add("No directory settings found, using defaults.");
        }
      } catch (Exception e) {
        logger.error("Cannot restore directory settings", e);
        resumeResult.add("Failed to restore directory settings, using defaults.");
      }
    }

    return resumeResult;
  }
}
