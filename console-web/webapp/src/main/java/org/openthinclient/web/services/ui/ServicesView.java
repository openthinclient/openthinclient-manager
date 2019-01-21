package org.openthinclient.web.services.ui;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SERVICES_CAPTION;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.navigator.View;
import com.vaadin.ui.*;
import org.openthinclient.service.apacheds.DirectoryService;
import org.openthinclient.service.common.ServiceManager;
import org.openthinclient.service.dhcp.DHCPService;
import org.openthinclient.service.nfs.NFSService;
import org.openthinclient.syslogd.SyslogService;
import org.openthinclient.tftp.TFTPService;
import org.openthinclient.web.dashboard.DashboardNotificationService;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;
import org.vaadin.spring.sidebar.annotation.ThemeIcon;

import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Responsive;
import com.vaadin.spring.annotation.SpringView;

import javax.annotation.PostConstruct;

@SpringView(name = "services")
@SideBarItem(sectionId = ManagerSideBarSections.SERVER_MANAGEMENT, captionCode = "UI_SERVICES_CAPTION", order = 3)
@ThemeIcon("icon/eye-white.svg")
public class ServicesView extends Panel implements View {

  private static final long serialVersionUID = 7856636768058411222L;
  private final IMessageConveyor mc;

  @Autowired
  private DhcpServiceConfigurationForm dhcpServiceConfigurationForm;
  private final ServicePanel directoryServicePanel;
  private final ServicePanel tftpServicePanel;
  private final ServicePanel syslogServicePanel;
  private final ServicePanel nfsServicePanel;
  private final ServicePanel dhcpServicePanel;

  @Autowired
  public ServicesView(ServiceManager serviceManager,
                              EventBus.SessionEventBus eventBus,
                              DashboardNotificationService notificationService) {

    directoryServicePanel = new ServicePanel(serviceManager, DirectoryService.class);
    tftpServicePanel = new ServicePanel(serviceManager, TFTPService.class);
    syslogServicePanel = new ServicePanel(serviceManager, SyslogService.class);
    nfsServicePanel = new ServicePanel(serviceManager, NFSService.class);
    dhcpServicePanel = new ServicePanel(serviceManager, DHCPService.class);
    mc = new MessageConveyor(UI.getCurrent().getLocale());

    setSizeFull();
    eventBus.publish(this, new DashboardEvent.UpdateHeaderLabelEvent(mc.getMessage(UI_SERVICES_CAPTION)));
  }

  @PostConstruct
  private void init() {
    setContent(buildContent());
  }

  private Component buildContent() {
    HorizontalLayout servicePanels = new HorizontalLayout();
    servicePanels.setStyleName("services-wrap");
    servicePanels.setSpacing(true);
    servicePanels.setMargin(false);
    servicePanels.addComponent(directoryServicePanel);
    servicePanels.addComponent(tftpServicePanel);
    servicePanels.addComponent(syslogServicePanel);
    servicePanels.addComponent(nfsServicePanel);
    servicePanels.addComponent(dhcpServicePanel);

    VerticalLayout content = new VerticalLayout();
    content.addComponent(servicePanels);
    content.addComponent(dhcpServiceConfigurationForm);
    Responsive.makeResponsive(content);
    return content;
  }

  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {
    directoryServicePanel.refresh();
    tftpServicePanel.refresh();
    syslogServicePanel.refresh();
    nfsServicePanel.refresh();
    dhcpServicePanel.refresh();
    dhcpServiceConfigurationForm.refresh();
  }
}
