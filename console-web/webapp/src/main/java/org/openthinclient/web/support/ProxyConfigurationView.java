package org.openthinclient.web.support;

import ch.qos.cal10n.MessageConveyor;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Responsive;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.manager.util.http.config.NetworkConfiguration.ProxyConfiguration;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;

import javax.annotation.PostConstruct;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_PROXY_CONFIGURATION_HEADER;

@SpringView(name = "proxy-config")
// @SideBarItem(sectionId = ManagerSideBarSections.SERVER_MANAGEMENT, captionCode = "UI_SUPPORT_PROXY_CONFIGURATION_HEADER", order = 10)
public class ProxyConfigurationView extends Panel implements View {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigurationView.class);

  @Autowired
  private ManagerHome managerHome;
  @Autowired
  private DownloadManager downloadManager;

  final MessageConveyor mc;
  final VerticalLayout root ;

  public ProxyConfigurationView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {

    mc = new MessageConveyor(UI.getCurrent().getLocale());
    setSizeFull();
    eventBus.publish(this, new DashboardEvent.UpdateHeaderLabelEvent(mc.getMessage(UI_SUPPORT_PROXY_CONFIGURATION_HEADER)));

    root = new VerticalLayout();
    root.setSizeFull();
    root.setMargin(true);
    setContent(root);
    Responsive.makeResponsive(root);

  }

  @Override
  public String getCaption() {
     return mc.getMessage(UI_SUPPORT_PROXY_CONFIGURATION_HEADER);
  }

  @PostConstruct
  private void init() {

      buildContent();
  }
  
  private void buildContent() {

    VerticalLayout content = new VerticalLayout();

    final Label successLabel = new Label(mc.getMessage(ConsoleWebMessages.UI_SUPPORT_PROXY_CONFIGURATION_SUCCESS), ContentMode.HTML);
    successLabel.setVisible(false);
    final Label errorLabel = new Label(mc.getMessage(ConsoleWebMessages.UI_SUPPORT_PROXY_CONFIGURATION_ERROR), ContentMode.HTML);
    errorLabel.setVisible(false);
    errorLabel.setStyleName("unexpected_error");

    final Label labelDescription = new Label(mc.getMessage(ConsoleWebMessages.UI_SUPPORT_PROXY_CONFIGURATION_DESCRIPTION), ContentMode.HTML);
    content.addComponent(labelDescription);

    PackageManagerConfiguration configuration = managerHome.getConfiguration(PackageManagerConfiguration.class);
    ProxyConfiguration proxyConfiguration = configuration.getProxyConfiguration();
    if (proxyConfiguration == null) {
      proxyConfiguration = new ProxyConfiguration();
      configuration.setProxyConfiguration(proxyConfiguration);
    }

    ProxyConfiguration finalProxyConfiguration = proxyConfiguration;
    ProxyConfigurationForm proxyConfigurationForm = new ProxyConfigurationForm(proxyConfiguration) {
      @Override
      public void saveValues() {
        successLabel.setVisible(false);
        errorLabel.setVisible(false);

        commit();
        cleanupValues();
        try {
          managerHome.save(PackageManagerConfiguration.class);
          downloadManager.setProxy(finalProxyConfiguration);
          successLabel.setVisible(true);
          super.resetValues();
        } catch (Exception e) {
          LOGGER.error("Failed to save proxy-settings", e);
          errorLabel.setVisible(true);
        }
      }

      @Override
      public void resetValues() {
        super.resetValues();
        successLabel.setVisible(false);
        errorLabel.setVisible(false);
      }
    };
    content.addComponent(proxyConfigurationForm);
    content.addComponents(successLabel, errorLabel);

    root.addComponent(content);
    root.setExpandRatio(content, 1);
  }

  /**
   * Do not store any values if proxy is disabled
   */
  public void cleanupValues() {
    PackageManagerConfiguration configuration = managerHome.getConfiguration(PackageManagerConfiguration.class);
    ProxyConfiguration proxyConfiguration = configuration.getProxyConfiguration();
    if (!proxyConfiguration.isEnabled()) {
      proxyConfiguration.setHost(null);
      proxyConfiguration.setPort(0);
      proxyConfiguration.setUser(null);
      proxyConfiguration.setPassword(null);
    }
  }

  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {

  }

}
