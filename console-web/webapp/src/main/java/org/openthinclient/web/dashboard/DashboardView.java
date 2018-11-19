package org.openthinclient.web.dashboard;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.server.Resource;
import com.vaadin.server.Responsive;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.*;
import com.vaadin.ui.renderers.ButtonRenderer;
import com.vaadin.ui.themes.ValoTheme;
import org.openthinclient.common.model.UnrecognizedClient;
import org.openthinclient.common.model.service.ApplicationService;
import org.openthinclient.common.model.service.ClientService;
import org.openthinclient.common.model.service.DeviceService;
import org.openthinclient.common.model.service.UnrecognizedClientService;
import org.openthinclient.web.event.DashboardEvent;
import org.openthinclient.web.thinclient.ClientView;
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
  @Autowired
  private UnrecognizedClientService unrecognizedClientService;

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
                                                          String.valueOf(clientService.findAll().size()));
    InfoContentPanel applicationInfo = new InfoContentPanel(mc.getMessage(UI_APPLICATION_HEADER),
                                                          new ThemeResource("icon/packages-white.svg"),
                                                        String.valueOf(applicationService.findAll().size()));
    InfoContentPanel devicesInfo = new InfoContentPanel(mc.getMessage(UI_DEVICE_HEADER),
                                                        new ThemeResource("icon/display-white.svg"),
                                                        String.valueOf(deviceService.findAll().size()));

    dashboardPanels.addComponents(thinclientInfo, applicationInfo, devicesInfo);

    UnregisteredClientsPanel ucp = new UnregisteredClientsPanel("Unregistered " + mc.getMessage(UI_CLIENT_HEADER),
                                                    new ThemeResource("icon/logo-white.svg"));
    dashboardPanels.addComponent(ucp);

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

    public InfoContentPanel(String message, ThemeResource themeResource, String caption) {
      super(message, themeResource);
      setHeight(70, Unit.PIXELS);
      addImageStyleName("dashboard-panel-image-circle");

      if (caption != null) {
        Label label = new Label(caption);
        label.addStyleName("content-panel-number-large");
        addComponent(label);
      }
    }
  }

  class UnregisteredClientsPanel extends ContentPanel {

    public UnregisteredClientsPanel(String title, Resource resource) {
      super(title, resource);
      setSpacing(false);
      setHeight(180, Unit.PIXELS);
      addImageStyleName("dashboard-panel-unregistered-clients-image-circle");

      Grid<UnrecognizedClient> newTCGrid = new Grid<>();
      newTCGrid.addStyleName("dashboard-panel-unregistered-clients-grid");
      newTCGrid.removeHeaderRow(0);
      newTCGrid.setDataProvider(new ListDataProvider<>(unrecognizedClientService.findAll()));
      newTCGrid.addColumn(UnrecognizedClient::getMacAddress);
      newTCGrid.addColumn(uc -> "+",
          new ButtonRenderer(clickEvent -> {
            UI.getCurrent().getNavigator().navigateTo(ClientView.NAME + "/register/" + ((UnrecognizedClient) clickEvent.getItem()).getMacAddress());
          }));
      newTCGrid.setHeightByRows(3);
      newTCGrid.setHeight(80, Unit.PIXELS);
      newTCGrid.setWidth(200, Unit.PIXELS);

      Button btn = new Button("Liste aktualisieren");
      btn.addStyleName("dashboard-panel-unregistered-clients-button");
      btn.setIcon(VaadinIcons.REFRESH);
      btn.addStyleName(ValoTheme.BUTTON_BORDERLESS);
      btn.addClickListener(event -> {
        newTCGrid.setDataProvider(new ListDataProvider<>(unrecognizedClientService.findAll()));
      });

      addComponent(btn);
      addComponent(newTCGrid);
      setExpandRatio(newTCGrid, 1);
    }
  }


}
