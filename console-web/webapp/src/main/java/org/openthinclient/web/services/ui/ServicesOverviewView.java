package org.openthinclient.web.services.ui;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SERVICESOVERVIEW_CAPTION;

import com.vaadin.ui.*;
import org.openthinclient.service.apacheds.DirectoryService;
import org.openthinclient.service.common.ServiceManager;
import org.openthinclient.service.dhcp.DhcpService;
import org.openthinclient.service.nfs.NFSService;
import org.openthinclient.syslogd.SyslogService;
import org.openthinclient.tftp.TFTPService;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.ui.OtcView;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;
import org.vaadin.spring.sidebar.annotation.ThemeIcon;

import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Responsive;
import com.vaadin.spring.annotation.SpringView;

import javax.annotation.PostConstruct;

@SpringView(name = "services-overview")
@SideBarItem(sectionId = ManagerSideBarSections.SERVICE_MANAGEMENT, captionCode = "UI_SERVICESOVERVIEW_CAPTION", order = 1)
@ThemeIcon("icon/eye.svg")
public class ServicesOverviewView extends OtcView {

  private static final long serialVersionUID = 7856636768058411222L;

  private final ServiceOverviewPanel directoryServiceOverviewPanel;
  private final ServiceOverviewPanel tftpServiceOverviewPanel;
  private final ServiceOverviewPanel syslogServiceOverviewPanel;
  private final ServiceOverviewPanel nfsServiceOverviewPanel;
  private final ServiceOverviewPanel dhcpServiceOverviewPanel;

  @Autowired
  public ServicesOverviewView(ServiceManager serviceManager,
                              EventBus.SessionEventBus eventBus,
                              DashboardNotificationService notificationService) {
    super(UI_SERVICESOVERVIEW_CAPTION, eventBus, notificationService);

    directoryServiceOverviewPanel = new ServiceOverviewPanel(serviceManager, DirectoryService.class);
    tftpServiceOverviewPanel = new ServiceOverviewPanel(serviceManager, TFTPService.class);
    syslogServiceOverviewPanel = new ServiceOverviewPanel(serviceManager, SyslogService.class);
    nfsServiceOverviewPanel = new ServiceOverviewPanel(serviceManager, NFSService.class);
    dhcpServiceOverviewPanel = new ServiceOverviewPanel(serviceManager, DhcpService.class);
  }

  @PostConstruct
  private void init() {
    setPanelContent(buildContent());
  }

  private Component buildContent() {
    HorizontalLayout content = new HorizontalLayout();
    content.setStyleName("services-wrap");

    content.setSpacing(true);
    content.setMargin(false);

    content.addComponent(directoryServiceOverviewPanel);
    content.addComponent(tftpServiceOverviewPanel);
    content.addComponent(syslogServiceOverviewPanel);
    content.addComponent(nfsServiceOverviewPanel);
    content.addComponent(dhcpServiceOverviewPanel);

    Responsive.makeResponsive(content);
    return content;
  }

  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {
    super.enter(event);
    directoryServiceOverviewPanel.refresh();
    tftpServiceOverviewPanel.refresh();
    syslogServiceOverviewPanel.refresh();
    nfsServiceOverviewPanel.refresh();
    dhcpServiceOverviewPanel.refresh();
  }
}
