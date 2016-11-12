package org.openthinclient.web.services.ui;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Responsive;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.HorizontalLayout;

import org.openthinclient.service.apacheds.DirectoryService;
import org.openthinclient.service.common.ServiceManager;
import org.openthinclient.service.dhcp.DhcpService;
import org.openthinclient.service.nfs.NFSService;
import org.openthinclient.syslogd.SyslogService;
import org.openthinclient.tftp.TFTPService;
import org.openthinclient.web.view.DashboardSections;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

@SpringView(name = "services-overview")
@SideBarItem(sectionId = DashboardSections.SERVICE_MANAGEMENT, captionCode = "UI_SERVICESOVERVIEW_CAPTION")
public class ServicesOverviewView extends HorizontalLayout implements View {

  private final ServiceOverviewPanel directoryServiceOverviewPanel;
  private final ServiceOverviewPanel tftpServiceOverviewPanel;
  private final ServiceOverviewPanel syslogServiceOverviewPanel;
  private final ServiceOverviewPanel nfsServiceOverviewPanel;
  private final ServiceOverviewPanel dhcpServiceOverviewPanel;

  @Autowired
  public ServicesOverviewView(ServiceManager serviceManager) {
    Responsive.makeResponsive(this);
    setWidth(100, Unit.PERCENTAGE);
    directoryServiceOverviewPanel = new ServiceOverviewPanel(serviceManager, DirectoryService.class);
    tftpServiceOverviewPanel = new ServiceOverviewPanel(serviceManager, TFTPService.class);
    syslogServiceOverviewPanel = new ServiceOverviewPanel(serviceManager, SyslogService.class);
    nfsServiceOverviewPanel = new ServiceOverviewPanel(serviceManager, NFSService.class);
    dhcpServiceOverviewPanel = new ServiceOverviewPanel(serviceManager, DhcpService.class);

    setSpacing(true);
    setMargin(true);

    addComponent(directoryServiceOverviewPanel);
    addComponent(tftpServiceOverviewPanel);
    addComponent(syslogServiceOverviewPanel);
    addComponent(nfsServiceOverviewPanel);
    addComponent(dhcpServiceOverviewPanel);

    setExpandRatio(directoryServiceOverviewPanel, 1);
    setExpandRatio(tftpServiceOverviewPanel, 1);
    setExpandRatio(syslogServiceOverviewPanel, 1);
    setExpandRatio(nfsServiceOverviewPanel, 1);
    setExpandRatio(dhcpServiceOverviewPanel, 1);
  }


  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {
    directoryServiceOverviewPanel.refresh();
    tftpServiceOverviewPanel.refresh();
    syslogServiceOverviewPanel.refresh();
    nfsServiceOverviewPanel.refresh();
    dhcpServiceOverviewPanel.refresh();
  }
}
