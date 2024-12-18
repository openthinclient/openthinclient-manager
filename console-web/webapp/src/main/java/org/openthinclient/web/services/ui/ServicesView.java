package org.openthinclient.web.services.ui;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import org.openthinclient.service.common.ServiceManager;
import org.openthinclient.service.dhcp.DHCPService;
import org.openthinclient.service.nfs.NFSService;
import org.openthinclient.syslogd.SyslogService;
import org.openthinclient.tftp.TFTPService;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.openthinclient.web.ui.SettingsUI;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

import javax.annotation.PostConstruct;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SpringView(name = "services", ui = SettingsUI.class)
@SideBarItem(sectionId = ManagerSideBarSections.SERVER_MANAGEMENT, captionCode = "UI_SERVICES_CAPTION", order = 20)
public class ServicesView extends Panel implements View {

  private static final long serialVersionUID = 7856636768058411222L;
  private final IMessageConveyor mc;

  @Autowired
  private DhcpServiceConfigurationForm dhcpServiceConfigurationForm;
  private final ServicePanel tftpServicePanel;
  private final ServicePanel syslogServicePanel;
  private final ServicePanel nfsServicePanel;
  private final ServicePanel dhcpServicePanel;

  @Autowired
  public ServicesView(ServiceManager serviceManager) {

    mc = new MessageConveyor(UI.getCurrent().getLocale());

    tftpServicePanel = new ServicePanel(serviceManager, TFTPService.class, mc.getMessage(UI_SERVICE_CAPTION_TFTP));
    syslogServicePanel = new ServicePanel(serviceManager, SyslogService.class, mc.getMessage(UI_SERVICE_CAPTION_SYSLOG));
    nfsServicePanel = new ServicePanel(serviceManager, NFSService.class, mc.getMessage(UI_SERVICE_CAPTION_NFS));
    dhcpServicePanel = new ServicePanel(serviceManager, DHCPService.class, mc.getMessage(UI_SERVICE_CAPTION_DHCP));

    setSizeFull();
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
    servicePanels.addComponent(tftpServicePanel);
    servicePanels.addComponent(syslogServicePanel);
    servicePanels.addComponent(nfsServicePanel);
    servicePanels.addComponent(dhcpServicePanel);

    VerticalLayout content = new VerticalLayout();
    content.addComponent(servicePanels);
    content.addComponent(dhcpServiceConfigurationForm);
    content.addComponent(new Label(mc.getMessage(UI_SERVICE_DHCP_CONF_DESCRIPTION), ContentMode.HTML));
    return content;
  }

  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {
    tftpServicePanel.refresh();
    syslogServicePanel.refresh();
    nfsServicePanel.refresh();
    dhcpServicePanel.refresh();
    dhcpServiceConfigurationForm.refresh();
  }
}
