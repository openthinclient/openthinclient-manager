package org.openthinclient.web.dashboard;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.navigator.View;
import com.vaadin.server.Responsive;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.DeviceService;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.thinclient.ThinclientView;
import org.openthinclient.web.ui.ManagerSideBarSections;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.sidebar.annotation.SideBarItem;
import org.vaadin.spring.sidebar.annotation.ThemeIcon;

import javax.annotation.PostConstruct;

import static org.openthinclient.web.i18n.ConsoleWebMessages.*;

@SuppressWarnings("serial")
@SpringView(name= DashboardView.NAME)
@SideBarItem(sectionId = ManagerSideBarSections.DASHBOARD, caption = "Dashboard")
@ThemeIcon("icon/meter.svg")
public class DashboardView extends Panel implements View {

  public final static String NAME = "";

  @Autowired
  private ClientService clientService;
  @Autowired
  private ApplicationService applicationService;
  @Autowired
  private DeviceService deviceService;

  final IMessageConveyor mc;
  private CssLayout dashboardPanels;

  @Autowired
  public DashboardView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
    setSizeFull();
    mc = new MessageConveyor(UI.getCurrent().getLocale());
    eventBus.publish(this, new DashboardEvent.UpdateHeaderLabelEvent(mc.getMessage(UI_DASHBOARDVIEW_HEADER)));
  }

  @PostConstruct
  public void init() {
    setContent(buildContent());
  }

  private Component buildContent() {
    dashboardPanels = new CssLayout();
    dashboardPanels.addStyleName("dashboard-panels");
    Responsive.makeResponsive(dashboardPanels);

    InfoContentPanel thinclientInfo = new InfoContentPanel(mc.getMessage(UI_CLIENT_HEADER),
                                                           new ThemeResource("icon/logo-white.svg"),
                                                           clientService.findAll().size());
    InfoContentPanel applicationInfo = new InfoContentPanel(mc.getMessage(UI_APPLICATION_HEADER),
                                                          new ThemeResource("icon/packages-white.svg"),
                                                          applicationService.findAll().size());
    InfoContentPanel devicesInfo = new InfoContentPanel(mc.getMessage(UI_DEVICE_HEADER),
                                                        new ThemeResource("icon/display-white.svg"),
                                                        deviceService.findAll().size());

    dashboardPanels.addComponents(thinclientInfo, applicationInfo, devicesInfo);

      ContentPanel helpPanel = new ContentPanel(mc.getMessage(UI_DASHBOARDVIEW_PANEL_HELP_TITLE), new ThemeResource("icon/help.svg"));
      helpPanel.addComponent(new Label(mc.getMessage(UI_DASHBOARDVIEW_PANEL_HELP_CONTENT), ContentMode.HTML));

      ContentPanel otcPanel = new ContentPanel(mc.getMessage(UI_DASHBOARDVIEW_PANEL_OTC_TITLE), new ThemeResource("icon/logo.svg"));
      otcPanel.addComponent(new Label(mc.getMessage(UI_DASHBOARDVIEW_PANEL_OTC_CONTENT), ContentMode.HTML));

      ContentPanel toolsPanel = new ContentPanel(mc.getMessage(UI_DASHBOARDVIEW_PANEL_TOOLS_TITLE), new ThemeResource("icon/meter.svg"));
      toolsPanel.addComponent(new Label(mc.getMessage(UI_DASHBOARDVIEW_PANEL_TOOLS_CONTENT), ContentMode.HTML));

      dashboardPanels.addComponents(helpPanel, otcPanel, toolsPanel);

      return dashboardPanels;
  }

  class InfoContentPanel extends ContentPanel {

    public InfoContentPanel(String message, ThemeResource themeResource, int size) {
      super(message, themeResource);
      setHeight(70, Unit.PIXELS);
      addImageStyleName("dashboard-panel-image-circle");

      Label label = new Label(String.valueOf(size));
      label.addStyleName("content-panel-number-large");
      addComponent(label);
    }
  }



}
