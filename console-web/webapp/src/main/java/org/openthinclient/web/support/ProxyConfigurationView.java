package org.openthinclient.web.support;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_APPLICATION_HEADER;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_APPLICATION_UPDATE_EXIT;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_APPLICATION_UPDATE_RUNNING;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_APPLICATION_UPDATE_SUCCESS;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_CHECK_APPLICATION_VERSION_NOTIFICATION_CAPTION;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_CHECK_APPLICATION_VERSION_NOTIFICATION_FAIL;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_CHECK_APPLICATION_VERSION_NOTIFICATION_OK;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_CHECK_APPLICATION_VERSION_NOTIFICATION_UPDATE;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_CONSOLE_ABOUT_HEADER;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SUPPORT_PROXY_CONFIGURATION_HEADER;

import ch.qos.cal10n.MessageConveyor;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Responsive;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import java.net.URI;
import javax.annotation.PostConstruct;
import org.openthinclient.api.proc.RuntimeProcessExecutor;
import org.openthinclient.api.versioncheck.AvailableVersionChecker;
import org.openthinclient.api.versioncheck.UpdateDescriptor;
import org.openthinclient.manager.util.http.DownloadManager;
import org.openthinclient.manager.util.http.config.NetworkConfiguration;
import org.openthinclient.manager.util.http.config.NetworkConfiguration.ProxyConfiguration;
import org.openthinclient.pkgmgr.PackageManagerConfiguration;
import org.openthinclient.pkgmgr.db.Version;
import org.openthinclient.progress.NoopProgressReceiver;
import org.openthinclient.service.common.home.Configuration;
import org.openthinclient.service.common.home.ManagerHome;
import org.openthinclient.web.component.NotificationDialog;
import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.i18n.ConsoleWebMessages;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.vaadin.spring.sidebar.annotation.SideBarItem;
import org.vaadin.viritin.button.MButton;

@SpringView(name = "proxy-config")
@SideBarItem(sectionId = DashboardSections.SUPPORT, captionCode = "UI_SUPPORT_PROXY_CONFIGURATION_HEADER", order = 10)
public class ProxyConfigurationView extends Panel implements View {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigurationView.class);

  @Autowired
  private ManagerHome managerHome;
  @Autowired
  private DownloadManager downloadManager;

  final MessageConveyor mc;
  final VerticalLayout root ;

  public ProxyConfigurationView() {

     mc = new MessageConveyor(UI.getCurrent().getLocale());
     
     addStyleName(ValoTheme.PANEL_BORDERLESS);
     setSizeFull();
     DashboardEventBus.register(this);

     root = new VerticalLayout();
     root.setSizeFull();
     root.setMargin(true);
     root.addStyleName("dashboard-view");
     setContent(root);
     Responsive.makeResponsive(root);

     root.addComponent(new ViewHeader(mc.getMessage(UI_SUPPORT_PROXY_CONFIGURATION_HEADER)));
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
        cleanupValues();
        commit();
        managerHome.save(PackageManagerConfiguration.class);
        downloadManager.setProxy(finalProxyConfiguration);
      }
    };
    content.addComponent(proxyConfigurationForm);

    root.addComponent(content);
    root.setExpandRatio(content, 1);

  }

  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {

  }

}
