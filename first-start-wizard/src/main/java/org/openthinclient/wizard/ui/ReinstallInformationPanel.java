package org.openthinclient.wizard.ui;

import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_RESUME_BUTTON_INSTALLATION;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_RESUME_BUTTON_SKIP;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_RESUME_BUTTON_START;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_RESUME_RESULT;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_RESUME_RESULT_DB_DEFAULT;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_RESUME_RESULT_DB_FAIL;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_RESUME_RESULT_DB_RESTORED;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_RESUME_RESULT_DIR_FAIL;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_RESUME_RESULT_DIR_RESTORED;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_RESUME_RESULT_PROXY_DEFAULT;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_RESUME_RESULT_PROXY_FAIL;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_RESUME_RESULT_PROXY_RESTORED;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_RESUME_TEXT;
import static org.openthinclient.wizard.FirstStartWizardMessages.UI_FIRSTSTART_RESUME_TITLE;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.openthinclient.common.model.OrganizationalUnit;
import org.openthinclient.db.DatabaseConfiguration;
import org.openthinclient.db.DatabaseConfiguration.DatabaseType;
import org.openthinclient.manager.util.installation.InstallationDirectoryUtil;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.service.apacheds.DirectoryServiceConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.service.common.home.impl.ManagerHomeFactory;
import org.openthinclient.wizard.model.DatabaseModel;
import org.openthinclient.wizard.model.DatabaseModel.MySQLConfiguration;
import org.openthinclient.wizard.model.DirectoryModel;
import org.openthinclient.wizard.model.NetworkConfigurationModel;
import org.openthinclient.wizard.model.SystemSetupModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ReinstallInformationPanel
 */
public class ReinstallInformationPanel extends Panel {

  Logger logger = LoggerFactory.getLogger(getClass());

  private SystemSetupModel systemSetupModel;
  private IMessageConveyor mc;

  public ReinstallInformationPanel(VerticalLayout root, SystemSetupModel systemSetupModel) {

    this.systemSetupModel = systemSetupModel;

    mc = new MessageConveyor(UI.getCurrent().getLocale());

    setWidth(95, Unit.PERCENTAGE);
    addStyleName("resume-panel");

    final VerticalLayout layout = new VerticalLayout();
    layout.setMargin(true);
    layout.setSpacing(true);

    final Image logoImage = new Image();
    logoImage.setSource(new ThemeResource("img/OpenThinClient-logo.svg.png"));
    layout.addComponent(logoImage);
    layout.setComponentAlignment(logoImage, Alignment.MIDDLE_CENTER);

    Label title = new Label(mc.getMessage(UI_FIRSTSTART_RESUME_TITLE));
    title.setStyleName(ValoTheme.LABEL_HUGE);
    layout.addComponent(title);
    Label text = new Label(mc.getMessage(UI_FIRSTSTART_RESUME_TEXT), ContentMode.HTML);
    text.setStyleName(ValoTheme.LABEL_LARGE);
    layout.addComponent(text);

    HorizontalLayout buttonLine = new HorizontalLayout();
    Button startInstallation = new Button(mc.getMessage(UI_FIRSTSTART_RESUME_BUTTON_INSTALLATION));
    startInstallation.setVisible(false);
    startInstallation.addClickListener(e ->  {
      root.removeComponent(this);
      exitPanel();
    });
    Button skipResumeButton = new Button(mc.getMessage(UI_FIRSTSTART_RESUME_BUTTON_SKIP));
    skipResumeButton.addClickListener(e -> {
      root.removeComponent(this);
      exitPanel();
    });
    Button resumeButton = new Button(mc.getMessage(UI_FIRSTSTART_RESUME_BUTTON_START));
    resumeButton.addClickListener(e -> {
      List<String> list = restoreSavedProperties();
      Label result = new Label(mc.getMessage(UI_FIRSTSTART_RESUME_RESULT) + "<ul>" + list.stream().map(s -> "<li>" + s + "</li>").collect(
          Collectors.joining()) + "</ul>", ContentMode.HTML);
      result.setStyleName(ValoTheme.LABEL_LARGE);
      layout.addComponent(result, layout.getComponentIndex(text) + 1);
      startInstallation.setVisible(true);
      resumeButton.setVisible(false);
      skipResumeButton.setVisible(false);
    });
    buttonLine.addComponents(skipResumeButton, resumeButton, startInstallation);
    layout.addComponent(buttonLine);

    setContent(layout);
  }

  public void exitPanel() { }

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
          resumeResult.add(mc.getMessage(UI_FIRSTSTART_RESUME_RESULT_PROXY_RESTORED));
        } else {
          logger.info("No proxy settings found, using defaults.");
          resumeResult.add(mc.getMessage(UI_FIRSTSTART_RESUME_RESULT_PROXY_DEFAULT));
        }
      } catch (Exception e) {
        logger.error("Cannot restore proxy settings", e);
        resumeResult.add(mc.getMessage(UI_FIRSTSTART_RESUME_RESULT_PROXY_FAIL));
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
          resumeResult.add(mc.getMessage(UI_FIRSTSTART_RESUME_RESULT_DB_RESTORED));
        } else {
          logger.info("No database settings found, using defaults.");
          resumeResult.add(mc.getMessage(UI_FIRSTSTART_RESUME_RESULT_DB_DEFAULT));
        }
      } catch (Exception e) {
        logger.error("Cannot restore database settings", e);
        resumeResult.add(mc.getMessage(UI_FIRSTSTART_RESUME_RESULT_DB_FAIL));
      }

      try {
        DirectoryServiceConfiguration directoryServiceConfiguration = managerHome.getConfiguration(DirectoryServiceConfiguration.class);
        DirectoryModel directoryModel = systemSetupModel.getDirectoryModel();
        if (directoryServiceConfiguration != null) {
          OrganizationalUnit primaryOU = directoryModel.getPrimaryOU();
          primaryOU.setName(directoryServiceConfiguration.getPrimaryOU());
          resumeResult.add(mc.getMessage(UI_FIRSTSTART_RESUME_RESULT_DIR_RESTORED));
        } else {
          logger.info("No directory settings found, using defaults.");
          resumeResult.add(mc.getMessage(UI_FIRSTSTART_RESUME_RESULT_DB_DEFAULT));
        }
      } catch (Exception e) {
        logger.error("Cannot restore directory settings", e);
        resumeResult.add(mc.getMessage(UI_FIRSTSTART_RESUME_RESULT_DIR_FAIL));
      }
    }

    return resumeResult;
  }
}
