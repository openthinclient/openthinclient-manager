package org.openthinclient.web.services.ui;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SERVICESOVERVIEW_CAPTION;

import com.vaadin.ui.*;
import org.openthinclient.service.apacheds.DirectoryService;
import org.openthinclient.service.common.ServiceManager;
import org.openthinclient.service.dhcp.DhcpService;
import org.openthinclient.service.nfs.NFSService;
import org.openthinclient.syslogd.SyslogService;
import org.openthinclient.tftp.TFTPService;
import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.ui.Sparklines;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Responsive;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.themes.ValoTheme;

import ch.qos.cal10n.MessageConveyor;

@SpringView(name = "services-overview")
@SideBarItem(sectionId = DashboardSections.SERVICE_MANAGEMENT, captionCode = "UI_SERVICESOVERVIEW_CAPTION", order = 1)
public class ServicesOverviewView extends Panel implements View {

  /** serialVersionUID */
  private static final long serialVersionUID = 7856636768058411222L;

  private final ServiceOverviewPanel directoryServiceOverviewPanel;
  private final ServiceOverviewPanel tftpServiceOverviewPanel;
  private final ServiceOverviewPanel syslogServiceOverviewPanel;
  private final ServiceOverviewPanel nfsServiceOverviewPanel;
  private final ServiceOverviewPanel dhcpServiceOverviewPanel;

  @Autowired
  public ServicesOverviewView(ServiceManager serviceManager) {
     
     MessageConveyor mc = new MessageConveyor(UI.getCurrent().getLocale());
     
     addStyleName(ValoTheme.PANEL_BORDERLESS);
     setSizeFull();
     DashboardEventBus.register(this);

     VerticalLayout root = new VerticalLayout();
     root.setSizeFull();
     root.addStyleName("dashboard-view");
     setContent(root);
     Responsive.makeResponsive(root);

     root.addComponent(new ViewHeader(mc.getMessage(UI_SERVICESOVERVIEW_CAPTION)));

     HorizontalLayout content = new HorizontalLayout();
     content.setStyleName("services-wrap");
     root.addComponent(content);
     root.setExpandRatio(content, 1);

    directoryServiceOverviewPanel = new ServiceOverviewPanel(serviceManager, DirectoryService.class);
    tftpServiceOverviewPanel = new ServiceOverviewPanel(serviceManager, TFTPService.class);
    syslogServiceOverviewPanel = new ServiceOverviewPanel(serviceManager, SyslogService.class);
    nfsServiceOverviewPanel = new ServiceOverviewPanel(serviceManager, NFSService.class);
    dhcpServiceOverviewPanel = new ServiceOverviewPanel(serviceManager, DhcpService.class);

    content.setSpacing(true);
    content.setMargin(false);

    content.addComponent(directoryServiceOverviewPanel);
    content.addComponent(tftpServiceOverviewPanel);
    content.addComponent(syslogServiceOverviewPanel);
    content.addComponent(nfsServiceOverviewPanel);
    content.addComponent(dhcpServiceOverviewPanel);

    Responsive.makeResponsive(content);
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
