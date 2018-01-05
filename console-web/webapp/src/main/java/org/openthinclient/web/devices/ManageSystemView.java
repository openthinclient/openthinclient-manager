package org.openthinclient.web.devices;

import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SYSTEMMANAGEMENT_CONSOLE_ABOUT_HEADER;
import static org.openthinclient.web.i18n.ConsoleWebMessages.UI_SYSTEMMANAGEMENT_HEADER;

import ch.qos.cal10n.MessageConveyor;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Responsive;
import com.vaadin.server.ThemeResource;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.BrowserFrame;
import com.vaadin.ui.Component;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.openthinclient.web.event.DashboardEventBus;
import org.openthinclient.web.ui.ViewHeader;
import org.openthinclient.web.view.DashboardSections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.vaadin.spring.sidebar.annotation.SideBarItem;

@SpringView(name = "system")
@SideBarItem(sectionId = DashboardSections.DEVICE_MANAGEMENT, captionCode = "UI_SYSTEMMANAGEMENT_HEADER", order = -100)
public class ManageSystemView extends Panel implements View {

  /** serialVersionUID */
  private static final long serialVersionUID = -8836200902351197949L;

  private static final Logger LOGGER = LoggerFactory.getLogger(ManageSystemView.class);

  @Value("${novnc.server.host}")
  private String novncServerHost;
  @Value("${novnc.server.port}")
  private String novncServerPort;
  @Value("${novnc.server.encrypt}")
  private String novncServerEncrypt;

  final MessageConveyor mc;
  final VerticalLayout root ;

  public ManageSystemView() {

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

     root.addComponent(new ViewHeader(mc.getMessage(UI_SYSTEMMANAGEMENT_CONSOLE_ABOUT_HEADER)));
  }

  @Override
  public String getCaption() {
     return mc.getMessage(UI_SYSTEMMANAGEMENT_HEADER);
  }

  @PostConstruct
  private void init() {
     Component content = buildContent();
     root.addComponent(content);
     root.setExpandRatio(content, 1);
  }
  
  private Component buildContent() {

    String host;
    try {
      host = StringUtils.isNotBlank(novncServerHost) ? novncServerHost.trim() : InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      LOGGER.error("Cannot obtain host-address for localhost to set noVNC-server address, using 'localhost' as value.", e);
      host = "localhost";
    }
    String port = StringUtils.isNotBlank(novncServerPort) ? novncServerPort.trim() : "5900";
    String encrypt = StringUtils.isNotBlank(novncServerEncrypt) ? novncServerEncrypt.trim() : "0";

    ThemeResource tr = new ThemeResource("novnc/vnc.html?host=" + host  + "&port=" + port + "&encrypt=" + encrypt);
    BrowserFrame browser = new BrowserFrame(null, tr);
    browser.setWidth("1100px");
    browser.setHeight("780px");

    return browser;
  }
  
  @Override
  public void enter(ViewChangeListener.ViewChangeEvent event) {

  }
}
