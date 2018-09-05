package org.openthinclient.web.dashboard;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import com.vaadin.server.Responsive;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import org.openthinclient.web.ui.OtcView;
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
public class DashboardView extends OtcView {

    public final static String NAME = "";

    final IMessageConveyor mc;
//    private final VerticalLayout root;
//    private final EventBus.SessionEventBus eventBus;
//    private final DashboardNotificationService notificationService;
//    private Label titleLabel;
//    private NotificationsButton notificationsButton;
    private CssLayout dashboardPanels;
//    private Window notificationsWindow;

    @Autowired
    public DashboardView(EventBus.SessionEventBus eventBus, DashboardNotificationService notificationService) {
        super(UI_DASHBOARDVIEW_HEADER, eventBus, notificationService);
        mc = new MessageConveyor(UI.getCurrent().getLocale());
    }

    @PostConstruct
    private void init() {
        setPanelContent(buildContent());
    }

    private Component buildContent() {
        dashboardPanels = new CssLayout();
        dashboardPanels.addStyleName("dashboard-panels");
        Responsive.makeResponsive(dashboardPanels);

        ContentPanel helpPanel = new ContentPanel(mc.getMessage(UI_DASHBOARDVIEW_PANEL_HELP_TITLE), new ThemeResource("icon/help.svg"));
        helpPanel.addComponent(new Label(mc.getMessage(UI_DASHBOARDVIEW_PANEL_HELP_CONTENT), ContentMode.HTML));

        ContentPanel otcPanel = new ContentPanel(mc.getMessage(UI_DASHBOARDVIEW_PANEL_OTC_TITLE), new ThemeResource("icon/logo.svg"));
        otcPanel.addComponent(new Label(mc.getMessage(UI_DASHBOARDVIEW_PANEL_OTC_CONTENT), ContentMode.HTML));

        ContentPanel toolsPanel = new ContentPanel(mc.getMessage(UI_DASHBOARDVIEW_PANEL_TOOLS_TITLE), new ThemeResource("icon/meter.svg"));
        toolsPanel.addComponent(new Label(mc.getMessage(UI_DASHBOARDVIEW_PANEL_TOOLS_CONTENT), ContentMode.HTML));

        dashboardPanels.addComponents(helpPanel, otcPanel, toolsPanel);

        return dashboardPanels;
    }





}
